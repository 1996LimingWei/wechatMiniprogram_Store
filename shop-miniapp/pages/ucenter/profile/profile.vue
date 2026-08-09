<template>
	<view class="page">
		<!-- 头像区 -->
		<view class="avatar-section" @tap="chooseAvatar">
			<image class="avatar" :src="avatarUrl" mode="aspectFill"></image>
			<view class="avatar-tip">
				<text class="avatar-tip-text">点击更换头像</text>
			</view>
		</view>

		<!-- 表单区 -->
		<view class="form-card">
			<!-- 昵称 -->
			<view class="form-item">
				<text class="form-label">昵称</text>
				<input class="form-input" v-model="nickname" placeholder="请输入昵称" placeholder-class="placeholder" maxlength="20" />
			</view>

			<!-- 手机号 -->
			<view class="form-item form-item-last">
				<text class="form-label">手机号</text>
				<view class="mobile-wrap">
					<text class="mobile-text">{{ mobile || '未绑定' }}</text>
					<button class="mobile-btn" open-type="getPhoneNumber" @getphonenumber="onGetPhoneNumber">
						<text>更换</text>
					</button>
				</view>
			</view>
		</view>

		<!-- 保存按钮 -->
		<view class="save-btn" :class="{ disabled: saving }" @tap="saveProfile">
			<text class="save-btn-text">{{ saving ? '保存中...' : '保存' }}</text>
		</view>
	</view>
</template>

<script>
const util = require('@/utils/util.js');
const api = require('@/utils/api.js');

const DEFAULT_AVATAR = 'https://platform-wxmall.oss-cn-beijing.aliyuncs.com/upload/20180727/150547696d798c.png';

export default {
	data() {
		return {
			nickname: '',
			avatar: '',
			mobile: '',
			avatarChanged: false,
			saving: false
		};
	},
	computed: {
		avatarUrl() {
			return this.avatar || DEFAULT_AVATAR;
		}
	},
	methods: {
		// 选择头像
		chooseAvatar() {
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (res) => {
					this.avatar = res.tempFilePaths[0];
					this.avatarChanged = true;
				}
			});
		},

		// 更换手机号
		onGetPhoneNumber(e) {
			if (e.detail.errMsg !== 'getPhoneNumber:ok') {
				return;
			}
			const phoneCode = e.detail.code;
			if (!phoneCode) {
				util.toast('手机号获取失败');
				return;
			}

			// 先获取 wx.login code，再用 phoneCode 换手机号
			uni.login({
				success: (loginRes) => {
					if (!loginRes.code) {
						util.toast('登录凭证获取失败');
						return;
					}
					// 调用后端接口换取手机号
					util.request(api.AuthPhoneLogin, {
						code: loginRes.code,
						phoneCode: phoneCode
					}, 'POST', 'application/json').then(res => {
						if (res.code === 0 && res.data) {
							// 更新本地 token 和用户信息
							uni.setStorageSync('token', res.data.token);
							uni.setStorageSync('userId', res.data.userId);
							if (res.data.userInfo) {
								this.mobile = res.data.userInfo.mobile || '';
								uni.setStorageSync('userInfo', res.data.userInfo);
							}
							util.toast('手机号已更新');
						}
					}).catch(() => {
						util.toast('手机号更换失败');
					});
				}
			});
		},

		// 上传头像到服务器
		uploadAvatar() {
			return new Promise((resolve, reject) => {
				util.uploadFile('upload/image', this.avatar).then(res => {
					if (res.code === 0 && res.data) {
						resolve(res.data);
					} else {
						reject(new Error('上传失败'));
					}
				}).catch(reject);
			});
		},

		// 保存资料
		async saveProfile() {
			if (this.saving) return;

			const trimNick = (this.nickname || '').trim();
			if (!trimNick) {
				util.toast('请输入昵称');
				return;
			}

			this.saving = true;
			try {
				let avatarUrl = this.avatar;

				// 如果头像改了，先上传
				if (this.avatarChanged && this.avatar.startsWith('wxfile://') || this.avatar.startsWith('http://tmp')) {
					try {
						const uploaded = await this.uploadAvatar();
						avatarUrl = uploaded;
					} catch (e) {
						// 上传失败就用本地路径（开发阶段可以接受）
						console.log('[Profile] 头像上传失败，使用本地路径');
					}
				}

				// 调用更新接口
				const res = await util.request(api.MemberProfile, {
					nickname: trimNick,
					avatar: avatarUrl
				}, 'POST', 'application/json');

				if (res.code === 0 && res.data) {
					// 更新本地存储
					const userInfo = {
						...uni.getStorageSync('userInfo'),
						nickname: res.data.nickname,
						avatar: res.data.avatar,
						mobile: res.data.mobile || '',
						memberLevel: res.data.memberLevel || 1
					};
					uni.setStorageSync('userInfo', userInfo);
					getApp().globalData.userInfo = userInfo;

					uni.showToast({ title: '保存成功', icon: 'success' });
					setTimeout(() => {
						uni.navigateBack();
					}, 800);
				} else {
					util.toast(res.msg || '保存失败');
				}
			} catch (e) {
				console.error('[Profile] 保存异常:', e);
				util.toast('保存失败，请重试');
			} finally {
				this.saving = false;
			}
		}
	},

	onLoad() {
		const userInfo = uni.getStorageSync('userInfo');
		if (userInfo) {
			this.nickname = userInfo.nickname || '';
			this.avatar = userInfo.avatar || '';
			this.mobile = userInfo.mobile || '';
		}
	}
};
</script>

<style lang="scss">
$green: #4D704D;
$green-light: #E8ECE8;
$green-bg: #FDFDF8;
$text-primary: #36454F;
$text-secondary: #667166;
$text-hint: #9A9A9A;

page {
	background: $green-bg;
}

.page {
	min-height: 100vh;
	padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
}

/* 头像区 */
.avatar-section {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 60rpx 0 50rpx;
	background: linear-gradient(145deg, #97AC96 0%, #879F8C 58%, #78907C 100%);
}

.avatar {
	width: 160rpx;
	height: 160rpx;
	border-radius: 50%;
	border: 6rpx solid rgba(255, 255, 255, 0.4);
	box-shadow: 0 10rpx 30rpx rgba(77, 95, 82, 0.2);
}

.avatar-tip {
	margin-top: 20rpx;
}

.avatar-tip-text {
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.8);
}

/* 表单区 */
.form-card {
	margin: 30rpx 24rpx 0;
	background: #FEFEFC;
	border-radius: 24rpx;
	box-shadow: 0 10rpx 24rpx rgba(77, 112, 77, 0.05);
	overflow: hidden;
}

.form-item {
	display: flex;
	align-items: center;
	padding: 32rpx 30rpx;
	border-bottom: 1rpx solid rgba(111, 142, 117, 0.08);
}

.form-item-last {
	border-bottom: none;
}

.form-label {
	font-size: 30rpx;
	color: $text-primary;
	font-weight: 500;
	width: 140rpx;
	flex-shrink: 0;
}

.form-input {
	flex: 1;
	font-size: 30rpx;
	color: $text-primary;
	text-align: right;
}

.placeholder {
	color: $text-hint;
}

/* 手机号行 */
.mobile-wrap {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: flex-end;
}

.mobile-text {
	font-size: 30rpx;
	color: $text-secondary;
	margin-right: 16rpx;
}

.mobile-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	padding: 0 24rpx;
	height: 56rpx;
	background: $green;
	border-radius: 28rpx;
	border: none;
	font-size: 24rpx;
	color: #FEFEFC;
	margin: 0;
	line-height: 56rpx;

	&::after {
		border: none;
	}
}

/* 保存按钮 */
.save-btn {
	margin: 50rpx 24rpx 0;
	height: 96rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, $green 0%, #6B9E6A 100%);
	border-radius: 48rpx;
	box-shadow: 0 12rpx 28rpx rgba(77, 112, 77, 0.15);

	&.disabled {
		opacity: 0.6;
	}
}

.save-btn-text {
	font-size: 32rpx;
	color: #FEFEFC;
	font-weight: 600;
	letter-spacing: 4rpx;
}
</style>
