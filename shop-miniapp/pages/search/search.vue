<template>
	<view class="page">
		<!-- 搜索头部 -->
		<view class="search-header">
			<view class="search-input-box">
				<text class="search-icon-text">🔍</text>
				<input class="search-input" :focus="true" v-model="keyword" confirm-type="search"
					@input="inputChange" @focus="inputFocus" @confirm="onKeywordConfirm"
					:placeholder="defaultKeyword.keyword || '搜索商品'" />
				<view class="search-clear" v-if="keyword" @tap="clearKeyword">✕</view>
			</view>
			<view class="search-cancel" @tap="closeSearch">取消</view>
		</view>

		<!-- 未搜索状态 -->
		<view class="search-body" v-if="!searchStatus">
			<!-- 历史搜索 -->
			<view class="keyword-section" v-if="!keyword && historyKeyword.length">
				<view class="keyword-header">
					<text class="keyword-title">搜索历史</text>
					<view class="keyword-clear" @tap="clearHistory">
						<text>清空</text>
					</view>
				</view>
				<view class="keyword-tags">
					<view class="keyword-tag" v-for="(item, index) in historyKeyword" :key="index"
						@tap="onKeywordTap" :data-keyword="item">{{item}}</view>
				</view>
			</view>

			<!-- 热门搜索 -->
			<view class="keyword-section" v-if="!keyword">
				<view class="keyword-header">
					<text class="keyword-title">热门搜索</text>
				</view>
				<view class="keyword-tags">
					<view :class="'keyword-tag' + (item.isHot === 1 ? ' hot' : '')"
						v-for="(item, index) in hotKeyword" :key="index"
						@tap="onKeywordTap" :data-keyword="item.keyword">{{item.keyword}}</view>
				</view>
			</view>

			<!-- 搜索建议 -->
			<view class="suggest-list" v-if="keyword">
				<view class="suggest-item" v-for="(item, index) in helpKeyword" :key="index"
					@tap="onKeywordTap" :data-keyword="item">{{item}}</view>
			</view>
		</view>

		<!-- 搜索结果 -->
		<view class="result-body" v-if="searchStatus && goodsList.length">
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
					:url="'/pages/goods/goods?id='+item.id">
					<image class="goods-img" :src="imageUrl(item.listPicUrl)" mode="aspectFill" @error="setImageFallback(item, 'listPicUrl')"></image>
					<view class="goods-info">
						<text class="goods-name">{{item.name}}</text>
						<text class="goods-price">¥{{item.retailPrice}}</text>
					</view>
				</navigator>
			</view>
		</view>

		<!-- 空结果 -->
		<view class="empty-result" v-if="!goodsList.length && searchStatus">
			<text class="empty-icon">🔍</text>
			<text class="empty-text">未找到相关商品</text>
			<text class="empty-hint">换个关键词试试吧</text>
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
			keyword: '',
			searchStatus: false,
			goodsList: [],
			helpKeyword: [],
			historyKeyword: [],
			categoryFilter: false,
			filterCategory: [],
			defaultKeyword: {},
			hotKeyword: [],
			page: 1,
			size: 20,
			currentSortType: 'id',
			currentSortOrder: 'desc',
			categoryId: 0
		}
	},
	methods: {
		imageUrl(url) {
			return imageUtil.normalizeImageUrl(url);
		},
		setImageFallback(item, field) {
			if (item && item[field] !== imageUtil.FALLBACK_IMAGE) this.$set(item, field, imageUtil.FALLBACK_IMAGE);
		},
		closeSearch() {
			uni.navigateBack();
		},
		clearKeyword() {
			this.keyword = '';
			this.searchStatus = false;
		},
		getSearchKeyword() {
			util.request(api.SearchIndex).then(res => {
				if (res.code === 0) {
					this.historyKeyword = res.data.historyKeywordList;
					this.defaultKeyword = res.data.defaultKeyword;
					this.hotKeyword = res.data.hotKeywordList;
				}
			});
		},
		inputChange(e) {
			this.keyword = e.detail.value;
			this.searchStatus = false;
			this.getHelpKeyword();
		},
		getHelpKeyword() {
			util.request(api.SearchHelper, { keyword: this.keyword }).then(res => {
				if (res.code === 0) this.helpKeyword = res.data;
			});
		},
		inputFocus() {
			this.searchStatus = false;
			this.goodsList = [];
			if (this.keyword) this.getHelpKeyword();
		},
		clearHistory() {
			this.historyKeyword = [];
			util.request(api.SearchClearHistory);
		},
		getGoodsList() {
			util.request(api.GoodsList, {
				keyword: this.keyword,
				page: this.page,
				size: this.size,
				sort: this.currentSortType,
				order: this.currentSortOrder,
				categoryId: this.categoryId
			}).then(res => {
				if (res.code === 0) {
					this.searchStatus = true;
					this.categoryFilter = false;
					this.goodsList = res.data.goodsList.records;
					this.filterCategory = res.data.filterCategory;
					this.page = res.data.goodsList.current;
					this.size = res.data.goodsList.size;
				}
				this.getSearchKeyword();
			});
		},
		onKeywordTap(event) {
			this.getSearchResult(event.target.dataset.keyword);
		},
		getSearchResult(keyword) {
			this.keyword = keyword;
			this.page = 1;
			this.categoryId = 0;
			this.goodsList = [];
			this.getGoodsList();
		},
		openSortFilter(event) {
			let currentId = event.currentTarget.id;
			switch (currentId) {
				case 'categoryFilter':
					this.categoryFilter = !this.categoryFilter;
					this.currentSortOrder = 'asc';
					break;
				case 'priceSort':
					this.currentSortType = 'price';
					this.currentSortOrder = this.currentSortOrder == 'asc' ? 'desc' : 'asc';
					this.categoryFilter = false;
					this.getGoodsList();
					break;
				default:
					this.currentSortType = 'default';
					this.currentSortOrder = 'desc';
					this.categoryFilter = false;
					this.getGoodsList();
			}
		},
		selectCategory(event) {
			let currentIndex = event.target.dataset.categoryIndex;
			let filterCategory = this.filterCategory;
			for (let key in filterCategory) {
				filterCategory[key].selected = (key == currentIndex);
			}
			this.filterCategory = filterCategory;
			this.categoryFilter = false;
			this.categoryId = filterCategory[currentIndex].id;
			this.page = 1;
			this.goodsList = [];
			this.getGoodsList();
		},
		onKeywordConfirm(event) {
			this.getSearchResult(event.detail.value);
		}
	},
	onLoad() {
		this.getSearchKeyword();
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
	height: 100%;
}

.page {
	min-height: 100%;
}

/* 搜索头部 */
.search-header {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	height: 100rpx;
	background: #FEFEFC;
	display: flex;
	align-items: center;
	padding: 0 24rpx;
	z-index: 100;
	box-shadow: 0 2rpx 8rpx rgba(91, 140, 90, 0.06);
}

.search-input-box {
	flex: 1;
	height: 68rpx;
	background: $green-bg;
	border-radius: 34rpx;
	display: flex;
	align-items: center;
	padding: 0 24rpx;
}

.search-icon-text {
	font-size: 28rpx;
	margin-right: 12rpx;
}

.search-input {
	flex: 1;
	height: 68rpx;
	font-size: 28rpx;
	color: $text-primary;
}

.search-clear {
	width: 40rpx;
	height: 40rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 24rpx;
	color: $text-hint;
	background: #ddd;
	border-radius: 50%;
}

.search-cancel {
	margin-left: 20rpx;
	font-size: 28rpx;
	color: $text-secondary;
	flex-shrink: 0;
}

/* 搜索主体 */
.search-body {
	padding-top: 116rpx;
}

/* 关键词区 */
.keyword-section {
	background: #FEFEFC;
	margin: 0 0 16rpx;
	padding: 28rpx;
}

.keyword-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20rpx;
}

.keyword-title {
	font-size: 28rpx;
	color: $text-primary;
	font-weight: 600;
}

.keyword-clear {
	font-size: 24rpx;
	color: $text-hint;
}

.keyword-tags {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.keyword-tag {
	height: 56rpx;
	line-height: 56rpx;
	padding: 0 24rpx;
	background: $green-bg;
	border-radius: 28rpx;
	font-size: 24rpx;
	color: $text-primary;

	&.hot {
		color: $green;
		background: $green-light;
		font-weight: 600;
	}
}

/* 搜索建议 */
.suggest-list {
	background: #FEFEFC;
	margin-top: 0;
	padding: 0 28rpx;
}

.suggest-item {
	height: 88rpx;
	line-height: 88rpx;
	font-size: 28rpx;
	color: $text-primary;
	border-bottom: 1rpx solid $green-bg;
}

/* 搜索结果 */
.result-body {
	padding-top: 100rpx;
}

/* 排序栏 */
.sort-bar {
	position: fixed;
	top: 100rpx;
	left: 0;
	right: 0;
	height: 84rpx;
	background: #FEFEFC;
	display: flex;
	align-items: center;
	padding: 0 48rpx;
	z-index: 99;
	border-bottom: 1rpx solid $green-bg;
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
	position: fixed;
	top: 184rpx;
	left: 0;
	right: 0;
	background: #FEFEFC;
	padding: 24rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
	z-index: 98;
	box-shadow: 0 8rpx 16rpx rgba(0,0,0,0.05);
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
	padding: 100rpx 16rpx 16rpx;
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

/* 空结果 */
.empty-result {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 300rpx;
}

.empty-icon {
	font-size: 100rpx;
	opacity: 0.4;
}

.empty-text {
	font-size: 30rpx;
	color: $text-primary;
	margin-top: 24rpx;
}

.empty-hint {
	font-size: 26rpx;
	color: $text-hint;
	margin-top: 12rpx;
}
</style>
