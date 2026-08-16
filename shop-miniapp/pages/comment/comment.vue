<template>
	<view class="container">
		<view class="tab-bar">
			<view :class="'tab-item ' + (showType == 0 ? 'active' : '')" @tap="switchTab(0)">
				<text class="tab-text">全部({{allCount}})</text>
			</view>
			<view :class="'tab-item ' + (showType == 1 ? 'active' : '')" @tap="switchTab(1)">
				<text class="tab-text">有图({{hasPicCount}})</text>
			</view>
		</view>
		<view class="comment-list">
			<view class="comment-item" v-for="item in comments" :key="item.id">
				<view class="comment-header">
					<view class="comment-user">
						<image class="user-avatar" :src="$imageUrl(item.userInfo.avatar)" @error="$setImageFallback(item.userInfo, 'avatar')"></image>
						<text class="user-name">{{item.userInfo.nickname}}</text>
					</view>
					<text class="comment-time">{{item.addTime}}</text>
				</view>
				<view class="comment-content">{{item.content}}</view>
				<view class="comment-imgs" v-if="item.picList && item.picList.length > 0">
					<image class="comment-pic" v-for="pitem in item.picList" :key="pitem.id" :src="$imageUrl(pitem.picUrl)" mode="aspectFill" @error="$setImageFallback(pitem, 'picUrl')"></image>
				</view>
			</view>
			<view class="empty-view" v-if="comments.length === 0">
				<text class="empty-text">暂无评论</text>
			</view>
		</view>
		<view class="post-entry" @tap="goToPost">发表评价</view>
	</view>
</template>

<script>
	const api = require('@/utils/api.js');
	const util = require("@/utils/util.js");
	export default {
		data() {
			return {
				comments: [],
				allCommentList: [],
				picCommentList: [],
				typeId: 0,
				valueId: 0,
				showType: 0,
				allCount: 0,
				hasPicCount: 0,
				allPage: 1,
				picPage: 1,
				size: 20
			}
		},
		methods: {
			refreshComments: function() {
				this.comments = [];
				this.allCommentList = [];
				this.picCommentList = [];
				this.allPage = 1;
				this.picPage = 1;
				this.getCommentCount();
				this.getCommentList();
			},
			getCommentCount: function() {
				let that = this;
				util.request(api.CommentCount, { valueId: that.valueId, typeId: that.typeId }).then(function(res) {
					if (res.code === 0) {
						that.allCount = res.data.allCount || 0;
						that.hasPicCount = res.data.hasPicCount || 0;
					}
				});
			},
			getCommentList: function() {
				let that = this;
				util.request(api.CommentList, {
					valueId: that.valueId,
					typeId: that.typeId,
					size: that.size,
					page: (that.showType == 0 ? that.allPage : that.picPage),
					showType: that.showType
				}).then(function(res) {
					if (res.code === 0) {
						let data = res.data.data || res.data.records || [];
						if (that.showType == 0) {
							that.allCommentList = that.allCommentList.concat(data);
							that.comments = that.allCommentList;
						} else {
							that.picCommentList = that.picCommentList.concat(data);
							that.comments = that.picCommentList;
						}
					}
				});
			},
			switchTab: function(type) {
				if (this.showType === type) return;
				this.showType = type;
				if (type === 0) {
					this.comments = this.allCommentList;
				} else {
					this.comments = this.picCommentList;
				}
				if (this.comments.length === 0) {
					this.getCommentList();
				}
			},
			goToPost: function() {
				if (!util.getToken()) {
					util.modal('温馨提示', '登录后才能发表评论，是否去登录？', true, confirm => {
						if (!confirm) return;
						uni.setStorageSync('navUrl', '/pages/comment/comment?valueId=' + this.valueId + '&typeId=' + this.typeId);
						uni.navigateTo({ url: '/pages/auth/btnAuth/btnAuth' });
					});
					return;
				}
				uni.navigateTo({
					url: '/pages/commentPost/commentPost?valueId=' + this.valueId + '&typeId=' + this.typeId
				});
			}
		},
		onLoad: function(options) {
			this.typeId = options.typeId;
			this.valueId = options.valueId;
		},
		onShow: function() {
			if (this.valueId) this.refreshComments();
		},
		onReachBottom: function() {
			if (this.showType == 0) {
				if (this.allCount / this.size < this.allPage) return;
				this.allPage++;
			} else {
				if (this.hasPicCount / this.size < this.picPage) return;
				this.picPage++;
			}
			this.getCommentList();
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;

	page {
		background: $green-bg;
	}

	.container {
		padding-top: 90rpx;
	}

	.tab-bar {
		position: fixed;
		top: 0;
		left: 0;
		z-index: 100;
		width: 100%;
		display: flex;
		background: #FEFEFC;
		height: 84rpx;
		box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
	}

	.tab-item {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		height: 84rpx;
	}

	.tab-text {
		font-size: 28rpx;
		color: #666;
		height: 80rpx;
		line-height: 80rpx;
		padding: 0 10rpx;
	}

	.tab-item.active .tab-text {
		color: $green;
		font-weight: 500;
		border-bottom: 4rpx solid $green;
	}

	.comment-list {
		padding: 20rpx 24rpx;
	}

	.comment-item {
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 28rpx;
		margin-bottom: 16rpx;
		box-shadow: 0 2rpx 8rpx rgba(91,140,90,0.06);
	}

	.comment-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 18rpx;
	}

	.comment-user {
		display: flex;
		align-items: center;
	}

	.user-avatar {
		width: 60rpx;
		height: 60rpx;
		border-radius: 50%;
		margin-right: 14rpx;
	}

	.user-name {
		font-size: 26rpx;
		color: #333;
	}

	.comment-time {
		font-size: 22rpx;
		color: #999;
	}

	.comment-content {
		font-size: 28rpx;
		color: #333;
		line-height: 1.7;
		margin-bottom: 16rpx;
	}

	.comment-imgs {
		display: flex;
		flex-wrap: wrap;
	}

	.comment-pic {
		width: 150rpx;
		height: 150rpx;
		border-radius: 8rpx;
		margin-right: 12rpx;
		margin-bottom: 12rpx;
	}

	.empty-view {
		padding: 100rpx 0;
		text-align: center;
	}

	.empty-text {
		font-size: 28rpx;
		color: #999;
	}

	.post-entry {
		position: fixed;
		right: 30rpx;
		bottom: 40rpx;
		padding: 20rpx 34rpx;
		border-radius: 40rpx;
		font-size: 26rpx;
		color: #FEFEFC;
		background: $green;
		box-shadow: 0 8rpx 20rpx rgba(91, 140, 90, 0.24);
	}
</style>
