<template>
	<view class="page">
		<!-- 支付成功 -->
		<view class="result-card" v-if="status === 'success'">
			<view class="result-icon success-icon">✓</view>
			<text class="result-title">支付成功</text>
			<text class="result-desc">感谢您的购买，祝您身体健康</text>
			<view class="result-btns">
				<navigator class="btn-primary" url="/pages/ucenter/order/order" open-type="redirect">查看订单</navigator>
				<navigator class="btn-outline" url="/pages/index/index" open-type="switchTab">继续逛逛</navigator>
			</view>
		</view>

		<!-- 支付结果确认中 -->
		<view class="result-card" v-else-if="status === 'pending'">
			<view class="result-icon pending-icon">···</view>
			<text class="result-title">支付结果确认中</text>
			<text class="result-desc">正在同步微信支付结果，请稍后在订单中查看</text>
			<view class="result-btns">
				<navigator class="btn-primary" url="/pages/ucenter/order/order" open-type="redirect">查看订单</navigator>
				<navigator class="btn-outline" url="/pages/index/index" open-type="switchTab">返回首页</navigator>
			</view>
		</view>

		<!-- 支付失败 -->
		<view class="result-card" v-else>
			<view class="result-icon fail-icon">!</view>
			<text class="result-title fail-title">支付未完成</text>
			<text class="result-desc">请在 <text class="highlight">1小时</text> 内完成付款，否则订单将被取消</text>
			<view class="result-btns">
				<view class="btn-primary" @tap="payOrder">重新付款</view>
				<navigator class="btn-outline" url="/pages/ucenter/order/order" open-type="redirect">查看订单</navigator>
			</view>
		</view>
	</view>
</template>

<script>
const util = require("@/utils/util.js");

export default {
	data() {
		return {
			status: 'failed',
			orderId: 0
		}
	},
	methods: {
		confirmPaymentResult() {
			if (!this.orderId) return;
			this.status = 'pending';
			util.waitForPaymentResult(parseInt(this.orderId)).then(() => {
				this.status = 'success';
			}).catch((error) => {
				this.status = error && error.pending ? 'pending' : 'failed';
			});
		},
		payOrder() {
			this.status = 'pending';
			util.payOrder(parseInt(this.orderId)).then(() => {
				this.status = 'success';
			}).catch((error) => {
				this.status = error && error.pending ? 'pending' : 'failed';
				util.toast(error && error.pending ? '支付结果确认中' : '支付失败');
			});
		}
	},
	onLoad(options) {
		this.orderId = Number(options.orderId || 0);
		this.status = options.status === '1' ? 'success' : options.status === '2' ? 'pending' : 'failed';
		if (this.status !== 'failed') this.confirmPaymentResult();
	}
}
</script>

<style lang="scss">
$green: #5B8C5A;
$green-light: #E8F2E7;
$green-bg: #F6F7F4;
$green-dark: #3D6B3C;
$text-primary: #2D3A2E;
$text-secondary: #5C6B5D;
$text-hint: #9CA89D;
$red: #CF4A3E;

page {
	background: $green-bg;
	min-height: 100vh;
}

.page {
	display: flex;
	align-items: center;
	justify-content: center;
	min-height: 100vh;
	padding: 0 40rpx;
}

.result-card {
	width: 100%;
	background: #FEFEFC;
	border-radius: 24rpx;
	padding: 80rpx 48rpx 60rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	box-shadow: 0 8rpx 32rpx rgba(91, 140, 90, 0.08);
}

.result-icon {
	width: 120rpx;
	height: 120rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 56rpx;
	font-weight: 700;
	color: #FEFEFC;
	margin-bottom: 32rpx;
}

.success-icon {
	background: linear-gradient(135deg, $green, $green-dark);
}

.fail-icon {
	background: linear-gradient(135deg, #E8A44A, #D4863A);
}

.pending-icon {
	background: #6F8171;
	font-size: 40rpx;
}

.result-title {
	font-size: 36rpx;
	font-weight: 700;
	color: $text-primary;
	margin-bottom: 16rpx;
}

.fail-title {
	color: $text-primary;
}

.result-desc {
	font-size: 26rpx;
	color: $text-hint;
	text-align: center;
	line-height: 1.6;
	margin-bottom: 56rpx;
}

.highlight {
	color: $red;
	font-weight: 600;
}

.result-btns {
	width: 100%;
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

.btn-primary {
	height: 88rpx;
	line-height: 88rpx;
	text-align: center;
	background: linear-gradient(135deg, $green, $green-dark);
	color: #FEFEFC;
	border-radius: 44rpx;
	font-size: 30rpx;
	font-weight: 600;
	text-decoration: none;
}

.btn-outline {
	height: 84rpx;
	line-height: 80rpx;
	text-align: center;
	border: 2rpx solid $green;
	color: $green;
	border-radius: 44rpx;
	font-size: 30rpx;
	font-weight: 600;
	text-decoration: none;
}
</style>
