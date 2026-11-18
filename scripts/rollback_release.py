"""恢复已留存的应用镜像；不恢复数据库，不覆盖新增业务数据。"""
import argparse
import json
from pathlib import Path
import re
import subprocess
from urllib.request import urlopen

from delivery_backup import ROOT, compose, digest, mysql
from release_images import load_bundle
from release_preflight import read_env, validate


def expected_migrations(commit):
    if not re.fullmatch(r'[a-f0-9]{40}', commit):
        raise ValueError('回滚记录中的提交格式不正确')
    content = subprocess.check_output(['git', 'show', commit +
        ':shop-backend/shop-server/src/main/resources/delivery-migrations.properties'], cwd=ROOT).decode('utf-8')
    entries = {}
    for line in content.splitlines():
        if line.strip() and not line.startswith('#'):
            key, value = line.split('=', 1)
            entries[key.strip()] = value.strip()
    return entries


def validate_schema(expected, actual):
    # 只自动允许同一迁移集；跨结构版本必须先单独审查兼容性。
    if not expected or expected != actual:
        raise ValueError('数据库迁移集与目标源码不一致，停止自动回滚；请先审查结构兼容性，禁止恢复旧库覆盖新订单')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env', type=Path, required=True)
    parser.add_argument('--release', type=Path, required=True, help='目标版本的已部署发布记录')
    parser.add_argument('--bundle', type=Path, required=True, help='该记录对应的已验证镜像包')
    args = parser.parse_args()
    record = json.loads(args.release.read_text(encoding='utf-8'))
    if record.get('status') != 'deployed-awaiting-business-acceptance':
        raise ValueError('不是受支持的已部署发布记录')
    if record.get('config_sha256') != digest(args.env):
        raise ValueError('环境配置与目标部署不同或旧记录未保留配置校验值，请先审查配置兼容性')
    if record.get('bundle_sha256') != digest(args.bundle / 'images.tar'):
        raise ValueError('镜像包与目标发布记录不匹配')
    values = read_env(args.env)
    errors = validate(values)
    if errors:
        raise ValueError('配置预检失败：' + '; '.join(errors))
    history = mysql(args, 'SELECT version,checksum FROM schema_migration_history ORDER BY version', values['MYSQL_DATABASE']).decode()
    actual = dict(line.split('\t') for line in history.splitlines() if line)
    validate_schema(expected_migrations(record['commit']), actual)
    args.images_override = load_bundle(args.bundle, expected_commit=record['commit'])
    # 保持数据库、Redis和所有数据卷不变；先停入口，失败保留维护状态。
    compose(args, 'stop', 'nginx', 'admin', 'backend')
    compose(args, 'up', '-d', '--no-build', '--no-deps', '--wait', '--wait-timeout', '240', 'backend', 'admin')
    compose(args, 'up', '-d', '--no-build', '--no-deps', '--wait', '--wait-timeout', '120', 'nginx')
    try:
        with urlopen(values['APP_EXTERNAL_BASE_URL'].rstrip('/') + '/readyz', timeout=10) as response:
            if response.status != 200 or json.load(response).get('status') != 'UP':
                raise ValueError('目标应用未就绪')
    except Exception:
        compose(args, 'stop', 'nginx')
        raise
    print('应用镜像回滚及就绪检查通过；数据库和素材未恢复或清理，请继续业务核对。')


if __name__ == '__main__':
    from delivery_lock import operation_lock
    with operation_lock():
        main()
