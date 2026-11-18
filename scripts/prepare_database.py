"""正式新库初始化与不可变迁移；新库清理演示数据后才允许启动应用。"""
import argparse
import hashlib
import os
from pathlib import Path
import re
import tempfile
from delivery_backup import ROOT, compose, mysql, validate_database


def execute(args, sql):
    with tempfile.TemporaryFile() as stream:
        stream.write(sql.encode('utf-8'))
        stream.seek(0)
        return mysql(args, None, args.database, stream)


def migrations():
    for path in sorted((ROOT / 'sql/migrations').glob('V*__*.sql')):
        match = re.fullmatch(r'V([0-9_]+)__([a-z0-9_]+)\.sql', path.name)
        if not match:
            raise ValueError('迁移文件名不合法：' + path.name)
        sql = path.read_text(encoding='utf-8-sig').replace('\r\n', '\n')
        yield match.group(1), match.group(2), sql, hashlib.sha256(sql.encode()).hexdigest()


def migrate(args, *, fresh_bootstrap=False):
    history_exists = mysql(args, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='schema_migration_history'", args.database).strip() == b'1'
    if not history_exists and not fresh_bootstrap:
        raise ValueError('数据库没有迁移历史，禁止自动升级；请先在隔离副本核对来源和迁移基线，空库请使用 initialize')
    execute(args, '''CREATE TABLE IF NOT EXISTS schema_migration_history (
        version varchar(32) PRIMARY KEY, description varchar(128) NOT NULL,
        checksum varchar(64) NOT NULL, installed_on datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;''')
    entries = list(migrations())
    # 先检查全部历史，再进行任何新迁移，避免半途才发现旧文件被修改。
    applied = {}
    for version, description, sql, checksum in entries:
        recorded = mysql(args, "SELECT checksum FROM schema_migration_history WHERE version='" + version + "'", args.database).decode().strip()
        if recorded and recorded != checksum:
            raise ValueError('历史迁移校验和不一致，停止并先在副本核对：' + version)
        applied[version] = recorded
    for version, description, sql, checksum in entries:
        if applied[version]:
            continue
        execute(args, sql)
        execute(args, "INSERT INTO schema_migration_history(version,description,checksum) VALUES ('%s','%s','%s');" % (version, description, checksum))
        print('已执行迁移：' + version)


def initialize(args):
    import bcrypt
    password = os.environ.get('DELIVERY_ADMIN_PASSWORD', '')
    if len(password) < 16 or len(password.encode()) > 72:
        raise ValueError('DELIVERY_ADMIN_PASSWORD 必须为 16 字符以上且不超过 72 字节')
    count = mysql(args, 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()', args.database)
    if count.strip() != b'0':
        raise ValueError('初始化仅允许空库，禁止清理已有客户数据；旧库请使用 migrate')
    sql = (ROOT / 'sql/init.sql').read_text(encoding='utf-8-sig')
    sql = re.sub(r'^\s*(?:CREATE DATABASE[^\n]+|USE `shop`;)[\r\n]*', '', sql, flags=re.M)
    execute(args, sql)
    migrate(args, fresh_bootstrap=True)
    # 仅作用于本次刚刚初始化的空库。运行前外部应用必须尚未启动。
    tables = mysql(args, 'SHOW TABLES', args.database).decode().splitlines()
    keep = {'sys_role', 'sys_permission', 'sys_role_permission', 'sys_admin_user',
            'sys_admin_user_role', 'schema_migration_history', 'shop_display_config'}
    for table in tables:
        if table not in keep:
            if not re.fullmatch(r'[A-Za-z0-9_]+', table):
                raise ValueError('非预期表名')
            execute(args, 'DELETE FROM `' + table + '`;')
    hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt(rounds=12)).decode()
    execute(args, "UPDATE sys_admin_user SET password='%s',status=1 WHERE username='admin';" % hashed)
    print('正式新库初始化完成：已清理演示业务数据，并设置独立管理员密码。')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['initialize', 'migrate'])
    parser.add_argument('--env', required=True, type=Path)
    parser.add_argument('--database', required=True)
    args = parser.parse_args()
    validate_database(args.database)
    if not args.env.is_file():
        parser.error('环境文件不存在')
    from subprocess import PIPE
    running = compose(args, 'ps', '--status', 'running', '--services', stdout=PIPE).stdout.decode().splitlines()
    if 'backend' in running:
        raise ValueError('请先停止后端再初始化/升级数据库，避免并发业务写入')
    (initialize if args.action == 'initialize' else migrate)(args)


if __name__ == '__main__':
    main()
