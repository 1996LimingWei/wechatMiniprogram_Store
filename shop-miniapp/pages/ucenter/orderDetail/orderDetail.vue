<template>
	<view class="container">
		<view class="order-info">
			<view class="info-row">
				<text class="info-label">下单时间</text>
				<text class="info-value">{{orderInfo.addTime}}</text>
			</view>
			<view class="info-row">
				<text class="info-label">订单编号</text>
				<text class="info-value">{{orderInfo.orderSn}}</text>
			</view>
			<view class="action-row">
				<view class="actual-price">
					实付：<text class="price-num">¥{{orderInfo.actualPrice}}</text>
				</view>
				<view class="action-btns">
					<view class="action-btn" v-if="orderInfo.handleOption && orderInfo.handleOption.cancel" @tap="cancelOrder">取消订单</view>
					<view class="action-btn primary" v-if="orderInfo.handleOption && orderInfo.handleOption.pay" @tap="payOrder">立即支付</view>
					<view class="action-btn primary" v-if="orderInfo.handleOption && orderInfo.handleOption.confirm" @tap="confirmOrder">确认收货</view>
				</view>
			</view>
		</view>

		<view class="order-goods">
			<view class="section-header">
				<text class="section-title">商品信息</text>
				<text class="order-status">{{orderInfo.orderStatusText}}</text>
			</view>
			<view class="goods-item" v-for="(item, index) in orderGoods" :key="item.id">
				<image class="goods-img" :src="item.listPicUrl" mode="aspectFill"></image>
				<view class="goods-info">
					<view class="goods-top">
						<text class="goods-name">{{item.goodsName}}</text>
						<text class="goods-num">x{{item.number}}</text>
					</view>
					<text class="goods-spec">{{item.goodsSpecifitionNameValue||''}}</text>
					<text class="goods-price">¥{{item.retailPrice}}</text>
				</view>
			</view>
		</view>

		<view class="order-address">
			<view class="address-info">
				<text class="address-name">{{orderInfo.consignee}}</text>
				<text class="address-tel">{{orderInfo.mobile}}</text>
			</view>
			<text class="address-detail">{{(orderInfo.fullRegion || '') + (orderInfo.address || '')}}</text>
		</view>

		<view class="order-total">
			<view class="total-row">
				<text class="total-label">商品合计</text>
				<text class="total-value">¥{{orderInfo.goodsPrice}}</text>
			</view>
			<view class="total-row">
				<text class="total-label">运费</text>
				<text class="total-value">¥{{orderInfo.freightPrice}}</text>
			</view>
			<view class="total-row final">
				<text class="total-label">实付金额</text>
				<text class="total-value highlight">¥{{orderInfo.actualPrice}}</text>
			</view>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				orderId: 0,
				orderInfo: {},
				orderGoods: [],
				handleOption: {}
			}
		},
		methods: {
			getOrderDetail() {
				let that = this;
				util.request(api.OrderDetail, { orderId: that.orderId }).then(function(res) {
					if (res.code === 0) {
						that.orderInfo = res.data.orderInfo || {};
						that.orderGoods = res.data.orderGoods || [];
						that.handleOption = res.data.handleOption || {};
					}
				});
			},
			cancelOrder() {
				let that = this;
				uni.showModal({
					title: '提示',
					content: '确定要取消此订单？',
					confirmColor: '#5B8C5A',
					success: function(res) {
						if (res.confirm) {
							util.request(api.OrderCancel, { orderId: that.orderInfo.id }).then(function(res) {
								if (res.code === 0) {
									uni.showModal({
										title: '提示',
										content: res.data || '订单已取消',
										showCancel: false,
										confirmColor: '#5B8C5A',
										success: function() {
											uni.navigateBack();
										}
									});
								}
							});
						}
					}
				});
			},
			payOrder() {
				let that = this;
				util.payOrder(parseInt(that.orderId)).then(res => {
					that.getOrderDetail();
				}).catch(res => {
					util.toast('支付失败');
				});
			},
			confirmOrder() {
				let that = this;
				uni.showModal({
					title: '提示',
					content: '确定已经收到商品？',
					confirmColor: '#5B8C5A',
					success: function(res) {
						if (res.confirm) {
							util.request(api.OrderConfirm, { orderId: that.orderInfo.id }).then(function(res) {
								if (res.code === 0) {
									uni.showModal({
										title: '提示',
										content: res.data || '已确认收货',
										showCancel: false,
										confirmColor: '#5B8C5A',
										success: function() {
											uni.navigateBack();
										}
									});
								}
							});
						}
					}
				});
			}
		},
		onLoad: function(options) {
			this.orderId = options.id;
			this.getOrderDetail();
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;
	$red: #CF4A3E;

	page {
		background: $green-bg;
	}

	.container {
		padding: 24rpx;
	}

	.order-info {
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 28rpx 30rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.info-row {
		display: flex;
		justify-content: space-between;
		margin-bottom: 16rpx;
	}

	.info-label {
		font-size: 26rpx;
		color: #999;
	}

	.info-value {
		font-size: 26rpx;
		color: #333;
	}

	.action-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding-top: 20rpx;
		border-top: 1rpx solid #f0f0f0;
		margin-top: 10rpx;
	}

	.actual-price {
		font-size: 26rpx;
		color: #333;
	}

	.price-num {
		color: $red;
		font-weight: bold;
		font-size: 30rpx;
	}

	.action-btns {
		display: flex;
	}

	.action-btn {
		display: inline-block;
		font-size: 24rpx;
		padding: 10rpx 24rpx;
		border-radius: 24rpx;
		border: 1rpx solid #ddd;
		color: #666;
		margin-left: 16rpx;

		&.primary {
			background: $green;
			color: #FEFEFC;
			border-color: $green;
		}
	}

	.order-goods {
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 0 30rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.section-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 88rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}

	.section-title {
		font-size: 28rpx;
		font-weight: bold;
		color: #333;
	}

	.order-status {
		font-size: 26rpx;
		color: $green;
		font-weight: 500;
	}

	.goods-item {
		display: flex;
		padding: 24rpx 0;
		border-bottom: 1rpx solid #f5f5f5;

		&:last-child {
			border-bottom: none;
		}
	}

	.goods-img {
		width: 150rpx;
		height: 150rpx;
		border-radius: 12rpx;
		margin-right: 20rpx;
	}

	.goods-info {
		flex: 1;
	}

	.goods-top {
		display: flex;
		justify-content: space-between;
		margin-bottom: 10rpx;
	}

	.goods-name {
		font-size: 26rpx;
		color: #333;
		flex: 1;
	}

	.goods-num {
		font-size: 26rpx;
		color: #999;
		margin-left: 10rpx;
	}

	.goods-spec {
		display: block;
		font-size: 22rpx;
		color: #999;
		margin-bottom: 12rpx;
	}

	.goods-price {
		font-size: 28rpx;
		color: #333;
		font-weight: 500;
	}

	.order-address {
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 28rpx 30rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.address-info {
		margin-bottom: 10rpx;
	}

	.address-name {
		font-size: 28rpx;
		color: #333;
		font-weight: 500;
		margin-right: 20rpx;
	}

	.address-tel {
		font-size: 26rpx;
		color: #666;
	}

	.address-detail {
		font-size: 24rpx;
		color: #999;
		line-height: 1.5;
	}

	.order-total {
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 24rpx 30rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.total-row {
		display: flex;
		justify-content: space-between;
		margin-bottom: 16rpx;

		&.final {
			padding-top: 16rpx;
			border-top: 1rpx solid #f0f0f0;
			margin-bottom: 0;
		}
	}

	.total-label {
		font-size: 26rpx;
		color: #666;
	}

	.total-value {
		font-size: 26rpx;
		color: #333;

		&.highlight {
			color: $red;
			font-weight: bold;
			font-size: 30rpx;
		}
	}
</style>
