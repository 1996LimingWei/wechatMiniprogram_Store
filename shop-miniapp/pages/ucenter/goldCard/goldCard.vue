<template>
	<view class="page">
		<!-- 自定义导航栏 -->
		<view class="nav-bar">
			<view class="nav-status-bar"></view>
			<view class="nav-content">
				<view class="nav-back" @tap="goBack">
					<text class="back-icon">&#x276E;</text>
				</view>
				<text class="nav-title">黄金会员卡</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<view v-if="loading" class="loading-wrap">
			<text class="loading-text">加载中...</text>
		</view>

		<template v-else>
			<!-- 黄金卡 Hero -->
			<view class="hero-section">
				<view class="hero-card">
					<view class="hero-shine"></view>
					<view class="hero-content">
						<text class="hero-crown">👑</text>
						<text class="hero-title">黄金会员卡</text>
						<text class="hero-sub">{{ purchaseEnabled ? '尊享五大权益 · 全年无忧购物' : purchaseMessage }}</text>
						<view class="hero-price-row" v-if="purchaseEnabled">
							<text class="hero-price">¥99</text>
							<text class="hero-price-unit">/年</text>
							<text class="hero-original">¥199</text>
							<view class="hero-save-badge">
								<text class="hero-save-text">省¥100</text>
							</view>
						</view>
						<text class="hero-daily">{{ dailyPrice }}</text>
					</view>
				</view>
			</view>

			<!-- 已是黄金会员 -->
			<view v-if="isGold" class="gold-active-section">
				<view class="gold-active-card">
					<text class="gold-active-icon">✅</text>
					<text class="gold-active-title">您已是黄金会员</text>
					<text class="gold-active-desc">全部权益已生效，尽情享受尊贵体验</text>
				</view>
			</view>

			<!-- 权益列表 -->
			<view class="section" v-if="purchaseEnabled">
				<view class="section-header">
					<text class="section-title">黄金会员权益</text>
				</view>
				<view class="benefit-cards">
					<view class="benefit-card" v-for="(item, index) in benefitsList" :key="index">
						<view class="benefit-card-icon">
							<text>{{ benefitIconMap[item.icon] || '✨' }}</text>
						</view>
						<view class="benefit-card-info">
							<text class="benefit-card-title">{{ item.title }}</text>
							<text class="benefit-card-desc">{{ item.desc }}</text>
						</view>
						<view class="benefit-card-check">
							<text class="check-mark">✓</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 白银 vs 黄金对比 -->
			<view class="section" v-if="purchaseEnabled">
				<view class="section-header">
					<text class="section-title">会员权益对比</text>
				</view>
				<view class="compare-table">
					<view class="compare-header">
						<text class="compare-col compare-label">权益</text>
						<text class="compare-col compare-silver">白银会员</text>
						<text class="compare-col compare-gold">黄金会员</text>
					</view>
					<view class="compare-row">
						<text class="compare-col compare-label">商品折扣</text>
						<text class="compare-col compare-silver">无</text>
						<text class="compare-col compare-gold">全场9折</text>
					</view>
					<view class="compare-row">
						<text class="compare-col compare-label">发货优先级</text>
						<text class="compare-col compare-silver">标准</text>
						<text class="compare-col compare-gold">优先发货</text>
					</view>
					<view class="compare-row">
						<text class="compare-col compare-label">每月优惠券</text>
						<text class="compare-col compare-silver">无</text>
						<text class="compare-col compare-gold">满100减20</text>
					</view>
					<view class="compare-row">
						<text class="compare-col compare-label">专属客服</text>
						<text class="compare-col compare-silver">无</text>
						<text class="compare-col compare-gold">VIP通道</text>
					</view>
					<view class="compare-row">
						<text class="compare-col compare-label">生日礼遇</text>
						<text class="compare-col compare-silver">无</text>
						<text class="compare-col compare-gold">双倍积分</text>
					</view>
				</view>
			</view>

			<!-- 底部操作区 -->
			<view class="bottom-section" v-if="!isGold && purchaseEnabled">
				<view class="bottom-card">
					<view class="bottom-price-info">
						<text class="bottom-label">限时特惠</text>
						<text class="bottom-price">¥99<text class="bottom-price-unit">/年</text></text>
					</view>
					<view class="bottom-btn" @tap="handleSubscribe" :class="{ 'btn-loading': subscribing }">
						<text class="bottom-btn-text">{{ subscribing ? '开通中...' : '立即开通黄金会员' }}</text>
					</view>
				</view>
			</view>
			<view class="bottom-section" v-else-if="!isGold">
				<view class="bottom-card unavailable-card">
					<text class="unavailable-text">{{ purchaseMessage }}</text>
				</view>
			</view>

			<!-- 底部占位 -->
			<view class="bottom-spacer" v-if="isGold"></view>
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
			currentLevel: 1,
			isGold: false,
			price: 9900,
			originalPrice: 19900,
			duration: '365天',
			dailyPrice: '0.27元/天',
			benefitsList: [],
			purchaseEnabled: false,
			purchaseMessage: '黄金会员服务暂未开放',
			subscribing: false
		};
	},
	computed: {
		benefitIconMap() {
			return {
				discount: '🏷️',
				shipping: '🚀',
				coupon: '🎫',
				service: '💬',
				birthday: '🎂'
			};
		}
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},
		async loadGoldCard() {
			try {
				const res = await util.request(api.MemberGoldCard);
				if (res && res.data) {
					const d = res.data;
					this.currentLevel = d.currentLevel || 1;
					this.isGold = d.isGold || false;
					this.price = d.price || 9900;
					this.originalPrice = d.originalPrice || 19900;
					this.duration = d.duration || '365天';
					this.dailyPrice = d.dailyPrice || '0.27元/天';
					this.benefitsList = d.benefits || [];
					this.purchaseEnabled = d.purchaseEnabled === true;
					this.purchaseMessage = d.purchaseMessage || '黄金会员服务暂未开放';
				}
			} catch (e) {
				uni.showToast({ title: '会员信息加载失败，请稍后再试', icon: 'none' });
			} finally {
				this.loading = false;
			}
		},
		async handleSubscribe() {
			if (this.subscribing || !this.purchaseEnabled) return;
			uni.showModal({
				title: '开通黄金会员',
				content: '确认开通黄金会员？',
				confirmColor: '#B8860B',
				confirmText: '确认开通',
				success: async (res) => {
					if (!res.confirm) return;
					this.subscribing = true;
					try {
						const result = await util.request(api.MemberGoldSubscribe, {}, 'POST');
						if (result && result.data) {
							uni.showToast({
								title: result.data.message || '开通成功',
								icon: 'none',
								duration: 2000
							});
							this.isGold = true;
							this.currentLevel = 2;
						}
					} catch (e) {
						uni.showToast({ title: '开通失败，请重试', icon: 'none' });
					} finally {
						this.subscribing = false;
					}
				}
			});
		}
	},
	onShow() {
		this.loading = true;
		this.loadGoldCard();
	}
};
</script>

<style lang="scss" scoped>
$green: #4D704D;
$green-bg: #FDFDF8;
$text-primary: #36454F;
$text-secondary: #667166;
$gold-dark: #B8860B;
$gold-mid: #DAA520;
$gold-light: #FAFAD2;
$gold-bg: #FFFBF0;

page {
	background: $green-bg;
}

.page {
	min-height: 100vh;
	padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
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

/* Hero */
.hero-section {
	padding: 16rpx 24rpx 32rpx;
}
.hero-card {
	background: linear-gradient(145deg, #C69C3F 0%, #B8860B 40%, #8B6508 100%);
	border-radius: 28rpx;
	padding: 48rpx 32rpx 40rpx;
	position: relative;
	overflow: hidden;
}
.hero-shine {
	position: absolute;
	top: -40rpx;
	right: -40rpx;
	width: 280rpx;
	height: 280rpx;
	background: radial-gradient(circle, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0) 70%);
	border-radius: 50%;
}
.hero-content {
	position: relative;
	z-index: 1;
	text-align: center;
}
.hero-crown {
	font-size: 64rpx;
	display: block;
}
.hero-title {
	display: block;
	font-size: 42rpx;
	font-weight: 800;
	color: #FEFEFC;
	margin-top: 12rpx;
	letter-spacing: 4rpx;
}
.hero-sub {
	display: block;
	font-size: 24rpx;
	color: rgba(255,255,255,0.7);
	margin-top: 10rpx;
}
.hero-price-row {
	display: flex;
	align-items: baseline;
	justify-content: center;
	gap: 6rpx;
	margin-top: 28rpx;
}
.hero-price {
	font-size: 56rpx;
	font-weight: 800;
	color: #FEFEFC;
}
.hero-price-unit {
	font-size: 24rpx;
	color: rgba(255,255,255,0.7);
}
.hero-original {
	font-size: 24rpx;
	color: rgba(255,255,255,0.4);
	text-decoration: line-through;
	margin-left: 12rpx;
}
.hero-save-badge {
	background: rgba(255,77,77,0.9);
	padding: 4rpx 14rpx;
	border-radius: 12rpx;
	margin-left: 12rpx;
}
.hero-save-text {
	font-size: 20rpx;
	color: #FEFEFC;
	font-weight: 600;
}
.hero-daily {
	display: block;
	font-size: 22rpx;
	color: rgba(255,255,255,0.5);
	margin-top: 8rpx;
}

/* 已是黄金会员 */
.gold-active-section {
	padding: 0 24rpx;
	margin-bottom: 28rpx;
}
.gold-active-card {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 36rpx 24rpx;
	background: $gold-bg;
	border-radius: 20rpx;
	border: 1rpx solid rgba(218,165,32,0.15);
}
.gold-active-icon {
	font-size: 56rpx;
}
.gold-active-title {
	font-size: 32rpx;
	font-weight: 700;
	color: $gold-dark;
	margin-top: 12rpx;
}
.gold-active-desc {
	font-size: 24rpx;
	color: #8B7355;
	margin-top: 8rpx;
}

/* 区块 */
.section {
	padding: 0 24rpx;
	margin-bottom: 28rpx;
}
.section-header {
	margin-bottom: 20rpx;
}
.section-title {
	font-size: 30rpx;
	font-weight: 600;
	color: $text-primary;
}

/* 权益卡片 */
.benefit-cards {
	display: flex;
	flex-direction: column;
	gap: 16rpx;
}
.benefit-card {
	display: flex;
	align-items: center;
	padding: 28rpx 24rpx;
	background: #FEFEFC;
	border-radius: 20rpx;
	box-shadow: 0 4rpx 16rpx rgba(77,112,77,0.06);
	gap: 20rpx;
}
.benefit-card-icon {
	width: 72rpx;
	height: 72rpx;
	border-radius: 16rpx;
	background: $gold-light;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 36rpx;
	flex-shrink: 0;
}
.benefit-card-info {
	flex: 1;
}
.benefit-card-title {
	display: block;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}
.benefit-card-desc {
	display: block;
	font-size: 24rpx;
	color: $text-secondary;
	margin-top: 4rpx;
}
.benefit-card-check {
	width: 40rpx;
	height: 40rpx;
	border-radius: 50%;
	background: $gold-dark;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.check-mark {
	font-size: 22rpx;
	color: #FEFEFC;
	font-weight: 700;
}

/* 对比表 */
.compare-table {
	background: #FEFEFC;
	border-radius: 20rpx;
	overflow: hidden;
	box-shadow: 0 4rpx 16rpx rgba(77,112,77,0.06);
}
.compare-header {
	display: flex;
	background: #F8F8F3;
	padding: 18rpx 0;
}
.compare-row {
	display: flex;
	padding: 20rpx 0;
	border-bottom: 1rpx solid #F5F5F0;
}
.compare-row:last-child {
	border-bottom: none;
}
.compare-col {
	flex: 1;
	text-align: center;
	font-size: 24rpx;
	padding: 0 8rpx;
}
.compare-label {
	color: $text-secondary;
	text-align: left;
	padding-left: 24rpx;
}
.compare-silver {
	color: $text-secondary;
}
.compare-gold {
	color: $gold-dark;
	font-weight: 600;
}
.compare-header .compare-col {
	font-weight: 600;
	font-size: 24rpx;
}
.compare-header .compare-gold {
	color: $gold-dark;
}

/* 底部操作区 */
.bottom-section {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
	background: $green-bg;
	z-index: 100;
	box-shadow: 0 -4rpx 16rpx rgba(0,0,0,0.06);
}
.bottom-card {
	display: flex;
	align-items: center;
	gap: 20rpx;
}
.unavailable-card {
	justify-content: center;
	min-height: 72rpx;
}
.unavailable-text {
	font-size: 24rpx;
	line-height: 1.5;
	color: $text-secondary;
	text-align: center;
}
.bottom-price-info {
	flex-shrink: 0;
}
.bottom-label {
	display: block;
	font-size: 20rpx;
	color: $gold-dark;
	font-weight: 500;
}
.bottom-price {
	font-size: 40rpx;
	font-weight: 800;
	color: $gold-dark;
}
.bottom-price-unit {
	font-size: 22rpx;
	font-weight: 500;
}
.bottom-btn {
	flex: 1;
	background: linear-gradient(135deg, $gold-mid 0%, $gold-dark 100%);
	padding: 24rpx 0;
	border-radius: 44rpx;
	text-align: center;
	box-shadow: 0 8rpx 24rpx rgba(184,134,11,0.3);
}
.bottom-btn.btn-loading {
	opacity: 0.6;
}
.bottom-btn-text {
	font-size: 30rpx;
	font-weight: 700;
	color: #FEFEFC;
}

.bottom-spacer {
	height: 40rpx;
}
</style>
