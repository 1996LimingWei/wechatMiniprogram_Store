"""验证升级的停写、独立备份校验和迁移顺序，不访问真实 Docker。"""
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import deploy_release as release


class UpgradeTests(unittest.TestCase):
    def exercise(self, tampered=False):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            tls = root / 'deploy/secrets/tls'
            tls.mkdir(parents=True)
            for name in ['fullchain.pem', 'privkey.pem']:
                (tls / name).touch()
            offsite = root / 'offsite'
            offsite.mkdir()
            archive = root / 'backup.zip'
            events = []

            def backup(args):
                self.assertTrue(args.keep_stopped)
                events.append('停写备份')
                args.archive.write_bytes(b'consistent-checkpoint')

            def transform(source, target, password, decrypt=False):
                events.append('读回解密' if decrypt else '加密独立副本')
                target.write_bytes(b'corrupted' if tampered and decrypt else source.read_bytes())

            def migrate(*args, **kwargs):
                events.append('迁移')
                raise RuntimeError('到达迁移入口')

            argv = ['deploy_release.py', 'upgrade', '--env', str(root / '.env'),
                    '--bundle', str(root / 'bundle'), '--backup', str(archive),
                    '--offsite-dir', str(offsite)]
            with patch.object(sys, 'argv', argv), patch.object(release, 'ROOT', root), \
                    patch.object(release.subprocess, 'check_output', return_value=b''), \
                    patch('release_images.load_bundle', return_value=root / 'override.json'), \
                    patch.object(release, 'read_env', return_value={
                        'MYSQL_DATABASE': 'shop', 'DELIVERY_BACKUP_PASSWORD': 'independent-backup-password'}), \
                    patch.object(release, 'validate', return_value=[]), \
                    patch.object(release.os.path, 'ismount', return_value=True), \
                    patch.object(release, 'backup', side_effect=backup), \
                    patch.object(release, 'verify_archive'), \
                    patch('backup_crypto.transform', side_effect=transform), \
                    patch.object(release, 'compose'), \
                    patch.object(release, 'checked_python', side_effect=migrate):
                if tampered:
                    with self.assertRaisesRegex(ValueError, '读回校验失败'):
                        release.main()
                    self.assertNotIn('迁移', events)
                else:
                    with self.assertRaisesRegex(RuntimeError, '到达迁移入口'):
                        release.main()
                    self.assertEqual(events, ['停写备份', '加密独立副本', '读回解密', '迁移'])

    def test_migration_requires_verified_readback(self):
        self.exercise()

    def test_failed_readback_blocks_database_changes(self):
        self.exercise(tampered=True)


if __name__ == '__main__':
    unittest.main()
