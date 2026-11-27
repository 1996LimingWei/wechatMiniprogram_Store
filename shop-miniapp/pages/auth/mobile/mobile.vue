<template>
	<view class="page">
		<view class="user-header">
			<image class="user-avatar" :src="userInfo.avatar || $shopLogo()" mode="aspectFill"></image>
			<text class="user-name">{{userInfo.nickname}}</text>
		</view>
		<view class="form-card">
			<view class="form-title">关联手机号</view>
			<form @submit="bindLoginMobilecode">
				<form @submit="bindGetPassCode">
					<view class="form-item">
						<input type="digit" name="mobile" @input="bindInputMobile" placeholder="请输入手机号" v-model="mobile" auto-focus class="form-input" />
					</view>
					<view class="form-item code-row">
						<input type="digit" name="code" placeholder="四位验证码" class="form-input" />
						<view class="code-btn" @tap="countDownPassCode" v-if="!disableGetMobileCode">{{getCodeButtonText}}</view>
						<view class="code-btn disabled" v-else>{{getCodeButtonText}}</view>
					</view>
				</form>
				<button class="submit-btn" formType="submit" :disabled="disableSubmitMobileCode">提交</button>
			</form>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	const app = getApp();
	export default {
		data() {
			return {
				mobile: '',
				userInfo: app.globalData.userInfo || {},
				disableGetMobileCode: false,
				disableSubmitMobileCode: true,
				getCodeButtonText: '获取验证码'
			}
		},
		methods: {
			bindCheckMobile: function(mobile) {
				if (!mobile) { util.toast('请输入手机号码'); return false; }
				if (!mobile.match(/^1[3-9][0-9]\d{8}$/)) { util.toast('手机号格式不正确'); return false; }
				return true;
			},
			bindGetPassCode: function() {
				this.disableGetMobileCode = true;
			},
			bindInputMobile: function(e) {
				this.mobile = e.detail.value;
			},
			countDownPassCode: function() {
				let that = this;
				if (!that.bindCheckMobile(that.mobile)) return;
				util.request(api.SmsCode, { phone: that.mobile }, 'POST', 'application/json').then(function(res) {
					if (res.code == 0) {
						util.toast('发送成功');
						let i = 60;
						let intervalId = setInterval(function() {
							i--;
							if (i <= 0) {
								that.disableGetMobileCode = false;
								that.disableSubmitMobileCode = false;
								that.getCodeButtonText = '获取验证码';
								clearInterval(intervalId);
							} else {
								that.getCodeButtonText = i + 's';
								that.disableGetMobileCode = true;
								that.disableSubmitMobileCode = false;
							}
						}, 1000);
					} else {
						util.toast('发送失败');
					}
				});
			},
			bindLoginMobilecode: function(e) {
				let mobile = this.mobile;
				if (!this.bindCheckMobile(mobile)) return;
				if (!(e.detail.value.code && e.detail.value.code.length === 4)) return;
				util.request(api.BindMobile, {
					mobileCode: e.detail.value.code,
					mobile: mobile
				}, "POST", "application/json").then(function(res) {
					if (res.code === 0) {
						util.toast('操作成功');
						uni.switchTab({ url: '/pages/ucenter/index/index' });
					} else {
						util.toast('验证码错误');
					}
				});
			}
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;

	page {
		height: 100%;
		background: $green-bg;
	}

	.page {
		min-height: 100%;
	}

	.user-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 60rpx 0 40rpx;
		background: linear-gradient(to bottom, $green 0%, $green-light 100%);
	}

	.user-avatar {
		width: 120rpx;
		height: 120rpx;
		border-radius: 50%;
		border: 4rpx solid rgba(255,255,255,0.5);
		margin-bottom: 16rpx;
	}

	.user-name {
		color: #FEFEFC;
		font-size: 28rpx;
	}

	.form-card {
		margin: -20rpx 24rpx 0;
		background: #FEFEFC;
		border-radius: 20rpx;
		padding: 40rpx 30rpx;
		box-shadow: 0 4rpx 20rpx rgba(91,140,90,0.12);
	}

	.form-title {
		font-size: 32rpx;
		font-weight: bold;
		color: #333;
		text-align: center;
		margin-bottom: 40rpx;
	}

	.form-item {
		height: 88rpx;
		border: 1rpx solid #e8e8e8;
		border-radius: 12rpx;
		margin-bottom: 24rpx;
		display: flex;
		align-items: center;
		padding: 0 20rpx;
	}

	.form-input {
		flex: 1;
		height: 88rpx;
		font-size: 28rpx;
		color: #333;
	}

	.code-row {
		.form-input {
			flex: 1;
			margin-right: 16rpx;
		}
	}

	.code-btn {
		white-space: nowrap;
		font-size: 26rpx;
		color: #FEFEFC;
		background: $green;
		padding: 12rpx 24rpx;
		border-radius: 8rpx;

		&.disabled {
			background: #ccc;
			color: #999;
		}
	}

	.submit-btn {
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		background: linear-gradient(135deg, $green 0%, $green-light 100%);
		color: #FEFEFC;
		font-size: 30rpx;
		font-weight: 500;
		border-radius: 44rpx;
		border: none;
		margin-top: 20rpx;

		&[disabled] {
			background: #ccc;
			color: #FEFEFC;
		}
	}
</style>
