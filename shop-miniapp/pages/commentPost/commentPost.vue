<template>
	<view class="container">
		<view class="post-card">
			<textarea class="post-textarea" focus v-model="content" maxlength="140"
			 placeholder="留言经过筛选后，对所有人可见" />
			<text class="char-count">{{140 - content.length}}</text>
		</view>
		<view class="img-section">
			<view class="img-list">
				<view class="img-item" v-for="(item, index) in picList" :key="index">
					<image class="img-thumb" :src="item" mode="aspectFill"></image>
					<view class="img-del" @tap="removePic(index)">×</view>
				</view>
				<view class="img-add" v-if="picList.length < 4" @tap="chooseImage">
					<text class="add-icon">+</text>
					<text class="add-text">图片</text>
				</view>
			</view>
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
                orderId: 0,
				content: '',
				picList: [],
				pendingUploads: 0,
				choosing: false,
				posting: false
			}
		},
		methods: {
			chooseImage() {
				let that = this;
				const remain = 4 - that.picList.length - that.pendingUploads;
				if (remain <= 0 || that.choosing || that.posting) return;
				that.choosing = true;
				uni.chooseImage({
					count: remain,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera'],
					success: function(res) {
						const paths = (res.tempFilePaths || []).slice(0, remain);
						paths.forEach(path => {
							that.uploadOne(path);
						});
					},
					complete() { that.choosing = false; }
				});
			},
			uploadOne(filePath) {
				let that = this;
				if (that.picList.length + that.pendingUploads >= 4 || that.posting) return;
				that.pendingUploads += 1;
				return util.uploadFile(api.UploadImage, filePath).then(res => {
					if (res.data && res.data.url) {
						that.picList.push(res.data.url);
					}
				}).catch(() => {
					util.toast('图片上传失败，请重新选择');
				}).finally(() => {
					that.pendingUploads -= 1;
				});
			},
			removePic(index) {
				this.picList.splice(index, 1);
			},
			onPost() {
				let that = this;
				if (that.posting) return;
				if (!that.content.trim()) { util.toast('请填写评论'); return; }
				if (that.pendingUploads > 0 || that.choosing) { util.toast('图片上传中，请稍候'); return; }
				that.posting = true;
				const picUrls = that.picList.join(',');
				util.request(api.CommentPost, {
					typeId: that.typeId,
					valueId: that.valueId,
                    orderId: that.orderId,
					content: that.content.trim(),
					picUrls: picUrls
				}).then(function(res) {
					if (res.code === 0) {
						uni.showToast({
							title: '评论成功',
							complete: function() { uni.navigateBack(); }
						});
					} else { that.posting = false; }
				}).catch(() => { that.posting = false; });
			},
			onClose: function() {
				uni.navigateBack();
			}
		},
		onLoad: function(options) {
			this.typeId = parseInt(options.typeId);
			this.valueId = parseInt(options.valueId);
            this.orderId = Number(options.orderId || 0);
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

	.img-section {
		margin-top: 20rpx;
	}

	.img-list {
		display: flex;
		flex-wrap: wrap;
	}

	.img-item {
		position: relative;
		width: 160rpx;
		height: 160rpx;
		margin-right: 16rpx;
		margin-bottom: 16rpx;
	}

	.img-thumb {
		width: 100%;
		height: 100%;
		border-radius: 12rpx;
	}

	.img-del {
		position: absolute;
		top: -10rpx;
		right: -10rpx;
		width: 40rpx;
		height: 40rpx;
		background: rgba(0,0,0,0.55);
		color: #fff;
		font-size: 28rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		line-height: 1;
	}

	.img-add {
		width: 160rpx;
		height: 160rpx;
		border: 2rpx dashed #c0c0c0;
		border-radius: 12rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		background: #FEFEFC;
	}

	.add-icon {
		font-size: 48rpx;
		color: #999;
		line-height: 1;
	}

	.add-text {
		font-size: 22rpx;
		color: #999;
		margin-top: 4rpx;
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
