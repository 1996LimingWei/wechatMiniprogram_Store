<template>
	<view class="page">
		<view class="bg-circle"></view>
		<view class="bg-circle bg-circle--2"></view>
		<view class="content">
			<view class="brand-area">
				<image class="logo" src="/static/images/logo.png" mode="aspectFit"></image>
				<view class="brand-name">药食同源</view>
				<view class="brand-slogan">传承经典 · 健康生活</view>
			</view>
			<view class="auth-card">
				<view class="auth-title">微信快捷登录</view>
				<view class="auth-desc">使用微信身份建立商城账号。我们不会在登录时获取您的微信昵称和头像。</view>
				<button class="login-btn" @tap="login">
					<text class="btn-text">微信一键登录</text>
				</button>
				<view class="agreement-row" @tap="toggleAgreement">
					<view class="agreement-check" :class="{ checked: agreed }">{{agreed ? '✓' : ''}}</view>
					<text>我已阅读并同意</text>
					<text class="agreement-link" @tap.stop="openAgreement">《用户协议》</text>
					<text>和</text>
					<text class="agreement-link" @tap.stop="openPrivacy">《隐私政策》</text>
				</view>
				<view class="skip-btn" @tap="skipLogin">暂不登录，先逛逛</view>
			</view>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js")
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				navUrl: '',
				agreed: false,
				submitting: false
			}
		},
		methods: {
			toggleAgreement() {
				this.agreed = !this.agreed;
			},
			openAgreement() {
				uni.navigateTo({ url: '/pages/legal/agreement/agreement' });
			},
			openPrivacy() {
				uni.navigateTo({ url: '/pages/legal/privacy/privacy' });
			},
			login() {
				if (!this.agreed) {
					util.toast('请先阅读并同意用户协议和隐私政策');
					return;
				}
				if (this.submitting) {
					return;
				}
				this.submitting = true;
				uni.login({
					success: (resp) => {
						if (!resp.code) {
							util.toast('获取微信登录凭证失败');
							this.submitting = false;
							return;
						}
						this.loginByWeixin(resp.code).then(() => {
							this.submitting = false;
						}, () => {
							this.submitting = false;
						});
					},
					fail: () => {
						util.toast('微信登录失败，请稍后重试');
						this.submitting = false;
					}
				});
			},
			loginByWeixin: function(code) {
				let that = this;
				return util.request(api.AuthLoginByWeixin, {
					code,
					privacyAccepted: true
				}, 'POST', 'application/json').then(res => {
					if (res.code === 0) {
						uni.setStorageSync('userInfo', res.data.userInfo);
						uni.setStorageSync('token', res.data.token);
						uni.setStorageSync('userId', res.data.userId);
						uni.setStorageSync('privacyAgreedAt', Date.now());
						that.goBack();
					} else {
						throw new Error(res.msg || '登录失败');
					}
				}).catch((error) => {
					util.toast(error.message || '登录失败，请稍后重试');
					throw error;
				});
			},
			skipLogin() {
				this.goBack();
			},
			goBack() {
				if (this.navUrl && this.navUrl == '/pages/index/index') {
					uni.switchTab({ url: this.navUrl })
				} else if (this.navUrl) {
					uni.redirectTo({ url: this.navUrl })
				} else {
					uni.switchTab({ url: '/pages/index/index' })
				}
			}
		},
		onLoad: function(options) {
			let that = this;
			if (uni.getStorageSync("navUrl")) {
				that.navUrl = uni.getStorageSync("navUrl")
			} else {
				that.navUrl = '/pages/index/index'
			}
			that.agreed = !!uni.getStorageSync('privacyAgreedAt');
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;
	$gold: #B8860B;

	page {
		height: 100%;
		background: linear-gradient(160deg, $green 0%, $green-light 50%, $green-bg 100%);
	}

	.page {
		height: 100%;
		position: relative;
		overflow: hidden;
	}

	.bg-circle {
		position: absolute;
		width: 600rpx;
		height: 600rpx;
		border-radius: 50%;
		background: rgba(255,255,255,0.06);
		top: -200rpx;
		right: -200rpx;

		&--2 {
			width: 400rpx;
			height: 400rpx;
			top: auto;
			right: auto;
			bottom: -100rpx;
			left: -150rpx;
		}
	}

	.content {
		height: 100%;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 0 60rpx;
		position: relative;
		z-index: 1;
	}

	.brand-area {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-bottom: 80rpx;
	}

	.logo {
		width: 180rpx;
		height: 180rpx;
		border-radius: 50%;
		background: #FEFEFC;
		box-shadow: 0 8rpx 40rpx rgba(0,0,0,0.1);
		margin-bottom: 30rpx;
	}

	.brand-name {
		font-size: 48rpx;
		font-weight: bold;
		color: #FEFEFC;
		letter-spacing: 4rpx;
	}

	.brand-slogan {
		font-size: 26rpx;
		color: rgba(255,255,255,0.85);
		margin-top: 12rpx;
		letter-spacing: 2rpx;
	}

	.auth-card {
		width: 100%;
		background: #FEFEFC;
		border-radius: 24rpx;
		padding: 60rpx 40rpx;
		box-shadow: 0 8rpx 60rpx rgba(91,140,90,0.2);
	}

	.auth-title {
		font-size: 36rpx;
		font-weight: bold;
		color: #333;
		text-align: center;
		margin-bottom: 16rpx;
	}

	.auth-desc {
		font-size: 24rpx;
		color: #999;
		text-align: center;
		line-height: 1.6;
		margin-bottom: 50rpx;
	}

	.login-btn {
		width: 100%;
		height: 88rpx;
		background: linear-gradient(135deg, $green 0%, $green-light 100%);
		border-radius: 44rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border: none;
		margin: 0;
		padding: 0;

		&::after {
			border: none;
		}
	}

	.btn-text {
		font-size: 32rpx;
		color: #FEFEFC;
		font-weight: 500;
		letter-spacing: 2rpx;
	}

	.skip-btn {
		text-align: center;
		font-size: 26rpx;
		color: #999;
		margin-top: 30rpx;
		padding: 10rpx;
	}

	.agreement-row {
		margin-top: 28rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-wrap: wrap;
		font-size: 22rpx;
		line-height: 1.8;
		color: #777;
	}

	.agreement-check {
		width: 28rpx;
		height: 28rpx;
		margin-right: 10rpx;
		border: 2rpx solid #9aa69a;
		border-radius: 6rpx;
		color: #fff;
		font-size: 20rpx;
		line-height: 28rpx;
		text-align: center;

		&.checked {
			background: $green;
			border-color: $green;
		}
	}

	.agreement-link {
		color: $green;
	}
</style>
