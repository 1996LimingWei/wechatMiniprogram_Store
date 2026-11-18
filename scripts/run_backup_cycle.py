"""生成并验证备份，加密后复制到已挂载的异机备份目录。"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
from urllib.request import Request, urlopen
from backup_crypto import transform
from delivery_backup import ROOT, backup, verify_archive
from release_preflight import read_env


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env', required=True, type=Path)
    parser.add_argument('--offsite-dir', required=True, type=Path)
    args = parser.parse_args()
    values = read_env(args.env)
    password = values.get('DELIVERY_BACKUP_PASSWORD', '')
    webhook = values.get('BACKUP_ALERT_URL', '')
    if len(password) < 20:
        raise ValueError('环境文件中必须配置独立 DELIVERY_BACKUP_PASSWORD')
    if not webhook.startswith('https://'):
        raise ValueError('必须先配置 HTTPS BACKUP_ALERT_URL，以便失败时通知负责人')
    args.database = values['MYSQL_DATABASE']
    destination = args.offsite_dir.resolve()
    state = {'time': int(time.time()), 'success': False}
    try:
        if not destination.is_dir() or not os.path.ismount(destination):
            raise ValueError('异机目录必须是已挂载的独立存储根目录，不能回退到本机目录')
        with tempfile.TemporaryDirectory(prefix='shanpu-backup-') as tmp:
            args.archive = Path(tmp) / 'backup.zip'
            backup(args)
            with tempfile.TemporaryDirectory() as check:
                verify_archive(args.archive, Path(check))
            encrypted = Path(tmp) / 'backup.enc'
            transform(args.archive, encrypted, password)
            name = 'shanpu-%d.enc' % state['time']
            target = destination / name
            # 先以独占创建写临时副本，核对哈希后才发布完整文件名。
            temporary = destination / (name + '.partial')
            try:
                with encrypted.open('rb') as source, temporary.open('xb') as output:
                    os.chmod(temporary, 0o600)
                    shutil.copyfileobj(source, output, 1024 * 1024)
                    output.flush()
                    os.fsync(output.fileno())
                from delivery_backup import digest
                if digest(temporary) != digest(encrypted):
                    raise ValueError('异机副本校验失败')
                os.link(temporary, target)
            finally:
                temporary.unlink(missing_ok=True)
            state.update(success=True, file=name)
    except Exception:
        # 不把路径、数据库内容、密码或供应商错误正文发送到通知渠道。
        payload = json.dumps({'text': '陕铺自动备份失败，请检查交付服务器的备份任务日志。'}).encode()
        try:
            with urlopen(Request(webhook, data=payload,
                                 headers={'Content-Type': 'application/json'}), timeout=10) as response:
                if response.status >= 300:
                    raise ValueError('通知失败')
        except Exception:
            print('备份失败，且告警投递未成功；请检查备份状态文件。', file=sys.stderr)
        raise
    finally:
        status = ROOT / 'deploy/backup-status.json'
        status.parent.mkdir(parents=True, exist_ok=True)
        status.write_text(json.dumps(state), encoding='utf-8')
    print('加密异机备份完成：', state['file'])


if __name__ == '__main__':
    from delivery_lock import operation_lock
    with operation_lock():
        main()
