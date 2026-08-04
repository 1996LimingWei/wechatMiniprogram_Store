# Issue #3：商品管理（分类树 + SPU 列表 + SKU 编辑）

> 计划日期：2026-08-03
> 状态：✅ 已完成

---

## 目标

实现管理后台商品管理模块的全部前端页面和后端 API 补充，包括：
1. 分类管理 — 树形列表展示、增删改
2. 商品列表 — 分页表格、筛选、上架/下架
3. 商品新增/编辑 — 完整表单（基础信息 + 图片 + 价格 + 详情）
4. SKU 规格管理 — 动态规格维度 + SKU 矩阵编辑
5. 商品状态操作 — 列表内快捷上架/下架

## 变更清单

### 后端（shop-module-product）

| 文件 | 变更 |
|------|------|
| `AdminProductController.java` | 分页接口增加 name/categoryId/status 筛选参数；新增 `/detail` 接口 |
| `ProductSpuService.java` | `getAdminSpuPage` 增加筛选条件 |
| `AdminProductSkuController.java` | **新建** — SKU list + save-batch 两个接口 |

### 前端 API 层（shop-admin/src/api/）

| 文件 | 变更 |
|------|------|
| `types.ts` | ProductSku 新增 weight/volume 字段 |
| `product.ts` | 新增 getProductDetail；getProductPage 支持筛选参数 |
| `sku.ts` | **新建** — getSkuList + saveSkuBatch |

### 前端页面（shop-admin/src/views/product/）

| 文件 | 说明 |
|------|------|
| `category/index.vue` | 分类管理 — el-table 树形结构 + 对话框 CRUD |
| `spu/index.vue` | 商品列表 — 筛选栏 + 分页表格 + 状态切换 |
| `spu-form/index.vue` | **新建** — 商品表单 + SKU 规格编辑器 |

### 路由

| 文件 | 变更 |
|------|------|
| `router/modules/product.ts` | 新增 `/product/spu-form` 和 `/product/spu-form/:id` 隐藏路由 |

## API 接口清单

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/admin-api/product/spu/page` | 商品分页（+name/categoryId/status 筛选） | 增强 |
| GET | `/admin-api/product/spu/detail?id=` | 商品详情 | 新增 |
| POST | `/admin-api/product/spu/create` | 创建商品 | 已有 |
| PUT | `/admin-api/product/spu/update` | 更新商品 | 已有 |
| DELETE | `/admin-api/product/spu/delete?id=` | 删除商品 | 已有 |
| GET | `/admin-api/product/sku/list?spuId=` | 查询 SKU 列表 | **新增** |
| POST | `/admin-api/product/sku/save-batch?spuId=` | 批量保存 SKU | **新增** |
| GET | `/admin-api/product/category/list` | 分类列表 | 已有 |
| POST | `/admin-api/product/category/create` | 创建分类 | 已有 |
| PUT | `/admin-api/product/category/update` | 更新分类 | 已有 |
| DELETE | `/admin-api/product/category/delete?id=` | 删除分类 | 已有 |

## 技术要点

- 价格单位：后端存储为**分**（Integer），前端 UI 显示和输入为**元**（/ 100 和 * 100 转换）
- SKU properties 格式：JSON 数组 `[{"id":1,"valueId":1,"name":"颜色","valueName":"红色"}]`
- sliderPicUrls 格式：JSON 数组字符串 `["url1","url2"]`，前端兼容解析
- SKU 规格编辑器：动态添加维度 → 笛卡尔积生成矩阵 → 逐行编辑价格/库存
- 新增商品后跳转编辑页添加 SKU（因 create 接口不返回新 ID）

## 验收步骤

1. 启动后端 `mvn spring-boot:run -pl shop-server`（端口 8085）
2. 启动前端 `pnpm dev`（端口 8848）
3. 登录管理后台 → 侧边栏「商品管理」

### 分类管理验收
4. 进入「分类管理」，确认树形表格正确显示顶级/子级分类
5. 点击「新增顶级分类」，填写信息后确认创建
6. 点击某分类的「新增子分类」，确认 parentId 自动填充
7. 编辑某分类名称/排序/状态，保存后列表更新
8. 删除无子分类的分类，确认删除成功

### 商品列表验收
9. 进入「商品列表」，确认分页表格显示商品数据
10. 使用名称/分类/状态筛选，确认结果正确
11. 点击「上架/下架」切换，确认状态变更
12. 点击「编辑」跳转到表单页，确认数据回填

### 商品新增/编辑验收
13. 点击「新增商品」，填写基础信息后保存
14. 从列表进入编辑页，修改商品信息后保存
15. 在编辑页添加规格维度，生成 SKU 矩阵
16. 编辑 SKU 价格和库存，保存后重新加载确认数据持久化

### TypeScript 检查
17. `npx vue-tsc --noEmit` → EXIT_CODE=0
