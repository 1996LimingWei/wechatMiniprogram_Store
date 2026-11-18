import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from release_images import load_bundle
from delivery_backup import digest


class ReleaseImageIntegrityTests(unittest.TestCase):
    def check_rejected(self, change, message):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'images.tar').write_bytes(b'isolated image fixture')
            manifest = {'format': 1, 'commit': 'a' * 40,
                        'images': {name: 'sha256:' + 'b' * 64 for name in ['backend', 'admin', 'mysql', 'redis', 'nginx']},
                        'archive_sha256': digest(root / 'images.tar')}
            change(manifest)
            (root / 'manifest.json').write_text(json.dumps(manifest), encoding='utf-8')
            with patch('release_images.commit', return_value='a' * 40), patch('release_images.subprocess.run') as docker:
                with self.assertRaisesRegex(ValueError, message):
                    load_bundle(root)
                docker.assert_not_called()

    def test_modified_archive_rejected_before_docker(self):
        self.check_rejected(lambda manifest: manifest.update(archive_sha256='0' * 64), '校验失败')

    def test_other_source_version_rejected_before_docker(self):
        self.check_rejected(lambda manifest: manifest.update(commit='c' * 40), '版本不匹配')

    def test_mutable_tag_rejected_before_docker(self):
        self.check_rejected(lambda manifest: manifest['images'].update(backend='shanpu/backend:latest'), '版本不匹配')


if __name__ == '__main__':
    unittest.main()
