<template>
	<view class="legal-page">
		<view class="title">隐私政策</view>
		<view class="updated">更新日期：2026年7月31日</view>
		<view class="section">
			<view class="heading">一、我们处理的信息</view>
			<view class="text">当您选择登录时，我们通过微信登录凭证在服务端获取用于识别账号的 OpenID，不会把微信会话密钥返回到小程序。您使用收货、下单、支付、收藏、足迹、评价或反馈功能时，我们会处理您主动填写的联系人、手机号、地址及相应业务记录。</view>
		</view>
		<view class="section">
			<view class="heading">二、使用目的</view>
			<view class="text">这些信息仅用于账号登录、订单履约、支付退款、售后服务、课程权益、客服反馈、安全风控以及法律法规要求的交易留存。未登录用户仍可浏览公开商品内容。</view>
		</view>
		<view class="section">
			<view class="heading">三、存储与保护</view>
			<view class="text">我们采用访问控制、传输加密、日志脱敏和最小权限等措施保护信息。支付由微信支付处理，本小程序不会收集银行卡号或支付密码。交易记录会在法律法规要求的期限内保存。</view>
		</view>
		<view class="section">
			<view class="heading">四、共享与第三方</view>
			<view class="text">为完成支付、物流配送、对象存储和客户服务，我们仅向必要的服务提供方传递完成服务所需的信息。除法律要求或获得您的另行同意外，我们不会出售您的个人信息。</view>
		</view>
		<view class="section">
			<view class="heading">五、您的权利</view>
			<view class="text">您可以在小程序中管理地址、收藏和足迹，退出登录，或通过“我的—在线客服”申请查询、更正、删除个人信息和注销账号。依法需要保留的交易记录将在留存期内限制处理。</view>
		</view>
		<view class="section">
			<view class="heading">六、联系我们</view>
			<view class="text">如对隐私政策有疑问，或需要行使个人信息权利，请通过小程序“我的—在线客服”联系我们。运营主体以微信小程序资料页公示的信息为准。</view>
		</view>
	</view>
</template>

<style>
	page { background: #f7f8f5; }
	.legal-page { padding: 36rpx 32rpx 80rpx; color: #334033; }
	.title { font-size: 42rpx; font-weight: 700; text-align: center; }
	.updated { margin-top: 16rpx; color: #8a948a; font-size: 24rpx; text-align: center; }
	.section { margin-top: 38rpx; }
	.heading { font-size: 30rpx; font-weight: 600; }
	.text { margin-top: 14rpx; font-size: 27rpx; color: #5f675f; line-height: 1.9; text-align: justify; }
</style>
