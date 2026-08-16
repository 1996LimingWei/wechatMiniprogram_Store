import Vue from 'vue'
import App from './App'
import store from './store'
const imageUtil = require('./utils/image.js')
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
Vue.prototype.$setImageFallback = function(item, field) {
	if (item && item[field] !== imageUtil.FALLBACK_IMAGE) this.$set(item, field, imageUtil.FALLBACK_IMAGE)
}
App.mpType = 'app'

const app = new Vue({
	store,
	...App
})
app.$mount()
