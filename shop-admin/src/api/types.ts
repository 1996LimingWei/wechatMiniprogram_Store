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
  id: number;
  orderSn: string;
  userId: number;
  status: number;
  payStatus: number;
  orderStatusText: string;
  goodsPrice: string;
  freightPrice: string;
  couponPrice: string;
  orderPrice: string;
  actualPrice: string;
  consignee?: string;
  mobile?: string;
  fullRegion?: string;
  address?: string;
  payTime?: string;
  expireTime?: string;
  closeTime?: string;
  closeReason?: string;
  addTime?: string;
  goodsList?: TradeOrderItem[];
  handleOption?: TradeHandleOption;
  logistics?: TradeLogistics;
  afterSale?: AfterSale;
}

/** 订单明细 */
export interface TradeOrderItem {
  id: number;
  goodsId: number;
  productId: number;
  goodsName: string;
  listPicUrl?: string;
  goodsSpecifitionNameValue?: string;
  retailPrice: string;
  number: number;
}

/** 订单可操作项 */
export interface TradeHandleOption {
  pay?: boolean;
  cancel?: boolean;
  ship?: boolean;
  logistics?: boolean;
  confirm?: boolean;
  refund?: boolean;
  refundApprove?: boolean;
  refundCancel?: boolean;
}

/** 物流信息 */
export interface TradeLogistics {
  hasLogistics: boolean;
  id?: number;
  orderId?: number;
  orderStatus?: number;
  logisticsCompany?: string;
  logisticsNo?: string;
  deliveryTime?: string;
  traces?: LogisticsTrace[];
}

export interface LogisticsTrace {
  time: string;
  text: string;
}

/** 订单操作日志 */
export interface OrderLog {
  id: number;
  orderId: number;
  operatorType: string;
  action: string;
  actionText?: string;
  fromStatus?: number;
  toStatus?: number;
  fromStatusText?: string;
  toStatusText?: string;
  fromPayStatus?: number;
  toPayStatus?: number;
  fromPayStatusText?: string;
  toPayStatusText?: string;
  remark?: string;
  createTime?: string;
}

export interface PayOrderInfo {
  hasPayOrder: boolean;
  id?: number;
  paySn?: string;
  amount?: string;
  channel?: string;
  status?: number;
  statusText?: string;
  payTime?: string;
}

export interface TradeOrderDetail {
  orderInfo: TradeOrder;
  orderGoods: TradeOrderItem[];
  handleOption: TradeHandleOption;
  logistics: TradeLogistics;
  afterSale: AfterSale;
  payOrder: PayOrderInfo;
  orderLogs: OrderLog[];
}

/** 售后单 */
export interface AfterSale {
  hasAfterSale?: boolean;
  id: number;
  orderId: number;
  userId: number;
  orderSn?: string;
  afterSaleSn: string;
  type: number;
  typeText?: string;
  status: number;
  statusText?: string;
  refundAmount: string;
  reason?: string;
  applyRemark?: string;
  beforeOrderStatus?: number;
  rejectReason?: string;
  refundProvider?: string;
  providerRefundNo?: string;
  refundMessage?: string;
  applyTime?: string;
  auditTime?: string;
  refundTime?: string;
  rejectTime?: string;
  cancelTime?: string;
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
