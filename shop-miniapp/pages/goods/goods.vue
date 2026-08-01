<template>
	<view class="page">
		<scroll-view class="scroll-area" :style="'height:' + winHeight + 'rpx'" :scroll-y="true">
			<!-- 正常展示模式 -->
			<view v-if="!openAttr">
				<!-- 商品轮播图 -->
				<swiper class="gallery-swiper" indicator-dots circular autoplay :interval="3000"
					indicator-color="rgba(255,255,255,0.4)" indicator-active-color="#FEFEFC">
					<swiper-item v-for="(item, index) in gallery" :key="item.id">
						<image class="gallery-img" :src="item.imgUrl" mode="aspectFill"></image>
					</swiper-item>
				</swiper>

				<!-- 价格信息区 -->
				<view class="price-section">
					<view class="price-row">
						<text class="price-symbol">¥</text>
						<text class="price-value">{{goods.retailPrice || '0'}}</text>
						<text class="price-market" v-if="goods.counterPrice">¥{{goods.counterPrice}}</text>
					</view>
					<view class="sales-info" v-if="goods.sellVolume">
						<text>已售 {{goods.sellVolume}}</text>
					</view>
				</view>

				<!-- VIP会员权益卡 -->
				<view class="vip-section" v-if="goods.retailPrice">
					<view class="vip-left">
						<view class="vip-badge-wrap">
							<text class="vip-badge">黄金会员</text>
							<text class="vip-price-label">专享价 ¥{{ (parseFloat(goods.retailPrice) * 0.9).toFixed(2) }}</text>
						</view>
						<text class="vip-note">开卡后下单立享折扣</text>
					</view>
					<navigator url="/pages/ucenter/coupon/coupon" class="vip-right">
						<text class="vip-action-text">立即开通</text>
					</navigator>
				</view>

				<!-- 商品标题区 -->
				<view class="info-section">
					<text class="goods-name">{{goods.name || ''}}</text>
					<text class="goods-brief">{{goods.goodsBrief || ''}}</text>
					<!-- 品牌标签 -->
					<navigator v-if="brand.name" class="brand-tag" :url="'../brandDetail/brandDetail?id='+ brand.id">
						<text>{{brand.name}}</text>
					</navigator>
				</view>

				<!-- 服务保障 -->
				<view class="service-tags">
					<view class="service-tag">
						<text class="tag-dot">✓</text>
						<text>正品保障</text>
					</view>
					<view class="service-tag">
						<text class="tag-dot">✓</text>
						<text>极速退款</text>
					</view>
					<view class="service-tag">
						<text class="tag-dot">✓</text>
						<text>满88元免邮</text>
					</view>
				</view>

				<!-- 药食百科科普看板 -->
				<view class="science-board-card" v-if="scienceData">
					<view class="science-header">
						<view class="science-mark"></view>
						<text class="science-title">药食同源 · 养生百科</text>
					</view>
					<view class="science-grid">
						<view class="science-item">
							<text class="sci-label">四气五味</text>
							<text class="sci-val">{{scienceData.nature}}</text>
						</view>
						<view class="science-item">
							<text class="sci-label">归经</text>
							<text class="sci-val">{{scienceData.meridian}}</text>
						</view>
						<view class="science-item">
							<text class="sci-label">主要活性成分</text>
							<text class="sci-val highlight">{{scienceData.components}}</text>
						</view>
						<view class="science-item">
							<text class="sci-label">每日推荐剂量</text>
							<text class="sci-val">{{scienceData.dosage}}</text>
						</view>
					</view>
				</view>

				<!-- 选择规格 -->
				<view class="spec-entry" @tap="switchAttrPop">
					<text class="spec-label">选择</text>
					<text class="spec-value">{{checkedSpecText}}</text>
					<text class="spec-arrow">›</text>
				</view>

				<!-- 评论区 -->
				<view class="comment-section" v-if="comment.count > 0">
					<navigator class="comment-header" :url="'../comment/comment?valueId='+goods.id+'&typeId=0'">
						<text class="comment-title">用户评价 ({{comment.count > 999 ? '999+' : comment.count}})</text>
						<text class="comment-more">查看全部 ›</text>
					</navigator>
					<view class="comment-item" v-if="comment.data">
						<view class="comment-user">
							<image class="comment-avatar" :src="comment.data.avatar"></image>
							<text class="comment-name">{{comment.data.nickname}}</text>
							<text class="comment-time">{{comment.data.addTime}}</text>
						</view>
						<text class="comment-content">{{comment.data.content}}</text>
						<view class="comment-imgs" v-if="comment.data.picList && comment.data.picList.length > 0">
							<image class="comment-pic" v-for="(item, index) in comment.data.picList" :key="index" :src="item.picUrl" mode="aspectFill"></image>
						</view>
					</view>
				</view>

				<!-- 商品参数 -->
				<view class="attr-section" v-if="attribute && attribute.length > 0">
					<text class="attr-title">商品参数</text>
					<view class="attr-list">
						<view class="attr-item" v-for="(item, index) in attribute" :key="item.name">
							<text class="attr-key">{{item.name}}</text>
							<text class="attr-val">{{item.value}}</text>
						</view>
					</view>
				</view>

				<!-- 经典膳食推荐搭配 -->
				<view class="combo-recommend-card" v-if="comboData">
					<view class="combo-header">
						<text class="combo-title">膳食养生搭配推荐</text>
						<text class="combo-subtitle">为这款商品精选一组更顺口的日常组合</text>
					</view>
					<view class="combo-body">
						<view class="combo-goods">
							<view class="combo-goods-item curr">
								<image class="combo-img" :src="goods.listPicUrl" mode="aspectFill"></image>
								<text class="combo-name">{{goods.name}}</text>
							</view>
							<text class="combo-plus">+</text>
							<view class="combo-goods-item partner">
								<image class="combo-img" :src="comboData.partnerPic" mode="aspectFill"></image>
								<text class="combo-name">{{comboData.partnerName}}</text>
							</view>
						</view>
						<view class="combo-action-area">
							<view class="combo-benefit">
								<text class="combo-tip">双效温补，气血调和</text>
								<text class="combo-price-tip">搭配购买更省心，也更适合送礼自用</text>
							</view>
							<button class="combo-add-btn" @tap="addComboToCart">
								<text>一键搭配加购</text>
							</button>
						</view>
					</view>
				</view>

				<!-- 图文详情 -->
				<view class="detail-section">
					<view class="detail-divider">
						<view class="divider-line"></view>
						<text class="divider-text">商品详情</text>
						<view class="divider-line"></view>
					</view>
					<view class="detail-content">
						<uParse :content="goods.goodsDesc" noData="" />
					</view>
				</view>

				<!-- 常见问题 -->
				<view class="faq-section" v-if="issueList && issueList.length > 0">
					<view class="detail-divider">
						<view class="divider-line"></view>
						<text class="divider-text">常见问题</text>
						<view class="divider-line"></view>
					</view>
					<view class="faq-item" v-for="(item, index) in issueList" :key="item.id">
						<view class="faq-q">
							<text class="faq-dot"></text>
							<text>{{item.question}}</text>
						</view>
						<text class="faq-a">{{item.answer}}</text>
					</view>
				</view>

				<!-- 相关推荐 -->
				<view class="related-section" v-if="relatedGoods.length > 0">
					<view class="detail-divider">
						<view class="divider-line"></view>
						<text class="divider-text">大家都在看</text>
						<view class="divider-line"></view>
					</view>
					<view class="related-grid">
						<navigator class="related-item" v-for="(item, index) in relatedGoods" :key="item.id" :url="'/pages/goods/goods?id='+item.id">
							<image class="related-img" :src="item.listPicUrl" mode="aspectFill"></image>
							<text class="related-name">{{item.name}}</text>
							<text class="related-price">¥{{item.retailPrice}}</text>
						</navigator>
					</view>
				</view>
			</view>

			<!-- 规格选择模式 -->
			<view v-if="openAttr" class="sku-panel">
				<view class="sku-header">
					<image class="sku-img" :src="goods.listPicUrl" mode="aspectFill"></image>
					<view class="sku-meta">
						<text class="sku-price">¥{{goods.retailPrice}}</text>
						<text class="sku-selected">{{checkedSpecText}}</text>
						<text class="sku-stock">库存 {{ selectedSku ? selectedSku.stock : '-' }}</text>
					</view>
				</view>
				<view class="sku-body">
					<view class="sku-group" v-for="(item, index) in specificationList" :key="item.specificationId">
						<text class="sku-group-name">{{item.name}}</text>
						<view class="sku-values">
							<view
								class="sku-value"
								:class="{selected: vitem.checked, disabled: vitem.disabled}"
								v-for="(vitem, vindex) in item.valueList"
								:key="vitem.id"
								@tap="clickSkuValue(item.specificationId, vitem.id)"
							>{{vitem.value}}</view>
						</view>
					</view>
					<view class="sku-quantity">
						<text class="sku-group-name">数量</text>
						<view class="sku-stepper">
							<view class="stepper-btn" @tap="cutNumber"><text>−</text></view>
							<text class="stepper-num">{{number}}</text>
							<view class="stepper-btn plus" @tap="addNumber"><text>+</text></view>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 底部操作栏 -->
		<view class="action-bar">
			<view class="action-icon" @tap="closeAttrOrCollect">
				<image class="action-icon-img" :src="collectBackImage" mode="aspectFit"></image>
				<text class="action-icon-text">{{openAttr ? '返回' : '收藏'}}</text>
			</view>
			<view class="action-icon" @tap="openCartPage">
				<view class="cart-badge" v-if="cartGoodsCount > 0">{{cartGoodsCount}}</view>
				<image class="action-icon-img" src="/static/images/ic_menu_shoping_nor.png" mode="aspectFit"></image>
				<text class="action-icon-text">购物车</text>
			</view>
			<view class="action-btn action-cart" @tap="addToCart">
				<text>加入购物车</text>
			</view>
			<view class="action-btn action-buy" @tap="buyGoods">
				<text>立即购买</text>
			</view>
		</view>
	</view>
</template>

<script>
const util = require('@/utils/util.js');
const api = require('@/utils/api.js');
const skuUtil = require('@/utils/sku.js');
import uParse from '@/components/uParse/src/wxParse';

export default {
	components: { uParse },
	data() {
		return {
			winHeight: '',
			id: 0,
			goods: {},
			gallery: [],
			baseGallery: [],
			attribute: [],
			issueList: [],
			comment: [],
			brand: {},
			specificationList: [],
			productList: [],
			baseGoods: {},
			selectedSku: null,
			relatedGoods: [],
			cartGoodsCount: 0,
			userHasCollect: 0,
			number: 1,
			checkedSpecText: '请选择规格数量',
			openAttr: false,
			noCollectImage: '/static/images/icon_collect.png',
			hasCollectImage: '/static/images/icon_collect_checked.png',
			collectBackImage: '/static/images/icon_collect.png',
			scienceData: null,
			comboData: null,
			scienceDb: {
				1: { nature: '性温，味甘', meridian: '归肺、肝、肾经', components: '明胶原蛋白、18种氨基酸、微量元素', dosage: '建议每日 3-9 克', partnerId: 2, partnerName: '同仁堂枸杞', partnerPic: 'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=150' },
				2: { nature: '性平，味甘', meridian: '归肝、肾、肺经', components: '枸杞多糖、甜菜碱、胡萝卜素、维C', dosage: '建议每日 10-15 克', partnerId: 7, partnerName: '西洋参切片', partnerPic: 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=150' },
				3: { nature: '性微温，味甘、微苦', meridian: '归脾、肺、心、肾经', components: '人参皂苷、人参多糖、挥发油、氨基酸', dosage: '建议每日 3-6 克', partnerId: 2, partnerName: '同仁堂枸杞', partnerPic: 'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=150' },
				5: { nature: '性平，味甘', meridian: '归脾、胃、肺、大肠经', components: '天然果糖、葡萄糖、活性淀粉酶', dosage: '建议每日 10-20 克', partnerId: 19, partnerName: '薄壳手剥碧根果', partnerPic: 'https://images.unsplash.com/photo-1585238342024-78d387f4a707?w=150' },
				6: { nature: '性温，味甘、微苦', meridian: '归肝、胃经', components: '三七总皂苷、三七素、槲皮素', dosage: '建议每日 3-6 克', partnerId: 1, partnerName: '东阿阿胶糕', partnerPic: 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=150' }
			}
		}
	},
	methods: {
		getGoodsInfo() {
			util.request(api.GoodsDetail, { id: this.id }).then(res => {
				if (res.code === 0) {
					this.baseGoods = res.data.info;
					this.goods = Object.assign({}, this.baseGoods);
					this.baseGallery = res.data.gallery || [];
					this.gallery = this.baseGallery;
					this.attribute = res.data.attribute;
					this.issueList = res.data.issue;
					this.comment = res.data.comment;
					this.brand = res.data.brand;
					this.specificationList = res.data.specificationList;
					this.productList = (res.data.productList || []).map(product => this.normalizeProduct(product));
					this.userHasCollect = res.data.userHasCollect;
					this.setDefSpecInfo(this.specificationList);
					this.refreshSkuState();
					this.collectBackImage = this.userHasCollect == 1 ? this.hasCollectImage : this.noCollectImage;
					if (util.getToken()) {
						util.request(api.FootprintRecord, { goodsId: this.id }, 'POST', 'application/json', false, true).catch(() => {});
					}
					this.getGoodsRelated();
					this.initScienceInfo(this.id);
				}
			});
		},
		initScienceInfo(goodsId) {
			const data = this.scienceDb[goodsId];
			if (data) {
				this.scienceData = {
					nature: data.nature,
					meridian: data.meridian,
					components: data.components,
					dosage: data.dosage
				};
				this.comboData = {
					partnerId: data.partnerId,
					partnerName: data.partnerName,
					partnerPic: data.partnerPic
				};
			} else {
				this.scienceData = {
					nature: '性平，味甘',
					meridian: '归脾、胃、肺经',
					components: '活性多糖、膳食纤维、微量矿物质',
					dosage: '建议日常随膳食适量食用'
				};
				this.comboData = null;
			}
		},
		addComboToCart() {
			if (!this.comboData) return;
			if (!this.isCheckedAllSpec() || !this.selectedSku) {
				this.switchAttrPop();
				uni.showToast({ title: '请先选择完整规格', icon: 'none' });
				return;
			}
			if (this.selectedSku.stock < 1) {
				uni.showToast({ title: '所选规格暂时缺货', icon: 'none' });
				return;
			}
			let that = this;
			util.request(api.CartAdd, { goodsId: this.goods.id, number: 1, productId: this.selectedSku.id }, 'POST', 'application/json').then(res => {
				if (res.code === 0) {
					util.request(api.CartAdd, { goodsId: this.comboData.partnerId, number: 1, productId: 1 }, 'POST', 'application/json').then(resPartner => {
						if (resPartner.code === 0) {
							uni.showToast({
								title: '黄金搭档搭配加购成功',
								icon: 'success'
							});
							that.cartGoodsCount = resPartner.data.cartTotal.goodsCount;
						}
					});
				}
			});
		},
		getGoodsRelated() {
			util.request(api.GoodsRelated, { id: this.id }).then(res => {
				if (res.code === 0) this.relatedGoods = res.data.goodsList;
			});
		},
		clickSkuValue(specNameId, specValueId) {
			if (this.isValueDisabled(specNameId, specValueId)) return;
			let _specificationList = this.specificationList;
			for (let i = 0; i < _specificationList.length; i++) {
				if (_specificationList[i].specificationId == specNameId) {
					for (let j = 0; j < _specificationList[i].valueList.length; j++) {
						if (_specificationList[i].valueList[j].id == specValueId) {
							_specificationList[i].valueList[j].checked = !_specificationList[i].valueList[j].checked;
						} else {
							_specificationList[i].valueList[j].checked = false;
						}
					}
				}
			}
			this.changeSpecInfo();
		},
		getCheckedSpecValue() {
			let checkedValues = [];
			let _specificationList = this.specificationList;
			for (let i = 0; i < _specificationList.length; i++) {
				let _checkedObj = {
					nameId: _specificationList[i].specificationId,
					valueId: 0,
					valueText: ''
				};
				for (let j = 0; j < _specificationList[i].valueList.length; j++) {
					if (_specificationList[i].valueList[j].checked) {
						_checkedObj.valueId = _specificationList[i].valueList[j].id;
						_checkedObj.valueText = _specificationList[i].valueList[j].value;
					}
				}
				checkedValues.push(_checkedObj);
			}
			return checkedValues;
		},
		isCheckedAllSpec() {
			return !this.getCheckedSpecValue().some(v => v.valueId == 0);
		},
		changeSpecInfo() {
			let checkedNameValue = this.getCheckedSpecValue();
			let checkedValue = checkedNameValue.filter(v => v.valueId != 0).map(v => v.valueText);
			if (checkedValue.length > 0) {
				this.checkedSpecText = checkedValue.join('　');
			} else {
				this.checkedSpecText = '请选择规格数量';
			}
			this.refreshSkuState();
		},
		getCheckedProductItem() {
			let selected = this.getCheckedSpecValue().filter(v => v.valueId != 0);
			if (selected.length !== this.specificationList.length) return null;
			return this.productList.find(product => this.productMatchesSelection(product, selected, true));
		},
		normalizeProduct(product) {
			return skuUtil.normalizeProduct(product, this.baseGoods);
		},
		productMatchesSelection(product, selected, requireComplete) {
			return skuUtil.productMatchesSelection(product, selected, requireComplete);
		},
		isValueDisabled(specificationId, valueId) {
			let selected = this.getCheckedSpecValue().filter(v => v.nameId != specificationId && v.valueId != 0);
			selected.push({ nameId: Number(specificationId), valueId: Number(valueId) });
			return !this.productList.some(product => product.available && this.productMatchesSelection(product, selected, false));
		},
		refreshSkuState() {
			let sku = this.getCheckedProductItem();
			this.selectedSku = sku && sku.available ? sku : null;
			if (this.selectedSku) {
				this.goods = Object.assign({}, this.baseGoods, { retailPrice: sku.retailPrice, counterPrice: sku.counterPrice, listPicUrl: sku.picUrl || this.baseGoods.listPicUrl, picUrl: sku.picUrl || this.baseGoods.picUrl });
				this.gallery = sku.hasSkuPic ? [{ id: sku.id, imgUrl: sku.picUrl }] : this.baseGallery;
				this.number = Math.min(this.number, sku.stock);
			} else {
				this.goods = Object.assign({}, this.baseGoods);
				this.gallery = this.baseGallery;
			}
			for (let i = 0; i < this.specificationList.length; i++) for (let j = 0; j < this.specificationList[i].valueList.length; j++) this.$set(this.specificationList[i].valueList[j], 'disabled', this.isValueDisabled(this.specificationList[i].specificationId, this.specificationList[i].valueList[j].id));
		},
		switchAttrPop() {
			if (this.openAttr == false) {
				this.openAttr = !this.openAttr;
				this.collectBackImage = "/static/images/detail_back.png";
			}
		},
		closeAttrOrCollect() {
			let that = this;
			if (that.openAttr) {
				that.openAttr = false;
				that.collectBackImage = that.userHasCollect == 1 ? that.hasCollectImage : that.noCollectImage;
			} else {
				util.request(api.CollectAddOrDelete, { typeId: 0, valueId: that.id }, "POST", "application/json").then(res => {
					if (res.code == 0) {
						if (res.data.type == 'add') {
							that.userHasCollect = 1;
							that.collectBackImage = that.hasCollectImage;
						} else {
							that.userHasCollect = 0;
							that.collectBackImage = that.noCollectImage;
						}
					} else {
						uni.showToast({ image: '/static/images/icon_error.png', title: res.msg, mask: true });
					}
				});
			}
		},
		openCartPage() {
			uni.switchTab({ url: '/pages/cart/cart' });
		},
		buyGoods() {
			let that = this;
			if (that.openAttr == false) {
				that.openAttr = !that.openAttr;
				that.collectBackImage = "/static/images/detail_back.png";
			} else {
				if (!that.isCheckedAllSpec()) {
					uni.showToast({ title: '请选择规格数量', icon: 'none' });
					return false;
				}
				let checkedProduct = that.getCheckedProductItem();
				if (!checkedProduct || !checkedProduct.available || checkedProduct.stock < that.number) {
					uni.showToast({ title: '所选规格库存不足', icon: 'none' });
					return false;
				}
				util.request(api.BuyAdd, { goodsId: that.goods.id, number: that.number, productId: checkedProduct.id }, "POST", 'application/json').then(res => {
					if (res.code == 0) {
						that.openAttr = !that.openAttr;
						uni.navigateTo({ url: '/pages/shopping/checkout/checkout?isBuy=true' });
					} else {
						uni.showToast({ image: '/static/images/icon_error.png', title: res.msg, mask: true });
					}
				});
			}
		},
		addToCart() {
			let that = this;
			if (that.openAttr == false) {
				that.openAttr = !that.openAttr;
				that.collectBackImage = "/static/images/detail_back.png";
			} else {
				if (!that.isCheckedAllSpec()) {
					uni.showToast({ title: '请选择完整规格', icon: 'none' });
					return false;
				}
				let checkedProduct = that.getCheckedProductItem();
				if (!checkedProduct || !checkedProduct.available || checkedProduct.stock < that.number) {
					uni.showToast({ title: '所选规格库存不足', icon: 'none' });
					return false;
				}
				util.request(api.CartAdd, { goodsId: that.goods.id, number: that.number, productId: checkedProduct.id }, 'POST', 'application/json').then(res => {
					if (res.code == 0) {
						uni.showToast({ title: '添加成功' });
						that.openAttr = !that.openAttr;
						that.cartGoodsCount = res.data.cartTotal.goodsCount;
						that.collectBackImage = that.userHasCollect == 1 ? that.hasCollectImage : that.noCollectImage;
					} else {
						uni.showToast({ image: '/static/images/icon_error.png', title: res.msg, mask: true });
					}
				});
			}
		},
		cutNumber() {
			this.number = (this.number - 1 > 1) ? this.number - 1 : 1;
		},
		addNumber() {
			if (!this.selectedSku) return;
			this.number = Math.min(this.number + 1, this.selectedSku.stock);
		},
		setDefSpecInfo(specificationList) {
			let that = this;
			if (!specificationList) return;
			for (let i = 0; i < specificationList.length; i++) {
				let specification = specificationList[i];
				let specNameId = specification.specificationId;
				if (specification.valueList && specification.valueList.length == 1) {
					let specValueId = specification.valueList[0].id;
					that.clickSkuValue(specNameId, specValueId);
				}
			}
		}
	},
	onLoad(options) {
		this.id = parseInt(options.id);
		let that = this;
		this.getGoodsInfo();
		util.request(api.CartGoodsCount).then(res => {
			if (res.code === 0) {
				that.cartGoodsCount = res.data.cartTotal.goodsCount;
			}
		});
		uni.getSystemInfo({
			success: function(res) {
				var clientHeight = res.windowHeight,
					clientWidth = res.windowWidth,
					rpxR = 750 / clientWidth;
				that.winHeight = clientHeight * rpxR - 110;
			}
		});
	},
	onShareAppMessage() {
		return {
			title: this.goods.name || '药食同源好物推荐',
			path: '/pages/goods/goods?id=' + this.id
		}
	}
}
</script>

<style lang="scss">
.page {
	background: #FEFEFC;
}

.scroll-area {
	width: 100%;
}

/* 轮播图 */
.gallery-swiper {
	width: 750rpx;
	height: 750rpx;
}

.gallery-img {
	width: 750rpx;
	height: 750rpx;
}

/* 价格区 */
.price-section {
	display: flex;
	align-items: baseline;
	justify-content: space-between;
	padding: 24rpx 30rpx 16rpx;
	background: #FEFEFC;
}

.price-row {
	display: flex;
	align-items: baseline;
}

.price-symbol {
	font-size: 28rpx;
	color: $red;
	font-weight: 700;
}

.price-value {
	font-size: 48rpx;
	color: $red;
	font-weight: 700;
	margin-right: 12rpx;
}

.price-market {
	font-size: 26rpx;
	color: $text-hint;
	text-decoration: line-through;
}

.sales-info {
	font-size: 22rpx;
	color: $text-hint;
}

/* VIP会员权益卡 */
.vip-section {
	margin: 0 30rpx 24rpx;
	background:
		linear-gradient(135deg, rgba(255, 255, 255, 0.30) 0%, rgba(255, 255, 255, 0.08) 42%, rgba(250, 244, 222, 0.22) 100%),
		linear-gradient(135deg, #F6F2E4 0%, #FDFBF4 100%);
	border: 1rpx solid rgba(160, 138, 95, 0.16);
	border-radius: 22rpx;
	padding: 22rpx 24rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
	box-shadow: 0 10rpx 24rpx rgba(126, 116, 92, 0.08);
	transition: all 0.2s ease;

	&:active {
		transform: scale(0.98);
		filter: brightness(0.98);
	}
}

.vip-left {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
}

.vip-badge-wrap {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 12rpx;
}

.vip-badge {
	background: rgba(145, 122, 78, 0.12);
	color: #8D7248;
	font-size: 20rpx;
	font-weight: 700;
	padding: 6rpx 14rpx;
	border-radius: 999rpx;
	letter-spacing: 1rpx;
}

.vip-price-label {
	font-size: 28rpx;
	color: #7D6847;
	font-weight: 700;
}

.vip-note {
	margin-top: 10rpx;
	font-size: 22rpx;
	color: rgba(118, 104, 77, 0.72);
}

.vip-right {
	display: flex;
	align-items: center;
	height: 68rpx;
	padding: 0 24rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, #7B957F 0%, #6A846F 100%);
	box-shadow: 0 8rpx 18rpx rgba(106, 132, 111, 0.16);
}

.vip-action-text {
	font-size: 22rpx;
	color: #FEFEFC;
	font-weight: 700;
}

/* 商品信息 */
.info-section {
	padding: 0 30rpx 24rpx;
	background: #FEFEFC;
}

.goods-name {
	font-size: 32rpx;
	color: $text-primary;
	font-weight: 600;
	line-height: 1.5;
	display: block;
}

.goods-brief {
	font-size: 26rpx;
	color: $text-secondary;
	margin-top: 8rpx;
	display: block;
}

.brand-tag {
	display: inline-block;
	margin-top: 16rpx;
	padding: 6rpx 20rpx;
	border: 2rpx solid $green;
	border-radius: 8rpx;
	font-size: 22rpx;
	color: $green;
}

/* 服务保障 */
.service-tags {
	display: flex;
	flex-wrap: wrap;
	gap: 14rpx;
	padding: 16rpx 30rpx 24rpx;
	background: #FEFEFC;
	border-bottom: 1rpx solid $green-bg;
}

.service-tag {
	display: flex;
	align-items: center;
	padding: 10rpx 16rpx;
	background: rgba(232, 236, 232, 0.65);
	border-radius: 999rpx;
	font-size: 22rpx;
	color: $text-secondary;
}

.tag-dot {
	color: $green;
	margin-right: 8rpx;
	font-weight: 700;
}

/* 规格选择入口 */
.spec-entry {
	display: flex;
	align-items: center;
	padding: 28rpx 30rpx;
	background: #FEFEFC;
	margin-top: 16rpx;
}

.spec-label {
	font-size: 26rpx;
	color: $text-hint;
	margin-right: 20rpx;
}

.spec-value {
	flex: 1;
	font-size: 28rpx;
	color: $text-primary;
}

.spec-arrow {
	font-size: 36rpx;
	color: $text-hint;
}

/* 评论区 */
.comment-section {
	margin-top: 16rpx;
	padding: 28rpx 30rpx;
	background: #FEFEFC;
}

.comment-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 24rpx;
}

.comment-title {
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
}

.comment-more {
	font-size: 24rpx;
	color: $text-hint;
}

.comment-item {
	padding-top: 20rpx;
	border-top: 1rpx solid $green-bg;
}

.comment-user {
	display: flex;
	align-items: center;
	margin-bottom: 16rpx;
}

.comment-avatar {
	width: 56rpx;
	height: 56rpx;
	border-radius: 50%;
	margin-right: 12rpx;
}

.comment-name {
	flex: 1;
	font-size: 24rpx;
	color: $text-primary;
}

.comment-time {
	font-size: 22rpx;
	color: $text-hint;
}

.comment-content {
	font-size: 26rpx;
	color: $text-primary;
	line-height: 1.6;
	display: block;
}

.comment-imgs {
	display: flex;
	gap: 12rpx;
	margin-top: 16rpx;
}

.comment-pic {
	width: 150rpx;
	height: 150rpx;
	border-radius: 12rpx;
}

/* 商品参数 */
.attr-section {
	margin-top: 16rpx;
	padding: 28rpx 30rpx;
	background: #FEFEFC;
}

.attr-title {
	font-size: 30rpx;
	font-weight: 700;
	color: $text-primary;
	display: block;
	margin-bottom: 20rpx;
}

.attr-list {
	background: $green-bg;
	border-radius: 12rpx;
	overflow: hidden;
}

.attr-item {
	display: flex;
	padding: 16rpx 24rpx;
	border-bottom: 1rpx solid #FEFEFC;
}

.attr-key {
	width: 160rpx;
	font-size: 24rpx;
	color: $text-hint;
	flex-shrink: 0;
}

.attr-val {
	flex: 1;
	font-size: 24rpx;
	color: $text-primary;
}

/* 详情分割线 */
.detail-divider {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 40rpx 0 24rpx;
}

.divider-line {
	width: 68rpx;
	height: 2rpx;
	background: linear-gradient(90deg, rgba(111, 142, 117, 0), rgba(111, 142, 117, 0.34), rgba(111, 142, 117, 0));
}

.divider-text {
	font-size: 24rpx;
	color: #889388;
	margin: 0 18rpx;
	letter-spacing: 4rpx;
}

/* 图文详情 */
.detail-section {
	background: #FEFEFC;
	margin-top: 16rpx;
	padding: 0 30rpx 30rpx;
}

.detail-content {
	width: 100%;
	overflow: hidden;
}

.detail-content image {
	width: 100% !important;
	display: block;
}

/* 常见问题 */
.faq-section {
	background: #FEFEFC;
	padding: 0 30rpx 30rpx;
}

.faq-item {
	margin-bottom: 24rpx;
}

.faq-q {
	display: flex;
	align-items: flex-start;
	font-size: 26rpx;
	color: $text-primary;
	font-weight: 600;
	margin-bottom: 8rpx;
}

.faq-dot {
	width: 10rpx;
	height: 10rpx;
	background: $green;
	border-radius: 50%;
	margin-top: 14rpx;
	margin-right: 12rpx;
	flex-shrink: 0;
}

.faq-a {
	font-size: 24rpx;
	color: $text-secondary;
	line-height: 1.6;
	padding-left: 22rpx;
	display: block;
}

/* 相关推荐 */
.related-section {
	background: #FEFEFC;
	margin-top: 16rpx;
	padding: 0 24rpx 30rpx;
}

.related-grid {
	overflow: hidden;
}

.related-item {
	float: left;
	width: 335rpx;
	background: $green-bg;
	border-radius: 16rpx;
	overflow: hidden;
	text-decoration: none;
	margin-bottom: 16rpx;

	&:nth-child(odd) {
		margin-right: 16rpx;
	}
}

.related-img {
	width: 100%;
	height: 300rpx;
}

.related-name {
	display: block;
	padding: 16rpx;
	font-size: 26rpx;
	color: $text-primary;
	overflow: hidden;
	white-space: nowrap;
	text-overflow: ellipsis;
}

.related-price {
	display: block;
	padding: 0 16rpx 16rpx;
	font-size: 28rpx;
	color: $red;
	font-weight: 700;
}

/* SKU 选择面板 */
.sku-panel {
	padding: 30rpx;
	background: #FEFEFC;
	min-height: 600rpx;
}

.sku-header {
	display: flex;
	padding-bottom: 30rpx;
	border-bottom: 1rpx solid $green-bg;
	margin-bottom: 30rpx;
}

.sku-img {
	width: 180rpx;
	height: 180rpx;
	border-radius: 16rpx;
	margin-right: 24rpx;
}

.sku-meta {
	display: flex;
	flex-direction: column;
	justify-content: center;
}

.sku-price {
	font-size: 40rpx;
	color: $red;
	font-weight: 700;
}

.sku-selected {
	font-size: 26rpx;
	color: $text-secondary;
	margin-top: 12rpx;
}

.sku-stock {
	font-size: 24rpx;
	color: $text-secondary;
	margin-top: 8rpx;
}

.sku-body {
	padding: 10rpx 0;
}

.sku-group {
	margin-bottom: 36rpx;
}

.sku-group-name {
	font-size: 28rpx;
	color: $text-primary;
	font-weight: 600;
	margin-bottom: 20rpx;
	display: block;
}

.sku-values {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.sku-value {
	height: 64rpx;
	padding: 0 32rpx;
	line-height: 60rpx;
	border: 2rpx solid #e0e0e0;
	border-radius: 32rpx;
	font-size: 26rpx;
	color: $text-primary;
	background: #FEFEFC;
	transition: all 0.15s ease-in-out;

	&:active {
		transform: scale(0.95);
	}

	&.selected {
		border-color: $green;
		color: $green;
		background: $green-light;
		font-weight: 600;
	}

	&.disabled {
		border-color: #eeeeee;
		color: $text-hint;
		background: #f5f5f5;
		opacity: 0.55;
	}
}

.sku-quantity {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 36rpx;
	padding-top: 30rpx;
	border-top: 1rpx solid $green-bg;
}

.sku-stepper {
	display: flex;
	align-items: center;
	height: 60rpx;
	background: $green-bg;
	border-radius: 30rpx;
}

.stepper-btn {
	width: 60rpx;
	height: 60rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 32rpx;
	color: $text-secondary;
	transition: all 0.15s ease;

	&:active {
		transform: scale(0.85);
	}

	&.plus {
		color: $green;
		font-weight: 700;
	}
}

.stepper-num {
	min-width: 60rpx;
	text-align: center;
	font-size: 28rpx;
	font-weight: 600;
	color: $text-primary;
}

/* 底部操作栏 */
.action-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	height: 110rpx;
	background: rgba(255, 255, 255, 0.88);
	backdrop-filter: blur(20rpx);
	-webkit-backdrop-filter: blur(20rpx);
	display: flex;
	align-items: center;
	padding: 0 20rpx;
	box-shadow: 0 -4rpx 24rpx rgba(77, 112, 77, 0.05);
	z-index: 100;
}

.action-icon {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	width: 100rpx;
	position: relative;
	transition: transform 0.15s ease;

	&:active {
		transform: scale(0.92);
	}
}

.action-icon-img {
	width: 44rpx;
	height: 44rpx;
}

.action-icon-text {
	font-size: 20rpx;
	color: $text-hint;
	margin-top: 4rpx;
}

.cart-badge {
	position: absolute;
	top: 8rpx;
	right: 20rpx;
	min-width: 28rpx;
	height: 28rpx;
	line-height: 28rpx;
	text-align: center;
	background: $red;
	color: #FEFEFC;
	font-size: 18rpx;
	border-radius: 14rpx;
	padding: 0 6rpx;
}

.action-btn {
	flex: 1;
	height: 76rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 38rpx;
	font-size: 28rpx;
	font-weight: 600;
	margin-left: 12rpx;
	transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);

	&:active {
		transform: scale(0.97);
		filter: brightness(0.92);
	}
}

.action-cart {
	background: linear-gradient(135deg, #EEF3ED 0%, #E1E9E0 100%);
	color: #567159;
	border: 2rpx solid rgba(111, 142, 117, 0.18);
}

.action-buy {
	background: linear-gradient(135deg, #D7E3D8 0%, #C8D7C9 100%);
	color: #4F6854;
}

/* 药食百科看板样式 */
.science-board-card {
	background:
		linear-gradient(180deg, rgba(255, 255, 255, 0.34) 0%, rgba(255, 255, 255, 0.10) 100%),
		linear-gradient(135deg, #F7FAF6 0%, #FDFDF8 100%);
	border: 1rpx solid rgba(111, 142, 117, 0.12);
	border-radius: 24rpx;
	margin: 20rpx 30rpx;
	padding: 26rpx 28rpx;
	box-shadow: 0 12rpx 26rpx rgba(94, 116, 97, 0.05);
}

.science-header {
	display: flex;
	align-items: center;
	margin-bottom: 22rpx;
}

.science-mark {
	width: 18rpx;
	height: 18rpx;
	border-radius: 50%;
	margin-right: 14rpx;
	background: linear-gradient(135deg, #8EAA92 0%, #6F8E75 100%);
	box-shadow: 0 0 0 8rpx rgba(111, 142, 117, 0.10);
}

.science-title {
	font-size: 26rpx;
	font-weight: 700;
	color: #4F6854;
}

.science-grid {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx 0;
}

.science-item {
	width: 50%;
	display: flex;
	flex-direction: column;
	padding: 8rpx 12rpx 8rpx 0;
	box-sizing: border-box;

	&:nth-child(2n) {
		padding-left: 20rpx;
		padding-right: 0;
		border-left: 1rpx dashed rgba(111, 142, 117, 0.14);
	}
}

.sci-label {
	font-size: 20rpx;
	color: $text-hint;
	margin-bottom: 6rpx;
}

.sci-val {
	font-size: 24rpx;
	color: $text-primary;
	font-weight: 500;
	line-height: 1.4;

	&.highlight {
		color: $green;
		font-weight: 700;
	}
}

/* 经典推荐搭配样式 */
.combo-recommend-card {
	background: linear-gradient(180deg, #FEFEFC 0%, #FAFCF9 100%);
	border-radius: 24rpx;
	margin: 24rpx 30rpx;
	padding: 28rpx;
	border: 1rpx solid rgba(111, 142, 117, 0.10);
	box-shadow: 0 12rpx 28rpx rgba(88, 109, 93, 0.05);
}

.combo-header {
	margin-bottom: 24rpx;
	display: flex;
	flex-direction: column;
}

.combo-title {
	font-size: 26rpx;
	font-weight: 700;
	color: $text-primary;
}

.combo-subtitle {
	font-size: 22rpx;
	color: $text-hint;
	margin-top: 8rpx;
}

.combo-body {
	display: flex;
	flex-direction: column;
	gap: 24rpx;
}

.combo-goods {
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 20rpx;
}

.combo-goods-item {
	display: flex;
	flex-direction: column;
	align-items: center;
	width: 180rpx;
}

.combo-img {
	width: 110rpx;
	height: 110rpx;
	border-radius: 12rpx;
	background: $green-bg;
}

.combo-name {
	font-size: 22rpx;
	color: $text-secondary;
	margin-top: 10rpx;
	text-align: center;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	width: 100%;
}

.combo-plus {
	font-size: 36rpx;
	color: $text-hint;
	font-weight: 300;
}

.combo-action-area {
	display: flex;
	align-items: center;
	justify-content: space-between;
	background: rgba(232, 236, 232, 0.52);
	padding: 18rpx 20rpx;
	border-radius: 16rpx;
}

.combo-benefit {
	display: flex;
	flex-direction: column;
}

.combo-tip {
	font-size: 22rpx;
	font-weight: 700;
	color: #4F6854;
}

.combo-price-tip {
	font-size: 20rpx;
	color: #879187;
	margin-top: 4rpx;
}

.combo-add-btn {
	background: linear-gradient(135deg, #6F8E75 0%, #5E7B64 100%);
	color: #FEFEFC;
	font-size: 24rpx;
	font-weight: 700;
	height: 64rpx;
	line-height: 64rpx;
	padding: 0 24rpx;
	border-radius: 32rpx;
	border: none;
	margin: 0;

	&:active {
		filter: brightness(0.9);
	}
}
</style>
