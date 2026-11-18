"""仅在 GitHub 临时运行器演练正式录入部署、隔离恢复及失败探测，禁止用于客户主机。"""
import base64
import json
import os
from pathlib import Path
import ssl
import subprocess
import sys
import time
from types import SimpleNamespace
from urllib.error import HTTPError
from urllib.request import Request, build_opener, HTTPSHandler, ProxyHandler

from backup_crypto import transform
from configure_delivery import generate
from delivery_backup import ROOT, backup, compose, digest, mysql, restore
from prepare_database import migrate
from release_preflight import read_env


def require(condition, message):
    if not condition:
        raise AssertionError(message)
    print('通过：' + message, flush=True)


def main():
    if os.environ.get('GITHUB_ACTIONS') != 'true' or not os.environ.get('RUNNER_TEMP'):
        raise ValueError('仅允许 GitHub 托管临时运行器执行')
    for name in ['deploy/data', 'deploy/secrets', '.env.prod']:
        if (ROOT / name).exists():
            raise ValueError('演练要求全新工作区，拒绝接触已有数据：' + name)
    domain = 'shop.delivery.test'
    env_path = ROOT / '.env.prod'
    generate(domain, env_path)
    values = read_env(env_path)
    for key, value in values.items():
        if value and ('PASSWORD' in key or 'SECRET' in key or 'KEY' in key):
            print('::add-mask::' + value, flush=True)
    tls = ROOT / 'deploy/secrets/tls'
    tls.mkdir(parents=True)
    subprocess.run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes',
                    '-keyout', str(tls / 'privkey.pem'), '-out', str(tls / 'fullchain.pem'),
                    '-days', '1', '-subj', '/CN=' + domain,
                    '-addext', 'subjectAltName=DNS:' + domain],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    environment = dict(os.environ, SSL_CERT_FILE=str(tls / 'fullchain.pem'),
                       NO_PROXY=domain + ',localhost,127.0.0.1',
                       no_proxy=domain + ',localhost,127.0.0.1')
    args = SimpleNamespace(env=env_path, database=values['MYSQL_DATABASE'])
    report = ROOT / 'deploy/reviews/catalog-runtime.json'
    report.parent.mkdir(parents=True, exist_ok=True)
    checkpoints = []
    try:
        bundle = ROOT / 'deploy/releases/runtime-bundle'
        subprocess.run([sys.executable, str(ROOT / 'scripts/release_images.py'), '--env', str(env_path),
                        '--output', str(bundle)], check=True, env=environment)
        subprocess.run([sys.executable, str(ROOT / 'scripts/deploy_release.py'),
                        'initialize', '--env', str(env_path), '--bundle', str(bundle)], check=True, env=environment)
        args.images_override = bundle / 'images.compose.json'
        checkpoints.append('正式部署脚本、Java 25 镜像及 HTTPS 就绪')
        opener = build_opener(ProxyHandler({}), HTTPSHandler(
            context=ssl.create_default_context(cafile=str(tls / 'fullchain.pem'))))
        token = ''

        def request(path, body=None, method=None, headers=None):
            effective_headers = dict(headers or {})
            if token:
                effective_headers['Authorization'] = 'Bearer ' + token
            if isinstance(body, dict):
                body = json.dumps(body, ensure_ascii=False).encode()
                effective_headers['Content-Type'] = 'application/json'
            req = Request('https://' + domain + path, body, effective_headers, method=method)
            try:
                with opener.open(req, timeout=20) as response:
                    return response.status, response.read()
            except HTTPError as error:
                return error.code, error.read()

        def api(path, body=None, method=None, headers=None):
            status, data = request(path, body, method, headers)
            result = json.loads(data)
            require(status == 200 and result.get('code') == 0, '接口成功 ' + path)
            return result['data']

        require(request('/')[0] == 200, '后台页面可访问')
        require(request('/admin-api/product/spu/page')[0] in (401, 403), '匿名访问后台被拒绝')
        require(request('/app-api/index/shop-config')[0] == 503, '录入模式封闭公众小程序入口')
        require(request('/admin-api/trade/order/page')[0] == 503, '录入模式封闭交易后台')
        require(mysql(args, 'SELECT COUNT(*) FROM product_spu', args.database).strip() == b'0', '新库不含演示商品')
        require(mysql(args, 'SELECT COUNT(*) FROM member_user', args.database).strip() == b'0', '新库不含演示会员')
        require(mysql(args, 'SELECT COUNT(*) FROM trade_order', args.database).strip() == b'0', '新库不含演示订单')
        require(mysql(args, 'SELECT COUNT(*) FROM sys_admin_user', args.database).strip() == b'1', '新库仅保留初始管理员')
        profile = api('/admin-api/auth/login', {'username': 'admin', 'password': values['DELIVERY_ADMIN_PASSWORD']})
        token = profile['token']
        require(profile['homePath'] == '/product/spu' and profile['deliveryMode'] == 'catalog', '登录进入商品录入模式')
        # 使用真实 PNG，经上传服务写入非 root 挂载目录，再由正式 Nginx 读出。
        picture = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aV1cAAAAASUVORK5CYII=')
        boundary = 'DeliveryRuntimeBoundary'
        payload = ('--' + boundary + '\r\nContent-Disposition: form-data; name="file"; filename="catalog.png"\r\n'
                   'Content-Type: image/png\r\n\r\n').encode() + picture + ('\r\n--' + boundary + '--\r\n').encode()
        asset = api('/admin-api/material/upload', payload, 'POST', {'Content-Type': 'multipart/form-data; boundary=' + boundary})
        image_url = asset['url']
        require(request(image_url.split(domain, 1)[1])[1] == picture, '上传图片可从 HTTPS 原样读取')
        api('/admin-api/system/shop', {'displayName': '陕铺演练店', 'logoUrl': image_url,
                                      'introduction': '仅用于临时验收', 'customerService': ''})
        require(api('/admin-api/content/shop-config')['displayName'] == '陕铺演练店', '店铺展示配置读写一致')
        delete_status, delete_body = request('/admin-api/material/delete?id=' + str(asset['id']), method='DELETE')
        require(delete_status == 409 and json.loads(delete_body).get('code') == 409,
                '正在使用的 Logo 删除请求被拒绝')
        require(mysql(args, 'SELECT COUNT(*) FROM material_asset WHERE deleted=b\'0\'', args.database).strip() == b'1', '正在使用的 Logo 不会被删除')
        api('/admin-api/product/category/create', {'parentId': 0, 'name': '演练分类', 'sort': 0, 'status': 1})
        category = api('/admin-api/product/category/list')[0]['id']
        spu = api('/admin-api/product/spu/save', {
            'spu': {'name': '交付演练商品', 'categoryId': category, 'status': 0, 'picUrl': image_url,
                    'sliderPicUrls': json.dumps([image_url]), 'description': '<p>演练商品</p>'},
            'skus': [{'skuCode': 'DELIVERY-CI', 'properties': '[]', 'price': 100,
                      'marketPrice': 100, 'stock': 10, 'warningStock': 2, 'picUrl': image_url}]})
        require(mysql(args, 'SELECT status FROM product_spu WHERE id=' + str(spu), args.database).strip() == b'0', '保存商品保持下架')
        admin_token = token
        for username, role in [('delivery_operator', 'PRODUCT_OPERATOR'), ('delivery_readonly', 'READONLY')]:
            mysql(args, "INSERT INTO sys_admin_user(username,password,nickname,status) "
                  "SELECT '%s',password,'临时角色演练',1 FROM sys_admin_user WHERE username='admin'" % username, args.database)
            mysql(args, "INSERT INTO sys_admin_user_role(admin_user_id,role_id) "
                  "SELECT u.id,r.id FROM sys_admin_user u JOIN sys_role r ON r.code='%s' WHERE u.username='%s'" % (role, username), args.database)
            role_profile = api('/admin-api/auth/login', {'username': username, 'password': values['DELIVERY_ADMIN_PASSWORD']})
            token = role_profile['token']
            require(role in role_profile['roles'], '实际账号具备预期角色 ' + role)
            api('/admin-api/product/spu/page')
            require(request('/admin-api/system/shop')[0] == 403, '非系统管理员不能修改店铺配置 ' + role)
            if role == 'READONLY':
                require(request('/admin-api/product/spu/save', {})[0] == 403, '只读账号通过实际安全过滤链禁止商品写入')
        token = admin_token
        from verify_catalog_browser import verify_browser
        verify_browser()
        checkpoints.append('真实 HTTP 登录、权限边界、素材、店铺设置及商品录入')
        # 验证完整迁移重跑无副作用，然后以实际数据库和素材执行备份恢复。
        compose(args, 'stop', 'backend')
        before = mysql(args, 'SELECT COUNT(*) FROM schema_migration_history', args.database)
        migrate(args)
        require(mysql(args, 'SELECT COUNT(*) FROM schema_migration_history', args.database) == before, '迁移重跑不新增历史')
        compose(args, 'up', '-d', '--wait', '--wait-timeout', '180', 'backend')
        archive = ROOT / 'deploy/backups/runtime.zip'
        backup(SimpleNamespace(**vars(args), archive=archive))
        encrypted = archive.with_suffix('.enc')
        decrypted = archive.with_suffix('.decrypted.zip')
        transform(archive, encrypted, values['DELIVERY_BACKUP_PASSWORD'])
        transform(encrypted, decrypted, values['DELIVERY_BACKUP_PASSWORD'], True)
        require(digest(archive) == digest(decrypted), '实际数据库图片归档加密解密一致')
        restored = 'shop_restore_ci'
        material = ROOT / 'deploy/restore-check/material'
        restore(SimpleNamespace(env=env_path, database=restored, archive=decrypted, material=material))
        for table in ['product_spu', 'product_sku', 'product_stock_log', 'material_asset',
                      'shop_display_config', 'sys_admin_user', 'schema_migration_history']:
            # 排序后的所有字段比较，覆盖库存、价格、URL、迁移校验和及账号哈希。
            require(mysql(args, 'SELECT * FROM `' + table + '` ORDER BY 1', args.database)
                    == mysql(args, 'SELECT * FROM `' + table + '` ORDER BY 1', restored), '隔离恢复数据一致 ' + table)
        original_files = sorted((ROOT / 'deploy/data/material').rglob('*'))
        for file in original_files:
            if file.is_file():
                require(digest(file) == digest(material / file.relative_to(ROOT / 'deploy/data/material')), '隔离恢复图片哈希一致')
        checkpoints.append('重复迁移、数据库图片成套备份、加密及隔离恢复')
        # 用恢复库和恢复素材启动独立应用，不能只比较 SQL 后就宣称应用可恢复。
        username = values['MYSQL_USER']
        if not username.replace('_', '').isalnum():
            raise ValueError('演练数据库用户名不符合预期')
        mysql(args, "GRANT ALL ON `" + restored + "`.* TO '" + username + "'@'%'")
        compose(args, 'run', '--rm', '--no-deps', '--user', '0', '--entrypoint', 'sh',
                '-v', str(material) + ':/app/material', 'backend', '-c', 'chown -R 10001:10001 /app/material')
        compose(args, 'run', '-d', '--no-deps', '--name', 'shanpu-restore-ci',
                '-p', '127.0.0.1:18085:8085', '-v', str(material) + ':/app/material',
                '-e', 'SPRING_DATA_REDIS_DATABASE=15',
                '-e', 'DB_URL=jdbc:mysql://mysql:3306/' + restored + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
                'backend', stdout=subprocess.PIPE)
        from urllib.request import urlopen
        restored_base = 'http://127.0.0.1:18085'
        deadline = time.monotonic() + 180
        while True:
            try:
                with urlopen(restored_base + '/actuator/health', timeout=5) as response:
                    if json.load(response).get('status') == 'UP':
                        break
            except Exception:
                if time.monotonic() >= deadline:
                    raise AssertionError('恢复应用在期限内未就绪')
                time.sleep(2)
            if time.monotonic() >= deadline:
                raise AssertionError('恢复应用在期限内未就绪')
        login_body = json.dumps({'username': 'admin', 'password': values['DELIVERY_ADMIN_PASSWORD']}).encode()
        with urlopen(Request(restored_base + '/admin-api/auth/login', login_body,
                             {'Content-Type': 'application/json'}), timeout=20) as response:
            restored_token = json.load(response)['data']['token']
        with urlopen(Request(restored_base + '/admin-api/product/spu/page',
                             headers={'Authorization': 'Bearer ' + restored_token}), timeout=20) as response:
            result = json.load(response)
            require(result.get('code') == 0 and '交付演练商品' in json.dumps(result, ensure_ascii=False),
                    '独立恢复应用可登录并读取恢复商品')
        subprocess.run(['docker', 'rm', '-f', 'shanpu-restore-ci'], check=True, stdout=subprocess.DEVNULL)
        checkpoints.append('恢复数据库及素材启动独立应用，真实登录并读取商品')
        # 回滚入口烟测：同一已部署包重新导入，核对部署后录入的数据仍在。
        # 这里没有跨版本镜像，不将此项写成前一业务版本回滚已验收。
        release_record = next((ROOT / 'deploy/releases').glob('release-*.json'))
        product_before = mysql(args, 'SELECT * FROM product_spu ORDER BY id', args.database)
        subprocess.run([sys.executable, str(ROOT / 'scripts/rollback_release.py'),
                        '--env', str(env_path), '--release', str(release_record),
                        '--bundle', str(bundle)], check=True, env=environment)
        require(mysql(args, 'SELECT * FROM product_spu ORDER BY id', args.database) == product_before,
                '回滚入口重部署后保留初始化后录入的商品')
        require(request(image_url.split(domain, 1)[1])[1] == picture, '回滚入口重部署后图片仍可访问')
        checkpoints.append('同版本回滚入口烟测及新增商品图片保留，非跨版本验收')
        compose(args, 'up', '-d', '--wait', '--wait-timeout', '180', 'backend')
        require(request('/readyz')[0] == 200, '备份后服务恢复正常')
        compose(args, 'stop', 'backend')
        require(request('/readyz')[0] != 200, '后端停止时业务就绪探测失败')
        checkpoints.append('故障注入：后端停止后就绪检查拒绝通过')
    finally:
        report.write_text(json.dumps({'checkpoints': checkpoints,
            'complete': len(checkpoints) == 6, 'scope': '临时 CI 演练，不代表跨版本回滚、客户生产或真实渠道验收'}, ensure_ascii=False, indent=2), encoding='utf-8')
        subprocess.run(['docker', 'rm', '-f', 'shanpu-restore-ci'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        compose(args, 'down', '--remove-orphans')


if __name__ == '__main__':
    main()
