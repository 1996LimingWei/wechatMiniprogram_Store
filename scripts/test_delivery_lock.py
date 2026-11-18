import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import delivery_lock


@unittest.skipUnless(os.name == 'posix', 'Linux运维互斥在CI执行')
class DeliveryLockTests(unittest.TestCase):
    def test_overlapping_operation_refused_and_exception_releases_lock(self):
        with tempfile.TemporaryDirectory() as temporary, patch.object(delivery_lock, 'ROOT', Path(temporary)):
            with self.assertRaisesRegex(RuntimeError, '故障演练'):
                with delivery_lock.operation_lock():
                    with self.assertRaisesRegex(ValueError, '已有部署'):
                        with delivery_lock.operation_lock():
                            self.fail('并发运维不应进入')
                    raise RuntimeError('故障演练')
            with delivery_lock.operation_lock():
                pass


if __name__ == '__main__':
    unittest.main()
