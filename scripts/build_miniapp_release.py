"""使用 HBuilderX 发行编译并验证目标 API；不上传、不提交审核。"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
from urllib.parse import urlparse
from delivery_backup import ROOT


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cli', required=True, type=Path)
    parser.add_argument('--api-root', required=True)
    parser.add_argument('--environment', choices=['staging', 'production'], default='production')
    args = parser.parse_args()
    url = urlparse(args.api_root)
    if url.scheme != 'https' or not url.hostname or url.username or url.password or url.path not in ('', '/') or url.query or url.fragment:
        raise ValueError('请使用 HTTPS 域名根地址，不包含 /app-api/、凭据或其他路径')
    if not args.cli.is_file():
        raise ValueError('HBuilderX CLI 不存在')
    root = args.api_root.rstrip('/') + '/'
    project = ROOT / 'shop-miniapp'
    source = project / 'config/build-target.js'
    lock = project / 'config/.release-build.lock'
    with lock.open('x') as stream:
        stream.write(str(os.getpid()))
    original = source.read_bytes()
    started = time.time()
    try:
        source.write_text('module.exports = ' + json.dumps({'appEnv': args.environment, 'apiBaseUrl': root}) + ';\n', encoding='utf-8')
        subprocess.run([str(args.cli), 'project', 'open', '--path', str(project)], check=True)
        subprocess.run([str(args.cli), 'publish', 'mp-weixin', '--project', str(project), '--upload', 'false'], check=True)
        output = project / 'unpackage/dist/build/mp-weixin'
        app = output / 'app.js'
        if not app.is_file() or app.stat().st_mtime < started:
            raise ValueError('未生成本次发行产物，不能把旧构建视为成功')
        if not any(root in file.read_text(encoding='utf-8') for file in output.rglob('*.js')):
            raise ValueError('发行产物未找到目标 API，禁止交付')
        record = {'environment': args.environment, 'apiRoot': root, 'upload': False,
                  'time': int(time.time()), 'output': str(output)}
        report = ROOT / 'deploy/reviews/miniapp-release.json'
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding='utf-8')
        print('发行构建与 API 目标检查通过，未上传微信平台：', output)
    finally:
        source.write_bytes(original)
        lock.unlink(missing_ok=True)


if __name__ == '__main__':
    main()
