<template>
	<view class="container">
		<view class="brand-list">
			<navigator :url="'../brandDetail/brandDetail?id='+item.id" class="brand-card" v-for="(item, index) in brandList" :key="index">
				<view class="brand-img">
					<image :src="$imageUrl(item.appListPicUrl || item.picUrl)" mode="aspectFill" @error="$setImageFallback(item, item.appListPicUrl ? 'appListPicUrl' : 'picUrl')"></image>
					<view class="brand-overlay"></view>
					<view class="brand-info">
						<text class="brand-name">{{item.name||''}}</text>
						<text class="brand-price">{{item.floorPrice||0}}元起</text>
					</view>
				</view>
			</navigator>
		</view>
		<view class="empty-view" v-if="brandList.length === 0">
			<text class="empty-text">暂无品牌数据</text>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				brandList: [],
				page: 1,
				size: 10,
				totalPages: 1
			}
		},
		methods: {
			getBrandList: function() {
				let that = this;
				util.request(api.BrandList, {
					page: that.page,
					size: that.size
				}).then(function(res) {
					if (res.code === 0) {
						let data = res.data;
						if (data.records) {
							that.brandList = that.brandList.concat(data.records);
							that.totalPages = data.pages || 1;
						} else if (data.brandList) {
							that.brandList = that.brandList.concat(data.brandList);
							that.totalPages = data.totalPages || 1;
						}
					}
				});
			}
		},
		onReachBottom() {
			if (this.totalPages > this.page) {
				this.page = this.page + 1;
				this.getBrandList();
			}
		},
		onLoad: function() {
			this.getBrandList();
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;
	$gold: #B8860B;

	page {
		background: $green-bg;
	}

	.container {
		padding: 24rpx;
	}

	.brand-list {
		width: 100%;
	}

	.brand-card {
		display: block;
		width: 100%;
		height: 360rpx;
		border-radius: 20rpx;
		overflow: hidden;
		margin-bottom: 24rpx;
		box-shadow: 0 4rpx 20rpx rgba(91,140,90,0.12);
	}

	.brand-img {
		position: relative;
		width: 100%;
		height: 100%;

		image {
			width: 100%;
			height: 100%;
		}
	}

	.brand-overlay {
		position: absolute;
		left: 0;
		bottom: 0;
		width: 100%;
		height: 50%;
		background: linear-gradient(to top, rgba(0,0,0,0.55), transparent);
	}

	.brand-info {
		position: absolute;
		left: 0;
		bottom: 0;
		width: 100%;
		padding: 30rpx 32rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.brand-name {
		font-size: 34rpx;
		font-weight: bold;
		color: #FEFEFC;
		letter-spacing: 2rpx;
	}

	.brand-price {
		font-size: 26rpx;
		color: $gold;
		background: rgba(255,255,255,0.15);
		padding: 6rpx 18rpx;
		border-radius: 20rpx;
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
