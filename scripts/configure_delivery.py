"""生成客户录入环境配置及独立随机密码，禁止覆盖已有配置。"""
import argparse
import os
from pathlib import Path
import re
import secrets
from delivery_backup import ROOT


def generate(domain, destination):
    if not re.fullmatch(r'(?=.{1,253}$)(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,63}', domain):
        raise ValueError('请填写域名，不包含协议、端口或路径')
    if domain.endswith('.example.com'):
        raise ValueError('请使用客户实际域名')
    destination = Path(destination)
    if destination.exists():
        raise ValueError('环境文件已存在，禁止覆盖数据库密码')
    text = (ROOT / '.env.prod.example').read_text(encoding='utf-8-sig')
    text = text.replace('shop.example.com', domain).replace('WECHAT_PAY_ENABLED=true', 'WECHAT_PAY_ENABLED=false')
    for placeholder in ['<change-me-root-password>', '<change-me-db-password>', '<change-me-redis-password>']:
        text = text.replace(placeholder, secrets.token_urlsafe(32))
    text += '\n# 首次初始化使用，客户修改管理员密码后删除此项。\nDELIVERY_ADMIN_PASSWORD=' + secrets.token_urlsafe(24) + '\n'
    text += 'DELIVERY_BACKUP_PASSWORD=' + secrets.token_urlsafe(32) + '\nBACKUP_ALERT_URL=\n'
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open('x', encoding='utf-8') as output:
        os.chmod(destination, 0o600)
        output.write(text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--domain', required=True)
    parser.add_argument('--output', type=Path, default=ROOT / '.env.prod')
    args = parser.parse_args()
    generate(args.domain, args.output)
    print('录入环境配置及随机密码已生成；请通过安全方式保管文件：', args.output)


if __name__ == '__main__':
    main()
