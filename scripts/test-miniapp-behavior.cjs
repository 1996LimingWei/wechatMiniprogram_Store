// 加载实际小程序源码，仅替换平台和网络边界，不使用复制的业务实现。
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
function loadUtil() {
  const sandbox = { module: { exports: {} }, require: () => ({ isProduction: true, apiBaseUrl: 'https://example/', validate() {} }),
    uni: { getStorageSync: () => '', showLoading() {}, hideLoading() {}, showToast() {} }, setTimeout, clearTimeout };
  vm.runInNewContext(fs.readFileSync(path.join(root, 'shop-miniapp/utils/util.js'), 'utf8') +
    '\nmodule.exports.__internal = utils;', sandbox);
  return { api: sandbox.module.exports, utils: sandbox.module.exports.__internal, uni: sandbox.uni };
}
function deferred() {
  let resolve, reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}
function loadComment(util = {}, page = 'pages/commentPost/commentPost.vue') {
  const source = fs.readFileSync(path.join(root, 'shop-miniapp', page), 'utf8');
  const sandbox = { module: { exports: {} }, uni: {}, require: name => name.includes('util') ? util : {} };
  vm.runInNewContext(source.match(/<script>([\s\S]*?)<\/script>/)[1].replace('export default', 'module.exports ='), sandbox);
  const component = sandbox.module.exports;
  const state = component.data();
  for (const [key, method] of Object.entries(component.methods)) state[key] = method.bind(state);
  return { state, uni: sandbox.uni, component };
}
test('支付结果页使用的查单方法必须导出', () => {
  assert.equal(typeof loadUtil().api.waitForPaymentResult, 'function');
});
test('预支付网络异常必须结束外层等待', async () => {
  const { api, utils } = loadUtil();
  // 旧代码会产生未处理拒绝；用可控thenable准确验证异常分支是否接入。
  let forwarded = false;
  utils.request = () => ({ then() { return { catch(handler) { forwarded = true; handler(new Error('网络中断')); } }; } });
  const payment = api.payOrder(1);
  const rejection = assert.rejects(payment, /网络中断/);
  assert.equal(forwarded, true);
  await rejection;
});
test('真实支付平台返回成功后仍等待服务端确认', async () => {
  const { api, utils, uni } = loadUtil();
  utils.request = async () => ({ code: 0, data: { timeStamp: '1' } });
  let checked = false;
  utils.waitForPaymentResult = async id => { assert.equal(id, 10); checked = true; return { orderStatus: 'paid' }; };
  uni.requestPayment = options => options.success();
  await api.payOrder(10);
  assert.equal(checked, true);
});
test('正式环境拒绝模拟支付', async () => {
  const { api, utils } = loadUtil();
  utils.toast = () => {};
  utils.request = async () => ({ code: 0, data: { mockPay: true } });
  await assert.rejects(api.payOrder(10), error => error.errMsg === 'requestPayment:mock-disabled');
});
test('所有评价图片上传完成前不得提交', async () => {
  const uploads = [deferred(), deferred()]; let index = 0, posts = 0;
  const { state } = loadComment({ uploadFile: () => uploads[index++].promise, toast() {},
    request: () => { posts++; return Promise.resolve({ code: 1 }); } });
  state.content = '评价';
  state.uploadOne('a'); state.uploadOne('b');
  uploads[0].resolve({ data: { url: 'https://example/a' } });
  await new Promise(setImmediate);
  state.onPost();
  assert.equal(posts, 0);
  uploads[1].resolve({ data: { url: 'https://example/b' } });
  await new Promise(setImmediate);
  state.onPost();
  assert.equal(posts, 1);
});
test('上传中再次选图不得超过四张', () => {
  const pending = deferred();
  const { state, uni } = loadComment({ uploadFile: () => pending.promise, toast() {} });
  state.picList = ['a', 'b', 'c'];
  state.uploadOne('d');
  let pickerCalled = false;
  uni.chooseImage = () => { pickerCalled = true; };
  state.chooseImage();
  assert.equal(pickerCalled, false);
});
test('所有注册页面使用的公共工具方法均存在', () => {
  const api = loadUtil().api;
  const pages = JSON.parse(fs.readFileSync(path.join(root, 'shop-miniapp/pages.json'), 'utf8')).pages;
  for (const page of pages) {
    const source = fs.readFileSync(path.join(root, 'shop-miniapp', page.path + '.vue'), 'utf8');
    const script = source.match(/<script[^>]*>([\s\S]*?)<\/script>/)?.[1] || '';
    for (const match of script.matchAll(/\butil\.([A-Za-z_]\w*)\s*\(/g)) {
      assert.equal(typeof api[match[1]], 'function', page.path + ': util.' + match[1]);
    }
  }
});
test('上传返回非JSON必须拒绝并结束加载', async () => {
  const { api, utils, uni } = loadUtil();
  uni.uploadFile = options => {
    options.success({ statusCode: 502, data: '<html>网关错误</html>' });
    options.complete();
  };
  await assert.rejects(api.uploadFile('file/upload', 'a.png'), error => error.code === 500);
  assert.equal(utils.loadingCount, 0);
});
test('上传网络中断必须结束加载', async () => {
  const { api, utils, uni } = loadUtil();
  uni.uploadFile = options => { options.fail({ errMsg: '断网' }); options.complete(); };
  await assert.rejects(api.uploadFile('file/upload', 'a.png'));
  assert.equal(utils.loadingCount, 0);
});
test('HTTP失败不得因响应体code为0被视为成功', async () => {
  const { api, utils, uni } = loadUtil();
  uni.request = options => { options.success({ statusCode: 500, data: { code: 0, data: {} } }); options.complete(); };
  await assert.rejects(api.request('order/submit'), error => error.code === 500);
  assert.equal(utils.loadingCount, 0);
});
test('评价提交防重复且失败后可重试', async () => {
  let posts = 0; const request = deferred();
  const { state } = loadComment({ toast() {}, request() { posts++; return request.promise; } });
  state.content = '评价';
  state.onPost(); state.onPost();
  assert.equal(posts, 1);
  request.reject(new Error('断网'));
  await new Promise(setImmediate);
  assert.equal(state.posting, false);
});
test('主动取消支付与结果待确认分别提示', () => {
  const { api } = loadUtil();
  assert.match(api.paymentErrorMessage({ errMsg: 'requestPayment:fail cancel' }), /已取消支付/);
  assert.equal(api.paymentErrorMessage({ pending: true }), '支付结果确认中');
});
test('支付结果页不接受URL参数作为成功依据', async () => {
  const query = deferred();
  const { state, component } = loadComment({ waitForPaymentResult: () => query.promise, toast() {} }, 'pages/payResult/payResult.vue');
  component.onLoad.call(state, { status: '1', orderId: '123' });
  assert.equal(state.status, 'pending');
  query.resolve({ orderStatus: 'paid' });
  await new Promise(setImmediate);
  assert.equal(state.status, 'success');
});
test('缺失订单号不得显示支付成功', () => {
  const { state, component } = loadComment({ toast() {} }, 'pages/payResult/payResult.vue');
  component.onLoad.call(state, { status: '1' });
  assert.equal(state.status, 'failed');
});
test('编辑购物车时全选可以再次取消全部失效商品', () => {
  const { state } = loadComment({}, 'pages/cart/cart.vue');
  state.isEditCart = true;
  state.cartGoods = [{ available: false, checked: false, number: 1 }];
  state.checkedAll();
  assert.equal(state.cartGoods[0].checked, true);
  state.checkedAll();
  assert.equal(state.cartGoods[0].checked, false);
});
test('实际请求工具的网络失败传播到支付调用方', { timeout: 1000 }, async () => {
  const { api, uni } = loadUtil();
  uni.request = options => { options.fail({ errMsg: 'request:fail timeout' }); options.complete(); };
  await assert.rejects(api.payOrder(12), error => error.errMsg === 'request:fail timeout');
});
test('查单只有服务端paid才成功，未确认返回pending', async () => {
  const { api, utils } = loadUtil();
  utils.request = async () => ({ code: 0, data: { orderStatus: 'unpaid' } });
  await assert.rejects(api.waitForPaymentResult(12, 1), error => error.pending === true);
  utils.request = async () => ({ code: 0, data: { orderStatus: 'paid' } });
  assert.equal((await api.waitForPaymentResult(12, 1)).orderStatus, 'paid');
});
test('并发401只刷新一次并使用新Token重试', async () => {
  const { api, uni } = loadUtil();
  let token = 'old', refreshes = 0, retries = 0, refreshOptions;
  uni.getStorageSync = () => token;
  uni.setStorageSync = (key, value) => { if (key === 'token') token = value; };
  uni.request = options => {
    if (options.url.endsWith('auth/refresh-token')) { refreshes++; refreshOptions = options; return; }
    if (options.header.Authorization === 'Bearer old') options.success({ statusCode: 401, data: { code: 401 } });
    else { retries++; options.success({ statusCode: 200, data: { code: 0 } }); }
    options.complete();
  };
  const requests = [api.request('order/list'), api.request('address/list')];
  assert.equal(refreshes, 1);
  refreshOptions.success({ statusCode: 200, data: { code: 0, data: { token: 'new' } } });
  await Promise.all(requests);
  assert.equal(retries, 2);
});
test('购物车接受服务端零元包邮门槛且进度不为NaN', async () => {
  const { state, component } = loadComment({ request: async () => ({ code: 0,
    data: { cartList: [], cartTotal: { checkedGoodsAmount: '0.00' }, freeFreightThreshold: '0.00' } }) }, 'pages/cart/cart.vue');
  state.getCartList();
  await new Promise(setImmediate);
  assert.equal(state.freeShippingThreshold, 0);
  assert.equal(component.computed.shippingPercent.call(state), 100);
});
