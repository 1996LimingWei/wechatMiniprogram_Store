<template>
	<view class="page">
		<view class="avatar-section">
			<image class="avatar" :src="avatarUrl" mode="aspectFill"></image>
		</view>
		<view class="info-card">
			<view class="info-item">
				<text class="info-label">昵称</text>
				<text class="info-value">{{ nickname || '未设置' }}</text>
			</view>
			<view class="info-item">
				<text class="info-label">手机号</text>
				<text class="info-value">{{ maskedMobile }}</text>
			</view>
		</view>
	</view>
</template>

<script>
const DEFAULT_AVATAR = '/static/images/logo.png';

export default {
	data() {
		return { nickname: '', avatar: '', mobile: '' };
	},
	computed: {
		avatarUrl() {
			return this.avatar || DEFAULT_AVATAR;
		},
		maskedMobile() {
			if (!this.mobile || this.mobile.length !== 11) return '未绑定';
			return `${this.mobile.slice(0, 3)}****${this.mobile.slice(7)}`;
		}
	},
	onShow() {
		const userInfo = uni.getStorageSync('userInfo') || {};
		this.nickname = userInfo.nickname || '';
		this.avatar = userInfo.avatar || '';
		this.mobile = userInfo.mobile || '';
	}
};
</script>

<style lang="scss">
page { background: #FDFDF8; }
.page { min-height: 100vh; padding-bottom: calc(40rpx + env(safe-area-inset-bottom)); }
.avatar-section { display: flex; justify-content: center; padding: 64rpx 0 48rpx; background: #879F8C; }
.avatar { width: 160rpx; height: 160rpx; border-radius: 50%; border: 6rpx solid rgba(255, 255, 255, 0.4); }
.info-card { margin: 30rpx 24rpx 0; background: #FEFEFC; border-radius: 16rpx; overflow: hidden; box-shadow: 0 8rpx 24rpx rgba(77, 112, 77, 0.06); }
.info-item { display: flex; align-items: center; justify-content: space-between; min-height: 100rpx; padding: 0 30rpx; border-bottom: 1rpx solid rgba(111, 142, 117, 0.1); }
.info-item:last-child { border-bottom: 0; }
.info-label { font-size: 30rpx; color: #36454F; }
.info-value { max-width: 440rpx; font-size: 30rpx; color: #667166; text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
