from pathlib import Path
import tempfile
import unittest
from backup_retention import plan


class RetentionTests(unittest.TestCase):
    def test_recent_daily_and_last_two_are_kept_and_other_files_untouched(self):
        now = 1800000000
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            ages = [0, 1800, 3600, 49 * 3600, 49 * 3600 + 10, 31 * 86400]
            paths = []
            for age in ages:
                path = root / ('shanpu-%d.enc' % (now - age))
                path.touch()
                paths.append(path)
            (root / 'upgrade-important.enc').touch()
            keep, remove = plan(root, now)
            self.assertEqual(set(keep), set(paths[:4]))
            self.assertEqual(set(remove), set(paths[4:]))
            self.assertTrue((root / 'upgrade-important.enc').exists())

    def test_last_two_survive_even_if_all_backups_are_old(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            for stamp in [1700000000, 1700000001, 1700000002]:
                (root / ('shanpu-%d.enc' % stamp)).touch()
            keep, remove = plan(root, 1800000000)
            self.assertEqual(len(keep), 2)
            self.assertEqual([path.name for path in remove], ['shanpu-1700000000.enc'])


if __name__ == '__main__':
    unittest.main()
