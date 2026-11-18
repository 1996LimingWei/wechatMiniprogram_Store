// 微信开发者工具前端空态烟测；网络使用显式测试夹具，不作为真实接口验收。
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const sdkPath = process.argv[2];
if (!sdkPath) throw new Error('请提供已安装的miniprogram-automator模块绝对路径');
const automator = require(path.resolve(sdkPath));
const output = path.resolve(__dirname, '../deploy/reviews/miniapp-simulator');
fs.mkdirSync(output, { recursive: true });
let miniProgram;
const results = [], exceptions = [];
const watchdog = setTimeout(async () => {
  fs.writeFileSync(path.join(output, 'report.json'), JSON.stringify({ complete: false, results, reason: '自动化运行超时' }, null, 2));
  if (miniProgram) {
    await Promise.race([miniProgram.restoreWxMethod('request').catch(() => {}),
      new Promise(resolve => setTimeout(resolve, 2000))]);
    await miniProgram.disconnect();
  }
  process.exit(2);
}, 90000);
(async () => {
  try {
    miniProgram = await automator.connect({ wsEndpoint: 'ws://127.0.0.1:9420' });
    miniProgram.on('exception', () => exceptions.push('模拟器脚本异常'));
    await miniProgram.mockWxMethod('request', function (options) {
      const data = { banner: [], channel: [], brandList: [], topicList: [], newGoodsList: [], hotGoodsList: [],
        categoryList: [], goodsList: { records: [], current: 1, pages: 0, total: 0 },
        hotKeywordList: [], historyKeywordList: [], defaultKeyword: {},
        displayName: '陕铺测试夹具', logoUrl: '', introduction: '', customerService: '' };
      const response = { statusCode: 200, data: { code: 0, data, msg: '测试夹具' } };
      return response;
    });
    for (const route of ['pages/index/index', 'pages/newGoods/newGoods', 'pages/hotGoods/hotGoods',
      'pages/legal/privacy/privacy', 'pages/payResult/payResult']) {
      await miniProgram.reLaunch('/' + route);
      const state = await miniProgram.evaluate(function () {
        const pages = getCurrentPages(); const page = pages[pages.length - 1];
        return { route: page.route, hasVue: !!page.$vm, status: page.$vm && page.$vm.status,
          loadFailed: page.$vm && page.$vm.loadFailed };
      });
      assert.equal(state.route, route);
      assert.equal(state.hasVue, true);
      if (route === 'pages/index/index') assert.equal(state.loadFailed, false);
      if (route === 'pages/payResult/payResult') assert.equal(state.status, 'failed');
      results.push({ route, passed: true });
    }
    assert.equal(exceptions.length, 0, '模拟器出现脚本异常');
    console.log('模拟器前端测试夹具烟测通过：' + results.length + '页；不代表真实后端或真机通过。');
  } finally {
    if (miniProgram) {
      await miniProgram.restoreWxMethod('request').catch(() => {});
      await miniProgram.reLaunch('/pages/index/index').catch(() => {});
      await miniProgram.disconnect();
    }
    clearTimeout(watchdog);
    fs.writeFileSync(path.join(output, 'report.json'), JSON.stringify({
      complete: results.length === 5 && exceptions.length === 0, results, exceptions,
      scope: '微信开发者工具前端测试夹具烟测，非真实后端/资金/真机验收'
    }, null, 2));
  }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
