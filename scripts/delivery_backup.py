"""数据库与素材成套备份；恢复仅允许新建的隔离数据库与空素材目录。"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent.parent


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def compose(args, *command, **kwargs):
    override = getattr(args, 'images_override', None)
    return subprocess.run(['docker', 'compose', '--env-file', str(args.env.resolve()),
                           '-f', str(ROOT / 'docker-compose.prod.yml'),
                           *(['-f', str(override)] if override else []), *command],
                          check=True, **kwargs)


def validate_database(name):
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]{0,63}', name):
        raise ValueError('数据库名称只能包含字母、数字及下划线')


def mysql(args, sql, database=None, stdin=None):
    # 密码只在容器内从现有环境读取，不进入宿主机命令参数及日志。
    command = ['exec', '-T', 'mysql', 'sh', '-c',
               'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql -uroot --default-character-set=utf8mb4 "$@"',
               'mysql']
    if database:
        command.append(database)
    if sql is not None:
        command.extend(['-N', '-B', '-e', sql])
    return compose(args, *command, stdin=stdin, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def backup(args):
    validate_database(args.database)
    if args.archive.exists():
        raise ValueError('备份文件已存在，禁止覆盖')
    material = (ROOT / 'deploy/data/material').resolve()
    if not material.is_dir():
        raise ValueError('素材目录不存在，拒绝生成不完整备份')
    args.archive.parent.mkdir(parents=True, exist_ok=True)
    if not os.access(args.archive.parent, os.W_OK):
        raise ValueError('备份目录不可写，请先核对部署用户权限；后端尚未停止')
    paused = False
    with tempfile.TemporaryDirectory() as tmp:
        sql = Path(tmp) / 'database.sql'
        try:
            # 预检 Docker，避免预检失败后错误地启动一个本来未运行的服务。
            state = compose(args, 'ps', '--status', 'running', '--services',
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout.decode().splitlines()
            if 'backend' not in state:
                raise ValueError('仅支持后端正在运行时备份；请确认服务状态')
            compose(args, 'stop', 'backend', stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            paused = True
            with sql.open('wb') as out:
                compose(args, 'exec', '-T', 'mysql', 'sh', '-c',
                        'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump -uroot --single-transaction --routines --triggers --hex-blob --default-character-set=utf8mb4 "$1"',
                        'dump', args.database, stdout=out, stderr=subprocess.PIPE)
            if sql.stat().st_size == 0:
                raise ValueError('数据库备份为空')
            records = {'database.sql': digest(sql)}
            for file in material.rglob('*'):
                if file.is_symlink():
                    raise ValueError('素材目录不能含符号链接')
                if file.is_file():
                    records['material/' + file.relative_to(material).as_posix()] = digest(file)
            # 独占创建且从第一个字节开始限制权限，不覆盖并发生成的归档。
            descriptor, temporary_name = tempfile.mkstemp(prefix='backup-', suffix='.partial', dir=args.archive.parent)
            temporary = Path(temporary_name)
            try:
                with os.fdopen(descriptor, 'w+b') as stream:
                    with zipfile.ZipFile(stream, 'w', zipfile.ZIP_DEFLATED) as archive:
                        archive.write(sql, 'database.sql')
                        for name in records:
                            if name.startswith('material/'):
                                archive.write(material / name[len('material/'):], name)
                        archive.writestr('manifest.json', json.dumps({'format': 1, 'database': args.database,
                            'files': records}, ensure_ascii=False))
                    stream.flush()
                    os.fsync(stream.fileno())
                os.link(temporary, args.archive)
            finally:
                temporary.unlink(missing_ok=True)
        finally:
            if paused and not getattr(args, 'keep_stopped', False):
                compose(args, 'start', 'backend', stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    print('成套备份完成；含客户数据，必须加密并复制到异机存储：', args.archive)


def verify_archive(path, target):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError('备份包含重复文件')
        manifest = json.loads(archive.read('manifest.json'))
        if manifest.get('format') != 1 or 'database.sql' not in manifest['files']:
            raise ValueError('备份格式不正确')
        if set(names) != set(manifest['files']) | {'manifest.json'}:
            raise ValueError('备份文件与清单不一致')
        for name, expected in manifest['files'].items():
            if name != 'database.sql' and not name.startswith('material/'):
                raise ValueError('备份包含非预期路径')
            dest = (target / name).resolve()
            if target.resolve() not in dest.parents or '\\' in name:
                raise ValueError('备份包含非法路径')
            dest.parent.mkdir(parents=True, exist_ok=True)
            h = hashlib.sha256()
            with archive.open(name) as src, dest.open('wb') as out:
                for data in iter(lambda: src.read(1024 * 1024), b''):
                    h.update(data)
                    out.write(data)
            if h.hexdigest() != expected:
                raise ValueError('备份校验失败：' + name)
        return manifest


def restore(args):
    validate_database(args.database)
    if '_restore_' not in args.database:
        raise ValueError('恢复仅允许名称含 _restore_ 的新隔离数据库，禁止覆盖生产库')
    destination = args.material.resolve()
    if destination.exists():
        raise ValueError('恢复素材目录必须尚不存在')
    with tempfile.TemporaryDirectory() as tmp:
        extracted = Path(tmp)
        verify_archive(args.archive, extracted)
        count = mysql(args, "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='" + args.database + "'")
        if count.strip() != b'0':
            raise ValueError('目标数据库已经存在，拒绝覆盖')
        mysql(args, 'CREATE DATABASE `' + args.database + '` CHARACTER SET utf8mb4')
        with (extracted / 'database.sql').open('rb') as sql:
            mysql(args, None, args.database, sql)
        import shutil
        shutil.copytree(extracted / 'material', destination) if (extracted / 'material').exists() else destination.mkdir(parents=True)
    print('隔离恢复完成；请核对商品、图片及订单金额。未改动生产数据库。')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['backup', 'restore', 'verify'])
    parser.add_argument('--env', type=Path, default=ROOT / '.env.prod')
    parser.add_argument('--database')
    parser.add_argument('--archive', required=True, type=Path)
    parser.add_argument('--material', type=Path)
    args = parser.parse_args()
    if args.action == 'verify':
        with tempfile.TemporaryDirectory() as tmp:
            verify_archive(args.archive, Path(tmp))
        print('备份清单及校验和通过')
    else:
        if not args.database or not args.env.is_file():
            parser.error('备份/恢复需要有效 --env 及 --database')
        if args.action == 'restore' and args.material is None:
            parser.error('恢复需要 --material 指定新的隔离素材目录')
        from delivery_lock import operation_lock
        with operation_lock():
            (backup if args.action == 'backup' else restore)(args)


if __name__ == '__main__':
    main()
