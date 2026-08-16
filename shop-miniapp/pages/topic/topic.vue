<template>
	<view class="container">
		<view class="topic-list" v-if="topicList.length > 0">
			<navigator class="topic-card" v-for="(item, index) in topicList" :key="index" :url="'../topicDetail/topicDetail?id='+item.id">
				<image class="topic-img" :src="$imageUrl(item.scenePicUrl)" mode="aspectFill" @error="$setImageFallback(item, 'scenePicUrl')"></image>
				<view class="topic-info">
					<text class="topic-title">{{item.title}}</text>
					<text class="topic-desc">{{item.subtitle}}</text>
					<view class="topic-bottom">
						<text class="topic-price">¥{{item.priceInfo}}起</text>
						<text class="topic-tag">查看详情</text>
					</view>
				</view>
			</navigator>
		</view>
		<view class="empty-view" v-else>
			<text class="empty-text">暂无专题</text>
		</view>
		<view class="page-nav" v-if="showPage">
			<view :class="'page-btn ' + (page <= 1 ? 'disabled' : '')" @tap="prevPage">上一页</view>
			<view class="page-info">{{page}} / {{Math.ceil(count / size) || 1}}</view>
			<view :class="'page-btn ' + ((count / size) < page + 1 ? 'disabled' : '')" @tap="nextPage">下一页</view>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js")
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				topicList: [],
				page: 1,
				size: 10,
				count: 0,
				showPage: false
			}
		},
		methods: {
			nextPage: function() {
				if (this.page + 1 > this.count / this.size) return;
				this.page = parseInt(this.page) + 1;
				this.getTopic();
			},
			prevPage: function() {
				if (this.page <= 1) return;
				this.page = parseInt(this.page) - 1;
				this.getTopic();
			},
			getTopic: function() {
				let that = this;
				that.topicList = [];
				that.showPage = false;
				util.request(api.TopicList, {
					page: that.page,
					size: that.size
				}).then(function(res) {
					if (res.code === 0) {
						that.topicList = res.data.records || [];
						that.showPage = true;
						that.count = res.data.total || 0;
					}
				});
			}
		},
		onLoad: function() {
			this.getTopic();
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;
	$red: #CF4A3E;
	$gold: #B8860B;

	page {
		background: $green-bg;
	}

	.container {
		padding: 24rpx;
		padding-bottom: 130rpx;
	}

	.topic-list {
		width: 100%;
	}

	.topic-card {
		display: block;
		width: 100%;
		background: #FEFEFC;
		border-radius: 20rpx;
		overflow: hidden;
		margin-bottom: 24rpx;
		box-shadow: 0 4rpx 16rpx rgba(91,140,90,0.1);
	}

	.topic-img {
		width: 100%;
		height: 360rpx;
	}

	.topic-info {
		padding: 24rpx 28rpx;
	}

	.topic-title {
		display: block;
		font-size: 32rpx;
		font-weight: bold;
		color: #333;
		margin-bottom: 10rpx;
	}

	.topic-desc {
		display: block;
		font-size: 24rpx;
		color: #999;
		margin-bottom: 20rpx;
	}

	.topic-bottom {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.topic-price {
		font-size: 30rpx;
		font-weight: bold;
		color: $red;
	}

	.topic-tag {
		font-size: 22rpx;
		color: $green;
		border: 1rpx solid $green;
		padding: 6rpx 16rpx;
		border-radius: 20rpx;
	}

	.page-nav {
		position: fixed;
		bottom: 0;
		left: 0;
		width: 100%;
		height: 100rpx;
		background: #FEFEFC;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.05);
	}

	.page-btn {
		padding: 14rpx 40rpx;
		font-size: 26rpx;
		color: #FEFEFC;
		background: $green;
		border-radius: 30rpx;

		&.disabled {
			background: #ddd;
			color: #999;
		}
	}

	.page-info {
		font-size: 26rpx;
		color: #666;
		margin: 0 30rpx;
	}

	.empty-view {
		padding-top: 200rpx;
		text-align: center;
	}

	.empty-text {
		font-size: 28rpx;
		color: #999;
	}
</style>
