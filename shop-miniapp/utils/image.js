const DEFAULT_LOGO = '/static/images/logo.png';

let shopStore = null;

/** 由 main.js 注入 Vuex store，供本模块读取后台配置的店铺 Logo */
function bindShopStore(store) {
  shopStore = store;
}

/**
 * 店铺 Logo：优先取后台「系统管理 → 店铺展示设置」配置的地址，
 * 未配置或接口拉取失败时退回本地默认图，保证任何情况下都有图。
 */
function shopLogo() {
  const shop = shopStore && shopStore.state && shopStore.state.shop;
  const url = shop && typeof shop.logoUrl === 'string' ? shop.logoUrl.trim() : '';
  return url || DEFAULT_LOGO;
}

function isTemporaryImageUrl(url) {
  return /^wxfile:\/\//i.test(url)
    || /^file:\/\//i.test(url)
    || /^http:\/\/tmp\//i.test(url)
    || /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/(?!uploads\/material\/)/i.test(url);
}

function normalizeImageUrl(url) {
  if (!url || typeof url !== 'string') return shopLogo();
  const trimmed = url.trim();
  if (!trimmed || isTemporaryImageUrl(trimmed)) return shopLogo();
  return trimmed;
}

module.exports = {
  DEFAULT_LOGO,
  bindShopStore,
  shopLogo,
  normalizeImageUrl
};

// 兼容历史页面：多处直接读 imageUtil.FALLBACK_IMAGE 做加载失败兜底。
// 用 getter 让它在每次读取时跟随店铺 Logo，无需逐个页面改造。
Object.defineProperty(module.exports, 'FALLBACK_IMAGE', {
  enumerable: true,
  get: shopLogo
});
