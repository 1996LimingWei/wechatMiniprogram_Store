<template>
	<view class="page">
		<!-- 收货地址 -->
		<view class="address-card" @tap="selectAddress" v-if="checkedAddress.id > 0">
			<view class="address-left">
				<view class="address-user">
					<text class="address-name">{{checkedAddress.userName||''}}</text>
					<text class="address-phone">{{checkedAddress.telNumber||''}}</text>
					<view class="address-default" v-if="checkedAddress.isDefault === 1">默认</view>
				</view>
				<text class="address-detail">{{checkedAddress.fullRegion + checkedAddress.detailInfo}}</text>
			</view>
			<text class="address-arrow">›</text>
		</view>
		<view class="address-card address-empty" @tap="addAddress" v-if="checkedAddress.id <= 0">
			<text class="empty-add">+ 添加收货地址</text>
			<text class="address-arrow">›</text>
		</view>

		<!-- 商品列表 -->
		<view class="goods-card">
			<view class="goods-item" v-for="(item, index) in checkedGoodsList" :key="item.id">
				<image class="goods-img" :src="item.listPicUrl" mode="aspectFill"></image>
				<view class="goods-info">
					<text class="goods-name">{{item.goodsName||''}}</text>
					<text class="goods-spec" v-if="item.goodsSpecifitionNameValue">{{item.goodsSpecifitionNameValue}}</text>
					<view class="goods-bottom">
						<text class="goods-price">¥{{item.retailPrice||''}}</text>
						<text class="goods-num">x{{item.number||''}}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 金额明细 -->
		<view class="amount-card">
			<view class="amount-row">
				<text class="amount-label">商品合计</text>
				<text class="amount-value">¥{{goodsTotalPrice}}</text>
			</view>
			<view class="amount-row">
				<text class="amount-label">运费</text>
				<text class="amount-value">¥{{freightPrice}}</text>
			</view>
		</view>

		<!-- 底部提交栏 -->
		<view class="submit-bar">
			<view class="submit-left">
				<text class="submit-label">实付</text>
				<text class="submit-price">¥{{actualPrice}}</text>
			</view>
			<view class="submit-btn" :class="{ disabled: submitting }" @tap="submitOrder">
				<text>{{submitting ? '正在提交...' : '提交订单'}}</text>
			</view>
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
			checkedGoodsList: [],
			checkedAddress: {},
			goodsTotalPrice: 0.00,
			freightPrice: 0.00,
			orderTotalPrice: 0.00,
			actualPrice: 0.00,
			addressId: 0,
			isBuy: false,
			buyType: '',
			submitting: false,
			requestId: ''
		}
	},
	methods: {
		getCheckoutInfo() {
			let buyType = this.isBuy ? 'buy' : 'cart';
			util.request(api.CartCheckout, {
				addressId: this.addressId,
				type: buyType
			}).then(res => {
				if (res.code === 0) {
					this.checkedGoodsList = res.data.checkedGoodsList;
					this.checkedAddress = res.data.checkedAddress;
					this.actualPrice = res.data.actualPrice;
					this.freightPrice = res.data.freightPrice;
					this.goodsTotalPrice = res.data.goodsTotalPrice;
					this.orderTotalPrice = res.data.orderTotalPrice;
					if (this.checkedAddress.id) {
						this.addressId = this.checkedAddress.id;
					} else {
						uni.showModal({
							title: '',
							content: '请添加默认收货地址!',
							confirmColor: '#5B8C5A',
							success: (res) => {
								if (res.confirm) this.selectAddress();
							}
						});
					}
				}
			});
		},
		selectAddress() {
			uni.navigateTo({ url: '/pages/shopping/address/address' });
		},
		addAddress() {
			uni.navigateTo({ url: '/pages/shopping/addressAdd/addressAdd' });
		},
		submitOrder() {
			if (this.submitting) {
				return;
			}
			if (this.addressId <= 0) {
				util.toast('请选择收货地址');
				return;
			}
			this.submitting = true;
			util.request(api.OrderSubmit, {
				addressId: this.addressId,
				type: this.buyType,
				requestId: this.requestId
			}, 'POST', 'application/json').then(res => {
				if (res.code === 0) {
					const orderId = res.data.orderInfo.id;
					getApp().globalData._payAmount = this.actualPrice;
					util.payOrder(parseInt(orderId)).then(() => {
						uni.redirectTo({ url: '/pages/payResult/payResult?status=1&orderId=' + orderId });
					}).catch(() => {
						uni.redirectTo({ url: '/pages/payResult/payResult?status=0&orderId=' + orderId });
					});
				} else {
					this.submitting = false;
					util.toast('下单失败');
				}
			}).catch(() => {
				this.submitting = false;
			});
		}
	},
	onShow() {
		try {
			var addressId = uni.getStorageSync('addressId');
			if (addressId) this.addressId = addressId;
		} catch (e) {}
		this.getCheckoutInfo();
	},
	onLoad(options) {
		if (options.isBuy != null) this.isBuy = options.isBuy;
		this.buyType = this.isBuy ? 'buy' : 'cart';
		this.requestId = 'MP' + Date.now() + Math.random().toString(36).slice(2, 10);
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
}

.page {
	min-height: 100vh;
	padding: 16rpx 24rpx 130rpx;
}

/* 地址卡片 */
.address-card {
	background: #FEFEFC;
	border-radius: 20rpx;
	padding: 28rpx;
	margin-bottom: 16rpx;
	display: flex;
	align-items: center;
	box-shadow: 0 2rpx 12rpx rgba(91, 140, 90, 0.05);
}

.address-left {
	flex: 1;
	overflow: hidden;
}

.address-user {
	display: flex;
	align-items: center;
	margin-bottom: 12rpx;
}

.address-name {
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
	margin-right: 16rpx;
}

.address-phone {
	font-size: 26rpx;
	color: $text-secondary;
}

.address-default {
	margin-left: 12rpx;
	font-size: 20rpx;
	color: $green;
	border: 2rpx solid $green;
	border-radius: 6rpx;
	padding: 2rpx 10rpx;
}

.address-detail {
	font-size: 26rpx;
	color: $text-secondary;
	line-height: 1.5;
	display: block;
}

.address-arrow {
	font-size: 36rpx;
	color: $text-hint;
	margin-left: 12rpx;
}

.address-empty {
	justify-content: center;
	padding: 40rpx 28rpx;
}

.empty-add {
	flex: 1;
	font-size: 28rpx;
	color: $green;
	font-weight: 600;
}

/* 商品卡片 */
.goods-card {
	background: #FEFEFC;
	border-radius: 20rpx;
	padding: 24rpx;
	margin-bottom: 16rpx;
	box-shadow: 0 2rpx 12rpx rgba(91, 140, 90, 0.05);
}

.goods-item {
	display: flex;
	padding: 16rpx 0;
	border-bottom: 1rpx solid $green-bg;

	&:last-child {
		border-bottom: none;
		padding-bottom: 0;
	}

	&:first-child {
		padding-top: 0;
	}
}

.goods-img {
	width: 140rpx;
	height: 140rpx;
	border-radius: 12rpx;
	background: $green-bg;
	margin-right: 20rpx;
	flex-shrink: 0;
}

.goods-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
	overflow: hidden;
}

.goods-name {
	font-size: 26rpx;
	color: $text-primary;
	display: -webkit-box;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	overflow: hidden;
	line-height: 1.4;
}

.goods-spec {
	font-size: 22rpx;
	color: $text-hint;
	margin-top: 8rpx;
	background: $green-bg;
	padding: 4rpx 12rpx;
	border-radius: 6rpx;
	align-self: flex-start;
}

.goods-bottom {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-top: 8rpx;
}

.goods-price {
	font-size: 28rpx;
	color: $red;
	font-weight: 700;
}

.goods-num {
	font-size: 24rpx;
	color: $text-hint;
}

/* 选项卡片 */
.option-card {
	background: #FEFEFC;
	border-radius: 20rpx;
	padding: 28rpx;
	margin-bottom: 16rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	box-shadow: 0 2rpx 12rpx rgba(91, 140, 90, 0.05);
}

.option-label {
	font-size: 28rpx;
	color: $text-primary;
}

.option-right {
	display: flex;
	align-items: center;
}

.option-value {
	font-size: 26rpx;
	color: $text-hint;
	margin-right: 8rpx;

	&.has-coupon {
		color: $red;
	}
}

.option-arrow {
	font-size: 32rpx;
	color: $text-hint;
}

/* 金额卡片 */
.amount-card {
	background: #FEFEFC;
	border-radius: 20rpx;
	padding: 24rpx 28rpx;
	margin-bottom: 16rpx;
	box-shadow: 0 2rpx 12rpx rgba(91, 140, 90, 0.05);
}

.amount-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 12rpx 0;
}

.amount-label {
	font-size: 26rpx;
	color: $text-secondary;
}

.amount-value {
	font-size: 26rpx;
	color: $text-primary;

	&.discount {
		color: $red;
	}
}

/* 底部提交栏 */
.submit-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	height: 110rpx;
	background: #FEFEFC;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 24rpx;
	box-shadow: 0 -4rpx 16rpx rgba(91, 140, 90, 0.08);
	z-index: 100;
}

.submit-left {
	display: flex;
	align-items: baseline;
}

.submit-label {
	font-size: 26rpx;
	color: $text-secondary;
	margin-right: 8rpx;
}

.submit-price {
	font-size: 40rpx;
	color: $red;
	font-weight: 700;
}

.submit-btn {
	height: 80rpx;
	padding: 0 56rpx;
	background: linear-gradient(135deg, $green, $green-dark);
	border-radius: 40rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 30rpx;
	color: #FEFEFC;
	font-weight: 600;

	&.disabled {
		opacity: 0.55;
	}
}
</style>
