<template>
	<view class="page">
		<!-- 状态Tab -->
		<scroll-view scroll-x class="status-tabs" :show-scrollbar="false">
			<view
				class="tab-item"
				:class="{active: showType === index}"
				v-for="(tab, index) in statusTabs"
				:key="index"
				@tap="switchType(index)"
			>
				<text>{{tab}}</text>
				<view class="tab-line" v-if="showType === index"></view>
			</view>
		</scroll-view>

		<!-- 订单列表 -->
		<view class="order-list" v-if="orderList.length > 0">
			<view class="order-card" v-for="(item, index) in orderList" :key="item.id"
				@tap="goDetail(item.id)">
				<!-- 订单头部 -->
				<view class="order-header">
					<text class="order-sn">订单号：{{item.orderSn||''}}</text>
					<text class="order-status">{{item.orderStatusText||''}}</text>
				</view>

				<!-- 商品列表 -->
				<view class="order-goods" v-for="(gitem, gi) in item.goodsList" :key="gi">
					<image class="order-goods-img" :src="gitem.listPicUrl" mode="aspectFill"></image>
					<view class="order-goods-info">
						<text class="order-goods-name">{{gitem.goodsName||''}}</text>
						<text class="order-goods-num">共{{gitem.number||0}}件</text>
					</view>
				</view>

				<!-- 底部：金额 + 操作按钮 -->
				<view class="order-footer">
					<text class="order-total">实付：<text class="order-price">¥{{item.actualPrice||'0'}}</text></text>
					<view class="order-actions">
						<!-- 取消 -->
						<view class="action-btn ghost" v-if="item.handleOption && item.handleOption.cancel"
							@tap.stop="cancelOrder(item, index)">取消订单</view>
						<!-- 立即支付 -->
						<view class="action-btn primary" v-if="item.handleOption && item.handleOption.pay"
							@tap.stop="payOrder(item, index)">立即支付</view>
						<view class="action-btn primary" v-if="tradeDevActionEnabled && item.handleOption && item.handleOption.ship"
							@tap.stop="mockShip(item, index)">模拟发货</view>
						<!-- 申请退款 -->
						<view class="action-btn ghost" v-if="item.handleOption && item.handleOption.refund"
							@tap.stop="applyRefund(item)">申请退款</view>
						<view class="action-btn ghost" v-if="item.handleOption && item.handleOption.refundCancel"
							@tap.stop="cancelRefund(item)">撤销申请</view>
						<view class="action-btn primary" v-if="tradeDevActionEnabled && item.handleOption && item.handleOption.refundApprove"
							@tap.stop="mockApproveRefund(item)">模拟退款通过</view>
						<!-- 确认收货 -->
						<view class="action-btn primary" v-if="item.handleOption && item.handleOption.confirm"
							@tap.stop="confirmOrder(item, index)">确认收货</view>
						<!-- 查看物流（待收货时） -->
						<view class="action-btn ghost" v-if="item.handleOption && item.handleOption.logistics"
							@tap.stop="viewLogistics(item)">查看物流</view>
						<!-- 去评价 -->
						<view class="action-btn primary" v-if="item.orderStatusText === '已完成'"
							@tap.stop="goReview">去评价</view>
						<!-- 再次购买 -->
						<view class="action-btn ghost" v-if="item.orderStatusText === '已完成'"
							@tap.stop="buyAgain">再次购买</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 空状态 -->
		<view class="empty-state" v-else>
			<text class="empty-icon">📋</text>
			<text class="empty-text">暂无{{statusTabs[showType]}}订单</text>
			<navigator class="empty-btn" url="/pages/index/index" open-type="switchTab">去逛逛</navigator>
		</view>
	</view>
</template>

<script>
const util = require("@/utils/util.js");
const api = require('@/utils/api.js');

export default {
	data() {
		return {
			showType: 0,
			statusTabs: ['全部', '待付款', '待发货', '待收货', '待评价', '退款/售后'],
			orderList: [],
			page: 1,
			size: 10,
			tradeDevActionEnabled: api.TradeDevActionEnabled === true,
			loading: false
		}
	},
	methods: {
		switchType(index) {
			if (this.showType === index) return;
			this.showType = index;
			this.reload();
		},
		reload() {
			this.orderList = [];
			this.page = 1;
			this.getOrderList();
		},
		getOrderList() {
			if (this.loading) return;
			this.loading = true;
			util.request(api.OrderList, {
				showType: this.showType,
				page: this.page,
				size: this.size
			}).then(res => {
				this.loading = false;
				if (res.code === 0) {
					this.orderList = this.orderList.concat(res.data.list || []);
					this.page++;
				}
			});
		},
		goDetail(id) {
			uni.navigateTo({ url: '../orderDetail/orderDetail?id=' + id });
		},
		payOrder(item, index) {
			getApp().globalData._payAmount = item.actualPrice;
			util.payOrder(parseInt(item.id)).then(() => {
				uni.showToast({ title: '支付成功', icon: 'success' });
				this.reload();
			}).catch(() => {
				util.toast('支付失败，请重试');
			});
		},
		cancelOrder(item, index) {
			util.modal('取消订单', '确定要取消该订单吗？', true, (confirm) => {
				if (!confirm) return;
				util.request(api.OrderCancel, { orderId: item.id }, 'POST', 'application/json').then(res => {
					if (res.code === 0) {
						uni.showToast({ title: '订单已取消', icon: 'success' });
						this.reload();
					}
				});
			});
		},
		confirmOrder(item, index) {
			util.modal('确认收货', '确认已收到商品？', true, (confirm) => {
				if (!confirm) return;
				util.request(api.OrderConfirm, { orderId: item.id }, 'POST', 'application/json').then(res => {
					if (res.code === 0) {
						uni.showToast({ title: '确认收货成功', icon: 'success' });
						this.reload();
					}
				});
			});
		},
		mockShip(item, index) {
			util.modal('模拟发货', '当前还没有管理后台，先用模拟发货让订单进入待收货，确定继续吗？', true, (confirm) => {
				if (!confirm) return;
				util.request(api.OrderMockShip, {
					orderId: item.id,
					logisticsCompany: '顺丰速运'
				}, 'POST', 'application/json').then(res => {
					if (res.code === 0) {
						uni.showToast({ title: '已模拟发货', icon: 'success' });
						this.reload();
					}
				});
			});
		},
		applyRefund(item) {
			uni.showModal({
				title: '申请退款',
				content: '将提交退款/售后申请，提交后商家会尽快处理，确定继续吗？',
				confirmColor: '#5B8C5A',
				success: (modalRes) => {
					if (!modalRes.confirm) return;
					util.request(api.OrderRefundApply, {
						orderId: item.id,
						reason: item.orderStatusText === '待发货' ? '未发货申请退款' : '收到商品后申请售后'
					}, 'POST', 'application/json').then(res => {
						if (res.code === 0) {
							uni.showToast({ title: '已提交申请', icon: 'success' });
							this.reload();
						}
					});
				}
			});
		},
		mockApproveRefund(item) {
			uni.showModal({
				title: '模拟退款通过',
				content: '当前还没有真实退款接口，先模拟审核通过并将订单标记为已退款，确定继续吗？',
				confirmColor: '#5B8C5A',
				success: (modalRes) => {
					if (!modalRes.confirm) return;
					util.request(api.OrderRefundMockApprove, {
						orderId: item.id
					}, 'POST', 'application/json').then(res => {
						if (res.code === 0) {
							uni.showToast({ title: '已退款', icon: 'success' });
							this.reload();
						}
					});
				}
			});
		},
		cancelRefund(item) {
			uni.showModal({
				title: '撤销申请',
				content: '确定撤销当前退款/售后申请吗？',
				confirmColor: '#5B8C5A',
				success: (modalRes) => {
					if (!modalRes.confirm) return;
					util.request(api.OrderRefundCancel, {
						orderId: item.id
					}, 'POST', 'application/json').then(res => {
						if (res.code === 0) {
							uni.showToast({ title: '已撤销', icon: 'success' });
							this.reload();
						}
					});
				}
			});
		},
		viewLogistics(item) {
			util.request(api.OrderLogistics, { orderId: item.id }).then(res => {
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
		goReview() {
			uni.showToast({ title: '评价入口暂未开放', icon: 'none' });
		},
		buyAgain() {
			uni.switchTab({ url: '/pages/index/index' });
		}
	},
	onReachBottom() {
		this.getOrderList();
	},
	onShow() {
		this.reload();
	},
	onLoad(options) {
		if (options.status) {
			this.showType = parseInt(options.status) || 0;
		}
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
	min-height: 100%;
}

.page {
	min-height: 100vh;
	padding-bottom: 40rpx;
}

/* 状态Tab */
.status-tabs {
	white-space: nowrap;
	background: #FEFEFC;
	box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
	position: sticky;
	top: 0;
	z-index: 10;
}

.tab-item {
	display: inline-flex;
	flex-direction: column;
	align-items: center;
	padding: 24rpx 28rpx 0;
	font-size: 26rpx;
	color: $text-hint;
	position: relative;

	&.active {
		color: $green;
		font-weight: 600;
	}
}

.tab-line {
	width: 32rpx;
	height: 6rpx;
	background: $green;
	border-radius: 3rpx;
	margin-top: 14rpx;
	margin-bottom: 0;
}

/* 订单列表 */
.order-list {
	padding: 16rpx 24rpx;
}

.order-card {
	background: #FEFEFC;
	border-radius: 20rpx;
	margin-bottom: 16rpx;
	padding: 24rpx;
	box-shadow: 0 2rpx 12rpx rgba(91,140,90,0.05);
}

.order-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding-bottom: 20rpx;
	border-bottom: 1rpx solid $green-bg;
}

.order-sn {
	font-size: 24rpx;
	color: $text-hint;
}

.order-status {
	font-size: 24rpx;
	color: $green;
	font-weight: 600;
}

/* 商品行 */
.order-goods {
	display: flex;
	align-items: center;
	padding: 20rpx 0;
	border-bottom: 1rpx solid $green-bg;
}

.order-goods-img {
	width: 120rpx;
	height: 120rpx;
	border-radius: 12rpx;
	background: $green-bg;
	flex-shrink: 0;
	margin-right: 20rpx;
}

.order-goods-info {
	flex: 1;
	overflow: hidden;
}

.order-goods-name {
	font-size: 26rpx;
	color: $text-primary;
	display: -webkit-box;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	overflow: hidden;
	line-height: 1.4;
}

.order-goods-num {
	display: block;
	font-size: 22rpx;
	color: $text-hint;
	margin-top: 8rpx;
}

/* 底部 */
.order-footer {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding-top: 20rpx;
}

.order-total {
	font-size: 24rpx;
	color: $text-secondary;
}

.order-price {
	color: $red;
	font-weight: 700;
	font-size: 28rpx;
}

.order-actions {
	display: flex;
	align-items: center;
}

.action-btn {
	height: 56rpx;
	line-height: 56rpx;
	padding: 0 24rpx;
	border-radius: 28rpx;
	font-size: 24rpx;
	margin-left: 12rpx;

	&.primary {
		background: linear-gradient(135deg, $green, $green-dark);
		color: #FEFEFC;
		font-weight: 600;
	}

	&.ghost {
		border: 1rpx solid #ddd;
		color: $text-secondary;
		background: #FEFEFC;
	}
}

/* 空状态 */
.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 200rpx;
}

.empty-icon {
	font-size: 100rpx;
	opacity: 0.5;
}

.empty-text {
	font-size: 28rpx;
	color: $text-hint;
	margin-top: 24rpx;
}

.empty-btn {
	margin-top: 40rpx;
	height: 72rpx;
	line-height: 72rpx;
	padding: 0 48rpx;
	background: $green;
	color: #FEFEFC;
	border-radius: 36rpx;
	font-size: 28rpx;
	text-decoration: none;
}
</style>
