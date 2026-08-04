<template>
	<view class="container">
		<view class="content-area">
			<uParse :content="topic.content" noData="" />
		</view>

		<view class="comment-section">
			<view class="section-header">
				<text class="section-title">精选留言</text>
				<view class="comment-add" @tap="postComment">
					<text>写留言</text>
				</view>
			</view>
			<view class="comment-list" v-if="commentList.length > 0">
				<view class="comment-item" v-for="(item, index) in commentList" :key="item.id">
					<view class="comment-user">
						<image class="user-avatar" :src="item.userInfo.avatar"></image>
						<view class="user-info">
							<text class="user-name">{{item.userInfo.nickname}}</text>
							<text class="comment-time">{{item.addTime}}</text>
						</view>
					</view>
					<view class="comment-content">{{item.content}}</view>
				</view>
				<navigator class="load-more" v-if="commentCount > 5"
				 :url="'/pages/topicComment/topicComment?valueId='+topic.id+'&typeId=1'">
					查看更多留言 ›
				</navigator>
			</view>
			<view class="no-comment" v-else>
				<text class="no-comment-text">暂无留言，快来抢沙发~</text>
			</view>
		</view>

		<view class="goods-section" v-if="goodsList.length > 0">
			<view class="section-header">
				<text class="section-title">专题好物</text>
			</view>
			<view class="goods-list">
				<view class="goods-item" v-for="(item, index) in goodsList" :key="index" @tap="goToGoods(item.id)">
					<image class="goods-img" :src="item.listPicUrl" mode="aspectFill"></image>
					<view class="goods-info">
						<text class="goods-name">{{item.name}}</text>
						<text class="goods-brief">{{item.goodsBrief}}</text>
						<view class="goods-bottom">
							<text class="goods-price">¥{{item.retailPrice}}</text>
							<view class="goods-btn">去看看</view>
						</view>
					</view>
				</view>
			</view>
		</view>

		<view class="recommend-section" v-if="topicList.length > 0">
			<view class="section-header">
				<text class="section-title">相关专题</text>
			</view>
			<view class="recommend-list">
				<navigator class="recommend-item" v-for="(item, index) in topicList" :key="index"
				 :url="'../topicDetail/topicDetail?id='+item.id">
					<image class="recommend-img" :src="item.scenePicUrl" mode="aspectFill"></image>
					<text class="recommend-title">{{item.title}}</text>
				</navigator>
			</view>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js")
	const api = require('@/utils/api.js');
	import uParse from '@/components/uParse/src/wxParse'
	export default {
		components: { uParse },
		data() {
			return {
				id: 0,
				topic: {},
				goodsList: [],
				topicList: [],
				commentCount: 0,
				commentList: []
			}
		},
		methods: {
			getCommentList() {
				let that = this;
				util.request(api.CommentList, {
					valueId: that.id,
					typeId: 1,
					size: 5
				}).then(function(res) {
					if (res.code === 0) {
						that.commentList = res.data.data || [];
						that.commentCount = res.data.count || 0;
					}
				});
			},
			goToGoods(id) {
				uni.navigateTo({ url: '/pages/goods/goods?id=' + id });
			},
			postComment() {
				uni.navigateTo({ url: '/pages/commentPost/commentPost?valueId=' + this.id + '&typeId=1' });
			}
		},
		onShow: function() {
			this.getCommentList();
		},
		onLoad: function(options) {
			let that = this;
			that.id = parseInt(options.id);
			util.request(api.TopicDetail, { id: that.id }).then(function(res) {
				if (res.code === 0) {
					that.topic = res.data;
					that.goodsList = res.data.goodsList || [];
				}
			});
			util.request(api.TopicRelated, { id: that.id }).then(function(res) {
				if (res.code === 0) {
					that.topicList = res.data || [];
				}
			});
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
		padding-bottom: 30rpx;
	}

	.content-area {
		width: 100%;
		background: #FEFEFC;
		font-size: 0;

		image {
			display: inline-block;
			width: 100%;
		}
	}

	.comment-section {
		margin: 20rpx 24rpx;
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 0 28rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.section-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 90rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: bold;
		color: #333;
	}

	.comment-add {
		font-size: 24rpx;
		color: $green;
		border: 1rpx solid $green;
		padding: 6rpx 18rpx;
		border-radius: 20rpx;
	}

	.comment-list {
		padding: 10rpx 0;
	}

	.comment-item {
		padding: 24rpx 0;
		border-bottom: 1rpx solid #f5f5f5;

		&:last-child {
			border-bottom: none;
		}
	}

	.comment-user {
		display: flex;
		align-items: center;
		margin-bottom: 16rpx;
	}

	.user-avatar {
		width: 60rpx;
		height: 60rpx;
		border-radius: 50%;
		margin-right: 16rpx;
	}

	.user-info {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: space-between;
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
		line-height: 1.6;
	}

	.load-more {
		text-align: center;
		font-size: 26rpx;
		color: $green;
		padding: 24rpx 0;
	}

	.no-comment {
		padding: 50rpx 0;
		text-align: center;
	}

	.no-comment-text {
		font-size: 26rpx;
		color: #999;
	}

	.recommend-section {
		margin: 0 24rpx;
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 0 28rpx 24rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.recommend-list {
		padding-top: 16rpx;
	}

	.recommend-item {
		display: block;
		width: 100%;
		margin-bottom: 20rpx;
		border-radius: 12rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
	}

	.recommend-img {
		width: 100%;
		height: 280rpx;
	}

	.recommend-title {
		display: block;
		padding: 16rpx 20rpx;
		font-size: 26rpx;
		color: #333;
		background: #FEFEFC;
	}

	/* 专题好物 */
	.goods-section {
		margin: 0 24rpx 24rpx;
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 0 28rpx 24rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.goods-list {
		padding-top: 16rpx;
	}

	.goods-item {
		display: flex;
		padding: 20rpx 0;
		border-bottom: 1rpx solid #F0F0F0;

		&:last-child {
			border-bottom: none;
		}
	}

	.goods-img {
		width: 180rpx;
		height: 180rpx;
		border-radius: 12rpx;
		margin-right: 20rpx;
		flex-shrink: 0;
	}

	.goods-info {
		flex: 1;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
	}

	.goods-name {
		font-size: 28rpx;
		color: #333;
		font-weight: 600;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		overflow: hidden;
	}

	.goods-brief {
		font-size: 24rpx;
		color: #999;
		margin-top: 8rpx;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.goods-bottom {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 12rpx;
	}

	.goods-price {
		font-size: 32rpx;
		color: #CF4A3E;
		font-weight: 700;
	}

	.goods-btn {
		font-size: 24rpx;
		color: #FEFEFC;
		background: $green;
		padding: 10rpx 24rpx;
		border-radius: 24rpx;
	}
</style>
