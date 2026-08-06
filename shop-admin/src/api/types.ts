/** 分页参数 */
export interface PageParam {
    pageNo?: number;
    pageSize?: number;
}

/** 分页结果 */
export interface PageResult<T> {
    list: T[];
    total: number;
}

/** 商品 SPU */
export interface ProductSpu {
    id?: number;
    categoryId: number;
    name: string;
    keyword?: string;
    introduction?: string;
    description?: string;
    picUrl?: string;
    sliderPicUrls?: string;
    videoUrl?: string;
    type?: number;
    price?: number;
    marketPrice?: number;
    stock?: number;
    salesCount?: number;
    sort?: number;
    status?: number;
    createTime?: string;
    updateTime?: string;
}

/** 商品分类 */
export interface Category {
    id?: number;
    parentId: number;
    name: string;
    icon?: string;
    sort?: number;
    status?: number;
    createTime?: string;
    updateTime?: string;
}

/** 商品 SKU */
export interface ProductSku {
    id?: number;
    spuId: number;
    properties?: string;
    price?: number;
    marketPrice?: number;
    stock?: number;
    picUrl?: string;
    weight?: number;
    volume?: number;
    createTime?: string;
}

/** 交易订单 */
export interface TradeOrder {
    id?: number;
    orderSn: string;
    userId: number;
    status: number;
    payStatus: number;
    goodsPrice: number;
    freightPrice: number;
    couponPrice: number;
    orderPrice: number;
    actualPrice: number;
    consignee?: string;
    mobile?: string;
    fullRegion?: string;
    address?: string;
    payTime?: string;
    expireTime?: string;
    closeTime?: string;
    closeReason?: string;
    createTime?: string;
    items?: TradeOrderItem[];
    logistics?: TradeLogistics;
    orderLogs?: OrderLog[];
}

/** 订单明细 */
export interface TradeOrderItem {
    id?: number;
    orderId: number;
    spuId: number;
    skuId: number;
    goodsName: string;
    goodsPicUrl?: string;
    specName?: string;
    price: number;
    count: number;
    totalPrice: number;
}

/** 物流信息 */
export interface TradeLogistics {
    id?: number;
    orderId: number;
    logisticsCompany?: string;
    logisticsNo?: string;
    deliveryTime?: string;
}

/** 订单操作日志 */
export interface OrderLog {
    id?: number;
    orderId: number;
    operatorType: string;
    action: string;
    fromStatus?: number;
    toStatus?: number;
    remark?: string;
    createTime?: string;
}

/** 售后单 */
export interface AfterSale {
    id?: number;
    orderId: number;
    userId: number;
    afterSaleSn: string;
    type: number;
    status: number;
    refundAmount: number;
    reason?: string;
    applyRemark?: string;
    beforeOrderStatus?: number;
    rejectReason?: string;
    applyTime?: string;
    auditTime?: string;
    rejectTime?: string;
    cancelTime?: string;
    createTime?: string;
}

/** 会员用户 */
export interface MemberUser {
    id?: number;
    openid?: string;
    nickname?: string;
    avatar?: string;
    mobile?: string;
    gender?: number;
    status?: number;
    createTime?: string;
    updateTime?: string;
}

/** 收货地址 */
export interface MemberAddress {
    id?: number;
    userName?: string;
    telNumber?: string;
    fullRegion?: string;
    detailInfo?: string;
    isDefault?: number;
}

/** 最近订单 */
export interface RecentOrder {
    id?: number;
    orderSn?: string;
    status?: number;
    payStatus?: number;
    actualPrice?: number;
    createTime?: string;
}

/** 会员详情（含关联数据） */
export interface MemberUserDetail {
    id?: number;
    nickname?: string;
    avatar?: string;
    mobile?: string;
    status?: number;
    createTime?: string;
    addresses?: MemberAddress[];
    orderCount?: number;
    recentOrders?: RecentOrder[];
    collectCount?: number;
    commentCount?: number;
}

/** 轮播图 */
export interface ContentBanner {
    id?: number;
    title: string;
    picUrl: string;
    url?: string;
    sort?: number;
    status?: number;
    createTime?: string;
}

/** 频道 */
export interface ContentChannel {
    id?: number;
    name: string;
    iconUrl?: string;
    url?: string;
    sort?: number;
    status?: number;
    createTime?: string;
}

/** 品牌 */
export interface ContentBrand {
    id?: number;
    name: string;
    picUrl?: string;
    floorPrice?: number;
    sort?: number;
    status?: number;
    createTime?: string;
}

/** 专题 */
export interface ContentTopic {
    id?: number;
    title: string;
    subtitle?: string;
    picUrl?: string;
    priceInfo?: string;
    sort?: number;
    status?: number;
    createTime?: string;
}

/** 商品评论 */
export interface ProductComment {
    id?: number;
    userId: number;
    userNickname?: string;
    spuId: number;
    spuName?: string;
    content: string;
    status?: number;
    createTime?: string;
}

/** 数据看板 - 汇总指标 */
export interface DashboardSummary {
    todayOrderCount: number;
    todaySalesAmount: number;
    productCount: number;
    memberCount: number;
}

/** 数据看板 - 订单趋势 */
export interface OrderTrend {
    date: string;
    orderCount: number;
    salesAmount: number;
}

/** 数据看板 - 订单状态分布 */
export interface OrderStatusItem {
    name: string;
    value: number;
}

/** 数据看板 - 热销商品 */
export interface TopProduct {
    name: string;
    sales_count: number;
    sales_amount: number;
    pic_url: string;
}

/** 数据看板 - 最近订单 */
export interface DashboardRecentOrder {
    id: number;
    order_sn: string;
    status: number;
    pay_status: number;
    actual_price: number;
    consignee: string;
    create_time: string;
    item_count: number;
}
