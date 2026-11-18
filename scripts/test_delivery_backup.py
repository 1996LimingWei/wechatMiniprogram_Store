import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
from unittest.mock import patch
from argparse import Namespace
from subprocess import CompletedProcess

spec = importlib.util.spec_from_file_location('delivery_backup', Path(__file__).with_name('delivery_backup.py'))
backup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)


class ArchiveTests(unittest.TestCase):
    def run_backup(self, keep_stopped=False, collision=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'deploy/data/material').mkdir(parents=True)
            args = Namespace(database='shop', archive=root / 'test.zip', keep_stopped=keep_stopped)

            def docker(_args, *command, **kwargs):
                if command[0] == 'exec':
                    kwargs['stdout'].write(b'SELECT 1;')
                return CompletedProcess(command, 0, stdout=b'backend\n')

            real_link = backup.os.link

            def publish(source, destination):
                if collision:
                    destination.write_bytes(b'other-backup')
                return real_link(source, destination)

            with patch.object(backup, 'ROOT', root), patch.object(backup, 'compose', side_effect=docker) as calls, \
                    patch.object(backup.os, 'link', side_effect=publish):
                if collision:
                    with self.assertRaises(FileExistsError):
                        backup.backup(args)
                    self.assertEqual(args.archive.read_bytes(), b'other-backup')
                else:
                    backup.backup(args)
                    backup.verify_archive(args.archive, root / 'verify')
                starts = [call for call in calls.call_args_list if call.args[1:3] == ('start', 'backend')]
                self.assertEqual(len(starts), 0 if keep_stopped else 1)
                self.assertEqual(list(root.glob('*.partial')), [])

    def test_regular_backup_restores_service(self):
        self.run_backup()

    def test_upgrade_backup_keeps_service_stopped(self):
        self.run_backup(keep_stopped=True)

    def test_parallel_archive_is_never_overwritten(self):
        self.run_backup(collision=True)

    def test_unwritable_archive_directory_rejected_before_stopping_backend(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'deploy/data/material').mkdir(parents=True)
            args = Namespace(database='shop', archive=root / 'backups/test.zip')
            with patch.object(backup, 'ROOT', root), patch.object(backup.os, 'access', return_value=False), patch.object(backup, 'compose') as docker:
                with self.assertRaisesRegex(ValueError, '备份目录不可写'):
                    backup.backup(args)
                docker.assert_not_called()

    def write(self, root, files, hashes=None):
        archive = root / 'backup.zip'
        manifest = {'format': 1, 'files': hashes or {
            name: hashlib.sha256(value).hexdigest() for name, value in files.items()}}
        with zipfile.ZipFile(archive, 'w') as out:
            for name, value in files.items():
                out.writestr(name, value)
            out.writestr('manifest.json', json.dumps(manifest))
        return archive

    def test_roundtrip_database_and_images(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = self.write(root, {'database.sql': b'SELECT 1;', 'material/2026/logo.png': b'png'})
            target = root / 'restored'
            backup.verify_archive(source, target)
            self.assertEqual((target / 'material/2026/logo.png').read_bytes(), b'png')

    def test_tampered_content_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = self.write(root, {'database.sql': b'changed'}, {'database.sql': '0' * 64})
            with self.assertRaises(ValueError):
                backup.verify_archive(source, root / 'restored')

    def test_path_traversal_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = self.write(root, {'database.sql': b'sql', 'material/../../escape': b'bad'})
            with self.assertRaises(ValueError):
                backup.verify_archive(source, root / 'restored')
            self.assertFalse((root / 'escape').exists())

    def test_production_restore_rejected_before_any_command(self):
        from argparse import Namespace
        with self.assertRaises(ValueError):
            backup.restore(Namespace(database='shop'))

    def test_invalid_database_identifiers(self):
        for name in ['shop; DROP DATABASE shop', '../shop', 'shop`']:
            with self.assertRaises(ValueError):
                backup.validate_database(name)


if __name__ == '__main__':
    unittest.main()
