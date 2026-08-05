# Issue #7：会员中心（会员列表 + 评论管理）

> 日期：2026-08-03
> 状态：已完成

## 概述

为管理后台新增会员中心模块，包含会员列表（分页搜索 + 详情抽屉）和评论管理（分页 + 状态审核）两个子页面。后端补充 4 个接口，前端实现 2 个完整页面。

## 后端实现

### 1. ProductCommentDO + ProductCommentMapper（shop-module-product）
- `ProductCommentDO`：映射 `product_comment` 表，字段 id, userId, spuId, content, status, createTime, updateTime, deleted
- `ProductCommentMapper`：继承 `BaseMapperX<ProductCommentDO>`，支持标准 MyBatis-Plus 分页
- 将 `product_comment` 从 JdbcTemplate 直接访问升级为 MyBatis-Plus 标准实体

### 2. AdminMemberService（shop-module-member）
- `getUserPage(PageParam, nickname, mobile)`：使用 `LambdaQueryWrapper` 按昵称/手机号模糊查询，返回 `PageResult<MemberUserDO>`
- `getUserDetail(id)`：返回会员基础信息 + 收货地址列表 + 订单统计 + 最近 5 条订单 + 收藏数 + 评论数
- 跨模块查询使用 `JdbcTemplate`（member 模块不依赖 trade/product 模块）

### 3. AdminMemberController（shop-module-member）
- `GET /admin-api/member/user/page` — 会员分页列表（参数：pageNo, pageSize, nickname, mobile）
- `GET /admin-api/member/user/detail` — 会员详情（参数：id）

### 4. AdminCommentController（shop-module-product）
- `GET /admin-api/product/comment/page` — 评论分页列表（参数：pageNo, pageSize, status）
  - 批量查询用户昵称和商品名称，避免 N+1 问题
  - 返回 `PageResult<Map>` 包含 userNickname 和 spuName
- `PUT /admin-api/product/comment/status` — 评论状态变更（参数：id, status）

### 关键设计决策
- Mapper 统一继承 `BaseMapperX`，使用项目标准 `PageParam`（pageNo/pageSize）+ `PageResult`（list/total）
- `MemberUserMapper` 从 `BaseMapper` 升级为 `BaseMapperX`

## 前端实现

### 5. API 层 + 类型定义更新
- `types.ts`：新增 `MemberAddress`、`RecentOrder`、`MemberUserDetail` 接口
- `types.ts`：`ProductComment` 补充 `userNickname`、`spuName` 字段
- `member.ts`：参数从 `page/size` 统一为 `pageNo/pageSize`

### 6. 会员列表页面（views/member/user/index.vue）
- 搜索栏：昵称 + 手机号输入框 + 搜索/重置按钮
- 分页表格：头像（el-avatar）、昵称、手机号、状态标签、注册时间
- 点击表格行打开会员详情抽屉

### 7. 会员详情抽屉（el-drawer）
- 基础信息卡片：头像 + 昵称 + 手机号 + 状态标签 + 注册时间
- 数据概览：订单数 / 收藏数 / 评论数（三列统计卡片）
- 收货地址列表：姓名 + 电话 + 地区 + 详细地址 + 默认标签
- 最近订单：订单号 + 状态标签 + 金额 + 时间

### 8. 评论管理页面（views/member/comment/index.vue）
- 筛选栏：状态下拉（全部/显示/隐藏）+ 搜索/重置按钮
- 分页表格：用户昵称、商品名称、评论内容、状态标签、评论时间
- 操作列：审核通过（status≠1 时显示）/ 隐藏（status≠0 时显示）

## 构建验证
- `mvn install -DskipTests -q` — 后端 0 错误
- `npx vue-tsc --noEmit` — 前端 0 错误

## 涉及文件

### 新建
- `shop-module-product/.../dal/dataobject/ProductCommentDO.java`
- `shop-module-product/.../dal/mysql/ProductCommentMapper.java`
- `shop-module-member/.../service/AdminMemberService.java`
- `shop-module-member/.../controller/AdminMemberController.java`
- `shop-module-product/.../controller/admin/AdminCommentController.java`
- `docs/superpowers/plans/2026-08-03-member-center.md`

### 修改
- `shop-module-member/.../dal/mysql/MemberUserMapper.java` — BaseMapper → BaseMapperX
- `shop-admin/src/api/member.ts` — 参数名对齐 + 返回类型升级
- `shop-admin/src/api/types.ts` — 新增 MemberAddress/RecentOrder/MemberUserDetail
- `shop-admin/src/views/member/user/index.vue` — 占位符 → 完整页面
- `shop-admin/src/views/member/comment/index.vue` — 占位符 → 完整页面
- `docs/superpowers/status.md` — 新增 Issue #7 完成记录

## 验收方式

1. 启动后端：`cd shop-backend && mvn spring-boot:run`（确保 MySQL 3307 + Redis 6380 已启动）
2. 启动前端：`cd shop-admin && npm run dev`
3. **会员列表**：访问 `/member/user`，验证分页翻页、昵称搜索、手机号搜索
4. **会员详情**：点击任意会员行，验证抽屉展示基础信息、地址列表、订单统计、最近订单
5. **评论列表**：访问 `/member/comment`，验证分页翻页、状态筛选
6. **评论操作**：点击"审核通过"或"隐藏"，验证状态变更和 UI 更新
