<template>
	<view class="container">
		<view class="comment-list">
			<view class="comment-item" v-for="(item, index) in comments" :key="item.id">
				<view class="comment-header">
					<view class="comment-user">
						<image class="user-avatar" :src="$imageUrl(item.userInfo.avatar)" @error="$setImageFallback(item.userInfo, 'avatar')"></image>
						<text class="user-name">{{item.userInfo.nickname||''}}</text>
					</view>
					<text class="comment-time">{{item.addTime||''}}</text>
				</view>
				<view class="comment-content">{{item.content||''}}</view>
				<view class="comment-imgs" v-if="item.picList && item.picList.length > 0">
					<image class="comment-pic" v-for="(pitem, pindex) in item.picList" :key="pitem.id"
					 :src="$imageUrl(pitem.picUrl)" mode="aspectFill" @error="$setImageFallback(pitem, 'picUrl')"></image>
				</view>
			</view>
			<view class="empty-view" v-if="comments.length === 0">
				<text class="empty-text">暂无留言</text>
			</view>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				comments: [],
				allCommentList: [],
				typeId: 0,
				valueId: 0,
				allPage: 1,
				size: 20,
				allCount: 0
			}
		},
		methods: {
			getCommentCount: function() {
				let that = this;
				util.request(api.CommentCount, { valueId: that.valueId, typeId: that.typeId }).then(function(res) {
					if (res.code === 0) {
						that.allCount = res.data.allCount || 0;
					}
				});
			},
			getCommentList: function() {
				let that = this;
				util.request(api.CommentList, {
					valueId: that.valueId,
					typeId: that.typeId,
					size: that.size,
					page: that.allPage
				}).then(function(res) {
					if (res.code === 0) {
						let data = res.data.data || res.data.records || [];
						that.allCommentList = that.allCommentList.concat(data);
						that.comments = that.allCommentList;
					}
				});
			}
		},
		onLoad: function(options) {
			this.typeId = options.typeId;
			this.valueId = options.valueId;
			this.getCommentCount();
			this.getCommentList();
		},
		onReachBottom: function() {
			if (this.allCount / this.size < this.allPage) return;
			this.allPage++;
			this.getCommentList();
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-bg: #F6F7F4;

	page {
		background: $green-bg;
	}

	.container {
		padding: 24rpx;
	}

	.comment-list {
		width: 100%;
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
</style>
