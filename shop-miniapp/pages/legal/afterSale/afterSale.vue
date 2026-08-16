<template>
	<view class="page">
		<view class="title">售后政策</view>
		<view class="section">
			<view class="heading">一、售后范围</view>
			<view class="text">用户收到商品后，如遇商品破损、错发、漏发、质量问题或其他符合法律法规及页面承诺的情形，可在订单详情中提交售后申请。</view>
		</view>
		<view class="section">
			<view class="heading">二、申请方式</view>
			<view class="text">请进入“我的订单”，选择对应订单后提交退款或退货退款申请，并填写售后原因。必要时请保留商品、包装、物流面单及问题照片，便于客服核验。</view>
		</view>
		<view class="section">
			<view class="heading">三、退款处理</view>
			<view class="text">售后申请通过后，系统将按订单原支付路径发起退款。退款到账时间以微信支付及发卡机构处理结果为准，可在订单详情查看退款状态。</view>
		</view>
		<view class="section">
			<view class="heading">四、退货寄回</view>
			<view class="text">需要退货的订单，请在审核通过后的指定期限内寄回商品，并确保商品、赠品、票据和包装符合退货要求。超期未寄回或影响二次销售的，可能无法继续处理。</view>
		</view>
		<view class="section">
			<view class="heading">五、客服协助</view>
			<view class="text">如对售后规则或处理结果有疑问，可通过“我的 > 在线客服”联系运营人员。最终售后口径以商品页面说明、订单状态和法律法规要求为准。</view>
		</view>
	</view>
</template>

<style lang="scss">
page {
	background: #FDFDF8;
}

.page {
	min-height: 100vh;
	padding: 34rpx 30rpx 56rpx;
	box-sizing: border-box;
	color: #314036;
}

.title {
	font-size: 42rpx;
	font-weight: 700;
	margin-bottom: 32rpx;
}

.section {
	margin-bottom: 28rpx;
	padding: 28rpx;
	background: #FEFEFC;
	border-radius: 20rpx;
	box-shadow: 0 8rpx 22rpx rgba(77, 112, 77, 0.05);
}

.heading {
	font-size: 30rpx;
	font-weight: 700;
	margin-bottom: 14rpx;
}

.text {
	font-size: 27rpx;
	line-height: 1.75;
	color: #5E6D61;
}
</style>
