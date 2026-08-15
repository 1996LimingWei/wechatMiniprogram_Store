<template>
	<view class="page">
		<!-- 顶部用户区（绿色渐变背景） -->
		<view class="header">
			<view class="user-section" @tap="onUserTap">
				<view class="user-main">
					<image class="avatar" :src="avatarUrl" mode="aspectFill"></image>
					<view class="user-info">
						<text class="nickname">{{ nicknameText }}</text>
						<text class="user-desc" v-if="!isLogin">点击登录，享受更多权益</text>
						<text class="user-desc" v-else>{{ userInfo.mobile || '欢迎回来' }}</text>
					</view>
				</view>
				<view class="vip-card" @tap.stop="goMember">
					<text class="vip-badge">{{ memberBadge }}</text>
					<text class="vip-title">会员中心</text>
					<text class="vip-sub">{{ memberLevel === 2 ? '尊享全部权益' : '专享折扣 / 优先发货' }}</text>
				</view>
			</view>
		</view>

		<!-- 数据统计行 -->
		<view class="stats-row">
			<navigator url="/pages/ucenter/collect/collect" class="stat-item">
				<text class="stat-num">{{ collectCount }}</text>
				<text class="stat-label">收藏</text>
			</navigator>
			<navigator url="/pages/ucenter/footprint/footprint" class="stat-item">
				<text class="stat-num">{{ footprintCount }}</text>
				<text class="stat-label">足迹</text>
			</navigator>
			<view class="stat-item" @tap="goMember">
				<text class="stat-num stat-vip">{{ memberLevel === 2 ? '黄金卡' : '黄金卡' }}</text>
				<text class="stat-label stat-label-vip">{{ memberLevel === 2 ? '已开通' : '立即开通' }}</text>
			</view>
		</view>

		<!-- 我的订单 -->
		<view class="section-card">
			<view class="section-header">
				<text class="section-title">我的订单</text>
				<navigator url="/pages/ucenter/order/order" class="section-more">
					<text>全部订单</text>
					<text class="arrow">›</text>
				</navigator>
			</view>
			<view class="order-tabs">
				<navigator :url="'/pages/ucenter/order/order?status=1'" class="order-tab">
					<view class="order-status-dot hot"></view>
					<image class="order-icon-img" src="/static/images/service/order_pay.svg"></image>
					<text class="order-label">待付款</text>
				</navigator>
				<navigator :url="'/pages/ucenter/order/order?status=2'" class="order-tab">
					<view class="order-status-dot"></view>
					<image class="order-icon-img" src="/static/images/service/order_shipping.svg"></image>
					<text class="order-label">待发货</text>
				</navigator>
				<navigator :url="'/pages/ucenter/order/order?status=3'" class="order-tab">
					<view class="order-status-dot"></view>
					<image class="order-icon-img" src="/static/images/service/order_delivery.svg"></image>
					<text class="order-label">待收货</text>
				</navigator>
				<navigator :url="'/pages/ucenter/order/order?status=4'" class="order-tab">
					<view class="order-status-dot subtle"></view>
					<image class="order-icon-img" src="/static/images/service/order_rate.svg"></image>
					<text class="order-label">已完成</text>
				</navigator>
				<navigator :url="'/pages/ucenter/order/order?status=5'" class="order-tab">
					<view class="order-status-dot subtle"></view>
					<image class="order-icon-img" src="/static/images/service/order_refund.svg"></image>
					<text class="order-label">退款/售后</text>
				</navigator>
			</view>
		</view>

		<!-- 我的服务 -->
		<view class="section-card">
			<view class="section-header">
				<text class="section-title">我的服务</text>
			</view>
			<view class="service-group">
				<view class="service-group-header">
					<text class="service-group-title">高频服务</text>
					<text class="service-group-desc">常用入口一键直达</text>
				</view>
				<view class="service-grid primary">
					<navigator url="/pages/ucenter/address/address" class="service-item">
						<view class="service-icon" style="background: linear-gradient(135deg, #E3EFE4 0%, #F3F8F3 100%);">
							<image class="service-icon-img" src="/static/images/service/service_address.svg"></image>
						</view>
						<text class="service-label">地址管理</text>
					</navigator>
					<navigator url="/pages/ucenter/collect/collect" class="service-item">
						<view class="service-icon" style="background: linear-gradient(135deg, #F6ECD1 0%, #FBF5E7 100%);">
							<image class="service-icon-img" src="/static/images/service/service_coupon.svg"></image>
						</view>
						<text class="service-label">我的收藏</text>
					</navigator>
<navigator url="/pages/ucenter/coupon/coupon" class="service-item">
						<view class="service-icon" style="background: linear-gradient(135deg, #FFE8D6 0%, #FFF3EB 100%);">
							<image class="service-icon-img" src="/static/images/service/service_coupon.svg"></image>
						</view>
						<text class="service-label">优惠券</text>
					</navigator>
					<button class="service-item service-btn" open-type="contact">
						<view class="service-icon" style="background: linear-gradient(135deg, #E4ECE4 0%, #F2F7F2 100%);">
							<image class="service-icon-img" src="/static/images/service/service_kefu.svg"></image>
						</view>
						<text class="service-label">在线客服</text>
					</button>
					<navigator url="/pages/ucenter/footprint/footprint" class="service-item">
						<view class="service-icon" style="background: linear-gradient(135deg, #F2E6C1 0%, #F9F2DE 100%);">
							<image class="service-icon-img" src="/static/images/service/service_vip.svg"></image>
						</view>
						<text class="service-label">浏览记录</text>
					</navigator>
				</view>
			</view>
			<view class="service-group service-group-secondary">
				<view class="service-group-header">
					<text class="service-group-title">更多工具</text>
					<text class="service-group-desc">账户、反馈与帮助服务</text>
				</view>
				<view class="service-grid secondary">
					<navigator url="/pages/legal/privacy/privacy" class="service-item">
						<view class="service-icon" style="background: linear-gradient(135deg, #E7EEE8 0%, #F6F8F6 100%);">
							<image class="service-icon-img" src="/static/images/service/service_distribution.svg"></image>
						</view>
						<text class="service-label">隐私政策</text>
					</navigator>
					<navigator url="/pages/legal/agreement/agreement" class="service-item">
						<view class="service-icon" style="background: linear-gradient(135deg, #EEF2EC 0%, #FAFBF8 100%);">
							<image class="service-icon-img" src="/static/images/service/service_wallet.svg"></image>
						</view>
						<text class="service-label">用户协议</text>
					</navigator>
					<navigator url="/pages/ucenter/feedback/feedback" class="service-item">
						<view class="service-icon" style="background: linear-gradient(135deg, #E6EEE7 0%, #F4F8F4 100%);">
							<image class="service-icon-img" src="/static/images/service/service_feedback.svg"></image>
						</view>
						<text class="service-label">意见反馈</text>
					</navigator>
					<navigator url="/pages/ucenter/settings/settings" class="service-item">
						<view class="service-icon" style="background: linear-gradient(135deg, #F2F3EC 0%, #FBFBF8 100%);">
							<image class="service-icon-img" src="/static/images/service/service_help.svg"></image>
						</view>
						<text class="service-label">账号设置</text>
					</navigator>
				</view>
			</view>
		</view>

		<!-- 品牌收尾区 -->
		<view class="brand-footer">
			<view class="brand-footer-card">
				<view class="brand-footer-line"></view>
				<text class="brand-footer-title">药食同源</text>
				<text class="brand-footer-desc">甄选原料，安心到家，让健康触手可及</text>
			</view>
		</view>

		<!-- 退出登录 -->
		<view class="logout-btn" v-if="isLogin" @tap="exitLogin">
			<text>退出登录</text>
		</view>
	</view>
</template>

<script>
const util = require('@/utils/util.js');
const api = require('@/utils/api.js');
const app = getApp();

const DEFAULT_AVATAR = '/static/images/logo.png';

export default {
	data() {
		return {
			userInfo: {},
			memberLevel: 1,
			couponCount: 0,
			collectCount: 0,
			footprintCount: 0
		};
	},
	computed: {
		isLogin() {
			return !!(this.userInfo && this.userInfo.nickname && this.userInfo.nickname !== 'Hi, 游客');
		},
		nicknameText() {
			return this.userInfo.nickname || '点击登录';
		},
		avatarUrl() {
			return this.userInfo.avatar || DEFAULT_AVATAR;
		},
		memberBadge() {
			return this.memberLevel === 2 ? 'GOLD' : 'SILVER';
		}
	},
	methods: {
		onUserTap() {
			if (!this.isLogin) {
				uni.navigateTo({ url: '/pages/auth/btnAuth/btnAuth' });
			} else {
				uni.navigateTo({ url: '/pages/ucenter/profile/profile' });
			}
		},
		goMember() {
			uni.navigateTo({ url: '/pages/ucenter/member/member' });
		},
		exitLogin() {
			uni.showModal({
				title: '提示',
				content: '确定退出登录？',
				confirmColor: '#5B8C5A',
				success: (res) => {
					if (res.confirm) {
						util.request(api.AuthLogout, {}, 'POST', 'application/json')
							.catch(() => null)
							.finally(() => {
								uni.removeStorageSync('token');
								uni.removeStorageSync('userInfo');
								app.globalData.userInfo = {};
								app.globalData.token = '';
								this.userInfo = {};
								util.toast('已退出登录');
							});
					}
				}
			});
		}
	},
	onShow() {
		let userInfo = uni.getStorageSync('userInfo');
		let token = uni.getStorageSync('token');
		if (userInfo && token) {
			this.userInfo = userInfo;
			this.memberLevel = userInfo.memberLevel || 1;
			app.globalData.userInfo = userInfo;
			app.globalData.token = token;
		} else {
			this.userInfo = {};
			this.memberLevel = 1;
		}
	}
};
</script>

<style lang="scss">
$green: #4D704D;
$green-light: #E8ECE8;
$green-bg: #FDFDF8;
$green-dark: #667166;
$gold: #FAFAD2;
$gold-light: #F5F5E0;
$text-primary: #36454F;
$text-secondary: #667166;
$text-hint: #9A9A9A;
$red: #36454F;

page {
	background: $green-bg;
}

.page {
	min-height: 100vh;
	padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

/* 顶部区域 */
.header {
	background:
		radial-gradient(circle at 12% 18%, rgba(255, 255, 255, 0.18) 0, rgba(255, 255, 255, 0.08) 18%, transparent 42%),
		linear-gradient(145deg, #97AC96 0%, #879F8C 58%, #78907C 100%);
	padding: 74rpx 30rpx 72rpx;
	border-radius: 0 0 40rpx 40rpx;
	position: relative;
	overflow: hidden;
}

.header::after {
	content: '';
	position: absolute;
	right: -30rpx;
	top: 18rpx;
	width: 240rpx;
	height: 240rpx;
	background: radial-gradient(circle, rgba(244, 232, 190, 0.24) 0%, rgba(244, 232, 190, 0.08) 48%, rgba(244, 232, 190, 0) 74%);
}

.user-section {
	display: flex;
	align-items: stretch;
	position: relative;
	z-index: 1;
	gap: 20rpx;
}

.user-main {
	flex: 1;
	display: flex;
	align-items: center;
	padding: 18rpx 0;
}

.avatar {
	width: 112rpx;
	height: 112rpx;
	border-radius: 50%;
	border: 4rpx solid rgba(255, 255, 255, 0.36);
	flex-shrink: 0;
	box-shadow: 0 10rpx 24rpx rgba(77, 95, 82, 0.14);
}

.user-info {
	flex: 1;
	margin-left: 24rpx;
	overflow: hidden;
}

.nickname {
	font-size: 38rpx;
	font-weight: 700;
	color: #FEFEFC;
	display: block;
}

.user-desc {
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.78);
	margin-top: 8rpx;
	display: block;
}

.vip-card {
	width: 178rpx;
	flex-shrink: 0;
	background: linear-gradient(135deg, rgba(151, 128, 90, 0.84) 0%, rgba(239, 227, 184, 0.86) 100%);
	border-radius: 24rpx 24rpx 24rpx 8rpx;
	padding: 16rpx 18rpx;
	display: flex;
	flex-direction: column;
	justify-content: center;
	box-shadow: 0 10rpx 18rpx rgba(104, 95, 60, 0.09);
}

.vip-badge {
	font-size: 18rpx;
	color: rgba(255, 255, 255, 0.76);
	letter-spacing: 2rpx;
}

.vip-title {
	font-size: 26rpx;
	color: #FEFEFC;
	font-weight: 700;
	margin-top: 6rpx;
}

.vip-sub {
	font-size: 18rpx;
	color: rgba(255, 255, 255, 0.78);
	margin-top: 4rpx;
	line-height: 1.45;
}

/* 数据统计行 */
.stats-row {
	display: flex;
	background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(252, 252, 249, 0.98) 100%);
	margin: -42rpx 24rpx 0;
	border-radius: 24rpx;
	padding: 30rpx 0;
	position: relative;
	z-index: 10;
	box-shadow: 0 12rpx 28rpx rgba(77, 112, 77, 0.08);
}

.stat-item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	text-decoration: none;
}

.stat-num {
	font-size: 34rpx;
	font-weight: 700;
	color: $text-primary;
}

.stat-vip {
	color: #C9AE63;
	font-size: 30rpx;
}

.stat-label-vip {
	color: #B59955;
}

.stat-label {
	font-size: 22rpx;
	color: $text-hint;
	margin-top: 6rpx;
}

/* 区块卡片 */
.section-card {
	background: #FEFEFC;
	margin: 22rpx 24rpx 0;
	border-radius: 24rpx;
	padding: 28rpx 28rpx 18rpx;
	box-shadow: 0 10rpx 24rpx rgba(77, 112, 77, 0.05);
}

.section-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 24rpx;
}

.section-title {
	font-size: 34rpx;
	font-weight: 700;
	color: $text-primary;
}

.section-more {
	display: flex;
	align-items: center;
	font-size: 24rpx;
	color: $text-hint;
	text-decoration: none;
}

.arrow {
	font-size: 32rpx;
	margin-left: 4rpx;
}

/* 订单Tab */
.order-tabs {
	display: flex;
	padding-bottom: 16rpx;
}

.order-tab {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	text-decoration: none;
	padding: 8rpx 0;
	position: relative;
}

.order-icon-img {
	width: 52rpx;
	height: 52rpx;
	margin-bottom: 12rpx;
}

.order-status-dot {
	position: absolute;
	top: 2rpx;
	right: 34rpx;
	width: 12rpx;
	height: 12rpx;
	border-radius: 50%;
	background: rgba(111, 142, 117, 0.24);
}

.order-status-dot.hot {
	background: #D29A68;
	box-shadow: 0 0 0 6rpx rgba(210, 154, 104, 0.12);
}

.order-status-dot.subtle {
	background: rgba(111, 142, 117, 0.16);
}

.order-label {
	font-size: 22rpx;
	color: $text-secondary;
}

/* 服务网格 */
.service-group {
	padding-bottom: 12rpx;
}

.service-group-secondary {
	margin-top: 8rpx;
	padding-top: 28rpx;
	border-top: 1rpx solid rgba(111, 142, 117, 0.08);
}

.service-group-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 12rpx;
}

.service-group-title {
	font-size: 24rpx;
	font-weight: 700;
	color: #506553;
}

.service-group-desc {
	font-size: 20rpx;
	color: $text-hint;
}

.service-grid {
	display: flex;
	flex-wrap: wrap;
}

.service-grid.primary .service-item {
	padding-top: 24rpx;
	padding-bottom: 20rpx;
}

.service-grid.secondary .service-item {
	padding-top: 20rpx;
	padding-bottom: 18rpx;
}

.service-item {
	width: 25%;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 20rpx 0;
	text-decoration: none;
}

.service-btn {
	margin: 0;
	padding: 20rpx 0;
	border: 0;
	background: transparent;
	border-radius: 0;
	line-height: normal;
	font-size: inherit;

	&::after {
		border: none;
	}
}

.service-icon {
	width: 88rpx;
	height: 88rpx;
	border-radius: 26rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 12rpx;
	box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.78), 0 8rpx 18rpx rgba(100, 118, 102, 0.08);
}

.service-icon-img {
	width: 42rpx;
	height: 42rpx;
}

.service-label {
	font-size: 22rpx;
	color: $text-primary;
	font-weight: 500;
}

/* 品牌收尾 */
.brand-footer {
	padding: 28rpx 24rpx 0;
}

.brand-footer-card {
	padding: 18rpx 0 8rpx;
	text-align: center;
}

.brand-footer-line {
	width: 72rpx;
	height: 4rpx;
	border-radius: 999rpx;
	margin: 0 auto 18rpx;
	background: linear-gradient(90deg, rgba(111, 142, 117, 0) 0%, rgba(111, 142, 117, 0.45) 50%, rgba(111, 142, 117, 0) 100%);
}

.brand-footer-title {
	font-size: 24rpx;
	font-weight: 600;
	color: #8A958A;
	display: block;
	letter-spacing: 2rpx;
}

.brand-footer-desc {
	font-size: 22rpx;
	color: #A0AAA0;
	display: block;
	margin-top: 10rpx;
	line-height: 1.6;
}

/* 退出登录 */
.logout-btn {
	margin: 24rpx 24rpx 0;
	height: 88rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #FEFEFC;
	border-radius: 24rpx;
	font-size: 28rpx;
	color: $red;
	box-shadow: 0 10rpx 24rpx rgba(77, 112, 77, 0.05);
}
</style>
