"""独立检查备份成功时间，发现定时任务停摆时通知负责人。"""
import argparse
import json
from pathlib import Path
import time
from urllib.request import Request, urlopen
from delivery_backup import ROOT
from release_preflight import read_env


def healthy(status, now):
    return status.get('success') is True and 0 <= now - status.get('time', 0) <= 3600


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env', required=True, type=Path)
    args = parser.parse_args()
    try:
        state = json.loads((ROOT / 'deploy/backup-status.json').read_text())
    except (OSError, ValueError):
        state = {}
    if healthy(state, int(time.time())):
        print('最近备份状态正常')
        return
    webhook = read_env(args.env).get('BACKUP_ALERT_URL', '')
    if not webhook.startswith('https://'):
        raise ValueError('备份状态异常，且没有配置可用 HTTPS 告警入口')
    request = Request(webhook, json.dumps({'text': '陕铺超过 60 分钟没有有效备份，请检查备份任务、异机存储与服务器状态。'}).encode(),
                      headers={'Content-Type': 'application/json'})
    with urlopen(request, timeout=10) as response:
        if response.status >= 300:
            raise ValueError('备份状态异常，告警未成功')
    raise SystemExit(1)


if __name__ == '__main__':
    main()
