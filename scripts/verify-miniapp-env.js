const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const path = require('node:path');
const modulePath = path.resolve(__dirname, '../shop-miniapp/config/env.js');
for (const value of ['https://shop.example.com', 'https://shop.example.com/', 'https://shop.example.com/app-api/']) {
  const actual = execFileSync(process.execPath, ['-e', `process.stdout.write(require(${JSON.stringify(modulePath)}).apiBaseUrl)`], {
    env: { ...process.env, VUE_APP_ENV: 'production', VUE_APP_API_BASE_URL: value }
  }).toString();
  assert.equal(actual, 'https://shop.example.com/');
}
for (const value of ['', 'http://shop.example.com']) {
  assert.throws(() => execFileSync(process.execPath, ['-e', `require(${JSON.stringify(modulePath)}).validate()`], {
    env: { ...process.env, VUE_APP_ENV: 'production', VUE_APP_API_BASE_URL: value, VUE_APP_PROD_API_BASE_URL: '' },
    stdio: 'pipe'
  }));
}
console.log('小程序环境验证通过：3 个地址规范化及 2 个非法配置拒绝用例。');
