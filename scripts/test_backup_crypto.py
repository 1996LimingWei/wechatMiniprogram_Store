from pathlib import Path
import tempfile
import unittest
from backup_crypto import transform


class BackupEncryptionTests(unittest.TestCase):
    def test_roundtrip_and_wrong_password(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            original = root / 'original'
            original.write_bytes(b'private customer data' * 100000)
            encrypted = root / 'encrypted'
            transform(original, encrypted, 'test-password-strong-enough-123')
            target = root / 'restored'
            transform(encrypted, target, 'test-password-strong-enough-123', True)
            self.assertEqual(original.read_bytes(), target.read_bytes())
            bad = root / 'incorrect'
            with self.assertRaises(Exception):
                transform(encrypted, bad, 'wrong-password-strong-enough-123', True)
            self.assertFalse(bad.exists())
            self.assertFalse(list(root.glob('.backup-*')))

    def test_tampering_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / 'source'
            source.write_bytes(b'customer data')
            encrypted = root / 'encrypted'
            transform(source, encrypted, 'test-password-strong-enough-123')
            data = bytearray(encrypted.read_bytes())
            data[-17] ^= 1
            encrypted.write_bytes(data)
            target = root / 'target'
            with self.assertRaises(Exception):
                transform(encrypted, target, 'test-password-strong-enough-123', True)
            self.assertFalse(target.exists())


if __name__ == '__main__':
    unittest.main()
