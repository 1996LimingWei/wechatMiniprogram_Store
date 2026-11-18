"""发布前静态检查；只输出字段名，不输出密钥。未通过时拒绝继续。"""
import argparse
from pathlib import Path
import re
import sys
from urllib.parse import urlparse


def read_env(path):
    values = {}
    for number, line in enumerate(path.read_text(encoding='utf-8-sig').splitlines(), 1):
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        if '=' not in line:
            raise ValueError('环境文件第 %d 行缺少等号' % number)
        key, value = line.split('=', 1)
        key = key.strip()
        if key in values:
            raise ValueError('环境文件字段重复：' + key)
        values[key] = value.strip().strip('"').strip("'")
    return values


def validate(values):
    errors = []
    mode = values.get('APP_DELIVERY_MODE')
    if mode not in ('catalog', 'commerce'):
        errors.append('APP_DELIVERY_MODE 必须为 catalog 或 commerce')
    required = ['APP_DOMAIN', 'APP_EXTERNAL_BASE_URL', 'WEB_CORS_ALLOWED_ORIGIN_PATTERNS',
                'MYSQL_DATABASE', 'MYSQL_USER', 'MYSQL_PASSWORD', 'MYSQL_ROOT_PASSWORD',
                'REDIS_PASSWORD', 'MATERIAL_PUBLIC_BASE_URL']
    if mode == 'commerce':
        required += ['WX_MA_APPID', 'WX_MA_SECRET', 'WECHAT_PAY_APP_ID', 'WECHAT_PAY_MCH_ID',
                     'WECHAT_PAY_MERCHANT_SERIAL_NO', 'WECHAT_PAY_API_V3_KEY',
                     'WECHAT_PAY_NOTIFY_URL', 'WECHAT_PAY_REFUND_NOTIFY_URL',
                     'KUAIDI100_CUSTOMER', 'KUAIDI100_KEY']
    for key in required:
        value = values.get(key, '')
        if not value or '<' in value or 'example.com' in value or 'change-me' in value:
            errors.append(key + ' 未填写真实值')
    for key in ['MYSQL_PASSWORD', 'MYSQL_ROOT_PASSWORD', 'REDIS_PASSWORD']:
        if len(values.get(key, '')) < 16:
            errors.append(key + ' 至少 16 字符')
    if values.get('MYSQL_USER', '').lower() == 'root':
        errors.append('应用数据库用户不能使用 root')
    for key in ['APP_EXTERNAL_BASE_URL', 'MATERIAL_PUBLIC_BASE_URL'] + (
            ['WECHAT_PAY_NOTIFY_URL', 'WECHAT_PAY_REFUND_NOTIFY_URL'] if mode == 'commerce' else []):
        url = urlparse(values.get(key, ''))
        if url.scheme != 'https' or not url.hostname or url.username or url.password:
            errors.append(key + ' 必须为无凭据的 HTTPS 地址')
    origins = values.get('WEB_CORS_ALLOWED_ORIGIN_PATTERNS', '')
    if '*' in origins or not origins.startswith('https://'):
        errors.append('CORS 必须为明确 HTTPS 来源')
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]{0,63}', values.get('MYSQL_DATABASE', '')):
        errors.append('MYSQL_DATABASE 格式不正确')
    if mode == 'commerce':
        if values.get('WX_MA_APPID') != values.get('WECHAT_PAY_APP_ID'):
            errors.append('小程序与支付 AppID 不一致')
        if values.get('WECHAT_PAY_ENABLED') != 'true':
            errors.append('commerce 必须启用真实支付')
        if len(values.get('WECHAT_PAY_API_V3_KEY', '').encode()) != 32:
            errors.append('API v3 Key 必须为 32 字节')
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--env', type=Path, required=True)
    args = parser.parse_args()
    errors = validate(read_env(args.env))
    if errors:
        for error in errors:
            print('拒绝发布：' + error, file=sys.stderr)
        return 1
    print('环境静态预检通过；仍需验证证书、运行状态、迁移、备份和真实渠道。')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
