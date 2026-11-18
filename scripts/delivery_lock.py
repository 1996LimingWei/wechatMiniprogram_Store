"""客户 Linux 主机上的部署、备份、恢复与回滚使用同一互斥锁。"""
from contextlib import contextmanager
import os
from delivery_backup import ROOT


@contextmanager
def operation_lock():
    if os.name != 'posix':
        raise ValueError('正式运维入口仅支持 Linux；Windows 可执行单元测试和离线预检')
    import fcntl
    path = ROOT / 'deploy/delivery-operation.lock'
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor = os.open(path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    try:
        try:
            fcntl.flock(descriptor, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError('已有部署、备份、恢复或回滚任务正在执行，请等待其完成；本次未改动服务') from None
        yield
    finally:
        os.close(descriptor)
