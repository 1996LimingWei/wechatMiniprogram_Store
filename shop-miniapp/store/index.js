import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const store = new Vuex.Store({
	state: {
		shop: { displayName: "陕铺", logoUrl: "", introduction: "", customerService: "" },
		//app版本
		version: "1.0.0",
		//当前是否有网络连接
		networkConnected: true
	},
	mutations: {
        setShop(state, value) { state.shop = Object.assign({}, state.shop, value); },
		networkChange(state, payload) {
			state.networkConnected = payload.isConnected
		}
	}
})

export default store
