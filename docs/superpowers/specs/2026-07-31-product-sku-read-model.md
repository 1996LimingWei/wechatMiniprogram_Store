# 商品多规格与库存可售性读模型

> 对应 Issue：[ #15 商品多规格与库存可售性读模型 ](https://github.com/QtImM/wechatMiniprogram_Store/issues/15)
> 日期：2026-07-31

## 目标

将 `product_sku.properties` 中的全部规格维度转换为商品详情的稳定读模型，并让小程序基于精确 SKU 展示价格、图片和可售库存。

## 接口契约

- `specificationList` 按规格 ID、规格值 ID 升序去重，保留全部有效维度。
- `productList` 保留兼容字段 `goodsSpecificationIds`、`goodsNumber`，并新增/保证 `specificationValueIds`、`properties`、`stock`、`available`、`retailPrice`、`counterPrice`、`picUrl`。
- 空、非法或包含非整数规格 ID 的 `properties` 不得造成详情接口失败，也不得污染有效规格维度。

## 前端规则

- 规格组合必须精确匹配同一 SKU；未选全不得加购或立即购买。
- 缺货组合不可选，数量不得超过选中 SKU 的库存。
- 选中 SKU 后更新价格、SKU 图片和库存；没有 SKU 专属图片时保留 SPU 原轮播图。
- 继续支持旧接口或 Mock 中仅含 `goodsSpecificationIds`、`goodsNumber` 的 SKU 数据。

## 协作边界

仅修改商品读模型、商品详情页、对应测试和文档；不修改 `shop-module-trade/**`、支付/订单/购物车接口或 `sql/migrations/**`，可与 Issue #14 并行。
