import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('preflight', Path(__file__).with_name('release_preflight.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class PreflightTests(unittest.TestCase):
    def config(self):
        return dict(APP_DELIVERY_MODE='catalog', APP_DOMAIN='shop.test.cn',
                    APP_EXTERNAL_BASE_URL='https://shop.test.cn',
                    WEB_CORS_ALLOWED_ORIGIN_PATTERNS='https://shop.test.cn',
                    MYSQL_DATABASE='shop', MYSQL_USER='shop_app', MYSQL_PASSWORD='a' * 24,
                    MYSQL_ROOT_PASSWORD='b' * 24, REDIS_PASSWORD='c' * 24,
                    MATERIAL_PUBLIC_BASE_URL='https://shop.test.cn/uploads/material/')

    def test_catalog_does_not_require_channels(self):
        self.assertEqual(module.validate(self.config()), [])

    def test_commerce_requires_channels(self):
        values = self.config()
        values['APP_DELIVERY_MODE'] = 'commerce'
        self.assertTrue(any('WX_MA_SECRET' in value for value in module.validate(values)))

    def test_weak_password_and_wildcard_rejected(self):
        values = self.config()
        values.update(MYSQL_PASSWORD='root', WEB_CORS_ALLOWED_ORIGIN_PATTERNS='*')
        self.assertGreaterEqual(len(module.validate(values)), 2)

    def test_unknown_mode_rejected(self):
        values = self.config()
        values['APP_DELIVERY_MODE'] = 'demo'
        self.assertTrue(module.validate(values))


if __name__ == '__main__':
    unittest.main()
