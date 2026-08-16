<template>
	<view class="container">
		<view class="form-card">
			<picker @change="bindPickerChange" v-model="index" :range="array">
				<view class="form-item form-item--picker">
					<text class="form-label">反馈类型</text>
					<text class="picker-value">{{array[index]}}</text>
					<text class="picker-arrow">›</text>
				</view>
			</picker>
			<view class="form-item form-item--textarea">
				<textarea class="form-textarea" placeholder="对我们的商品、服务有什么建议吗？请告诉我们..."
				 @input="contentInput" :maxlength="500" v-model="content"></textarea>
				<text class="text-count">{{contentLength}}/500</text>
			</view>
			<view class="form-item">
				<text class="form-label">手机号码</text>
				<input class="form-input" :maxlength="11" type="number" placeholder="方便我们与您联系"
				 @input="mobileInput" v-model="mobile" />
			</view>
		</view>
		<view class="submit-btn" :class="{ disabled: submitting }" @tap="submitFeedback">{{ submitting ? '提交中...' : '提交反馈' }}</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				array: ['请选择反馈类型', '商品相关', '物流状况', '客户服务', '优惠活动', '功能异常', '产品建议', '其他'],
				index: 0,
				content: '',
				contentLength: 0,
				mobile: '',
				submitting: false
			}
		},
		methods: {
			bindPickerChange: function(e) {
				this.index = e.detail.value;
			},
			mobileInput: function(e) {
				this.mobile = e.detail.value;
			},
			contentInput: function(e) {
				this.contentLength = e.detail.cursor;
				this.content = e.detail.value;
			},
			submitFeedback: function() {
				let that = this;
				if (that.submitting) return;
				if (that.index == 0) { util.toast('请选择反馈类型'); return; }
				if (that.content.trim().length < 5) { util.toast('反馈内容不少于5个字符'); return; }
				if (that.mobile && !/^1\d{10}$/.test(that.mobile)) { util.toast('请输入正确的手机号码'); return; }

				that.submitting = true;
				util.request(api.FeedbackAdd, {
					mobile: that.mobile,
					type: Number(that.index),
					content: that.content.trim()
				}, "POST", "application/json").then(function(res) {
					if (res.code === 0) {
						uni.showToast({
							title: '提交成功',
							icon: 'success',
							duration: 2000,
							complete: function() {
								setTimeout(function() {
									uni.switchTab({ url: '/pages/ucenter/index/index' });
								}, 2000)
							}
						});
					} else {
						util.toast(res.msg || '提交失败');
					}
				}).catch(() => util.toast('提交失败，请稍后重试')).finally(() => { that.submitting = false; });
			}
		},
		onLoad() {
			this.mobile = (uni.getStorageSync('userInfo') || {}).mobile || '';
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;

	page {
		background: $green-bg;
		min-height: 100%;
	}

	.container {
		padding: 24rpx;
		padding-bottom: 140rpx;
	}

	.form-card {
		background: #FEFEFC;
		border-radius: 20rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(91,140,90,0.08);
	}

	.form-item {
		display: flex;
		align-items: center;
		padding: 28rpx 30rpx;
		border-bottom: 1rpx solid #f5f5f5;

		&:last-child {
			border-bottom: none;
		}

		&--picker {
			justify-content: space-between;
		}

		&--textarea {
			flex-direction: column;
			align-items: stretch;
		}
	}

	.form-label {
		font-size: 28rpx;
		color: #333;
		font-weight: 500;
		margin-right: 20rpx;
		white-space: nowrap;
	}

	.form-input {
		flex: 1;
		font-size: 28rpx;
		color: #333;
	}

	.picker-value {
		flex: 1;
		font-size: 28rpx;
		color: #666;
		text-align: right;
	}

	.picker-arrow {
		font-size: 36rpx;
		color: #ccc;
		margin-left: 10rpx;
	}

	.form-textarea {
		width: 100%;
		height: 240rpx;
		font-size: 28rpx;
		color: #333;
		line-height: 1.6;
	}

	.text-count {
		text-align: right;
		font-size: 22rpx;
		color: #999;
		margin-top: 10rpx;
	}

	.submit-btn {
		position: fixed;
		bottom: 30rpx;
		left: 24rpx;
		right: 24rpx;
		height: 90rpx;
		line-height: 90rpx;
		text-align: center;
		background: linear-gradient(135deg, $green 0%, $green-light 100%);
		border-radius: 45rpx;
		color: #FEFEFC;
		font-size: 30rpx;
		font-weight: 500;
		box-shadow: 0 6rpx 24rpx rgba(91,140,90,0.3);
	}
	.submit-btn.disabled { opacity: 0.65; }
</style>
