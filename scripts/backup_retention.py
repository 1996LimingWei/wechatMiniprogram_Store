"""预览或执行定期加密备份保留；保留48小时全部恢复点和30天每日最新副本。"""
import argparse
from datetime import datetime, timezone
from pathlib import Path
import os
import re
import tempfile
import time

from backup_crypto import transform
from delivery_backup import digest, verify_archive
from release_preflight import read_env


def plan(directory, now):
    entries = []
    for path in directory.iterdir():
        match = re.fullmatch(r'shanpu-(\d{10})\.enc', path.name)
        if match and path.is_file() and not path.is_symlink():
            entries.append((int(match[1]), path))
    entries.sort(reverse=True)
    keep = {path for _, path in entries[:2]}
    days = set()
    for stamp, path in entries:
        age = now - stamp
        if age <= 48 * 3600:
            keep.add(path)
        if 0 <= age <= 30 * 86400:
            day = datetime.fromtimestamp(stamp, timezone.utc).date()
            if day not in days:
                keep.add(path)
                days.add(day)
    return [path for _, path in entries if path in keep], [path for _, path in entries if path not in keep]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offsite-dir', type=Path, required=True)
    parser.add_argument('--env', type=Path, required=True)
    parser.add_argument('--apply', action='store_true', help='不加此参数只预览，不删除')
    args = parser.parse_args()
    directory = args.offsite_dir.resolve()
    if directory == Path(directory.anchor) or not directory.is_dir() or not os.path.ismount(directory):
        raise ValueError('仅允许经核实的专用备份存储挂载目录，不能为文件系统根目录')
    keep, remove = plan(directory, int(time.time()))
    print('保留定期备份：', len(keep), '；待清理：', len(remove))
    for path in remove:
        print('待清理：', path.name)
    if not args.apply or not remove:
        return
    if len(keep) < 2:
        raise ValueError('至少保留两个恢复点，拒绝清理')
    password = read_env(args.env).get('DELIVERY_BACKUP_PASSWORD', '')
    # 两个最新保留副本均须实际读回、解密、逐文件校验成功；失败不删除。
    snapshots = {path: (path.stat(), digest(path)) for path in remove}
    with tempfile.TemporaryDirectory(prefix='shanpu-retention-') as temporary:
        for index, path in enumerate(keep[:2]):
            archive = Path(temporary) / (str(index) + '.zip')
            transform(path, archive, password, decrypt=True)
            verify_archive(archive, Path(temporary) / ('verify-' + str(index)))
    for path, (before, checksum) in snapshots.items():
        current = path.stat()
        if path.is_symlink() or (before.st_ino, before.st_size, before.st_mtime_ns) != (
                current.st_ino, current.st_size, current.st_mtime_ns) or digest(path) != checksum:
            raise ValueError('待清理副本发生变化，停止；请重新预览')
    for path in remove:
        path.unlink()
        print('已清理过期加密备份：', path.name)


if __name__ == '__main__':
    from delivery_lock import operation_lock
    with operation_lock():
        main()
