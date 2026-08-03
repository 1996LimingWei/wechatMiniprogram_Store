# 商品搜索与搜索历史真实化实施计划

> 对应 Issue：[Issue #22](https://github.com/QtImM/wechatMiniprogram_Store/issues/22)
> 对应总体规格：[Mock 契约优先与可替换数据源设计](../specs/2026-08-01-mock-contract-first-design.md)

## 目标

保持小程序现有搜索 API 和响应字段不变，将搜索首页、联想词和搜索历史从硬编码 Mock 切换为 MySQL 数据，并保证用户历史隔离。

## 任务 1：数据库迁移与模型

- [x] 新增独立迁移 `V20260801_01__product_search_history.sql`。
- [x] 新增搜索历史表、用户关键词唯一键和用户更新时间索引。
- [x] 同步 `sql/init.sql`，建立 DO 与 Mapper。

## 任务 2：真实搜索服务

- [x] 新增搜索首页、联想词、记录历史和清空历史服务。
- [x] 热门词和默认词从已上架商品生成，不硬编码业务词。
- [x] 联想词只读取已上架、未删除商品及有效分类。
- [x] 登录会员历史隔离、同词幂等更新；匿名请求返回空历史且不写入。

## 任务 3：路由与商品列表接入

- [x] 新增正式搜索 Controller，保持 `/app-api/search/**` 路径和响应结构。
- [x] 从 `AppMockController` 删除三个正式搜索路由。
- [x] 商品关键词列表第一页成功查询时记录当前会员历史。

## 任务 4：自动化与 Docker 验收

- [x] 覆盖用户隔离、重复关键词、匿名访问、清空历史和下架商品过滤测试。
- [x] 隔离数据库首次迁移与重复迁移通过。
- [x] Docker 下完成搜索首页、联想词、记录和清空历史 HTTP 验收。
- [x] 商品模块测试与全量构建通过。

## 任务 5：交付

- [x] 更新 `status.md` 与本计划实施记录。
- [x] 中文提交并推送 `feat/product-search-history`。
- [x] 回写并关闭 Issue #22（PR #26 已合并并自动关闭 Issue）。

## 实施记录

- 新增 `product_search_history` 表、稳定唯一键及更新时间索引；4 个迁移版本首次执行与重复幂等执行均通过。
- 新增 `ProductSearchService` 和正式搜索 Controller；热门词、默认词与联想词只读取有效数据库商品。
- 商品列表第一页按当前会员幂等记录规范化关键词，匿名用户不产生搜索历史。
- 新增 5 项搜索服务测试，商品模块共 14 项测试通过；Docker 11 模块全量构建通过。
- `scripts/verify-product-search.ps1` 已通过双用户隔离、重复关键词、清空历史、匿名访问、下架过滤与自动清理验收。
- 已推送提交，[PR #26](https://github.com/QtImM/wechatMiniprogram_Store/pull/26) 已合并到 `main`，Issue #22 已自动关闭。
