const env = require('../config/env.js');

const utils = {
	//接口地址
	interfaceUrl: function () {
		env.validate();
		return env.apiBaseUrl + 'app-api/'
	},
	toast: function (text, duration, success) {
		uni.showToast({
			title: text || "出错啦~",
			icon: success || 'none',
			duration: duration || 2000
		})
	},
	modal: function (title, content, showCancel = false, callback, confirmColor, confirmText, cancelColor, cancelText) {
		uni.showModal({
			title: title || '提示',
			content: content,
			showCancel: showCancel,
			cancelColor: cancelColor || "#555",
			confirmColor: confirmColor || "#5B8C5A",
			confirmText: confirmText || "确定",
			cancelText: cancelText || "取消",
			success(res) {
				if (res.confirm) {
					callback && callback(true)
				} else {
					callback && callback(false)
				}
			}
		})
	},
	isAndroid: function () {
		const res = uni.getSystemInfoSync();
		return res.platform.toLocaleLowerCase() == "android"
	},
	isIphoneX: function () {
		const res = uni.getSystemInfoSync();
		let iphonex = false;
		let models = ['iphonex', 'iphonexr', 'iphonexsmax', 'iphone11', 'iphone11pro', 'iphone11promax']
		const model = res.model.replace(/\s/g, "").toLowerCase()
		if (models.includes(model)) {
			iphonex = true;
		}
		return iphonex;
	},
	constNum: function () {
		let time = 0;
		// #ifdef APP-PLUS
		time = this.isAndroid() ? 300 : 0;
		// #endif
		return time
	},
	delayed: null,
	refreshPromise: null,
	/**
	 * 请求数据处理
	 * @param string url 请求地址
	 * @param {*} postData 请求参数
	 * @param string method 请求方式
	 *  GET or POST
	 * @param string contentType 数据格式
	 *  'application/x-www-form-urlencoded'
	 *  'application/json'
	 * @param bool isDelay 是否延迟显示loading
	 * @param bool hideLoading 是否隐藏loading
	 *  true: 隐藏
	 *  false:显示
	 */
	request: function (url, postData = {}, method = "POST", contentType = "application/x-www-form-urlencoded", isDelay, hideLoading, hasRetried = false) {
		//接口请求
		let loadding = false;
		utils.delayed && uni.hideLoading();
		clearTimeout(utils.delayed);
		utils.delayed = null;
		if (!hideLoading) {
			utils.delayed = setTimeout(() => {
				uni.showLoading({
					mask: true,
					title: '请稍候...',
					success(res) {
						loadding = true
					}
				})
			}, isDelay ? 1000 : 0)
		}

		return new Promise((resolve, reject) => {
			const token = utils.getToken();
			const headers = {
				'content-type': contentType
			};
			if (token) {
				headers.Authorization = 'Bearer ' + token;
			}
			uni.request({
				url: utils.interfaceUrl() + url,
				data: postData,
				header: headers,
				method: method, //'GET','POST'
				timeout: 15000,
				success: (res) => {
					if (loadding && !hideLoading) {
						uni.hideLoading()
					}
					if (res.statusCode !== 200) {
						utils.toast('服务暂时不可用，请稍后再试')
						reject(res)
						return
					}
					const data = res.data || { code: 500, msg: '接口返回为空' }
					if (data.code === 401) {
							// 尝试刷新 Token
							const oldToken = utils.getToken();
							if (oldToken && !hasRetried && !url.includes('auth/refresh-token')) {
								utils._refreshToken(oldToken).then(newToken => {
									if (newToken) {
										// 用新 token 重发原请求
										utils.request(url, postData, method, contentType, isDelay, hideLoading, true).then(resolve).catch(reject);
									} else {
										utils._handleUnauthorized();
										resolve(data);
									}
								}).catch(() => {
									utils._handleUnauthorized();
									resolve(data);
								});
							} else {
								utils._handleUnauthorized();
								resolve(data);
							}
						} else {
							if (data.code !== 0) utils.toast(data.msg || '请求失败')
							resolve(data);
						}
				},
				fail: (res) => {
					utils.toast("网络不给力，请稍后再试~")
					reject(res)
				},
				complete: function () {
					clearTimeout(utils.delayed)
					utils.delayed = null;
					uni.hideLoading()
				}
			})
		})
	},
	/**
	 * 上传文件
	 * @param string url 请求地址
	 * @param string src 文件路径
	 */
	uploadFile: function (url, src) {
		uni.showLoading({
			title: '请稍候...'
		})
		return new Promise((resolve, reject) => {
			const uploadTask = uni.uploadFile({
				url: utils.interfaceUrl() + url,
				filePath: src,
				name: 'file',
				header: {
					'content-type': 'multipart/form-data',
					'Authorization': 'Bearer ' + utils.getToken()
				},
				success: function (res) {
					uni.hideLoading()
					let data = JSON.parse(res.data.replace(/\ufeff/g, "") || "{}")
					if (data.code == 0) {
						//返回图片地址
						resolve(data)
					} else {
						utils.toast(data.msg || '上传失败');
						reject(data);
					}
				},
				fail: function (res) {
					utils.toast("网络不给力，请稍后再试~")
					reject(res)
				}
			})
		})
	},
	tuiJsonp: function (url, callback, callbackname) {
		// #ifdef H5
		window[callbackname] = callback;
		let tuiScript = document.createElement("script");
		tuiScript.src = url;
		tuiScript.type = "text/javascript";
		document.head.appendChild(tuiScript);
		document.head.removeChild(tuiScript);
		// #endif
	},
	//设置用户信息
	setUserInfo: function (mobile, token) {
		uni.setStorageSync("token", token)
		uni.setStorageSync("mobile", mobile)
	},
	//获取token
	getToken: function () {
		return uni.getStorageSync("token")
	},
	//去空格
	trim: function (value) {
		return value.replace(/(^\s*)|(\s*$)/g, "");
	},
	//内容替换
	replaceAll: function (text, repstr, newstr) {
		return text.replace(new RegExp(repstr, "gm"), newstr);
	},
	//格式化手机号码
	formatNumber: function (num) {
		return num.length === 11 ? num.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : num;
	},
	//金额格式化
	rmoney: function (money) {
		return parseFloat(money).toFixed(2).toString().split('').reverse().join('').replace(/(\d{3})/g, '$1,').replace(
			/\,$/, '').split('').reverse().join('');
	},
	// 时间格式化输出，如11:03 25:19 每1s都会调用一次
	dateformat: function (micro_second) {
		// 总秒数
		var second = Math.floor(micro_second / 1000);
		// 天数
		var day = Math.floor(second / 3600 / 24);
		// 小时
		var hr = Math.floor(second / 3600 % 24);
		// 分钟
		var min = Math.floor(second / 60 % 60);
		// 秒
		var sec = Math.floor(second % 60);
		return {
			day,
			hr: hr < 10 ? '0' + hr : hr,
			min: min < 10 ? '0' + min : min,
			sec: sec < 10 ? '0' + sec : sec,
			second: second
		}
	},
	//日期格式化
	formatDate: function (formatStr, fdate) {
		if (fdate) {
			if (~fdate.indexOf('.')) {
				fdate = fdate.substring(0, fdate.indexOf('.'));
			}
			fdate = fdate.toString().replace('T', ' ').replace(/\-/g, '/');
			var fTime, fStr = 'ymdhis';
			if (!formatStr)
				formatStr = "y-m-d h:i:s";
			if (fdate)
				fTime = new Date(fdate);
			else
				fTime = new Date();
			var month = fTime.getMonth() + 1;
			var day = fTime.getDate();
			var hours = fTime.getHours();
			var minu = fTime.getMinutes();
			var second = fTime.getSeconds();
			month = month < 10 ? '0' + month : month;
			day = day < 10 ? '0' + day : day;
			hours = hours < 10 ? ('0' + hours) : hours;
			minu = minu < 10 ? '0' + minu : minu;
			second = second < 10 ? '0' + second : second;
			var formatArr = [
				fTime.getFullYear().toString(),
				month.toString(),
				day.toString(),
				hours.toString(),
				minu.toString(),
				second.toString()
			]
			for (var i = 0; i < formatArr.length; i++) {
				formatStr = formatStr.replace(fStr.charAt(i), formatArr[i]);
			}
			return formatStr;
		} else {
			return "";
		}
	},
	getDistance: function (lat1, lng1, lat2, lng2) {
		function Rad(d) {
			return d * Math.PI / 180.0;
		}
		if (!lat1 || !lng1) {
			return '';
		}
		// lat1用户的纬度
		// lng1用户的经度
		// lat2商家的纬度
		// lng2商家的经度
		let radLat1 = Rad(lat1);
		let radLat2 = Rad(lat2);
		let a = radLat1 - radLat2;
		let b = Rad(lng1) - Rad(lng2);
		let s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(
			Math.sin(b / 2), 2)));
		s = s * 6378.137;
		s = Math.round(s * 10000) / 10000;
		s = '(距您' + s.toFixed(2) + '公里)' //保留两位小数
		return s
	},
	isMobile: function (mobile) {
		if (!mobile) {
			utils.toast('请输入手机号码');
			return false
		}
		if (!mobile.match(/^1[3-9][0-9]\d{8}$/)) {
			utils.toast('手机号不正确');
			return false
		}
		return true
	},
	rgbToHex: function (r, g, b) {
		return "#" + utils.toHex(r) + utils.toHex(g) + utils.toHex(b)
	},
	toHex: function (n) {
		n = parseInt(n, 10);
		if (isNaN(n)) return "00";
		n = Math.max(0, Math.min(n, 255));
		return "0123456789ABCDEF".charAt((n - n % 16) / 16) +
			"0123456789ABCDEF".charAt(n % 16);
	},
	hexToRgb(hex) {
		let result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
		return result ? {
			r: parseInt(result[1], 16),
			g: parseInt(result[2], 16),
			b: parseInt(result[3], 16)
		} : null;
	},
	transDate: function (date, fmt) {
		if (!date) {
			return '--'
		}
		let _this = new Date(date * 1000)
		let o = {
			'M+': _this.getMonth() + 1,
			'd+': _this.getDate(),
			'h+': _this.getHours(),
			'm+': _this.getMinutes(),
			's+': _this.getSeconds(),
			'q+': Math.floor((_this.getMonth() + 3) / 3),
			'S': _this.getMilliseconds()
		}
		if (/(y+)/.test(fmt)) {
			fmt = fmt.replace(RegExp.$1, (_this.getFullYear() + '').substr(4 - RegExp.$1.length))
		}
		for (let k in o) {
			if (new RegExp('(' + k + ')').test(fmt)) {
				fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (('00' + o[k]).substr(('' + o[k]).length)))
			}
		}
		return fmt
	},
	isNumber: function (val) {
		let regPos = /^\d+(\.\d+)?$/; //非负浮点数
		let regNeg = /^(-(([0-9]+\.[0-9]*[1-9][0-9]*)|([0-9]*[1-9][0-9]*\.[0-9]+)|([0-9]*[1-9][0-9]*)))$/; //负浮点数
		if (regPos.test(val) || regNeg.test(val)) {
			return true;
		} else {
			return false;
		}
	},
	//判断字符串是否为空
	isEmpty: function (str) {
		if (str === '' || str === undefined || str === null) {
			return true;
		} else {
			return false;
		}
	},
	expireTime: function (str) {
		if (!str) {
			return;
		}
		let NowTime = new Date().getTime();
		//IOS系统直接使用new Date('2018-10-29 11:25:21')，在IOS上获取不到对应的时间对象。
		let totalSecond = Date.parse(str.replace(/-/g, '/')) - NowTime || [];
		if (totalSecond < 0) {
			return;
		}
		return totalSecond / 1000
	},

	/**
	 * 统一下单请求
	 */
	payOrder: function (orderId) {
		return new Promise(function (resolve, reject) {
			utils.request('pay/prepay', {
				orderId: orderId
			}, 'POST').then((res) => {
				if (res.code === 0) {
					let payParam = res.data;
					if (payParam.mockPay) {
						const app = getApp();
						app.globalData._payResolve = resolve;
						app.globalData._payReject = reject;
						app.globalData._payAmount = payParam.amount || app.globalData._payAmount || '0.00';
						uni.navigateTo({
							url: '/pages/payMock/payMock?orderId=' + orderId + '&amount=' + app.globalData._payAmount
						});
						return;
					}
					uni.requestPayment({
						'timeStamp': payParam.timeStamp,
						'nonceStr': payParam.nonceStr,
						'package': payParam.package,
						'signType': payParam.signType,
						'paySign': payParam.paySign,
						'success': function (res) {
							console.log(res)
							resolve(res);
						},
						'fail': function (res) {
							console.log(res)
							reject(res);
						},
						'complete': function (res) {
							console.log(res)
							reject(res);
						}
					});
				} else {
					reject(res);
				}
			});
		});
	},

	/**
	 * 调用微信登录
	 */
	login: function () {
		return new Promise(function (resolve, reject) {
			uni.login({
				success: function (res) {
					if (res.code) {
						resolve(res);
					} else {
						reject(res);
					}
				},
				fail: function (err) {
					reject(err);
				}
			});
		});
	},

	/**
	 * 刷新 Token（内部方法，由 401 拦截器调用）
	 */
	_refreshToken: function (oldToken) {
		if (utils.refreshPromise) {
			return utils.refreshPromise;
		}
		utils.refreshPromise = new Promise((resolve) => {
			uni.request({
				url: utils.interfaceUrl() + 'auth/refresh-token',
				method: 'POST',
				data: { token: oldToken },
				header: { 'content-type': 'application/json' },
				success: (res) => {
					if (res.statusCode === 200 && res.data && res.data.code === 0 && res.data.data && res.data.data.token) {
						const newToken = res.data.data.token;
						uni.setStorageSync('token', newToken);
						resolve(newToken);
					} else {
						resolve(null);
					}
				},
				fail: () => {
					resolve(null);
				}
			});
		});
		utils.refreshPromise.then(() => {
			utils.refreshPromise = null;
		}, () => {
			utils.refreshPromise = null;
		});
		return utils.refreshPromise;
	},

	/**
	 * 未授权处理：清除 token 并跳转登录页
	 */
	_handleUnauthorized: function () {
		uni.removeStorageSync('token');
		uni.removeStorageSync('userId');
		utils.modal('温馨提示', '您还没有登录，是否去登录', true, (confirm) => {
			if (confirm) {
				uni.redirectTo({ url: '/pages/auth/btnAuth/btnAuth' });
			} else {
				uni.navigateBack({
					delta: 1,
					fail: () => {
						uni.switchTab({ url: '/pages/index/index' });
					}
				});
			}
		});
	}
}

module.exports = {
	interfaceUrl: utils.interfaceUrl,
	toast: utils.toast,
	modal: utils.modal,
	isAndroid: utils.isAndroid,
	isIphoneX: utils.isIphoneX,
	constNum: utils.constNum,
	request: utils.request,
	uploadFile: utils.uploadFile,
	tuiJsonp: utils.tuiJsonp,
	setUserInfo: utils.setUserInfo,
	getToken: utils.getToken,
	trim: utils.trim,
	replaceAll: utils.replaceAll,
	formatNumber: utils.formatNumber,
	rmoney: utils.rmoney,
	dateformat: utils.dateformat,
	formatDate: utils.formatDate,
	getDistance: utils.getDistance,
	isMobile: utils.isMobile,
	rgbToHex: utils.rgbToHex,
	hexToRgb: utils.hexToRgb,
	transDate: utils.transDate,
	isNumber: utils.isNumber,
	isEmpty: utils.isEmpty,
	expireTime: utils.expireTime,
	payOrder: utils.payOrder,
	login: utils.login
}
