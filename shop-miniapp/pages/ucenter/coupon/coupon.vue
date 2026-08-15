<template>
	<view class="page">
		<view class="tab-bar">
			<view class="tab-item" :class="{ active: tab === 0 }" @tap="switchTab(0)">可领取</view>
			<view class="tab-item" :class="{ active: tab === 1 }" @tap="switchTab(1)">未使用</view>
			<view class="tab-item" :class="{ active: tab === 2 }" @tap="switchTab(2)">已使用</view>
			<view class="tab-item" :class="{ active: tab === 3 }" @tap="switchTab(3)">已过期</view>
		</view>

		<!-- 可领取列表 -->
		<view v-if="tab === 0" class="coupon-list">
			<view v-if="claimableList.length === 0" class="empty-tip">暂无可领取的优惠券</view>
			<view v-for="item in claimableList" :key="item.id" class="coupon-card coupon-claimable">
				<view class="coupon-left">
					<text class="coupon-amount">¥{{item.discountAmount}}</text>
					<text class="coupon-threshold" v-if="item.thresholdAmount > 0">满¥{{item.thresholdAmount}}可用</text>
					<text class="coupon-threshold" v-else>无门槛</text>
				</view>
				<view class="coupon-right">
					<text class="coupon-name">{{item.name}}</text>
					<text class="coupon-info">剩余 {{(item.totalCount || '∞') - (item.claimedCount || 0)}} 张</text>
					<view class="claim-btn" @tap="handleClaim(item)">领取</view>
				</view>
			</view>
		</view>

		<!-- 未使用列表 -->
		<view v-if="tab === 1" class="coupon-list">
			<view v-if="unusedList.length === 0" class="empty-tip">暂无未使用的优惠券</view>
			<view v-for="item in unusedList" :key="item.id" class="coupon-card coupon-unused">
				<view class="coupon-left">
					<text class="coupon-amount">¥{{item.discountAmount}}</text>
					<text class="coupon-threshold" v-if="item.thresholdAmount > 0">满¥{{item.thresholdAmount}}可用</text>
					<text class="coupon-threshold" v-else>无门槛</text>
				</view>
				<view class="coupon-right">
					<text class="coupon-name">{{item.name}}</text>
					<text class="coupon-expire">有效期至 {{item.expireTime}}</text>
				</view>
			</view>
		</view>

		<!-- 已使用列表 -->
		<view v-if="tab === 2" class="coupon-list">
			<view v-if="usedList.length === 0" class="empty-tip">暂无已使用的优惠券</view>
			<view v-for="item in usedList" :key="item.id" class="coupon-card coupon-disabled">
				<view class="coupon-left">
					<text class="coupon-amount">¥{{item.discountAmount}}</text>
					<text class="coupon-threshold">已使用</text>
				</view>
				<view class="coupon-right">
					<text class="coupon-name">{{item.name}}</text>
					<text class="coupon-info">使用时间 {{item.usedTime || '-'}}</text>
				</view>
			</view>
		</view>

		<!-- 已过期列表 -->
		<view v-if="tab === 3" class="coupon-list">
			<view v-if="expiredList.length === 0" class="empty-tip">暂无已过期的优惠券</view>
			<view v-for="item in expiredList" :key="item.id" class="coupon-card coupon-disabled">
				<view class="coupon-left">
					<text class="coupon-amount">¥{{item.discountAmount}}</text>
					<text class="coupon-threshold">已过期</text>
				</view>
				<view class="coupon-right">
					<text class="coupon-name">{{item.name}}</text>
					<text class="coupon-info">过期时间 {{item.expireTime}}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
const api = require('@/utils/api.js');
const util = require('@/utils/util.js');

export default {
	data() {
		return {
			tab: 0,
			claimableList: [],
			unusedList: [],
			usedList: [],
			expiredList: []
		};
	},
	methods: {
		switchTab(index) {
			this.tab = index;
			this.loadData();
		},
		loadData() {
			if (this.tab === 0) {
				this.loadClaimable();
			} else {
				const statusMap = { 1: 0, 2: 1, 3: 2 };
				this.loadCoupons(statusMap[this.tab]);
			}
		},
		loadClaimable() {
			util.request(api.CouponClaimable).then(res => {
				if (res.code === 0) {
					this.claimableList = res.data.list || [];
				}
			});
		},
		loadCoupons(status) {
			util.request(api.CouponList, { status: status }).then(res => {
				if (res.code === 0) {
					const list = res.data.list || [];
					if (status === 0) this.unusedList = list;
					else if (status === 1) this.usedList = list;
					else if (status === 2) this.expiredList = list;
				}
			});
		},
		handleClaim(item) {
			util.request(api.CouponClaim, { templateId: item.id }, 'POST', 'application/json').then(res => {
				if (res.code === 0) {
					util.toast('领取成功');
					this.loadClaimable();
				} else {
					util.toast(res.msg || '领取失败');
				}
			}).catch(() => {
				util.toast('领取失败');
			});
		}
	},
	onShow() {
		this.loadData();
	}
};
</script>

<style lang="scss">
$green: #5B8C5A;
$green-light: #E8F2E7;
$red: #CF4A3E;
$text-primary: #2D3A2E;
$text-secondary: #5C6B5D;
$text-hint: #9CA89D;

page { background: #F6F7F4; }

.page { min-height: 100vh; }

.tab-bar {
	display: flex;
	background: #FEFEFC;
	border-bottom: 1rpx solid #E8ECE8;
	position: sticky;
	top: 0;
	z-index: 10;
}

.tab-item {
	flex: 1;
	text-align: center;
	padding: 24rpx 0;
	font-size: 28rpx;
	color: $text-secondary;
	position: relative;

	&.active {
		color: $green;
		font-weight: 600;

		&::after {
			content: '';
			position: absolute;
			bottom: 0;
			left: 30%;
			width: 40%;
			height: 4rpx;
			background: $green;
			border-radius: 2rpx;
		}
	}
}

.coupon-list {
	padding: 20rpx 24rpx;
}

.empty-tip {
	text-align: center;
	color: $text-hint;
	padding: 120rpx 0;
	font-size: 28rpx;
}

.coupon-card {
	display: flex;
	background: #FEFEFC;
	border-radius: 16rpx;
	margin-bottom: 20rpx;
	overflow: hidden;
	box-shadow: 0 2rpx 12rpx rgba(91, 140, 90, 0.06);
}

.coupon-disabled {
	opacity: 0.5;
}

.coupon-left {
	width: 200rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, $green 0%, #7BAF7A 100%);
	padding: 28rpx 16rpx;
	color: #fff;

	.coupon-disabled & {
		background: linear-gradient(135deg, #B0B8B0 0%, #C8CCC8 100%);
	}
}

.coupon-amount {
	font-size: 48rpx;
	font-weight: 700;
	line-height: 1.2;
}

.coupon-threshold {
	font-size: 22rpx;
	opacity: 0.85;
	margin-top: 4rpx;
}

.coupon-right {
	flex: 1;
	padding: 24rpx;
	display: flex;
	flex-direction: column;
	justify-content: center;
}

.coupon-name {
	font-size: 30rpx;
	color: $text-primary;
	font-weight: 500;
	margin-bottom: 8rpx;
}

.coupon-info {
	font-size: 24rpx;
	color: $text-hint;
}

.coupon-expire {
	font-size: 24rpx;
	color: $text-hint;
}

.claim-btn {
	margin-top: 12rpx;
	background: $green;
	color: #fff;
	text-align: center;
	padding: 12rpx 0;
	border-radius: 8rpx;
	font-size: 26rpx;
	font-weight: 500;
	width: 160rpx;

	&:active {
		opacity: 0.8;
	}
}
</style>
