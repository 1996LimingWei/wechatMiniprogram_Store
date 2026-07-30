# 首页内容真实化设计

## 目标

将小程序首页的 Banner、频道、品牌、专题、新品、热销和分类楼层从静态 Mock 数据切换到 MySQL，并保持既有 `/app-api/index/*` 响应结构兼容。

## 数据模型

- `content_banner`：首页轮播图。
- `content_channel`：首页频道入口，保存名称、图标和跳转地址。
- `content_brand`：首页品牌卡片，保存名称、图片和起售价。
- `content_topic`：首页专题卡片，保存标题、副标题、场景图和价格说明。
- 新品、热销和分类楼层复用已上架的 `product_spu` 与启用的 `product_category`，不复制商品数据。

所有内容表以 `status=1` 表示启用，并按 `sort DESC, id ASC` 稳定排序。商品楼层只返回 `status=1` 的商品。

## API 兼容

保持以下路径及字段不变：

- `index/banner` -> `banner`
- `index/channel` -> `channel`
- `index/brand` -> `brandList`
- `index/topic` -> `topicList`
- `index/newGoods` -> `newGoodsList`
- `index/hotGoods` -> `hotGoodsList`
- `index/category` -> `categoryList[].goodsList`

## 迁移与种子

新增一份可追踪增量迁移，为已有数据库补齐内容表和演示数据；同时将相同结构和种子写入 `sql/init.sql`，保证空数据库初始化后首页可展示。

## 不包含

收藏、足迹、评论、支付、库存扣减和运营后台不属于本次实现。
