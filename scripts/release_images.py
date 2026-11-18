"""构建一次并封存精确镜像，测试与客户主机导入同一镜像包，不携带数据库或密钥。"""
import argparse
import json
from pathlib import Path
import re
import subprocess
from delivery_backup import ROOT, compose, digest
from release_preflight import read_env


def commit():
    return subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT).decode().strip()


def create_bundle(args):
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT).strip():
        raise ValueError('请先提交源码并固定待构建版本')
    if args.output.exists():
        raise ValueError('镜像包目录已存在，禁止覆盖已验收产物')
    compose(args, 'config', '--quiet')
    compose(args, 'build')
    compose(args, 'pull', '--ignore-buildable')
    values = read_env(args.env)
    references = {'backend': values.get('SHOP_BACKEND_IMAGE', 'shanpu/backend:local'),
                  'admin': values.get('SHOP_ADMIN_IMAGE', 'shanpu/admin:local'),
                  'mysql': 'mysql:8.0', 'redis': 'redis:7-alpine', 'nginx': 'nginx:stable-alpine'}
    images = {}
    for service, reference in references.items():
        images[service] = subprocess.check_output(
            ['docker', 'image', 'inspect', '--format', '{{.Id}}', reference]).decode().strip()
    args.output.mkdir(parents=True)
    archive = args.output / 'images.tar'
    subprocess.run(['docker', 'image', 'save', '-o', str(archive), *sorted(set(images.values()))], check=True)
    manifest = {'format': 1, 'commit': commit(), 'images': images, 'archive_sha256': digest(archive)}
    (args.output / 'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    print('镜像包已封存；测试和正式部署必须使用这个包：', args.output)


def load_bundle(path, *, expected_commit=None):
    path = Path(path).resolve()
    manifest = json.loads((path / 'manifest.json').read_text(encoding='utf-8'))
    expected = {'backend', 'admin', 'mysql', 'redis', 'nginx'}
    if (manifest.get('format') != 1 or manifest.get('commit') != (expected_commit or commit())
            or set(manifest.get('images', {})) != expected
            or any(not re.fullmatch(r'sha256:[a-f0-9]{64}', value) for value in manifest['images'].values())):
        raise ValueError('镜像包格式、服务或源码版本不匹配')
    archive = path / 'images.tar'
    if digest(archive) != manifest.get('archive_sha256'):
        raise ValueError('镜像包校验失败，禁止导入')
    subprocess.run(['docker', 'image', 'load', '-i', str(archive)], check=True)
    for image in manifest['images'].values():
        found = subprocess.check_output(['docker', 'image', 'inspect', '--format', '{{.Id}}', image]).decode().strip()
        if found != image:
            raise ValueError('导入后的镜像摘要不一致')
    override = path / 'images.compose.json'
    override.write_text(json.dumps({'services': {service: {'image': image, 'pull_policy': 'never'}
                         for service, image in manifest['images'].items()}}, indent=2), encoding='utf-8')
    return override


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    create_bundle(parser.parse_args())
