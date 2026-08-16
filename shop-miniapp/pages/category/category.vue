<template>
	<view class="page">
		<!-- 顶部分类导航 -->
		<view class="nav-fixed">
			<scroll-view scroll-x class="nav-scroll" :scroll-left="scrollLeft">
				<view class="nav-item" :class="{active: id == item.id}"
					v-for="(item, index) in navList" :key="index"
					:data-id="item.id" :data-index="index" @tap="switchCate">
					<text class="nav-text">{{item.name}}</text>
				</view>
			</scroll-view>
		</view>

		<!-- 分类信息 -->
		<view class="cate-header">
			<text class="cate-name">{{currentCategory.name||''}}</text>
			<text class="cate-desc" v-if="currentCategory.frontName">{{currentCategory.frontName}}</text>
		</view>

		<!-- 商品列表 -->
		<view class="goods-grid">
			<navigator class="goods-card" v-for="(item, index) in goodsList" :key="index"
				:url="'/pages/goods/goods?id='+item.id">
				<image class="goods-img" :src="imageUrl(item.listPicUrl)" mode="aspectFill" @error="setImageFallback(item, 'listPicUrl')"></image>
				<view class="goods-info">
					<text class="goods-name">{{item.name||''}}</text>
					<text class="goods-price">¥{{item.retailPrice||''}}</text>
				</view>
			</navigator>
		</view>

		<!-- 加载更多 -->
		<view class="loadmore" v-if="goodsList.length > 4">
			<text v-if="nomore" class="loadmore-text">已加载全部</text>
			<text v-else class="loadmore-text">加载中...</text>
		</view>
	</view>
</template>

<script>
const util = require("@/utils/util.js");
const api = require('@/utils/api.js');
const imageUtil = require('@/utils/image.js');

export default {
	data() {
		return {
			navList: [],
			goodsList: [],
			id: 0,
			currentCategory: {},
			scrollLeft: 0,
			scrollTop: 0,
			scrollHeight: 0,
			page: 1,
			size: 10,
			loadmoreText: '正在加载更多数据',
			nomoreText: '全部加载完成',
			nomore: false,
			totalPages: 1
		}
	},
	methods: {
		imageUrl(url) {
			return imageUtil.normalizeImageUrl(url);
		},
		setImageFallback(item, field) {
			if (item && item[field] !== imageUtil.FALLBACK_IMAGE) this.$set(item, field, imageUtil.FALLBACK_IMAGE);
		},
		getCategoryInfo() {
			util.request(api.GoodsCategory, { id: this.id }).then(res => {
				if (res.code == 0) {
					this.navList = res.data.brotherCategory;
					this.currentCategory = res.data.currentCategory;
					let currentIndex = 0;
					for (let i = 0; i < this.navList.length; i++) {
						currentIndex += 1;
						if (this.navList[i].id == this.id) break;
					}
					if (currentIndex > this.navList.length / 2 && this.navList.length > 5) {
						this.scrollLeft = currentIndex * 60;
					}
					this.getGoodsList();
				}
			});
		},
		getGoodsList() {
			if (this.totalPages <= this.page - 1) {
				this.nomore = true;
				return;
			}
			util.request(api.GoodsList, {
				categoryId: this.id,
				page: this.page,
				size: this.size
			}).then(res => {
				this.goodsList = this.goodsList.concat(res.data.goodsList.records);
				this.page = res.data.goodsList.current + 1;
				this.totalPages = res.data.goodsList.pages;
			});
		},
		switchCate(event) {
			if (this.id == event.currentTarget.dataset.id) return;
			let clientX = event.detail.x;
			let currentTarget = event.currentTarget;
			if (clientX < 60) {
				this.scrollLeft = currentTarget.offsetLeft - 60;
			} else if (clientX > 330) {
				this.scrollLeft = currentTarget.offsetLeft;
			}
			this.id = event.currentTarget.dataset.id;
			this.page = 1;
			this.totalPages = 1;
			this.goodsList = [];
			this.nomore = false;
			this.getCategoryInfo();
		}
	},
	onReachBottom() {
		this.getGoodsList();
	},
	onLoad(options) {
		if (options.id) this.id = parseInt(options.id);
		this.getCategoryInfo();
	}
}
</script>

<style lang="scss">
$green: #5B8C5A;
$green-light: #E8F2E7;
$green-bg: #F6F7F4;
$green-dark: #3D6B3C;
$text-primary: #2D3A2E;
$text-secondary: #5C6B5D;
$text-hint: #9CA89D;
$red: #CF4A3E;

page {
	background: $green-bg;
}

.page {
	padding-top: 100rpx;
	min-height: 100vh;
}

/* 顶部导航 */
.nav-fixed {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	background: #FEFEFC;
	z-index: 100;
	box-shadow: 0 2rpx 8rpx rgba(91, 140, 90, 0.06);
}

.nav-scroll {
	white-space: nowrap;
	height: 88rpx;
	padding: 0 16rpx;
}

.nav-item {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	height: 88rpx;
	padding: 0 28rpx;

	&.active .nav-text {
		color: $green;
		font-weight: 700;
		border-bottom: 4rpx solid $green;
	}
}

.nav-text {
	font-size: 28rpx;
	color: $text-secondary;
	height: 88rpx;
	line-height: 84rpx;
}

/* 分类信息 */
.cate-header {
	padding: 32rpx 30rpx 16rpx;
	text-align: center;
}

.cate-name {
	display: block;
	font-size: 32rpx;
	color: $text-primary;
	font-weight: 700;
}

.cate-desc {
	display: block;
	font-size: 24rpx;
	color: $text-hint;
	margin-top: 8rpx;
}

/* 商品网格 */
.goods-grid {
	padding: 0 16rpx 16rpx;
	overflow: hidden;
}

.goods-card {
	float: left;
	width: 350rpx;
	background: #FEFEFC;
	border-radius: 16rpx;
	overflow: hidden;
	text-decoration: none;
	margin-bottom: 16rpx;

	&:nth-child(odd) {
		margin-right: 16rpx;
	}
}

.goods-img {
	width: 350rpx;
	height: 350rpx;
}

.goods-info {
	padding: 16rpx 20rpx 20rpx;
}

.goods-name {
	font-size: 26rpx;
	color: $text-primary;
	display: -webkit-box;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	overflow: hidden;
	line-height: 1.4;
}

.goods-price {
	display: block;
	margin-top: 10rpx;
	font-size: 30rpx;
	color: $red;
	font-weight: 700;
}

/* 加载更多 */
.loadmore {
	padding: 32rpx 0;
	text-align: center;
}

.loadmore-text {
	font-size: 24rpx;
	color: $text-hint;
}
</style>
