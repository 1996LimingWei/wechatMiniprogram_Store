<template>
	<view class="container">
		<view class="collect-list" v-if="collectList.length>0">
			<view class="collect-item" @tap="openGoods" @touchstart="touchStart" @touchend="touchEnd"
			 v-for="(item, index) in collectList" :key="item.id" :data-index="index">
				<image class="item-img" :src="$imageUrl(item.listPicUrl)" mode="aspectFill" @error="$setImageFallback(item, 'listPicUrl')"></image>
				<view class="item-info">
					<text class="item-name">{{item.name||''}}</text>
					<text class="item-brief">{{item.goodsBrief||''}}</text>
					<text class="item-price">¥{{item.retailPrice||''}}</text>
				</view>
			</view>
		</view>
		<show-empty v-else text="暂无收藏"></show-empty>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				typeId: 0,
				collectList: []
			}
		},
		methods: {
			getCollectList() {
				let that = this;
				util.request(api.CollectList, { typeId: that.typeId }).then(function(res) {
					if (res.code === 0) {
						that.collectList = res.data || [];
					}
				});
			},
			openGoods(event) {
				let that = this;
				let goodsId = that.collectList[event.currentTarget.dataset.index].valueId;
				var touchTime = that.touch_end - that.touch_start;
				if (touchTime > 350) {
					uni.showModal({
						title: '提示',
						content: '确定删除收藏吗？',
						confirmColor: '#5B8C5A',
						success: function(res) {
							if (res.confirm) {
								util.request(api.CollectAddOrDelete, {
									typeId: that.typeId,
									valueId: goodsId
								}, "POST", "application/json").then(function(res) {
									if (res.code === 0) {
										uni.showToast({ title: '删除成功', icon: 'success' });
										that.getCollectList();
									}
								});
							}
						}
					});
				} else {
					uni.navigateTo({ url: '/pages/goods/goods?id=' + goodsId });
				}
			},
			touchStart: function(e) { this.touch_start = e.timeStamp; },
			touchEnd: function(e) { this.touch_end = e.timeStamp; }
		},
		onPullDownRefresh() {
			this.getCollectList();
		},
		onShow: function() {
			this.getCollectList();
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-bg: #F6F7F4;
	$red: #CF4A3E;

	page {
		background: $green-bg;
		min-height: 100%;
	}

	.container {
		padding: 24rpx;
	}

	.collect-list {
		width: 100%;
	}

	.collect-item {
		display: flex;
		align-items: center;
		background: #FEFEFC;
		border-radius: 16rpx;
		padding: 24rpx;
		margin-bottom: 16rpx;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.item-img {
		width: 160rpx;
		height: 160rpx;
		border-radius: 12rpx;
		margin-right: 20rpx;
	}

	.item-info {
		flex: 1;
		display: flex;
		flex-direction: column;
		justify-content: center;
	}

	.item-name {
		font-size: 28rpx;
		color: #333;
		line-height: 1.4;
		margin-bottom: 8rpx;
	}

	.item-brief {
		font-size: 24rpx;
		color: #999;
		margin-bottom: 12rpx;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.item-price {
		font-size: 30rpx;
		font-weight: bold;
		color: $red;
	}
</style>
