import Vue from 'vue'
import App from './App'
import store from './store'
const imageUtil = require('./utils/image.js')
imageUtil.bindShopStore(store)
Vue.config.productionTip = false
// #ifdef H5 
window.QQmap = null;
// #endif
// #ifndef MP-TOUTIAO
//网络监听
setTimeout(() => {
	uni.onNetworkStatusChange(function(res) {
		store.commit("networkChange", {
			isConnected: res.isConnected
		})
	});
}, 100)
// #endif

Vue.prototype.$eventHub = Vue.prototype.$eventHub || new Vue()
Vue.prototype.$store = store
Vue.prototype.$imageUrl = function(url) {
	return imageUtil.normalizeImageUrl(url)
}
// 店铺 Logo：由后台「店铺展示设置」控制，未配置时退回本地默认图
Vue.prototype.$shopLogo = imageUtil.shopLogo
Vue.prototype.$setImageFallback = function(item, field) {
	const fallback = imageUtil.shopLogo()
	if (item && item[field] !== fallback) this.$set(item, field, fallback)
}
App.mpType = 'app'

const app = new Vue({
	store,
	...App
})
app.$mount()
