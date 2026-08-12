<template>
	<view class="page">
		<view class="form-card">
			<view class="form-item">
				<text class="form-label">姓名</text>
				<input class="form-input" placeholder="请输入收货人姓名" v-model="address.userName" />
			</view>
			<view class="form-item">
				<text class="form-label">电话</text>
				<input class="form-input" v-model="address.telNumber" maxlength="15" type="number" placeholder="请输入手机号码" />
			</view>
			<view class="form-item" @tap="chooseRegion">
				<text class="form-label">地区</text>
				<input class="form-input" v-model="address.fullRegion" disabled placeholder="请选择省份、城市、区县" />
				<text class="form-arrow">›</text>
			</view>
			<view class="form-item">
				<text class="form-label">详细地址</text>
				<input class="form-input" v-model="address.detailInfo" placeholder="街道、楼盘号等" />
			</view>
			<view class="form-item form-item--switch">
				<text class="form-label">设为默认地址</text>
				<view :class="'custom-switch ' + (address.isDefault ? 'active' : '')" @tap="bindIsDefault">
					<view class="switch-dot"></view>
				</view>
			</view>
		</view>

		<view class="btn-group">
			<view class="btn-cancel" @tap="cancelAddress">取消</view>
			<view class="btn-save" @tap="saveAddress">保存地址</view>
		</view>

		<!-- 区域选择弹层 -->
		<view class="region-mask" v-if="openSelectRegion" @tap="cancelSelectRegion"></view>
		<view class="region-panel" v-if="openSelectRegion">
			<view class="region-header">
				<view class="region-tabs">
					<view :class="'tab-item ' + ((regionType - 1) === index ? 'active' : '') + (item.id == 0 ? ' placeholder' : '')"
					 @tap="selectRegionType(index)" v-for="(item, index) in selectRegionList" :key="index">
						{{item.name}}
					</view>
				</view>
				<view :class="'region-done ' + (selectRegionDone ? 'active' : '')" @tap="doneSelectRegion">确定</view>
			</view>
			<scroll-view scroll-y class="region-body">
				<view :class="'region-item ' + (item.selected ? 'selected' : '')" @tap="selectRegion(index)"
				 v-for="(item, index) in regionList" :key="item.id">
					{{item.name}}
					<text class="check-mark" v-if="item.selected">✓</text>
				</view>
			</scroll-view>
		</view>
	</view>
</template>

<script>
	const util = require("@/utils/util.js");
	const api = require('@/utils/api.js');
	export default {
		data() {
			return {
				address: {
					id: 0,
					provinceId: 0,
					cityId: 0,
					districtId: 0,
					fullRegion: '',
					userName: '',
					telNumber: '',
					isDefault: 0
				},
				addressId: 0,
				openSelectRegion: false,
				selectRegionList: [
					{ id: 0, name: '省份', parentId: 1, type: 1 },
					{ id: 0, name: '城市', parentId: 1, type: 2 },
					{ id: 0, name: '区县', parentId: 1, type: 3 }
				],
				regionType: 1,
				regionList: [],
				selectRegionDone: false
			}
		},
		methods: {
			bindIsDefault() {
				this.address.isDefault = this.address.isDefault ? 0 : 1;
			},
			getAddressDetail() {
				let that = this;
				util.request(api.AddressDetail, { id: that.addressId }).then(function(res) {
					if (res.code === 0 && res.data) {
						that.address = res.data;
					}
				});
			},
			setRegionDoneStatus() {
				this.selectRegionDone = this.selectRegionList.every(item => item.id != 0);
			},
			chooseRegion() {
				let that = this;
				that.openSelectRegion = true;
				let address = that.address;
				if (address.provinceId > 0 && address.cityId > 0 && address.districtId > 0) {
					let selectRegionList = that.selectRegionList;
					selectRegionList[0].id = address.provinceId;
					selectRegionList[0].name = address.provinceName || address.province_name;
					selectRegionList[0].parentId = 1;
					selectRegionList[1].id = address.cityId;
					selectRegionList[1].name = address.cityName || address.city_name;
					selectRegionList[1].parentId = address.provinceId;
					selectRegionList[2].id = address.districtId;
					selectRegionList[2].name = address.districtName || address.district_name;
					selectRegionList[2].parentId = address.cityId;
					that.selectRegionList = selectRegionList;
					that.regionType = 3;
					that.getRegionList(address.cityId);
				} else {
					that.selectRegionList = [
						{ id: 0, name: '省份', parentId: 1, type: 1 },
						{ id: 0, name: '城市', parentId: 1, type: 2 },
						{ id: 0, name: '区县', parentId: 1, type: 3 }
					];
					that.regionType = 1;
					that.getRegionList(1);
				}
				that.setRegionDoneStatus();
			},
			selectRegionType(index) {
				let that = this;
				let selectRegionList = that.selectRegionList;
				if (index + 1 == that.regionType || (index - 1 >= 0 && selectRegionList[index - 1].id <= 0)) return;
				that.regionType = index + 1;
				let selectRegionItem = selectRegionList[index];
				this.getRegionList(selectRegionItem.parentId);
				this.setRegionDoneStatus();
			},
			selectRegion(regionIndex) {
				let that = this;
				let regionItem = that.regionList[regionIndex];
				let regionType = regionItem.type;
				let selectRegionList = that.selectRegionList;
				selectRegionList[regionType - 1] = regionItem;
				that.selectRegionList = selectRegionList;
				if (regionType != 3) {
					that.regionType = regionType + 1;
					that.getRegionList(regionItem.id);
				}
				selectRegionList.map((item, index) => {
					if (index > regionType - 1) {
						item.id = 0;
						item.name = index == 1 ? '城市' : '区县';
						item.parentId = 0;
					}
					return item;
				});
				that.selectRegionList = selectRegionList;
				that.regionList = that.regionList.map(item => {
					item.selected = (that.regionType == item.type && that.selectRegionList[that.regionType - 1].id == item.id);
					return item;
				});
				this.setRegionDoneStatus();
			},
			doneSelectRegion() {
				if (!this.selectRegionDone) return;
				let address = this.address;
				let selectRegionList = this.selectRegionList;
				address.provinceId = selectRegionList[0].id;
				address.cityId = selectRegionList[1].id;
				address.districtId = selectRegionList[2].id;
				address.provinceName = selectRegionList[0].name;
				address.cityName = selectRegionList[1].name;
				address.districtName = selectRegionList[2].name;
				address.fullRegion = selectRegionList.map(item => item.name).join('');
				this.address = address;
				this.openSelectRegion = false;
			},
			cancelSelectRegion() {
				this.openSelectRegion = false;
			},
			getRegionList(regionId) {
				let that = this;
				let regionType = that.regionType;
				util.request(api.RegionList, { parentId: regionId }).then(function(res) {
					if (res.code === 0) {
						that.regionList = (res.data || []).map(item => {
							item.selected = (regionType == item.type && that.selectRegionList[regionType - 1].id == item.id);
							return item;
						});
					}
				});
			},
			cancelAddress() {
				uni.navigateBack();
			},
			saveAddress() {
				let address = this.address;
				address.userName = (address.userName || '').trim();
				address.telNumber = (address.telNumber || '').trim();
				address.detailInfo = (address.detailInfo || '').trim();
				if (!address.userName || address.userName.length > 32) { util.toast('请输入正确的收货人姓名'); return; }
				if (!/^1[3-9]\d{9}$/.test(address.telNumber)) { util.toast('请输入正确的手机号码'); return; }
				if (address.districtId == 0) { util.toast('请选择省市区'); return; }
				if (address.detailInfo.length < 2 || address.detailInfo.length > 128) { util.toast('详细地址应为2至128个字符'); return; }

				util.request(api.AddressSave, {
					id: address.id,
					userName: address.userName,
					telNumber: address.telNumber,
					provinceId: address.provinceId,
					cityId: address.cityId,
					districtId: address.districtId,
					isDefault: address.isDefault,
					provinceName: address.provinceName,
					cityName: address.cityName,
					countyName: address.districtName,
					detailInfo: address.detailInfo,
				}, 'POST', 'application/json').then(function(res) {
					if (res.code === 0) {
						uni.navigateBack();
					}
				});
			}
		},
		onLoad: function(options) {
			if (options.id && options.id != 0) {
				this.addressId = options.id;
				this.getAddressDetail();
			}
			this.getRegionList(1);
		}
	}
</script>

<style lang="scss">
	$green: #5B8C5A;
	$green-light: #7BAF7A;
	$green-bg: #F6F7F4;

	page {
		height: 100%;
		background: $green-bg;
	}

	.page {
		padding: 24rpx;
		padding-bottom: 140rpx;
	}

	.form-card {
		background: #FEFEFC;
		border-radius: 20rpx;
		overflow: hidden;
		box-shadow: 0 2rpx 12rpx rgba(91,140,90,0.08);
	}

	.form-item {
		display: flex;
		align-items: center;
		padding: 28rpx 30rpx;
		border-bottom: 1rpx solid #f5f5f5;

		&:last-child {
			border-bottom: none;
		}

		&--switch {
			justify-content: space-between;
		}
	}

	.form-label {
		width: 140rpx;
		font-size: 28rpx;
		color: #333;
		font-weight: 500;
	}

	.form-input {
		flex: 1;
		font-size: 28rpx;
		color: #333;
		height: 44rpx;
		line-height: 44rpx;
	}

	.form-arrow {
		font-size: 36rpx;
		color: #ccc;
		margin-left: 10rpx;
	}

	.custom-switch {
		width: 88rpx;
		height: 48rpx;
		border-radius: 24rpx;
		background: #ddd;
		position: relative;
		transition: background 0.3s;

		&.active {
			background: $green;
		}
	}

	.switch-dot {
		position: absolute;
		top: 4rpx;
		left: 4rpx;
		width: 40rpx;
		height: 40rpx;
		border-radius: 50%;
		background: #FEFEFC;
		box-shadow: 0 2rpx 4rpx rgba(0,0,0,0.15);
		transition: transform 0.3s;
	}

	.custom-switch.active .switch-dot {
		transform: translateX(40rpx);
	}

	.btn-group {
		display: flex;
		position: fixed;
		bottom: 0;
		left: 0;
		width: 100%;
		height: 100rpx;
	}

	.btn-cancel {
		flex: 1;
		height: 100rpx;
		line-height: 100rpx;
		text-align: center;
		font-size: 30rpx;
		color: #666;
		background: #FEFEFC;
	}

	.btn-save {
		flex: 2;
		height: 100rpx;
		line-height: 100rpx;
		text-align: center;
		font-size: 30rpx;
		color: #FEFEFC;
		font-weight: 500;
		background: linear-gradient(135deg, $green 0%, $green-light 100%);
	}

	.region-mask {
		position: fixed;
		top: 0;
		left: 0;
		width: 100%;
		height: 100%;
		background: rgba(0,0,0,0.4);
		z-index: 99;
	}

	.region-panel {
		position: fixed;
		bottom: 0;
		left: 0;
		width: 100%;
		background: #FEFEFC;
		border-radius: 24rpx 24rpx 0 0;
		z-index: 100;
		overflow: hidden;
	}

	.region-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 30rpx;
		border-bottom: 1rpx solid #f0f0f0;
	}

	.region-tabs {
		display: flex;
	}

	.tab-item {
		font-size: 26rpx;
		color: #333;
		margin-right: 30rpx;
		padding-bottom: 10rpx;
		max-width: 140rpx;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;

		&.active {
			color: $green;
			border-bottom: 3rpx solid $green;
			font-weight: 500;
		}

		&.placeholder {
			color: #999;
		}
	}

	.region-done {
		font-size: 28rpx;
		color: #999;
		padding: 8rpx 20rpx;

		&.active {
			color: $green;
			font-weight: 500;
		}
	}

	.region-body {
		height: 500rpx;
		padding: 0 30rpx;
	}

	.region-item {
		height: 90rpx;
		line-height: 90rpx;
		font-size: 28rpx;
		color: #333;
		border-bottom: 1rpx solid #f8f8f8;
		display: flex;
		align-items: center;
		justify-content: space-between;

		&.selected {
			color: $green;
			font-weight: 500;
		}
	}

	.check-mark {
		font-size: 28rpx;
		color: $green;
	}
</style>
