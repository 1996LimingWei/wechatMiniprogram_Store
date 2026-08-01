const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const miniapp = path.join(root, 'shop-miniapp');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/^\uFEFF/, '');
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function compileVueScript(relativePath) {
  const source = read(relativePath);
  const match = source.match(/<script>([\s\S]*?)<\/script>/);
  assert(match, `${relativePath} 缺少脚本块`);
  const script = match[1]
    .replace(/^\s*import\s+.+?;?\s*$/gm, '')
    .replace(/export\s+default/, 'return');
  new Function(script);
}

assert(!fs.existsSync(path.join(miniapp, 'utils', 'mock.js')), '商品内容本地 Mock 文件仍然存在');

const util = read('shop-miniapp/utils/util.js');
assert(!/useMock|handleMock|require\(['"]\.\/mock\.js/.test(util), '请求层仍保留本地 Mock 分支');
assert(/payParam\.mockPay/.test(util), '受控开发支付适配不应被删除');
assert(/服务暂时不可用/.test(util) && /网络不给力/.test(util), '请求层缺少明确错误反馈');

const api = read('shop-miniapp/utils/api.js');
[
  'IndexUrlBanner', 'IndexUrlChannel', 'IndexUrlCategory', 'CatalogList',
  'GoodsList', 'GoodsDetail', 'GoodsRelated', 'SearchIndex', 'SearchHelper',
  'CollectAddOrDelete', 'FootprintRecord', 'CommentList', 'CommentPost'
].forEach(name => assert(new RegExp(`${name}:`).test(api), `正式 API 缺少 ${name}`));

const index = read('shop-miniapp/pages/index/index.vue');
const catalog = read('shop-miniapp/pages/catalog/catalog.vue');
const goods = read('shop-miniapp/pages/goods/goods.vue');
const comment = read('shop-miniapp/pages/comment/comment.vue');
const commentPost = read('shop-miniapp/pages/commentPost/commentPost.vue');

assert(/api\.IndexUrlChannel/.test(index) && /api\.CatalogList/.test(index), '首页频道或分类未读取正式 API');
assert(/item\.iconUrl/.test(index) && /category\.wapBannerUrl/.test(index), '首页仍未展示正式频道或分类内容');
assert(!/api\.Code|menuItems|categoryBanners|showNewUserModal/.test(index), '首页仍保留旧登录或伪造运营内容');
assert(!/quickAddToCart|currentProductId|productId\s*:\s*1/.test(index + catalog + goods), '商品页面仍使用固定 SKU 快捷加购');
assert(!/scienceDb|scienceData|comboData|partnerId/.test(goods), '商品详情仍保留硬编码商品业务数据');
assert(/loadingFailed/.test(goods) && /loadFailed/.test(index + catalog), '核心商品页面缺少接口失败状态');
assert(/goToPost/.test(comment), '商品评论列表缺少发表入口');
assert(!/CommentPost[\s\S]{0,180}application\/json/.test(commentPost), '评论发表仍以 JSON 调用 RequestParam 接口');

const productPages = [
  'pages/index/index.vue', 'pages/catalog/catalog.vue', 'pages/category/category.vue',
  'pages/search/search.vue', 'pages/goods/goods.vue', 'pages/newGoods/newGoods.vue',
  'pages/hotGoods/hotGoods.vue', 'pages/brand/brand.vue', 'pages/brandDetail/brandDetail.vue',
  'pages/topic/topic.vue', 'pages/topicDetail/topicDetail.vue', 'pages/comment/comment.vue',
  'pages/commentPost/commentPost.vue', 'pages/ucenter/collect/collect.vue',
  'pages/ucenter/footprint/footprint.vue'
];

productPages.forEach(relative => {
  const full = `shop-miniapp/${relative}`;
  const source = read(full);
  assert(!/mock\.js|handleMock|useMock/.test(source), `${relative} 仍依赖本地 Mock`);
  const directRequest = source.match(/util\.request\(\s*(['"])/);
  assert(!directRequest, `${relative} 存在未通过 utils/api.js 定义的接口调用`);
  compileVueScript(full);
});

new Function(util);
new Function(api);

const readme = read('shop-miniapp/README.md');
assert(/固定调用.+正式 API/.test(readme), '小程序 README 未说明正式 API 模式');
assert(!/默认启用.+Mock 模式|useMock:\s*true|mock\.js/.test(readme), '小程序 README 仍描述旧 Mock 模式');

console.log('MINIAPP_PRODUCT_API_STATIC_OK');
