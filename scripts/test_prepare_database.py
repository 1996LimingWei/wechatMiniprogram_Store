import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
from argparse import Namespace

sys.path.insert(0, str(Path(__file__).parent))
import prepare_database as db


class DatabasePreparationTests(unittest.TestCase):
    @patch.dict(os.environ, {'DELIVERY_ADMIN_PASSWORD': 'SafeExamplePassword123!'})
    def test_initialize_refuses_existing_tables_without_writing(self):
        with patch.object(db, 'mysql', return_value=b'2\n'), patch.object(db, 'execute') as execute:
            with self.assertRaises(ValueError):
                db.initialize(Namespace(database='shop'))
            execute.assert_not_called()

    def test_checksum_mismatch_prevents_new_migration_execution(self):
        with patch.object(db, 'migrations', return_value=iter([
                ('20260101_01', 'example', 'CREATE TABLE t(id int);', 'new-checksum')])), \
                patch.object(db, 'mysql', side_effect=[b'1', b'old-checksum']), \
                patch.object(db, 'execute') as execute:
            with self.assertRaises(ValueError):
                db.migrate(Namespace(database='shop'))
            # 唯一允许的写语句是确保迁移历史表存在，不能执行新迁移。
            self.assertEqual(execute.call_count, 1)
            self.assertNotIn('CREATE TABLE t', execute.call_args.args[1])

    def test_matching_migration_is_not_executed_again(self):
        with patch.object(db, 'migrations', return_value=iter([
                ('20260101_01', 'example', 'CREATE TABLE t(id int);', 'same')])), \
                patch.object(db, 'mysql', side_effect=[b'1', b'same']), patch.object(db, 'execute') as execute:
            db.migrate(Namespace(database='shop'))
            self.assertEqual(execute.call_count, 1)

    def test_unknown_database_without_history_is_never_modified(self):
        with patch.object(db, 'mysql', return_value=b'0'), patch.object(db, 'execute') as execute:
            with self.assertRaisesRegex(ValueError, '没有迁移历史'):
                db.migrate(Namespace(database='shop'))
            execute.assert_not_called()

    def test_repository_migrations_have_unique_versions(self):
        versions = [item[0] for item in db.migrations()]
        self.assertEqual(len(versions), len(set(versions)))
        self.assertIn('20261104_02', versions)


if __name__ == '__main__':
    unittest.main()
