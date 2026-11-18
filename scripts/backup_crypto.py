"""流式 AES-256-GCM 备份加密，认证成功前不发布解密文件。"""
import argparse
import getpass
import os
from pathlib import Path
import tempfile
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

MAGIC = b'SHANPUBK1'
CHUNK = 1024 * 1024


def derive(password, salt):
    if len(password) < 20:
        raise ValueError('备份密码至少 20 字符，请独立安全保存')
    return PBKDF2HMAC(algorithm=hashes.SHA256(), length=32, salt=salt,
                     iterations=600000).derive(password.encode('utf-8'))


def transform(source, destination, password, decrypt=False):
    source, destination = Path(source), Path(destination)
    if destination.exists():
        raise ValueError('目标文件已存在，禁止覆盖')
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with source.open('rb') as inp, tempfile.NamedTemporaryFile(
                dir=destination.parent, prefix='.backup-', delete=False) as out:
            temporary = Path(out.name)
            os.chmod(temporary, 0o600)
            if decrypt:
                header = inp.read(len(MAGIC) + 28)
                if len(header) != len(MAGIC) + 28 or not header.startswith(MAGIC):
                    raise ValueError('加密备份格式不正确')
                salt, nonce = header[len(MAGIC):len(MAGIC)+16], header[-12:]
                remaining = source.stat().st_size - len(header) - 16
                if remaining < 0:
                    raise ValueError('加密备份不完整')
                inp.seek(-16, 2)
                tag = inp.read(16)
                inp.seek(len(header))
                worker = Cipher(algorithms.AES(derive(password, salt)), modes.GCM(nonce, tag)).decryptor()
            else:
                salt, nonce = os.urandom(16), os.urandom(12)
                header = MAGIC + salt + nonce
                out.write(header)
                remaining = source.stat().st_size
                worker = Cipher(algorithms.AES(derive(password, salt)), modes.GCM(nonce)).encryptor()
            worker.authenticate_additional_data(header)
            while remaining:
                data = inp.read(min(CHUNK, remaining))
                if not data:
                    raise ValueError('备份读取不完整')
                remaining -= len(data)
                out.write(worker.update(data))
            out.write(worker.finalize())
            if not decrypt:
                out.write(worker.tag)
            out.flush()
            os.fsync(out.fileno())
        # 同目录原子落盘；错误密码或篡改认证失败不会留下可用明文文件。
        os.link(temporary, destination)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['encrypt', 'decrypt'])
    parser.add_argument('source', type=Path)
    parser.add_argument('destination', type=Path)
    args = parser.parse_args()
    password = os.environ.get('DELIVERY_BACKUP_PASSWORD') or getpass.getpass('备份密码（不回显）：')
    transform(args.source, args.destination, password, args.action == 'decrypt')
    print('备份' + ('解密' if args.action == 'decrypt' else '加密') + '完成')


if __name__ == '__main__':
    main()
