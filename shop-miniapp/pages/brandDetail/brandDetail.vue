<template>
	<view class="container">
		<view class="brand-header">
			<image class="brand-banner" :src="$imageUrl(brand.appListPicUrl || brand.picUrl)" mode="aspectFill" @error="$setImageFallback(brand, brand.appListPicUrl ? 'appListPicUrl' : 'picUrl')"></image>
			<view class="brand-overlay"></view>
			<view class="brand-title">
				<text class="brand-name">{{brand.name||''}}</text>
				<view class="brand-line"></view>
			</view>
		</view>
		<view class="brand-desc" v-if="brand.simpleDesc">
			<text>{{brand.simpleDesc}}</text>
		</view>
		<view class="goods-section">
			<view class="section-title">品牌商品</view>
			<view class="goods-grid">
				<navigator class="goods-item" v-for="(item, index) in goodsList" :key="index" :url="'../goods/goods?id='+item.id">
					<image class="goods-img" :src="$imageUrl(item.listPicUrl)" mode="aspectFill" @error="$setImageFallback(item, 'listPicUrl')"></image>
					<text class="goods-name">{{item.name||''}}</text>
					<text class="goods-price">¥{{item.retailPrice||0}}</text>
				</navigator>
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
				id: 0,
				brand: {},
				goodsList: [],
				page: 1,
				size: 1000
			}
		},
		methods: {
			getBrand: function() {
				let that = this;
				util.request(api.BrandDetail, { id: that.id }).then(function(res) {
					if (res.code === 0) {
						that.brand = res.data.brand;
						that.getGoodsList();
					}
				});
			},
			getGoodsList() {
				let that = this;
				util.request(api.GoodsList, {
					brandId: that.id,
					page: that.page,
					size: that.size
				}).then(function(res) {
					if (res.code === 0) {
						that.goodsList = res.data.goodsList ? res.data.goodsList.records : [];
					}
				});
			}
		},
		onLoad: function(options) {
			this.id = parseInt(options.id);
			this.getBrand();
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;
	$red: #CF4A3E;

	page {
		background: $green-bg;
	}

	.container {
		padding-bottom: 30rpx;
	}

	.brand-header {
		position: relative;
		width: 100%;
		height: 320rpx;
	}

	.brand-banner {
		width: 100%;
		height: 100%;
	}

	.brand-overlay {
		position: absolute;
		top: 0;
		left: 0;
		width: 100%;
		height: 100%;
		background: rgba(0,0,0,0.3);
	}

	.brand-title {
		position: absolute;
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		text-align: center;
	}

	.brand-name {
		display: block;
		font-size: 40rpx;
		font-weight: bold;
		color: #FEFEFC;
		letter-spacing: 4rpx;
	}

	.brand-line {
		width: 120rpx;
		height: 3rpx;
		background: rgba(255,255,255,0.7);
		margin: 20rpx auto 0;
	}

	.brand-desc {
		background: #FEFEFC;
		padding: 30rpx;
		font-size: 26rpx;
		color: #666;
		line-height: 1.8;
		text-align: center;
	}

	.goods-section {
		padding: 24rpx;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: bold;
		color: #333;
		margin-bottom: 20rpx;
		padding-left: 8rpx;
	}

	.goods-grid {
		overflow: hidden;
	}

	.goods-item {
		float: left;
		width: 335rpx;
		background: #FEFEFC;
		border-radius: 16rpx;
		overflow: hidden;
		margin-bottom: 20rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);

		&:nth-child(odd) {
			margin-right: 16rpx;
		}
	}

	.goods-img {
		width: 100%;
		height: 335rpx;
	}

	.goods-name {
		display: block;
		padding: 16rpx 20rpx 8rpx;
		font-size: 26rpx;
		color: #333;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.goods-price {
		display: block;
		padding: 0 20rpx 20rpx;
		font-size: 28rpx;
		font-weight: bold;
		color: $red;
	}
</style>
