<template>
	<view class="page">
		<view class="card">
			<navigator class="row" url="/pages/legal/privacy/privacy">
				<text>隐私政策</text>
				<text class="arrow">›</text>
			</navigator>
			<navigator class="row" url="/pages/legal/agreement/agreement">
				<text>用户协议</text>
				<text class="arrow">›</text>
			</navigator>
			<button class="row contact" open-type="contact">
				<text>联系在线客服</text>
				<text class="arrow">›</text>
			</button>
		</view>

		<view class="notice">
			<text class="notice-title">注销账号前请确认</text>
			<text>未完成的订单或售后处理完成后才能注销。注销后，地址、购物车、收藏、足迹和账号身份信息会被清除；依法需要留存的订单、支付及售后记录将继续受限保存。</text>
		</view>

		<button class="close-button" :disabled="closing" @tap="closeAccount">
			{{ closing ? '正在注销...' : '注销账号' }}
		</button>
	</view>
</template>

<script>
const util = require('@/utils/util.js');
const api = require('@/utils/api.js');

export default {
	data() {
		return {
			closing: false
		};
	},
	methods: {
		closeAccount() {
			if (!uni.getStorageSync('token')) {
				uni.navigateTo({ url: '/pages/auth/btnAuth/btnAuth' });
				return;
			}
			uni.showModal({
				title: '确认注销账号',
				content: '注销后账号身份信息和个人资料不可恢复，是否继续？',
				confirmText: '确认注销',
				confirmColor: '#B5473C',
				success: (result) => {
					if (!result.confirm || this.closing) return;
					this.closing = true;
					util.request(api.AccountClose, {
						confirmation: '确认注销'
					}, 'DELETE', 'application/json').then((res) => {
						if (res.code !== 0) {
							throw new Error(res.msg || '注销失败');
						}
						uni.removeStorageSync('token');
						uni.removeStorageSync('userInfo');
						getApp().globalData.token = '';
						getApp().globalData.userInfo = {};
						uni.showToast({ title: '账号已注销', icon: 'success' });
						setTimeout(() => {
							uni.switchTab({ url: '/pages/index/index' });
						}, 800);
					}).catch((error) => {
						util.toast(error.message || '注销失败，请稍后重试');
					}).finally(() => {
						this.closing = false;
					});
				}
			});
		}
	}
};
</script>

<style lang="scss">
page {
	background: #F6F7F4;
}

.page {
	min-height: 100vh;
	padding: 24rpx;
	box-sizing: border-box;
}

.card {
	background: #FEFEFC;
	border-radius: 20rpx;
	overflow: hidden;
}

.row {
	min-height: 96rpx;
	padding: 0 28rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	border: 0;
	border-bottom: 1rpx solid #EEF1EC;
	background: transparent;
	color: #2D3A2E;
	font-size: 28rpx;
	line-height: normal;
	box-sizing: border-box;
	text-decoration: none;

	&::after {
		border: 0;
	}

	&:last-child {
		border-bottom: 0;
	}
}

.contact {
	width: 100%;
	border-radius: 0;
}

.arrow {
	color: #9CA89D;
	font-size: 36rpx;
}

.notice {
	margin-top: 28rpx;
	padding: 28rpx;
	background: #FFF8EE;
	border-radius: 20rpx;
	color: #735D43;
	font-size: 24rpx;
	line-height: 1.7;
}

.notice-title {
	display: block;
	margin-bottom: 8rpx;
	font-weight: 700;
	font-size: 27rpx;
}

.close-button {
	margin-top: 32rpx;
	background: #FEFEFC;
	color: #B5473C;
	border: 1rpx solid #E7CBC7;
	border-radius: 20rpx;
	font-size: 28rpx;

	&::after {
		border: 0;
	}
}
</style>
