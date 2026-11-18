"""在客户 Linux 主机部署本目录的已审核版本，任何阶段失败即停止。"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
from urllib.request import urlopen
from delivery_backup import ROOT, backup, compose, digest, verify_archive
from release_preflight import read_env, validate


def checked_python(script, *arguments, env=None):
    import sys
    subprocess.run([sys.executable, str(ROOT / 'scripts' / script), *map(str, arguments)],
                   check=True, env=env)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['initialize', 'upgrade'])
    parser.add_argument('--env', required=True, type=Path)
    parser.add_argument('--backup', type=Path, help='升级前成套备份文件，必须为新的路径')
    parser.add_argument('--offsite-dir', type=Path, help='升级前加密备份的独立存储挂载根目录，须事先核实独立故障域')
    parser.add_argument('--bundle', required=True, type=Path, help='已经封存并在隔离环境验收的精确镜像包')
    args = parser.parse_args()
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT).strip():
        raise ValueError('源码包含未提交修改，请先完成审查并固定版本')
    from release_images import load_bundle
    args.images_override = load_bundle(args.bundle)
    values = read_env(args.env)
    errors = validate(values)
    if errors:
        raise ValueError('预检失败：' + '; '.join(errors))
    for name in ['fullchain.pem', 'privkey.pem']:
        if not (ROOT / 'deploy/secrets/tls' / name).is_file():
            raise ValueError('缺少 HTTPS 证书文件：' + name)
    for name in ['material', 'mysql', 'redis']:
        (ROOT / 'deploy/data' / name).mkdir(parents=True, exist_ok=True)
    # 在 Docker 创建 bind mount 前由部署用户建立父目录，避免备份目录归属 root。
    (ROOT / 'deploy/backups/mysql').mkdir(parents=True, exist_ok=True)
    if not os.access(ROOT / 'deploy/backups', os.W_OK):
        raise ValueError('备份目录不可写，请先由运维人员核对目录属主和部署用户权限')
    database = values['MYSQL_DATABASE']
    bootstrap_env = os.environ.copy()
    bootstrap_env['DELIVERY_ADMIN_PASSWORD'] = values.get('DELIVERY_ADMIN_PASSWORD', '')
    if args.action == 'upgrade':
        if args.backup is None:
            raise ValueError('升级必须指定 --backup，不允许跳过备份')
        if args.offsite_dir is None or not os.path.ismount(args.offsite_dir):
            raise ValueError('升级必须指定已核实独立故障域的 --offsite-dir 挂载根目录')
        if len(values.get('DELIVERY_BACKUP_PASSWORD', '')) < 20:
            raise ValueError('升级前必须配置独立备份密码，至少 20 字符')
        import tempfile
        import uuid
        from backup_crypto import transform
        args.database = database
        args.archive = args.backup
        # 从备份到迁移与验证保持同一停写窗口；失败时保留停写供运维处理。
        args.keep_stopped = True
        backup(args)
        with tempfile.TemporaryDirectory() as temporary:
            verify_archive(args.backup, Path(temporary) / 'verify')
            remote = args.offsite_dir / ('upgrade-' + uuid.uuid4().hex + '.enc')
            transform(args.backup, remote, values['DELIVERY_BACKUP_PASSWORD'])
            restored = Path(temporary) / 'verified.zip'
            transform(remote, restored, values['DELIVERY_BACKUP_PASSWORD'], decrypt=True)
            if digest(restored) != digest(args.backup):
                raise ValueError('独立备份读回校验失败，禁止迁移，后端保持停止')
    else:
        if len(bootstrap_env.get('DELIVERY_ADMIN_PASSWORD', '')) < 16:
            raise ValueError('首次部署须通过环境设置 DELIVERY_ADMIN_PASSWORD，至少 16 字符')
    # 配置输出可能包含密钥，只做静默校验，不打印展开后的配置。
    compose(args, 'config', '--quiet')
    compose(args, 'up', '-d', '--wait', '--wait-timeout', '180', 'mysql', 'redis')
    compose(args, 'stop', 'backend')
    checked_python('prepare_database.py', args.action if args.action == 'initialize' else 'migrate',
                   '--env', args.env, '--database', database, env=bootstrap_env)
    compose(args, 'run', '--rm', '--no-deps', '--user', '0', '--entrypoint', 'sh', 'backend',
            '-c', 'chown -R 10001:10001 /app/material /app/logs')
    compose(args, 'up', '-d', '--no-build', '--wait', '--wait-timeout', '240')
    url = values['APP_EXTERNAL_BASE_URL'].rstrip('/') + '/readyz'
    with urlopen(url, timeout=10) as response:
        if response.status != 200 or json.load(response).get('status') != 'UP':
            raise ValueError('业务就绪检查未通过')
    manifest = {
        'commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT).decode().strip(),
        'mode': values['APP_DELIVERY_MODE'], 'time': int(time.time()),
        'bundle_sha256': digest(args.bundle / 'images.tar'),
        'config_sha256': digest(args.env),
        'images': compose(args, 'images', '--format', 'json', stdout=subprocess.PIPE).stdout.decode(),
        'status': 'deployed-awaiting-business-acceptance'
    }
    destination = ROOT / 'deploy/releases' / ('release-%d.json' % manifest['time'])
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    print('部署与健康检查通过；请按验收清单完成业务验证。发布记录：', destination)


if __name__ == '__main__':
    from delivery_lock import operation_lock
    with operation_lock():
        main()
