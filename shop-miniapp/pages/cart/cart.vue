<template>
	<view class="page">
		<!-- 顶部服务保障 -->
		<view class="guarantee-bar" v-if="cartGoods.length > 0">
			<view class="guarantee-item">
				<text class="guarantee-dot">✓</text>
				<text>正品保障</text>
			</view>
			<view class="guarantee-item">
				<text class="guarantee-dot">✓</text>
				<text>售后服务</text>
			</view>
			<view class="guarantee-item">
				<text class="guarantee-dot">✓</text>
				<text>满299包邮</text>
			</view>
		</view>

		<!-- 包邮进度条 -->
		<view class="shipping-bar" v-if="cartGoods.length > 0">
			<view class="shipping-info" v-if="freeShippingDiff > 0">
				<text class="shipping-text">再买</text>
				<text class="shipping-amount">¥{{freeShippingDiff}}</text>
				<text class="shipping-text">即可享受包邮~</text>
			</view>
			<view class="shipping-info" v-else>
				<text class="shipping-done">已满足包邮条件</text>
			</view>
			<view class="shipping-progress">
				<view class="shipping-progress-fill" :style="{width: shippingPercent + '%'}"></view>
			</view>
		</view>

		<!-- 空购物车 -->
		<view class="empty-cart" v-if="cartGoods.length <= 0">
			<view class="empty-icon">
				<image class="empty-icon-img" src="/static/images/ic_menu_shoping_nor.png" mode="aspectFit"></image>
			</view>
			<text class="empty-title">购物车是空的</text>
			<text class="empty-desc">去逛逛，发现更多好物吧</text>
			<view class="empty-btn" @tap="toIndexPage">去逛逛</view>
		</view>

		<!-- 购物车列表 -->
		<view class="cart-list" v-if="cartGoods.length > 0">
			<view
				class="cart-swipe-row"
				v-for="(item, index) in cartGoods"
				:key="item.id"
				@touchstart="touchStart($event, index)"
				@touchmove="touchMove($event, index)"
				@touchend="touchEnd($event, index)"
			>
				<view class="swipe-delete" @tap.stop="deleteSingle(index)">删除</view>
				<view class="cart-item" :style="{transform: 'translateX(' + (item.swipeOffset || 0) + 'rpx)'}">
					<view class="checkbox" :class="{checked: item.checked}" @tap="checkedItem(index)">
						<text class="check-icon" v-if="item.checked">✓</text>
					</view>

					<navigator :url="'/pages/goods/goods?id=' + item.goodsId" class="item-img-wrap">
						<image class="item-img" :src="item.listPicUrl" mode="aspectFill"></image>
					</navigator>

					<view class="item-info">
						<text class="item-name">{{item.goodsName}}</text>
						<text class="item-spec" v-if="item.goodsSpecifitionNameValue">{{item.goodsSpecifitionNameValue}}</text>
						<view class="item-bottom">
							<text class="item-price">¥{{item.retailPrice}}</text>
							<view class="stepper">
								<view class="stepper-btn minus" :class="{disabled: item.number <= 1}" @tap="cutNumber(index)">
									<text>−</text>
								</view>
								<text class="stepper-num">{{item.number}}</text>
								<view class="stepper-btn plus" @tap="addNumber(index)">
									<text>+</text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- 凑单助手 - 仅未满包邮时显示 -->
		<view class="addon-section" v-if="freeShippingDiff > 0 && recommendList.length > 0">
			<view class="addon-header">
				<text class="addon-title">再凑¥{{freeShippingDiff}}即可包邮</text>
				<text class="addon-hint">左滑查看更多</text>
			</view>
			<scroll-view class="addon-scroll" scroll-x>
				<view class="addon-list">
					<navigator class="addon-card" v-for="(item, index) in recommendList"
					 :key="index" :url="'/pages/goods/goods?id='+item.id">
						<image class="addon-img" :src="item.listPicUrl" mode="aspectFill"></image>
						<text class="addon-name">{{item.name}}</text>
						<text class="addon-price">¥{{item.retailPrice}}</text>
					</navigator>
				</view>
			</scroll-view>
		</view>

		<!-- 底部结算栏 -->
		<view class="bottom-bar" v-if="cartGoods.length > 0">
			<view class="bottom-left">
				<view class="checkbox bottom-check" :class="{checked: checkedAllStatus}" @tap="checkedAll">
					<text class="check-icon" v-if="checkedAllStatus">✓</text>
				</view>
				<text class="bottom-all-text">全选</text>
			</view>

			<view class="bottom-center">
				<text class="total-label">合计:</text>
				<text class="total-price">¥{{cartTotal.checkedGoodsAmount}}</text>
			</view>

			<view class="bottom-right">
				<view class="edit-btn" @tap="editCart" v-if="!isEditCart">
					<text>编辑</text>
				</view>
				<view class="edit-btn" @tap="editCart" v-else>
					<text>完成</text>
				</view>

				<view class="delete-btn" @tap="deleteCart" v-if="isEditCart">
					<text>删除</text>
				</view>
				<view class="checkout-btn" @tap="checkoutOrder" v-if="!isEditCart">
					<text>结算({{cartTotal.checkedGoodsCount}})</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
const util = require('@/utils/util.js');
const api = require('@/utils/api.js');

export default {
	data() {
		return {
			cartGoods: [],
			cartTotal: {
				goodsCount: 0,
				goodsAmount: 0.00,
				checkedGoodsCount: 0,
				checkedGoodsAmount: 0.00
			},
			isEditCart: false,
			checkedAllStatus: true,
			recommendList: [],
			touchStartX: 0,
			touchStartY: 0,
			touchMoveX: 0
		}
	},
	computed: {
		freeShippingDiff() {
			const threshold = 299;
			const amount = parseFloat(this.cartTotal.checkedGoodsAmount) || 0;
			const diff = threshold - amount;
			return diff > 0 ? diff.toFixed(2) : 0;
		},
		shippingPercent() {
			const threshold = 299;
			const amount = parseFloat(this.cartTotal.checkedGoodsAmount) || 0;
			return Math.min(100, (amount / threshold) * 100);
		}
	},
	methods: {
		getCartList() {
			util.request(api.CartList).then(res => {
				if (res.code === 0) {
					this.cartGoods = (res.data.cartList || []).map(item => ({
						...item,
						swipeOffset: 0
					}));
					this.cartTotal = res.data.cartTotal;
				}
				this.checkedAllStatus = this.isCheckedAll();
			});
		},
		getRecommend() {
			util.request(api.GoodsList, { page: 1, size: 6 }).then(res => {
				if (res.code === 0 && res.data.goodsList) {
					this.recommendList = res.data.goodsList.records || [];
				}
			});
		},
		isCheckedAll() {
			return this.cartGoods.length > 0 && this.cartGoods.every(item => item.checked === true);
		},
		closeSwipe(exceptIndex) {
			this.cartGoods.forEach((item, index) => {
				if (index !== exceptIndex && item.swipeOffset !== 0) {
					this.$set(this.cartGoods[index], 'swipeOffset', 0);
				}
			});
		},
		touchStart(e, index) {
			this.touchStartX = e.touches[0].clientX;
			this.touchStartY = e.touches[0].clientY;
			this.touchMoveX = this.touchStartX;
			this.closeSwipe(index);
		},
		touchMove(e, index) {
			const currentX = e.touches[0].clientX;
			const currentY = e.touches[0].clientY;
			const diffX = currentX - this.touchStartX;
			const diffY = currentY - this.touchStartY;
			if (Math.abs(diffY) > Math.abs(diffX)) return;
			this.touchMoveX = currentX;
			if (diffX < -18) {
				this.$set(this.cartGoods[index], 'swipeOffset', -148);
			} else if (diffX > 18) {
				this.$set(this.cartGoods[index], 'swipeOffset', 0);
			}
		},
		touchEnd(e, index) {
			const diffX = this.touchMoveX - this.touchStartX;
			this.$set(this.cartGoods[index], 'swipeOffset', diffX < -60 ? -148 : 0);
		},
		getCheckedGoodsCount() {
			let count = 0;
			this.cartGoods.forEach(v => {
				if (v.checked) count += v.number;
			});
			return count;
		},
		checkedItem(index) {
			if (!this.isEditCart) {
				util.request(api.CartChecked, {
					productIds: this.cartGoods[index].productId,
					isChecked: this.cartGoods[index].checked ? 0 : 1
				}, 'POST', 'application/json').then(res => {
					if (res.code === 0) {
						this.cartGoods = res.data.cartList;
						this.cartTotal = res.data.cartTotal;
					}
					this.checkedAllStatus = this.isCheckedAll();
				});
			} else {
				this.cartGoods[index].checked = !this.cartGoods[index].checked;
				this.cartGoods = [...this.cartGoods];
				this.cartTotal.checkedGoodsCount = this.getCheckedGoodsCount();
				this.checkedAllStatus = this.isCheckedAll();
			}
		},
		checkedAll() {
			if (!this.isEditCart) {
				const productIds = this.cartGoods.map(v => v.productId);
				util.request(api.CartChecked, {
					productIds: productIds.join(','),
					isChecked: this.isCheckedAll() ? 0 : 1
				}, 'POST', 'application/json').then(res => {
					if (res.code === 0) {
						this.cartGoods = res.data.cartList;
						this.cartTotal = res.data.cartTotal;
					}
					this.checkedAllStatus = this.isCheckedAll();
				});
			} else {
				const allChecked = this.isCheckedAll();
				this.cartGoods = this.cartGoods.map(v => ({ ...v, checked: !allChecked }));
				this.cartTotal.checkedGoodsCount = this.getCheckedGoodsCount();
				this.checkedAllStatus = this.isCheckedAll();
			}
		},
		editCart() {
			if (this.isEditCart) {
				this.getCartList();
			} else {
				this.cartGoods = this.cartGoods.map(v => ({ ...v, checked: false }));
				this.cartTotal.checkedGoodsCount = 0;
				this.checkedAllStatus = false;
			}
			this.isEditCart = !this.isEditCart;
		},
		cutNumber(index) {
			const item = this.cartGoods[index];
			if (item.number <= 1) return;
			item.number -= 1;
			this.cartGoods = [...this.cartGoods];
			this.updateCart(item.productId, item.goodsId, item.number, item.id);
		},
		addNumber(index) {
			const item = this.cartGoods[index];
			item.number += 1;
			this.cartGoods = [...this.cartGoods];
			this.updateCart(item.productId, item.goodsId, item.number, item.id);
		},
		updateCart(productId, goodsId, number, id) {
			util.request(api.CartUpdate, { productId, goodsId, number, id }).then(res => {
				this.checkedAllStatus = this.isCheckedAll();
			});
		},
		deleteCart() {
			const productIds = this.cartGoods
				.filter(v => v.checked)
				.map(v => v.productId);
			if (productIds.length <= 0) return;

			util.request(api.CartDelete, {
				productIds: productIds.join(',')
			}, 'POST', 'application/json').then(res => {
				if (res.code === 0) {
					this.cartGoods = res.data.cartList.map(v => ({ ...v, checked: false, swipeOffset: 0 }));
					this.cartTotal = res.data.cartTotal;
				}
				this.checkedAllStatus = this.isCheckedAll();
			});
		},
		deleteSingle(index) {
			const item = this.cartGoods[index];
			if (!item) return;
			util.request(api.CartDelete, {
				productIds: String(item.productId)
			}, 'POST', 'application/json').then(res => {
				if (res.code === 0) {
					this.cartGoods = (res.data.cartList || []).map(v => ({ ...v, swipeOffset: 0 }));
					this.cartTotal = res.data.cartTotal;
				}
				this.checkedAllStatus = this.isCheckedAll();
			});
		},
		checkoutOrder() {
			const checked = this.cartGoods.filter(v => v.checked);
			if (checked.length <= 0) {
				uni.showToast({ title: '请选择商品', icon: 'none' });
				return;
			}
			uni.navigateTo({ url: '/pages/shopping/checkout/checkout' });
		},
		toIndexPage() {
			uni.switchTab({ url: '/pages/index/index' });
		}
	},
	onShow() {
		this.isEditCart = false;
		this.getCartList();
		this.getRecommend();
	}
}
</script>

<style lang="scss">
$green: #4D704D;
$green-light: #E8ECE8;
$green-bg: #FDFDF8;
$green-dark: #667166;
$text-primary: #36454F;
$text-secondary: #667166;
$text-hint: #9A9A9A;
$red: #36454F;

page {
	background: $green-bg;
	height: 100%;
}

.page {
	min-height: 100%;
	padding-bottom: 130rpx;
}

/* 服务保障 */
.guarantee-bar {
	display: flex;
	justify-content: center;
	padding: 20rpx 24rpx 14rpx;
	background: linear-gradient(180deg, #FEFEFC 0%, #FBFBF7 100%);
}

.guarantee-item {
	display: flex;
	align-items: center;
	font-size: 22rpx;
	color: $text-secondary;
	margin: 0 20rpx;
}

.guarantee-dot {
	color: $green;
	font-size: 22rpx;
	margin-right: 6rpx;
	font-weight: 700;
}

/* 包邮进度条 */
.shipping-bar {
	margin: 16rpx 24rpx 0;
	background: linear-gradient(180deg, #FEFEFC 0%, #FAFCF9 100%);
	border-radius: 18rpx;
	padding: 22rpx 24rpx;
	box-shadow: 0 10rpx 24rpx rgba(77,112,77,0.06);
	border: 1rpx solid rgba(111, 142, 117, 0.08);
}

.shipping-info {
	margin-bottom: 12rpx;
}

.shipping-text {
	font-size: 24rpx;
	color: $text-secondary;
}

.shipping-amount {
	font-size: 26rpx;
	color: $red;
	font-weight: 700;
	margin: 0 4rpx;
}

.shipping-done {
	font-size: 24rpx;
	color: $green;
	font-weight: 600;
}

.shipping-progress {
	height: 10rpx;
	background: rgba(221, 227, 219, 0.9);
	border-radius: 999rpx;
	overflow: hidden;
}

.shipping-progress-fill {
	height: 100%;
	background: linear-gradient(90deg, #B9CDBA 0%, $green 100%);
	border-radius: 999rpx;
	transition: width 0.3s;
}

/* 空购物车 */
.empty-cart {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: 220rpx;
}

.empty-icon {
	width: 164rpx;
	height: 164rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(180deg, #F4F7F1 0%, #EDF2EB 100%);
	box-shadow: inset 0 1rpx 0 rgba(255,255,255,0.8);
}

.empty-icon-img {
	width: 78rpx;
	height: 78rpx;
	opacity: 0.7;
}

.empty-title {
	font-size: 32rpx;
	color: $text-primary;
	margin-top: 24rpx;
	font-weight: 600;
}

.empty-desc {
	font-size: 26rpx;
	color: $text-hint;
	margin-top: 12rpx;
}

.empty-btn {
	margin-top: 48rpx;
	width: 240rpx;
	height: 80rpx;
	line-height: 80rpx;
	text-align: center;
	background: linear-gradient(135deg, $green 0%, $green-dark 100%);
	color: #FEFEFC !important;
	border-radius: 40rpx;
	font-size: 28rpx;
	font-weight: 600;
	box-shadow: 0 10rpx 24rpx rgba(77, 112, 77, 0.18);
}

/* 购物车列表 */
.cart-list {
	padding: 16rpx 24rpx 0;
}

.cart-swipe-row {
	position: relative;
	overflow: hidden;
	border-radius: 22rpx;
	margin-bottom: 16rpx;
}

.swipe-delete {
	position: absolute;
	top: 0;
	right: 0;
	width: 132rpx;
	height: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: #B65A52;
	color: #FEFEFC;
	font-size: 28rpx;
	font-weight: 600;
	border-radius: 0 22rpx 22rpx 0;
	z-index: 0;
}

.cart-item {
	position: relative;
	z-index: 1;
	display: flex;
	align-items: center;
	background: linear-gradient(180deg, #FEFEFC 0%, #FBFBF8 100%);
	border-radius: 22rpx;
	padding: 24rpx;
	box-shadow: 0 12rpx 24rpx rgba(77, 112, 77, 0.06);
	transition: transform 0.18s ease;
}

/* 选择框 */
.checkbox {
	width: 44rpx;
	height: 44rpx;
	border-radius: 50%;
	border: 3rpx solid rgba(155, 169, 156, 0.7);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;

	&.checked {
		background: linear-gradient(135deg, #D7E3D8 0%, #C8D7C9 100%);
		border-color: rgba(111, 142, 117, 0.32);
	}
}

.check-icon {
	color: #5E7963;
	font-size: 24rpx;
	font-weight: 700;
}

/* 商品图 */
.item-img-wrap {
	margin: 0 20rpx;
	flex-shrink: 0;
}

.item-img {
	width: 160rpx;
	height: 160rpx;
	border-radius: 16rpx;
	background: $green-bg;
}

/* 商品信息 */
.item-info {
	flex: 1;
	overflow: hidden;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
	min-height: 160rpx;
}

.item-name {
	font-size: 26rpx;
	color: $text-primary;
	display: -webkit-box;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	overflow: hidden;
	line-height: 1.4;
}

.item-spec {
	font-size: 22rpx;
	color: #839083;
	background: rgba(232, 236, 232, 0.8);
	padding: 6rpx 14rpx;
	border-radius: 999rpx;
	margin-top: 8rpx;
	align-self: flex-start;
}

.item-bottom {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 12rpx;
}

.item-price {
	font-size: 30rpx;
	color: $red;
	font-weight: 700;
}

/* 数量步进器 */
.stepper {
	display: flex;
	align-items: center;
	height: 52rpx;
	border-radius: 26rpx;
	background: linear-gradient(180deg, #F4F7F1 0%, #EEF2EB 100%);
	overflow: hidden;
}

.stepper-btn {
	width: 52rpx;
	height: 52rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28rpx;
	color: $text-primary;

	&.disabled {
		color: $text-hint;
		opacity: 0.5;
	}

	&.plus {
		color: #68846D;
		font-weight: 700;
	}
}

.stepper-num {
	min-width: 56rpx;
	text-align: center;
	font-size: 26rpx;
	color: $text-primary;
	font-weight: 600;
}

/* 凑单助手 */
.addon-section {
	padding: 26rpx 0 0;
	margin-top: 12rpx;
}

.addon-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 24rpx 16rpx;
}

.addon-title {
	font-size: 26rpx;
	color: #4C6651;
	font-weight: 600;
}

.addon-hint {
	font-size: 22rpx;
	color: $text-hint;
}

.addon-scroll {
	white-space: nowrap;
	padding-left: 24rpx;
}

.addon-list {
	display: inline-flex;
}

.addon-card {
	display: inline-flex;
	flex-direction: column;
	width: 200rpx;
	margin-right: 16rpx;
	background: linear-gradient(180deg, #FEFEFC 0%, #FBFBF8 100%);
	border-radius: 18rpx;
	overflow: hidden;
	box-shadow: 0 10rpx 22rpx rgba(77,112,77,0.06);
	text-decoration: none;
	flex-shrink: 0;
}

.addon-img {
	width: 200rpx;
	height: 200rpx;
}

.addon-name {
	font-size: 22rpx;
	color: $text-primary;
	padding: 10rpx 12rpx 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.addon-price {
	font-size: 26rpx;
	color: $red;
	font-weight: 700;
	padding: 4rpx 12rpx 14rpx;
}

/* 底部结算栏 */
.bottom-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	height: 110rpx;
	background: rgba(254, 254, 252, 0.92);
	backdrop-filter: blur(18rpx);
	-webkit-backdrop-filter: blur(18rpx);
	display: flex;
	align-items: center;
	padding: 0 24rpx;
	box-shadow: 0 -4rpx 16rpx rgba(77, 112, 77, 0.08);
	z-index: 100;
}

.bottom-left {
	display: flex;
	align-items: center;
}

.bottom-check {
	width: 40rpx;
	height: 40rpx;
}

.bottom-all-text {
	font-size: 26rpx;
	color: $text-secondary;
	margin-left: 12rpx;
}

.bottom-center {
	flex: 1;
	display: flex;
	align-items: baseline;
	justify-content: flex-end;
	margin-right: 20rpx;
}

.total-label {
	font-size: 26rpx;
	color: $text-secondary;
}

.total-price {
	font-size: 36rpx;
	color: $red;
	font-weight: 700;
	margin-left: 4rpx;
}

.bottom-right {
	display: flex;
	align-items: center;
}

.edit-btn {
	height: 68rpx;
	padding: 0 24rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	color: $text-secondary;
	border: 2rpx solid rgba(155, 169, 156, 0.48);
	border-radius: 34rpx;
	margin-right: 12rpx;
	background: rgba(255, 255, 255, 0.72);
}

.delete-btn {
	height: 68rpx;
	padding: 0 32rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	color: #FEFEFC !important;
	background: $red;
	border-radius: 34rpx;

	text {
		color: #FEFEFC !important;
	}
}

.checkout-btn {
	height: 68rpx;
	padding: 0 36rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 28rpx;
	color: #4F6854 !important;
	background: linear-gradient(135deg, #D9E6DA 0%, #C9D8CB 100%);
	border-radius: 34rpx;
	font-weight: 600;
	border: 1rpx solid rgba(111, 142, 117, 0.12);

	text {
		color: #4F6854 !important;
	}
}
</style>
