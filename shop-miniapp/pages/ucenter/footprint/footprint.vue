<template>
	<view class="container">
		<view class="footprint" v-if="footprintList.length>0">
			<view class="day-section" v-for="(item, index) in footprintList" :key="index">
				<view class="day-title">{{item[0].addTime}}</view>
				<view class="day-list">
					<view class="footprint-item" :data-footprint="iitem" @touchstart="touchStart" @touchend="touchEnd"
					 @tap="deleteItem" v-for="(iitem, iindex) in item" :key="iitem.id">
						<image class="item-img" :src="$imageUrl(iitem.listPicUrl)" mode="aspectFill" @error="$setImageFallback(iitem, 'listPicUrl')"></image>
						<view class="item-info">
							<text class="item-name">{{iitem.name||''}}</text>
							<text class="item-brief">{{iitem.goodsBrief||''}}</text>
							<text class="item-price">¥{{iitem.retailPrice||''}}</text>
						</view>
					</view>
				</view>
			</view>
		</view>
		<show-empty v-else text="暂无浏览记录"></show-empty>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				footprintList: []
			}
		},
		methods: {
			getFootprintList() {
				let that = this;
				util.request(api.FootprintList).then(function(res) {
					if (res.code === 0) {
						that.footprintList = (res.data && res.data.data) ? res.data.data : [];
					}
				});
			},
			deleteItem(event) {
				let that = this;
				let footprint = event.currentTarget.dataset.footprint;
				var touchTime = that.touch_end - that.touch_start;
				if (touchTime > 350) {
					uni.showModal({
						title: '提示',
						content: '要删除所选足迹？',
						confirmColor: '#5B8C5A',
						success: function(res) {
							if (res.confirm) {
								util.request(api.FootprintDelete, { footprintId: footprint.id }).then(function(res) {
									if (res.code === 0) {
										uni.showToast({ title: '删除成功', icon: 'success' });
										that.getFootprintList();
									}
								});
							}
						}
					});
				} else {
					uni.navigateTo({ url: '/pages/goods/goods?id=' + footprint.goodsId });
				}
			},
			touchStart: function(e) { this.touch_start = e.timeStamp; },
			touchEnd: function(e) { this.touch_end = e.timeStamp; }
		},
		onPullDownRefresh() {
			this.getFootprintList();
		},
		onShow: function() {
			this.getFootprintList();
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

	.day-section {
		margin-bottom: 24rpx;
	}

	.day-title {
		font-size: 26rpx;
		color: #999;
		margin-bottom: 12rpx;
		padding-left: 8rpx;
	}

	.day-list {
		background: #FEFEFC;
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 10rpx rgba(91,140,90,0.08);
	}

	.footprint-item {
		display: flex;
		align-items: center;
		padding: 24rpx;
		border-bottom: 1rpx solid #f5f5f5;

		&:last-child {
			border-bottom: none;
		}
	}

	.item-img {
		width: 140rpx;
		height: 140rpx;
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
