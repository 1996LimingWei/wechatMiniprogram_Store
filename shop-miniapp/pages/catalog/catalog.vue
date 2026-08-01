<template>
	<view class="page">
		<!-- 搜索栏 -->
		<view class="search-bar">
			<navigator url="/pages/search/search" class="search-box">
				<image class="search-icon" src="/static/images/icon-search.png" mode="aspectFit"></image>
				<text class="search-text">搜索商品, 共{{goodsCount}}款好物</text>
			</navigator>
		</view>

		<!-- 分类主体 -->
		<view class="catalog-body">
			<!-- 左侧一级分类 -->
			<scroll-view class="side-nav" scroll-y :style="{height: scrollHeight + 'px'}">
				<view
					class="nav-item"
					:class="{active: currentId === item.id}"
					v-for="(item, index) in navList"
					:key="index"
					@tap="switchCate(item.id)"
				>
					<view class="nav-indicator" v-if="currentId === item.id"></view>
					<text class="nav-text">{{item.name}}</text>
				</view>
			</scroll-view>

			<!-- 右侧商品列表 -->
			<scroll-view class="main-content" scroll-y :style="{height: scrollHeight + 'px'}"
			 @scrolltolower="loadMore">
				<!-- 当前分类名 -->
				<view class="cate-name-bar">
					<view class="cate-name-wrap">
						<text class="cate-name">{{currentName}}</text>
						<text class="cate-desc">按品类精选的日常养生好物</text>
					</view>
					<text class="cate-count">{{goodsList.length}}件商品</text>
				</view>

				<!-- 商品列表 -->
				<view class="goods-list">
					<view class="goods-card" v-for="(item, index) in goodsList" :key="index"
					 @tap="goToGoods(item.id)">
						<image class="goods-img" :src="item.listPicUrl" mode="aspectFill"></image>
						<view class="goods-info">
							<text class="goods-name">{{item.name||''}}</text>
							<text class="goods-brief">{{item.goodsBrief || ''}}</text>
							<view class="goods-bottom">
								<view class="goods-price-wrap">
									<text class="goods-price">¥{{item.retailPrice}}</text>
									<text class="goods-price-note">精选价</text>
								</view>
								<view class="goods-cart-btn" @tap.stop="goToGoods(item.id)">
									<text class="goods-cart-btn-icon">选</text>
								</view>
							</view>
						</view>
					</view>
				</view>

				<view class="load-tip" v-if="goodsList.length > 0">
					<text class="load-text">{{noMore ? '— 已加载全部 —' : '加载中...'}}</text>
				</view>
				<view class="empty-tip" v-if="goodsList.length === 0 && !loading">
					<text>{{loadFailed ? '加载失败，请检查后端服务后重试' : '该分类暂无商品'}}</text>
				</view>
			</scroll-view>
		</view>

	</view>
</template>

<script>
const util = require('@/utils/util.js');
const api = require('@/utils/api.js');

export default {
	data() {
		return {
			navList: [],
			currentId: 0,
			currentName: '',
			goodsList: [],
			goodsCount: 0,
			scrollHeight: 600,
			page: 1,
			size: 10,
			totalPages: 1,
			noMore: false,
			loading: false,
			loadFailed: false
		}
	},
	methods: {
		getCatalog() {
			this.loadFailed = false;
			util.request(api.CatalogList).then(res => {
				if (res.code === 0) {
					this.navList = res.data.categoryList;
					if (res.data.currentCategory) {
						this.currentId = res.data.currentCategory.id;
						this.currentName = res.data.currentCategory.name;
					} else if (this.navList.length > 0) {
						this.currentId = this.navList[0].id;
						this.currentName = this.navList[0].name;
					}
					this.loadGoods();
				}
			}).catch(() => { this.loadFailed = true; });
			util.request(api.GoodsCount).then(res => {
				if (res.code === 0) this.goodsCount = res.data.goodsCount;
			});
		},
		switchCate(id) {
			if (this.currentId === id) return;
			this.currentId = id;
			let found = this.navList.find(n => n.id === id);
			this.currentName = found ? found.name : '';
			this.page = 1;
			this.goodsList = [];
			this.noMore = false;
			this.loadGoods();
		},
		loadGoods() {
			if (this.noMore || this.loading) return;
			this.loading = true;
			this.loadFailed = false;
			util.request(api.GoodsList, {
				categoryId: this.currentId,
				page: this.page,
				size: this.size
			}).then(res => {
				if (res.code === 0 && res.data.goodsList) {
					let records = res.data.goodsList.records || [];
					this.goodsList = this.goodsList.concat(records);
					this.totalPages = res.data.goodsList.pages || 1;
					if (this.page >= this.totalPages || records.length < this.size) {
						this.noMore = true;
					}
					this.page++;
				}
				this.loading = false;
			}).catch(() => {
				this.loading = false;
				this.loadFailed = true;
			});
		},
		loadMore() {
			this.loadGoods();
		},
		calcHeight() {
			const sysInfo = uni.getSystemInfoSync();
			this.scrollHeight = sysInfo.windowHeight - 100;
		},
		goToGoods(id) {
			uni.navigateTo({ url: '/pages/goods/goods?id=' + id });
		},
	},
	onLoad() {
		this.calcHeight();
		this.getCatalog();
	}
}
</script>

<style lang="scss">
$green: #4D704D;
$green-light: #E8ECE8;
$green-bg: #FDFDF8;
$text-primary: #36454F;
$text-secondary: #667166;
$text-hint: #9A9A9A;
$red: #36454F;

page {
	height: 100%;
	background: #FEFEFC;
}

.page {
	height: 100%;
	display: flex;
	flex-direction: column;
}

/* 搜索栏 */
.search-bar {
	padding: 18rpx 24rpx 16rpx;
	background: linear-gradient(180deg, #FEFEFC 0%, #FBFBF7 100%);
	flex-shrink: 0;
	box-shadow: 0 8rpx 18rpx rgba(100, 118, 102, 0.04);
}

.search-box {
	display: flex;
	align-items: center;
	height: 72rpx;
	background: rgba(248, 250, 246, 0.96);
	border-radius: 36rpx;
	padding: 0 24rpx;
	text-decoration: none;
	border: 1rpx solid rgba(111, 142, 117, 0.10);
}

.search-icon {
	width: 28rpx;
	height: 28rpx;
	margin-right: 10rpx;
	opacity: 0.78;
}

.search-text {
	font-size: 24rpx;
	color: #8A958A;
}

/* 分类主体 */
.catalog-body {
	flex: 1;
	display: flex;
	overflow: hidden;
}

/* 左侧导航 */
.side-nav {
	width: 164rpx;
	background: linear-gradient(180deg, #F7F8F3 0%, #F2F4ED 100%);
	flex-shrink: 0;
	border-right: 1rpx solid rgba(111, 142, 117, 0.06);
}

.nav-item {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 96rpx;
	color: $text-secondary;
	transition: all 0.15s ease;

	&.active {
		background: linear-gradient(180deg, #FEFEFC 0%, #F9FBF7 100%);
		color: $green;
		font-weight: 700;
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.8);
	}
}

.nav-indicator {
	position: absolute;
	left: 0;
	top: 50%;
	transform: translateY(-50%);
	width: 6rpx;
	height: 40rpx;
	background: $green;
	border-radius: 0 4rpx 4rpx 0;
}

.nav-text {
	font-size: 25rpx;
	max-width: 128rpx;
	text-align: center;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

/* 右侧内容 */
.main-content {
	flex: 1;
	background: linear-gradient(180deg, #F8FAF6 0%, #FDFDF8 100%);
}

/* 分类名称条 */
.cate-name-bar {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	padding: 26rpx 24rpx 18rpx;
}

.cate-name-wrap {
	display: flex;
	flex-direction: column;
}

.cate-name {
	font-size: 34rpx;
	color: $text-primary;
	font-weight: 700;
}

.cate-desc {
	font-size: 20rpx;
	color: #95A095;
	margin-top: 8rpx;
}

.cate-count {
	font-size: 22rpx;
	color: $text-hint;
	padding-top: 8rpx;
}

/* 商品列表 */
.goods-list {
	padding: 0 18rpx 18rpx;
}

.goods-card {
	display: flex;
	align-items: stretch;
	width: 100%;
	background: linear-gradient(180deg, #FEFEFC 0%, #FCFCF8 100%);
	border-radius: 22rpx;
	overflow: hidden;
	margin-bottom: 16rpx;
	text-decoration: none;
	box-shadow: 0 12rpx 24rpx rgba(88, 109, 93, 0.07);
	border: 1rpx solid rgba(111, 142, 117, 0.06);
}

.goods-img {
	width: 188rpx;
	height: 188rpx;
	display: block;
	flex-shrink: 0;
}

.goods-info {
	flex: 1;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
	padding: 18rpx 18rpx 18rpx 20rpx;
}

.goods-name {
	font-size: 28rpx;
	color: $text-primary;
	display: -webkit-box;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	overflow: hidden;
	line-height: 1.45;
}

.goods-brief {
	font-size: 21rpx;
	color: #95A095;
	line-height: 1.5;
	margin-top: 10rpx;
	display: -webkit-box;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	overflow: hidden;
}

.goods-bottom {
	margin-top: 16rpx;
	display: flex;
	justify-content: space-between;
	align-items: flex-end;
}

.goods-price-wrap {
	display: flex;
	flex-direction: column;
}

.goods-price {
	font-size: 36rpx;
	color: #32453C;
	font-weight: 700;
}

.goods-price-note {
	font-size: 20rpx;
	color: #9AA49A;
	margin-top: 6rpx;
}

/* 加载提示 */
.load-tip {
	padding: 24rpx 0 44rpx;
	text-align: center;
}

.load-text {
	font-size: 22rpx;
	color: $text-hint;
}

.empty-tip {
	padding: 100rpx 30rpx;
	text-align: center;
	font-size: 26rpx;
	color: $text-hint;
}

/* 一键加购绿色小圆钮 */
.goods-cart-btn {
	width: 58rpx;
	height: 58rpx;
	background: linear-gradient(135deg, #D7E3D8 0%, #C7D7C9 100%);
	border-radius: 18rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 8rpx 16rpx rgba(111, 142, 117, 0.12);
	border: 1rpx solid rgba(111, 142, 117, 0.12);
	transition: transform 0.1s ease;

	&:active {
		transform: scale(0.85);
	}
}

.goods-cart-btn-icon {
	color: #5F7A64;
	font-size: 28rpx;
	font-weight: 700;
	line-height: 1;
}

/* 抛物线飞球样式 */
</style>
