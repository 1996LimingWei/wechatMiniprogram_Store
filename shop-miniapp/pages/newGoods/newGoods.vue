<template>
	<view class="page">
		<!-- Banner -->
		<view class="banner" v-if="bannerInfo.imgUrl">
			<image class="banner-img" :src="imageUrl(bannerInfo.imgUrl)" mode="aspectFill" @error="setImageFallback(bannerInfo, 'imgUrl')"></image>
			<view class="banner-mask">
				<text class="banner-title">{{bannerInfo.name||'新品推荐'}}</text>
			</view>
		</view>

		<!-- 排序栏 -->
		<view class="sort-bar">
			<view :class="'sort-item' + (currentSortType == 'default' ? ' active' : '')" @tap="openSortFilter" id="defaultSort">
				<text>综合</text>
			</view>
			<view :class="'sort-item' + (currentSortType == 'price' ? ' active' : '')" @tap="openSortFilter" id="priceSort">
				<text>价格</text>
				<text class="sort-arrow">{{currentSortOrder == 'asc' ? '↑' : '↓'}}</text>
			</view>
			<view :class="'sort-item' + (currentSortType == 'category' ? ' active' : '')" @tap="openSortFilter" id="categoryFilter">
				<text>分类</text>
			</view>
		</view>

		<!-- 分类筛选 -->
		<view class="filter-panel" v-if="categoryFilter">
			<view :class="'filter-tag' + (item.checked ? ' active' : '')"
				v-for="(item, index) in filterCategory" :key="item.id"
				:data-category-index="index" @tap="selectCategory">{{item.name}}</view>
		</view>

		<!-- 商品网格 -->
		<view class="goods-grid">
			<navigator class="goods-card" v-for="(item, index) in goodsList" :key="index"
				:url="'../goods/goods?id='+item.id">
				<image class="goods-img" :src="imageUrl(item.listPicUrl)" mode="aspectFill" @error="setImageFallback(item, 'listPicUrl')"></image>
				<view class="goods-info">
					<text class="goods-name">{{item.name||''}}</text>
					<text class="goods-price">¥{{item.retailPrice||''}}</text>
				</view>
			</navigator>
		</view>
	</view>
</template>

<script>
const api = require('@/utils/api.js');
const util = require("@/utils/util.js");
const imageUtil = require('@/utils/image.js');

export default {
	data() {
		return {
			bannerInfo: { imgUrl: '', name: '' },
			categoryFilter: false,
			filterCategory: [],
			goodsList: [],
			categoryId: 0,
			currentSortType: 'default',
			currentSortOrder: 'desc',
			page: 1,
			size: 1000
		}
	},
	methods: {
		imageUrl(url) {
			return imageUtil.normalizeImageUrl(url);
		},
		setImageFallback(item, field) {
			if (item && item[field] !== imageUtil.FALLBACK_IMAGE) this.$set(item, field, imageUtil.FALLBACK_IMAGE);
		},
		getData() {
			util.request(api.GoodsNew).then(res => {
				if (res.code === 0) {
					this.bannerInfo = res.data.bannerInfo;
					this.getGoodsList();
				}
			});
		},
		getGoodsList() {
			util.request(api.GoodsList, {
				isNew: 1,
				page: this.page,
				size: this.size,
				order: this.currentSortOrder,
				sort: this.currentSortType,
				categoryId: this.categoryId
			}).then(res => {
				if (res.code === 0) {
					this.goodsList = res.data.goodsList.records;
					this.filterCategory = res.data.filterCategory;
				}
			});
		},
		openSortFilter(event) {
			let currentId = event.currentTarget.id;
			switch (currentId) {
				case 'categoryFilter':
					this.categoryFilter = !this.categoryFilter;
					this.currentSortType = 'category';
					this.currentSortOrder = 'asc';
					break;
				case 'priceSort':
					this.currentSortType = 'price';
					this.currentSortOrder = this.currentSortOrder == 'asc' ? 'desc' : 'asc';
					this.categoryFilter = false;
					this.getData();
					break;
				default:
					this.currentSortType = 'default';
					this.currentSortOrder = 'desc';
					this.categoryFilter = false;
					this.getData();
			}
		},
		selectCategory(event) {
			let currentIndex = event.target.dataset.categoryIndex;
			this.categoryFilter = false;
			this.categoryId = this.filterCategory[currentIndex].id;
			this.getData();
		}
	},
	onLoad() {
		this.getData();
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
	min-height: 100vh;
}

/* Banner */
.banner {
	position: relative;
	width: 750rpx;
	height: 260rpx;
}

.banner-img {
	width: 100%;
	height: 260rpx;
}

.banner-mask {
	position: absolute;
	inset: 0;
	background: rgba(45, 58, 46, 0.3);
	display: flex;
	align-items: center;
	justify-content: center;
}

.banner-title {
	font-size: 36rpx;
	color: #FEFEFC;
	font-weight: 700;
	letter-spacing: 4rpx;
}

/* 排序栏 */
.sort-bar {
	display: flex;
	align-items: center;
	height: 84rpx;
	background: #FEFEFC;
	padding: 0 48rpx;
}

.sort-item {
	flex: 1;
	height: 84rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28rpx;
	color: $text-secondary;

	&.active {
		color: $green;
		font-weight: 600;
	}
}

.sort-arrow {
	margin-left: 4rpx;
	font-size: 22rpx;
}

/* 筛选面板 */
.filter-panel {
	background: #FEFEFC;
	padding: 24rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
	border-top: 1rpx solid $green-bg;
}

.filter-tag {
	height: 56rpx;
	line-height: 52rpx;
	padding: 0 24rpx;
	border: 2rpx solid #e0e0e0;
	border-radius: 28rpx;
	font-size: 24rpx;
	color: $text-primary;

	&.active {
		border-color: $green;
		color: $green;
		background: $green-light;
	}
}

/* 商品网格 */
.goods-grid {
	padding: 16rpx;
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
</style>
