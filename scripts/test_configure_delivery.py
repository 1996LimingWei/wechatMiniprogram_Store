from pathlib import Path
import tempfile
import unittest
from configure_delivery import generate
from release_preflight import read_env, validate


class ConfigureDeliveryTests(unittest.TestCase):
    def test_generated_catalog_configuration_passes_preflight_and_is_unique(self):
        with tempfile.TemporaryDirectory() as tmp:
            first, second = Path(tmp) / 'first', Path(tmp) / 'second'
            generate('shop.customer-test.cn', first)
            generate('shop.customer-test.cn', second)
            a, b = read_env(first), read_env(second)
            self.assertEqual(validate(a), [])
            self.assertNotEqual(a['MYSQL_PASSWORD'], b['MYSQL_PASSWORD'])
            self.assertGreaterEqual(len(a['DELIVERY_BACKUP_PASSWORD']), 20)
            with self.assertRaises(ValueError):
                generate('shop.customer-test.cn', first)

    def test_url_or_placeholder_domain_is_rejected(self):
        for domain in ['https://store.cn', 'shop.example.com', '../shop.cn']:
            with self.assertRaises(ValueError):
                generate(domain, Path('must-not-be-created'))


if __name__ == '__main__':
    unittest.main()
