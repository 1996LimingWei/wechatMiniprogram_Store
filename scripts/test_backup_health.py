import unittest
from check_backup_health import healthy


class BackupHealthTests(unittest.TestCase):
    def test_successful_fresh_backup(self):
        self.assertTrue(healthy({'success': True, 'time': 10000}, 10001))

    def test_stale_missing_failed_or_future_backup(self):
        for state in [{}, {'success': False, 'time': 10000}, {'success': True, 'time': 1},
                      {'success': True, 'time': 20000}]:
            self.assertFalse(healthy(state, 10000))


if __name__ == '__main__':
    unittest.main()
