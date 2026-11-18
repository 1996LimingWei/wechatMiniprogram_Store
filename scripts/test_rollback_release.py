import unittest
from rollback_release import validate_schema


class RollbackSchemaTests(unittest.TestCase):
    def test_same_schema_allowed(self):
        validate_schema({'20261104_02': 'hash'}, {'20261104_02': 'hash'})

    def test_added_changed_or_unknown_schema_rejected(self):
        for actual in [{}, {'20261104_02': 'changed'}, {'20261104_02': 'hash', 'new': 'hash'}]:
            with self.assertRaises(ValueError):
                validate_schema({'20261104_02': 'hash'}, actual)
        with self.assertRaises(ValueError):
            validate_schema({}, {})


if __name__ == '__main__':
    unittest.main()
