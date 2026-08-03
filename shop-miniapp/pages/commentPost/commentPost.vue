<template>
	<view class="container">
		<view class="post-card">
			<textarea class="post-textarea" focus v-model="content" maxlength="140"
			 placeholder="留言经过筛选后，对所有人可见" />
			<text class="char-count">{{140 - content.length}}</text>
		</view>
		<view class="btn-row">
			<view class="btn-cancel" @tap="onClose">取消</view>
			<view class="btn-post" @tap="onPost">发表</view>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				typeId: 0,
				valueId: 0,
				content: ''
			}
		},
		methods: {
			onPost() {
				let that = this;
				if (!that.content) { util.toast('请填写评论'); return; }
				util.request(api.CommentPost, {
					typeId: that.typeId,
					valueId: that.valueId,
					content: that.content
				}).then(function(res) {
					if (res.code === 0) {
						uni.showToast({
							title: '评论成功',
							complete: function() { uni.navigateBack(); }
						});
					}
				});
			},
			onClose: function() {
				uni.navigateBack();
			}
		},
		onLoad: function(options) {
			this.typeId = parseInt(options.typeId);
			this.valueId = parseInt(options.valueId);
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;

	page {
		height: 100%;
		background: $green-bg;
	}

	.container {
		padding: 24rpx;
	}

	.post-card {
		position: relative;
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 24rpx;
		min-height: 340rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.post-textarea {
		width: 100%;
		height: 280rpx;
		font-size: 28rpx;
		color: #333;
		line-height: 1.6;
	}

	.char-count {
		position: absolute;
		bottom: 20rpx;
		right: 24rpx;
		font-size: 24rpx;
		color: #999;
	}

	.btn-row {
		display: flex;
		justify-content: space-between;
		margin-top: 30rpx;
	}

	.btn-cancel {
		flex: 1;
		height: 80rpx;
		line-height: 80rpx;
		text-align: center;
		font-size: 28rpx;
		color: #666;
		background: #FEFEFC;
		border-radius: 40rpx;
		margin-right: 20rpx;
	}

	.btn-post {
		flex: 2;
		height: 80rpx;
		line-height: 80rpx;
		text-align: center;
		font-size: 28rpx;
		color: #FEFEFC;
		font-weight: 500;
		background: linear-gradient(135deg, $green 0%, $green-light 100%);
		border-radius: 40rpx;
	}
</style>
