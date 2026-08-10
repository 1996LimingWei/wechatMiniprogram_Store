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
					<view class="action-btn" v-if="orderInfo.handleOption && orderInfo.handleOption.logistics" @tap="viewLogistics">查看物流</view>
					<view class="action-btn" v-if="orderInfo.handleOption && orderInfo.handleOption.refund" @tap="applyRefund">申请退款</view>
					<view class="action-btn" v-if="orderInfo.handleOption && orderInfo.handleOption.refundCancel" @tap="cancelRefund">撤销申请</view>
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

		<view class="order-logistics" v-if="logistics && logistics.hasLogistics">
			<view class="section-header">
				<text class="section-title">物流信息</text>
			</view>
			<view class="logistics-row">
				<text class="logistics-label">物流公司</text>
				<text class="logistics-value">{{logistics.logisticsCompany}}</text>
			</view>
			<view class="logistics-row">
				<text class="logistics-label">物流单号</text>
				<text class="logistics-value">{{logistics.logisticsNo}}</text>
			</view>
			<view class="logistics-row">
				<text class="logistics-label">发货时间</text>
				<text class="logistics-value">{{logistics.deliveryTime}}</text>
			</view>
		</view>

		<view class="order-after-sale" v-if="afterSale && afterSale.hasAfterSale">
			<view class="section-header">
				<text class="section-title">退款/售后</text>
				<text class="order-status">{{afterSale.statusText}}</text>
			</view>
			<view class="after-row">
				<text class="after-label">售后单号</text>
				<text class="after-value">{{afterSale.afterSaleSn}}</text>
			</view>
			<view class="after-row">
				<text class="after-label">售后类型</text>
				<text class="after-value">{{afterSale.typeText}}</text>
			</view>
			<view class="after-row">
				<text class="after-label">退款金额</text>
				<text class="after-value price">¥{{afterSale.refundAmount}}</text>
			</view>
			<view class="after-row">
				<text class="after-label">申请原因</text>
				<text class="after-value">{{afterSale.reason}}</text>
			</view>
			<view class="after-row" v-if="afterSale.rejectReason">
				<text class="after-label">拒绝原因</text>
				<text class="after-value">{{afterSale.rejectReason}}</text>
			</view>
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
				handleOption: {},
				logistics: {},
				afterSale: {}
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
						that.logistics = res.data.logistics || {};
						that.afterSale = res.data.afterSale || {};
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
				util.payOrder(parseInt(that.orderId)).then(() => {
					uni.showToast({ title: '支付成功', icon: 'success' });
					that.getOrderDetail();
				}).catch(error => {
					util.toast(error && error.pending ? '支付结果确认中' : '支付失败');
					if (error && error.pending) that.getOrderDetail();
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
			},
			viewLogistics() {
				let that = this;
				util.request(api.OrderLogistics, { orderId: that.orderInfo.id }).then(function(res) {
					if (res.code !== 0 || !res.data || !res.data.hasLogistics) {
						util.toast('暂无物流信息');
						return;
					}
					uni.showModal({
						title: res.data.logisticsCompany || '物流信息',
						content: '物流单号：' + res.data.logisticsNo + '\n发货时间：' + res.data.deliveryTime,
						showCancel: false,
						confirmColor: '#5B8C5A'
					});
				});
			},
			applyRefund() {
				let that = this;
				const reasons = that.orderInfo.orderStatusText === '待发货'
					? ['不想要了', '信息填写错误', '拍错/多拍', '其他原因']
					: ['商品与描述不符', '商品损坏/变质', '少件/漏发', '其他原因'];
				uni.showActionSheet({
					itemList: reasons,
					success: function(actionRes) {
						const reason = reasons[actionRes.tapIndex] || '用户申请退款';
						uni.showModal({
							title: '申请退款',
							content: '申请原因：' + reason + '\n提交后商家会尽快处理，确定继续吗？',
							confirmColor: '#5B8C5A',
							success: function(res) {
								if (!res.confirm) return;
								util.request(api.OrderRefundApply, {
									orderId: that.orderInfo.id,
									reason: reason
								}, 'POST', 'application/json').then(function(res) {
									if (res.code === 0) {
										uni.showToast({ title: '已提交申请', icon: 'success' });
										that.getOrderDetail();
									}
								});
							}
						});
					}
				});
			},
			cancelRefund() {
				let that = this;
				uni.showModal({
					title: '撤销申请',
					content: '确定撤销当前退款/售后申请吗？',
					confirmColor: '#5B8C5A',
					success: function(res) {
						if (!res.confirm) return;
						util.request(api.OrderRefundCancel, {
							orderId: that.orderInfo.id
						}, 'POST', 'application/json').then(function(res) {
							if (res.code === 0) {
								uni.showToast({ title: '已撤销', icon: 'success' });
								that.getOrderDetail();
							}
						});
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

	.order-logistics {
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 0 30rpx 24rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.logistics-row {
		display: flex;
		justify-content: space-between;
		padding: 12rpx 0;
	}

	.logistics-label {
		font-size: 26rpx;
		color: #999;
	}

	.logistics-value {
		font-size: 26rpx;
		color: #333;
		text-align: right;
		max-width: 460rpx;
	}

	.order-after-sale {
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 0 30rpx 24rpx;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.after-row {
		display: flex;
		justify-content: space-between;
		padding: 12rpx 0;
	}

	.after-label {
		font-size: 26rpx;
		color: #999;
	}

	.after-value {
		font-size: 26rpx;
		color: #333;
		text-align: right;
		max-width: 460rpx;

		&.price {
			color: $red;
			font-weight: 600;
		}
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
