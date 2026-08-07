<template>
	<view class="page">
		<!-- 自定义导航栏 -->
		<view class="nav-bar">
			<view class="nav-status-bar"></view>
			<view class="nav-content">
				<view class="nav-back" @tap="goBack">
					<text class="back-icon">&#x276E;</text>
				</view>
				<text class="nav-title">会员中心</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<view v-if="loading" class="loading-wrap">
			<text class="loading-text">加载中...</text>
		</view>

		<template v-else>
			<!-- 会员卡区域 -->
			<view class="member-card-section" :class="isGold ? 'gold-theme' : 'silver-theme'">
				<view class="member-card">
					<view class="card-top">
						<view class="card-user">
							<image class="card-avatar" :src="avatarUrl" mode="aspectFill"></image>
							<view class="card-user-info">
								<text class="card-nickname">{{ nickname || '微信用户' }}</text>
								<view class="card-level-badge">
									<text class="badge-text">{{ memberLevelName }}</text>
								</view>
							</view>
						</view>
						<view class="card-brand">
							<text class="brand-name">药食同源</text>
							<text class="brand-sub">MEMBER</text>
						</view>
					</view>
					<view class="card-bottom">
						<text class="card-level-label">{{ isGold ? 'GOLD MEMBER' : 'SILVER MEMBER' }}</text>
						<text class="card-number">NO.{{ userId }}</text>
					</view>
				</view>
			</view>

			<!-- 当前权益 -->
			<view class="section">
				<view class="section-header">
					<text class="section-title">当前权益</text>
					<text class="section-tag">{{ memberLevelName }}</text>
				</view>
				<view class="benefit-list">
					<view class="benefit-item">
						<view class="benefit-icon" :class="isGold ? 'gold-icon' : 'silver-icon'">
							<text>{{ isGold ? '🏷️' : '📦' }}</text>
						</view>
						<view class="benefit-info">
							<text class="benefit-title">专享折扣</text>
							<text class="benefit-desc">{{ benefits.discount }}</text>
						</view>
					</view>
					<view class="benefit-item">
						<view class="benefit-icon" :class="isGold ? 'gold-icon' : 'silver-icon'">
							<text>{{ isGold ? '🚀' : '📮' }}</text>
						</view>
						<view class="benefit-info">
							<text class="benefit-title">发货优先级</text>
							<text class="benefit-desc">{{ benefits.shipping }}</text>
						</view>
					</view>
					<view class="benefit-item">
						<view class="benefit-icon" :class="isGold ? 'gold-icon' : 'silver-icon'">
							<text>{{ isGold ? '🎫' : '📋' }}</text>
						</view>
						<view class="benefit-info">
							<text class="benefit-title">专属优惠券</text>
							<text class="benefit-desc">{{ benefits.coupon }}</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 黄金卡推广（白银会员才显示） -->
			<view v-if="!isGold" class="section">
				<view class="gold-promo" @tap="goGoldCard">
					<view class="promo-bg"></view>
					<view class="promo-content">
						<view class="promo-left">
							<text class="promo-title">黄金会员卡</text>
							<text class="promo-desc">专享折扣 · 优先发货 · 每月优惠券</text>
							<view class="promo-price-row">
								<text class="promo-price">¥99</text>
								<text class="promo-price-unit">/年</text>
								<text class="promo-original">¥199</text>
							</view>
						</view>
						<view class="promo-btn">
							<text class="promo-btn-text">立即开通</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 已是黄金会员提示 -->
			<view v-else class="section">
				<view class="gold-active-notice">
					<text class="gold-active-icon">👑</text>
					<text class="gold-active-text">您已是黄金会员，尊享全部权益</text>
				</view>
			</view>

			<!-- 会员说明 -->
			<view class="section">
				<view class="section-header">
					<text class="section-title">会员说明</text>
				</view>
				<view class="member-rules">
					<view class="rule-item">
						<text class="rule-num">1</text>
						<text class="rule-text">微信手机号登录后自动绑定白银会员身份</text>
					</view>
					<view class="rule-item">
						<text class="rule-num">2</text>
						<text class="rule-text">开通黄金卡后享受全场9折和优先发货权益</text>
					</view>
					<view class="rule-item">
						<text class="rule-num">3</text>
						<text class="rule-text">黄金会员有效期365天，到期后自动恢复为白银会员</text>
					</view>
					<view class="rule-item">
						<text class="rule-num">4</text>
						<text class="rule-text">会员权益不可转让，仅限本人使用</text>
					</view>
				</view>
			</view>
		</template>
	</view>
</template>

<script>
const util = require('@/utils/util.js');
const api = require('@/utils/api.js');

export default {
	data() {
		return {
			loading: true,
			memberLevel: 1,
			memberLevelName: '白银会员',
			nickname: '',
			avatar: '',
			mobile: '',
			benefits: {
				discount: '无专属折扣',
				shipping: '标准发货',
				coupon: '无专属优惠券'
			},
			userId: ''
		};
	},
	computed: {
		isGold() {
			return this.memberLevel === 2;
		},
		avatarUrl() {
			return this.avatar || '/static/images/logo.png';
		}
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},
		goGoldCard() {
			uni.navigateTo({ url: '/pages/ucenter/goldCard/goldCard' });
		},
		async loadCenter() {
			try {
				const res = await util.request(api.MemberCenter);
				if (res && res.data) {
					const d = res.data;
					this.memberLevel = d.memberLevel || 1;
					this.memberLevelName = d.memberLevelName || '白银会员';
					this.nickname = d.nickname || '';
					this.avatar = d.avatar || '';
					this.mobile = d.mobile || '';
					if (d.benefits) {
						this.benefits = d.benefits;
					}
				}
				let userInfo = uni.getStorageSync('userInfo');
				if (userInfo) {
					this.userId = userInfo.userId || '';
				}
			} catch (e) {
				console.error('loadCenter error', e);
			} finally {
				this.loading = false;
			}
		}
	},
	onShow() {
		this.loading = true;
		this.loadCenter();
	}
};
</script>

<style lang="scss" scoped>
$green: #4D704D;
$green-light: #E8ECE8;
$green-bg: #FDFDF8;
$text-primary: #36454F;
$text-secondary: #667166;
$gold-dark: #B8860B;
$gold-mid: #DAA520;
$gold-light: #FAFAD2;
$gold-bg: #FFFBF0;
$silver-dark: #6B7B8D;
$silver-mid: #A0AEC0;
$silver-light: #EDF2F7;

page {
	background: $green-bg;
}

.page {
	min-height: 100vh;
	padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

/* 导航栏 */
.nav-bar {
	background: $green-bg;
	position: sticky;
	top: 0;
	z-index: 100;
}
.nav-status-bar {
	height: var(--status-bar-height, 44px);
}
.nav-content {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 88rpx;
	padding: 0 24rpx;
}
.nav-back {
	width: 64rpx;
	height: 64rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
.back-icon {
	font-size: 32rpx;
	color: $text-primary;
}
.nav-title {
	font-size: 34rpx;
	font-weight: 600;
	color: $text-primary;
}
.nav-placeholder {
	width: 64rpx;
}

.loading-wrap {
	padding: 200rpx 0;
	text-align: center;
}
.loading-text {
	font-size: 28rpx;
	color: $text-secondary;
}

/* 会员卡 */
.member-card-section {
	padding: 16rpx 24rpx 32rpx;
}
.member-card {
	border-radius: 24rpx;
	padding: 36rpx 32rpx 28rpx;
	position: relative;
	overflow: hidden;
}
.silver-theme .member-card {
	background: linear-gradient(145deg, #5A7A5A 0%, #4D704D 50%, #3D5C3D 100%);
}
.gold-theme .member-card {
	background: linear-gradient(145deg, #B8860B 0%, #996515 50%, #7A4F0E 100%);
}
.card-top {
	display: flex;
	justify-content: space-between;
	align-items: flex-start;
}
.card-user {
	display: flex;
	align-items: center;
	gap: 20rpx;
}
.card-avatar {
	width: 88rpx;
	height: 88rpx;
	border-radius: 50%;
	border: 3rpx solid rgba(255,255,255,0.35);
}
.card-user-info {
	display: flex;
	flex-direction: column;
	gap: 8rpx;
}
.card-nickname {
	font-size: 32rpx;
	font-weight: 700;
	color: #FEFEFC;
}
.card-level-badge {
	display: inline-flex;
	align-items: center;
	padding: 4rpx 16rpx;
	border-radius: 20rpx;
	background: rgba(255,255,255,0.2);
}
.badge-text {
	font-size: 20rpx;
	color: rgba(255,255,255,0.9);
	font-weight: 500;
}
.card-brand {
	text-align: right;
}
.brand-name {
	display: block;
	font-size: 22rpx;
	color: rgba(255,255,255,0.7);
	font-weight: 500;
}
.brand-sub {
	display: block;
	font-size: 18rpx;
	color: rgba(255,255,255,0.4);
	letter-spacing: 4rpx;
	margin-top: 4rpx;
}
.card-bottom {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-top: 28rpx;
	padding-top: 20rpx;
	border-top: 1rpx solid rgba(255,255,255,0.12);
}
.card-level-label {
	font-size: 22rpx;
	color: rgba(255,255,255,0.5);
	letter-spacing: 3rpx;
	font-weight: 600;
}
.card-number {
	font-size: 20rpx;
	color: rgba(255,255,255,0.35);
	font-family: monospace;
}

/* 区块 */
.section {
	padding: 0 24rpx;
	margin-bottom: 28rpx;
}
.section-header {
	display: flex;
	align-items: center;
	gap: 12rpx;
	margin-bottom: 20rpx;
}
.section-title {
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}
.section-tag {
	font-size: 20rpx;
	color: $green;
	background: $green-light;
	padding: 4rpx 14rpx;
	border-radius: 8rpx;
	font-weight: 500;
}

/* 权益列表 */
.benefit-list {
	background: #FEFEFC;
	border-radius: 20rpx;
	overflow: hidden;
	box-shadow: 0 4rpx 16rpx rgba(77,112,77,0.06);
}
.benefit-item {
	display: flex;
	align-items: center;
	padding: 28rpx 24rpx;
	gap: 20rpx;
	border-bottom: 1rpx solid #F5F5F0;
}
.benefit-item:last-child {
	border-bottom: none;
}
.benefit-icon {
	width: 72rpx;
	height: 72rpx;
	border-radius: 16rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 36rpx;
	flex-shrink: 0;
}
.silver-icon {
	background: $silver-light;
}
.gold-icon {
	background: $gold-light;
}
.benefit-info {
	flex: 1;
}
.benefit-title {
	display: block;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}
.benefit-desc {
	display: block;
	font-size: 24rpx;
	color: $text-secondary;
	margin-top: 4rpx;
}

/* 黄金卡推广 */
.gold-promo {
	border-radius: 20rpx;
	overflow: hidden;
	position: relative;
	background: $gold-bg;
	border: 1rpx solid rgba(218,165,32,0.15);
}
.promo-content {
	display: flex;
	align-items: center;
	padding: 32rpx 24rpx;
	gap: 16rpx;
}
.promo-left {
	flex: 1;
}
.promo-title {
	display: block;
	font-size: 32rpx;
	font-weight: 700;
	color: $gold-dark;
}
.promo-desc {
	display: block;
	font-size: 22rpx;
	color: #8B7355;
	margin-top: 8rpx;
}
.promo-price-row {
	display: flex;
	align-items: baseline;
	gap: 4rpx;
	margin-top: 12rpx;
}
.promo-price {
	font-size: 40rpx;
	font-weight: 800;
	color: $gold-dark;
}
.promo-price-unit {
	font-size: 22rpx;
	color: #8B7355;
}
.promo-original {
	font-size: 22rpx;
	color: #B0A090;
	text-decoration: line-through;
	margin-left: 8rpx;
}
.promo-btn {
	background: linear-gradient(135deg, $gold-mid 0%, $gold-dark 100%);
	padding: 16rpx 32rpx;
	border-radius: 36rpx;
	flex-shrink: 0;
	box-shadow: 0 6rpx 16rpx rgba(184,134,11,0.25);
}
.promo-btn-text {
	font-size: 26rpx;
	font-weight: 600;
	color: #FEFEFC;
	white-space: nowrap;
}

/* 已是黄金会员 */
.gold-active-notice {
	display: flex;
	align-items: center;
	gap: 12rpx;
	padding: 28rpx 24rpx;
	background: $gold-bg;
	border-radius: 20rpx;
	border: 1rpx solid rgba(218,165,32,0.15);
}
.gold-active-icon {
	font-size: 40rpx;
}
.gold-active-text {
	font-size: 28rpx;
	font-weight: 500;
	color: $gold-dark;
}

/* 会员说明 */
.member-rules {
	background: #FEFEFC;
	border-radius: 20rpx;
	padding: 8rpx 24rpx;
	box-shadow: 0 4rpx 16rpx rgba(77,112,77,0.06);
}
.rule-item {
	display: flex;
	align-items: flex-start;
	gap: 16rpx;
	padding: 20rpx 0;
	border-bottom: 1rpx solid #F5F5F0;
}
.rule-item:last-child {
	border-bottom: none;
}
.rule-num {
	width: 36rpx;
	height: 36rpx;
	border-radius: 50%;
	background: $green-light;
	color: $green;
	font-size: 22rpx;
	font-weight: 600;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	margin-top: 2rpx;
}
.rule-text {
	font-size: 26rpx;
	color: $text-secondary;
	line-height: 1.6;
	flex: 1;
}
</style>
