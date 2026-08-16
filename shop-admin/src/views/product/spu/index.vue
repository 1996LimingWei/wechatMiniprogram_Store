<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { UploadFile, UploadInstance } from "element-plus";
import { Download, Upload, UploadFilled } from "@element-plus/icons-vue";
import {
    getProductPage,
    updateProduct,
    deleteProduct,
    downloadProductImportTemplate,
    previewProductImport,
    confirmProductImport,
    exportProducts,
    batchUpdateProductStatus,
    batchUpdateProductCategory,
    batchUpdateProductSort,
    previewProductPriceBatch,
    batchUpdateProductPrice,
    batchUpdateProductStock
} from "@/api/product";
import { getCategoryList } from "@/api/category";
import type { ProductSpu, Category } from "@/api/types";
import type {
    ProductImportPreview,
    ProductImportRow,
    ProductBatchOperationResult,
    ProductBatchItemResult
} from "@/api/product";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "ProductList" });

const router = useRouter();
const canManageProduct = hasAnyPerms(["product:manage"]);

/* ---------- 分类字典 ---------- */
const categoryList = ref<Category[]>([]);
const categoryMap = computed(() => {
    const m = new Map<number, string>();
    categoryList.value.forEach(c => m.set(c.id!, c.name));
    return m;
});

/* ---------- 查询 ---------- */
const loading = ref(false);
const tableData = ref<ProductSpu[]>([]);
const total = ref(0);
const selectedRows = ref<ProductSpu[]>([]);
const selectedIds = computed(() => selectedRows.value.map(row => row.id!).filter(Boolean));
const query = reactive({
    pageNo: 1,
    pageSize: 10,
    name: "",
    categoryId: undefined as number | undefined,
    status: undefined as number | undefined,
    createTimeRange: [] as string[]
});

async function fetchData() {
    loading.value = true;
    try {
        const params: any = {
            pageNo: query.pageNo,
            pageSize: query.pageSize
        };
        if (query.name.trim()) params.name = query.name.trim();
        if (query.categoryId != null) params.categoryId = query.categoryId;
        if (query.status != null) params.status = query.status;

        const res = (await getProductPage(params)) as any;
        tableData.value = res.list ?? [];
        total.value = res.total ?? 0;
    } finally {
        loading.value = false;
    }
}

function handleSearch() {
    query.pageNo = 1;
    fetchData();
}

function handleReset() {
    query.name = "";
    query.categoryId = undefined;
    query.status = undefined;
    query.createTimeRange = [];
    query.pageNo = 1;
    fetchData();
}

/* ---------- 上架/下架 ---------- */
async function handleStatusChange(row: ProductSpu) {
    const newStatus = row.status === 1 ? 0 : 1;
    const label = newStatus === 1 ? "上架" : "下架";
    await ElMessageBox.confirm(
        `确定${label}「${row.name}」吗？`,
        `确认${label}`,
        { type: "warning" }
    );
    await updateProduct({ id: row.id, status: newStatus } as ProductSpu);
    row.status = newStatus;
    ElMessage.success(`${label}成功`);
}

/* ---------- 删除 ---------- */
async function handleDelete(row: ProductSpu) {
    await ElMessageBox.confirm(
        `确定删除商品「${row.name}」吗？删除后不可恢复。`,
        "确认删除",
        { type: "warning" }
    );
    await deleteProduct(row.id!);
    ElMessage.success("删除成功");
    fetchData();
}

/* ---------- 新增/编辑 导航 ---------- */
function goCreate() {
    router.push("/product/spu-form");
}

function goEdit(row: ProductSpu) {
    router.push(`/product/spu-form/${row.id}`);
}

function handleSelectionChange(rows: ProductSpu[]) {
    selectedRows.value = rows;
}

/* ---------- 导入/导出 ---------- */
const importDialogVisible = ref(false);
const importLoading = ref(false);
const exportLoading = ref(false);
const importUploadRef = ref<UploadInstance>();
const importFile = ref<File | null>(null);
const importPreview = ref<ProductImportPreview | null>(null);

function saveBlob(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}

async function handleDownloadTemplate() {
    const blob = await downloadProductImportTemplate();
    saveBlob(blob, "商品导入模板.csv");
}

function handleOpenImport() {
    importDialogVisible.value = true;
    importPreview.value = null;
    importFile.value = null;
    importUploadRef.value?.clearFiles();
}

function handleImportFileChange(file: UploadFile) {
    importFile.value = file.raw ?? null;
    importPreview.value = null;
}

function handleImportFileRemove() {
    importFile.value = null;
    importPreview.value = null;
}

async function handlePreviewImport() {
    if (!importFile.value) {
        ElMessage.warning("请先选择 CSV 文件");
        return;
    }
    importLoading.value = true;
    try {
        importPreview.value = await previewProductImport(importFile.value);
        if (importPreview.value.errorRows > 0) {
            ElMessage.warning("预校验发现错误，请修正后重新上传");
        } else {
            ElMessage.success("预校验通过，可以确认导入");
        }
    } finally {
        importLoading.value = false;
    }
}

async function handleConfirmImport() {
    if (!importFile.value) {
        ElMessage.warning("请先选择 CSV 文件");
        return;
    }
    if (!importPreview.value) {
        ElMessage.warning("请先执行预校验");
        return;
    }
    if (importPreview.value.errorRows > 0) {
        ElMessage.warning("存在错误行，不能确认导入");
        return;
    }
    await ElMessageBox.confirm(
        `确定导入 ${importPreview.value.validRows} 行商品 SKU 吗？`,
        "确认导入",
        { type: "warning" }
    );
    importLoading.value = true;
    try {
        importPreview.value = await confirmProductImport(importFile.value);
        ElMessage.success(
            `导入完成：新增 ${importPreview.value.createdProductCount} 个商品、${importPreview.value.createdSkuCount} 个 SKU`
        );
        importDialogVisible.value = false;
        fetchData();
    } finally {
        importLoading.value = false;
    }
}

async function handleExport() {
    exportLoading.value = true;
    ElMessage.info("正在导出商品文件，请稍候");
    try {
        const params: any = {};
        if (query.name.trim()) params.name = query.name.trim();
        if (query.categoryId != null) params.categoryId = query.categoryId;
        if (query.status != null) params.status = query.status;
        if (query.createTimeRange.length === 2) {
            params.startTime = query.createTimeRange[0];
            params.endTime = query.createTimeRange[1];
        }
        const blob = await exportProducts(params);
        saveBlob(blob, "商品导出.csv");
        ElMessage.success("商品导出完成");
    } finally {
        exportLoading.value = false;
    }
}

function importRowStatus(row: ProductImportRow) {
    return row.valid ? "success" : "danger";
}

function formatImportErrors(row: ProductImportRow) {
    if (!row.errors?.length) return "—";
    return row.errors.join("；");
}

function formatImportColumns(row: ProductImportRow) {
    if (!row.errorColumns?.length) return "—";
    return row.errorColumns.join("、");
}

/* ---------- 批量运营 ---------- */
const batchLoading = ref(false);
const batchResultVisible = ref(false);
const batchResultTitle = ref("");
const batchResult = ref<ProductBatchOperationResult | null>(null);
const categoryBatchVisible = ref(false);
const sortBatchVisible = ref(false);
const priceBatchVisible = ref(false);
const stockBatchVisible = ref(false);
const pricePreview = ref<ProductBatchOperationResult | null>(null);
const categoryBatchForm = reactive({ categoryId: undefined as number | undefined });
const sortBatchForm = reactive({ sort: 0 });
const priceBatchForm = reactive({
    priceAdjustType: "FIXED_AMOUNT" as "FIXED_AMOUNT" | "PERCENT",
    priceAdjustValue: 0
});
const stockBatchForm = reactive({
    stockDelta: 0,
    reason: ""
});

function ensureSelection() {
    if (selectedIds.value.length === 0) {
        ElMessage.warning("请先选择商品");
        return false;
    }
    return true;
}

function showBatchResult(title: string, result: ProductBatchOperationResult, refresh = true) {
    batchResultTitle.value = title;
    batchResult.value = result;
    batchResultVisible.value = true;
    if (result.failureCount > 0) {
        ElMessage.warning(`${title}完成：成功 ${result.successCount}，失败 ${result.failureCount}`);
    } else {
        ElMessage.success(`${title}完成：成功 ${result.successCount}`);
    }
    if (refresh) fetchData();
}

async function handleBatchStatus(status: number) {
    if (!ensureSelection()) return;
    const label = status === 1 ? "上架" : "下架";
    await ElMessageBox.confirm(
        `确定批量${label} ${selectedIds.value.length} 个商品吗？`,
        `批量${label}`,
        { type: "warning" }
    );
    batchLoading.value = true;
    try {
        const result = await batchUpdateProductStatus({
            ids: selectedIds.value,
            status,
            confirmCount: selectedIds.value.length
        });
        showBatchResult(`批量${label}`, result);
    } finally {
        batchLoading.value = false;
    }
}

function openCategoryBatch() {
    if (!ensureSelection()) return;
    categoryBatchForm.categoryId = undefined;
    categoryBatchVisible.value = true;
}

async function submitCategoryBatch() {
    if (!categoryBatchForm.categoryId) {
        ElMessage.warning("请选择目标分类");
        return;
    }
    batchLoading.value = true;
    try {
        const result = await batchUpdateProductCategory({
            ids: selectedIds.value,
            categoryId: categoryBatchForm.categoryId,
            confirmCount: selectedIds.value.length
        });
        categoryBatchVisible.value = false;
        showBatchResult("批量调整分类", result);
    } finally {
        batchLoading.value = false;
    }
}

function openSortBatch() {
    if (!ensureSelection()) return;
    sortBatchForm.sort = 0;
    sortBatchVisible.value = true;
}

async function submitSortBatch() {
    batchLoading.value = true;
    try {
        const result = await batchUpdateProductSort({
            ids: selectedIds.value,
            sort: sortBatchForm.sort,
            confirmCount: selectedIds.value.length
        });
        sortBatchVisible.value = false;
        showBatchResult("批量调整排序", result);
    } finally {
        batchLoading.value = false;
    }
}

function openPriceBatch() {
    if (!ensureSelection()) return;
    priceBatchForm.priceAdjustType = "FIXED_AMOUNT";
    priceBatchForm.priceAdjustValue = 0;
    pricePreview.value = null;
    priceBatchVisible.value = true;
}

async function handlePricePreview() {
    batchLoading.value = true;
    try {
        pricePreview.value = await previewProductPriceBatch({
            ids: selectedIds.value,
            priceAdjustType: priceBatchForm.priceAdjustType,
            priceAdjustValue: priceBatchForm.priceAdjustValue
        });
        showBatchResult("批量调价预览", pricePreview.value, false);
    } finally {
        batchLoading.value = false;
    }
}

async function submitPriceBatch() {
    if (!pricePreview.value) {
        ElMessage.warning("请先预览调价结果");
        return;
    }
    if (pricePreview.value.failureCount > 0) {
        ElMessage.warning("预览存在失败项，不能确认调价");
        return;
    }
    await ElMessageBox.confirm(
        `确定按预览结果调整 ${selectedIds.value.length} 个商品价格吗？`,
        "确认批量调价",
        { type: "warning" }
    );
    batchLoading.value = true;
    try {
        const result = await batchUpdateProductPrice({
            ids: selectedIds.value,
            priceAdjustType: priceBatchForm.priceAdjustType,
            priceAdjustValue: priceBatchForm.priceAdjustValue,
            confirmCount: selectedIds.value.length
        });
        priceBatchVisible.value = false;
        showBatchResult("批量调价", result);
    } finally {
        batchLoading.value = false;
    }
}

function openStockBatch() {
    if (!ensureSelection()) return;
    stockBatchForm.stockDelta = 0;
    stockBatchForm.reason = "";
    stockBatchVisible.value = true;
}

async function submitStockBatch() {
    const reason = stockBatchForm.reason.trim();
    if (reason.length < 4 || reason.length > 200) {
        ElMessage.warning("请填写 4 至 200 个字符的库存调整原因");
        return;
    }
    await ElMessageBox.confirm(
        `确定批量调整 ${selectedIds.value.length} 个商品的库存吗？`,
        "确认批量调库存",
        { type: "warning" }
    );
    batchLoading.value = true;
    try {
        const result = await batchUpdateProductStock({
            ids: selectedIds.value,
            stockDelta: stockBatchForm.stockDelta,
            reason,
            confirmCount: selectedIds.value.length
        });
        stockBatchVisible.value = false;
        showBatchResult("批量调库存", result);
    } finally {
        batchLoading.value = false;
    }
}

function resultStatusType(row: ProductBatchItemResult) {
    return row.success ? "success" : "danger";
}

/* ---------- 分页 ---------- */
function handlePageChange(page: number) {
    query.pageNo = page;
    fetchData();
}

/* ---------- 价格格式化（分→元） ---------- */
function formatPrice(cents?: number) {
    if (cents == null) return "—";
    return `¥${(cents / 100).toFixed(2)}`;
}

/* ---------- 状态标签 ---------- */
function statusLabel(s?: number) {
    if (s === 1) return "上架";
    if (s === 0) return "下架";
    return "未知";
}

function statusType(s?: number) {
    if (s === 1) return "success";
    if (s === 0) return "info";
    return "info";
}

onMounted(async () => {
    categoryList.value = (await getCategoryList()) as Category[];
    fetchData();
});
</script>

<template>
    <div class="app-container">
        <!-- 筛选栏 -->
        <el-card shadow="never" class="mb-4">
            <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
                <el-form-item label="商品名称">
                    <el-input
                        v-model="query.name"
                        placeholder="输入关键词搜索"
                        clearable
                        style="width: 200px"
                        @keyup.enter="handleSearch"
                    />
                </el-form-item>
                <el-form-item label="分类">
                    <el-select
                        v-model="query.categoryId"
                        placeholder="全部分类"
                        clearable
                        style="width: 160px"
                    >
                        <el-option
                            v-for="cat in categoryList"
                            :key="cat.id"
                            :label="cat.name"
                            :value="cat.id!"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select
                        v-model="query.status"
                        placeholder="全部"
                        clearable
                        style="width: 120px"
                    >
                        <el-option label="上架" :value="1" />
                        <el-option label="下架" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item label="创建时间">
                    <el-date-picker
                        v-model="query.createTimeRange"
                        type="datetimerange"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        range-separator="至"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        style="width: 360px"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 操作栏 + 表格 -->
        <el-card shadow="never">
            <div class="toolbar">
                <div class="toolbar-actions">
                    <el-button v-if="canManageProduct" type="primary" @click="goCreate">
                        新增商品
                    </el-button>
                    <el-button :icon="Download" @click="handleDownloadTemplate">
                        下载模板
                    </el-button>
                    <el-button v-if="canManageProduct" :icon="Upload" @click="handleOpenImport">
                        导入商品
                    </el-button>
                    <el-button :icon="Download" :loading="exportLoading" @click="handleExport">
                        导出商品
                    </el-button>
                    <el-divider v-if="canManageProduct" direction="vertical" />
                    <el-button v-if="canManageProduct" :disabled="selectedIds.length === 0" :loading="batchLoading" @click="handleBatchStatus(1)">
                        批量上架
                    </el-button>
                    <el-button v-if="canManageProduct" :disabled="selectedIds.length === 0" :loading="batchLoading" @click="handleBatchStatus(0)">
                        批量下架
                    </el-button>
                    <el-button v-if="canManageProduct" :disabled="selectedIds.length === 0" @click="openCategoryBatch">
                        批量分类
                    </el-button>
                    <el-button v-if="canManageProduct" :disabled="selectedIds.length === 0" @click="openSortBatch">
                        批量排序
                    </el-button>
                    <el-button v-if="canManageProduct" :disabled="selectedIds.length === 0" @click="openPriceBatch">
                        批量调价
                    </el-button>
                    <el-button v-if="canManageProduct" :disabled="selectedIds.length === 0" @click="openStockBatch">
                        批量调库存
                    </el-button>
                </div>
                <span class="total-label">
                    共 {{ total }} 件商品<span v-if="canManageProduct">，已选 {{ selectedIds.length }} 件</span>
                </span>
            </div>

            <el-table
                :data="tableData"
                v-loading="loading"
                border
                style="width: 100%; margin-top: 12px"
                @selection-change="handleSelectionChange"
            >
                <el-table-column v-if="canManageProduct" type="selection" width="46" align="center" />
                <el-table-column label="主图" width="80" align="center">
                    <template #default="{ row }">
                        <el-image
                            v-if="row.picUrl"
                            :src="row.picUrl"
                            style="width: 48px; height: 48px; border-radius: 4px"
                            fit="cover"
                        />
                        <span v-else class="text-gray-400">—</span>
                    </template>
                </el-table-column>
                <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
                <el-table-column label="分类" width="120" align="center">
                    <template #default="{ row }">
                        {{ categoryMap.get(row.categoryId) || "—" }}
                    </template>
                </el-table-column>
                <el-table-column label="售价" width="100" align="right">
                    <template #default="{ row }">
                        <span class="price-text">{{ formatPrice(row.price) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="市场价" width="100" align="right">
                    <template #default="{ row }">
                        <span class="text-gray-400">{{ formatPrice(row.marketPrice) }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="stock" label="库存" width="80" align="center" />
                <el-table-column prop="salesCount" label="销量" width="80" align="center" />
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusType(row.status)" size="small">
                            {{ statusLabel(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button
                            v-if="canManageProduct"
                            :type="row.status === 1 ? 'warning' : 'success'"
                            link
                            size="small"
                            @click="handleStatusChange(row)"
                        >
                            {{ row.status === 1 ? "下架" : "上架" }}
                        </el-button>
                        <el-button
                            v-if="canManageProduct"
                            type="primary"
                            link
                            size="small"
                            @click="goEdit(row)"
                        >
                            编辑
                        </el-button>
                        <el-button
                            v-if="canManageProduct"
                            type="danger"
                            link
                            size="small"
                            @click="handleDelete(row)"
                        >
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination-wrap">
                <el-pagination
                    v-model:current-page="query.pageNo"
                    :page-size="query.pageSize"
                    :total="total"
                    layout="total, prev, pager, next"
                    @current-change="handlePageChange"
                />
            </div>
        </el-card>

        <el-dialog
            v-model="importDialogVisible"
            title="导入商品"
            width="920px"
            destroy-on-close
        >
            <el-upload
                ref="importUploadRef"
                drag
                accept=".csv"
                :auto-upload="false"
                :limit="1"
                :on-change="handleImportFileChange"
                :on-remove="handleImportFileRemove"
            >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖拽 CSV 文件到此处，或点击选择文件</div>
            </el-upload>

            <div v-if="importPreview" class="import-summary">
                <el-tag type="info">总行数 {{ importPreview.totalRows }}</el-tag>
                <el-tag type="success">有效 {{ importPreview.validRows }}</el-tag>
                <el-tag :type="importPreview.errorRows > 0 ? 'danger' : 'success'">
                    错误 {{ importPreview.errorRows }}
                </el-tag>
                <el-tag v-if="!importPreview.dryRun" type="success">
                    已创建商品 {{ importPreview.createdProductCount }} 个，SKU {{ importPreview.createdSkuCount }} 个
                </el-tag>
            </div>

            <el-table
                v-if="importPreview"
                :data="importPreview.rows"
                border
                max-height="360"
                style="width: 100%; margin-top: 12px"
            >
                <el-table-column prop="rowNo" label="行号" width="70" align="center" />
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="importRowStatus(row)" size="small">
                            {{ row.valid ? "通过" : "错误" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="productName" label="商品名称" min-width="150" show-overflow-tooltip />
                <el-table-column prop="categoryName" label="分类" width="130" show-overflow-tooltip />
                <el-table-column prop="skuCode" label="SKU编码" width="140" show-overflow-tooltip />
                <el-table-column prop="price" label="售价" width="90" align="right" />
                <el-table-column prop="stock" label="库存" width="80" align="center" />
                <el-table-column label="错误列" min-width="150" show-overflow-tooltip>
                    <template #default="{ row }">{{ formatImportColumns(row) }}</template>
                </el-table-column>
                <el-table-column label="错误原因" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">{{ formatImportErrors(row) }}</template>
                </el-table-column>
            </el-table>

            <template #footer>
                <el-button @click="importDialogVisible = false">关闭</el-button>
                <el-button @click="handleDownloadTemplate">下载模板</el-button>
                <el-button type="primary" :loading="importLoading" @click="handlePreviewImport">
                    预校验
                </el-button>
                <el-button
                    type="success"
                    :disabled="!importPreview || importPreview.errorRows > 0"
                    :loading="importLoading"
                    @click="handleConfirmImport"
                >
                    确认导入
                </el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="categoryBatchVisible" title="批量调整分类" width="520px">
            <el-form label-width="100px">
                <el-form-item label="目标分类" required>
                    <el-select v-model="categoryBatchForm.categoryId" placeholder="请选择分类" style="width: 100%">
                        <el-option
                            v-for="cat in categoryList"
                            :key="cat.id"
                            :label="cat.name"
                            :value="cat.id!"
                        />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="categoryBatchVisible = false">取消</el-button>
                <el-button type="primary" :loading="batchLoading" @click="submitCategoryBatch">
                    确认调整
                </el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="sortBatchVisible" title="批量调整排序" width="480px">
            <el-form label-width="100px">
                <el-form-item label="排序值" required>
                    <el-input-number
                        v-model="sortBatchForm.sort"
                        :min="0"
                        :max="9999"
                        controls-position="right"
                        style="width: 100%"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="sortBatchVisible = false">取消</el-button>
                <el-button type="primary" :loading="batchLoading" @click="submitSortBatch">
                    确认调整
                </el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="priceBatchVisible" title="批量调价" width="720px">
            <el-form label-width="120px">
                <el-form-item label="调价方式" required>
                    <el-radio-group v-model="priceBatchForm.priceAdjustType" @change="pricePreview = null">
                        <el-radio-button label="FIXED_AMOUNT">固定金额</el-radio-button>
                        <el-radio-button label="PERCENT">百分比</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <el-form-item :label="priceBatchForm.priceAdjustType === 'FIXED_AMOUNT' ? '调整金额' : '调整比例'" required>
                    <el-input-number
                        v-model="priceBatchForm.priceAdjustValue"
                        :precision="2"
                        :step="1"
                        controls-position="right"
                        style="width: 220px"
                        @change="pricePreview = null"
                    />
                    <span class="field-unit">{{ priceBatchForm.priceAdjustType === "FIXED_AMOUNT" ? "元" : "%" }}</span>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="priceBatchVisible = false">取消</el-button>
                <el-button :loading="batchLoading" @click="handlePricePreview">预览</el-button>
                <el-button
                    type="primary"
                    :disabled="!pricePreview || pricePreview.failureCount > 0"
                    :loading="batchLoading"
                    @click="submitPriceBatch"
                >
                    确认调价
                </el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="stockBatchVisible" title="批量调库存" width="560px">
            <el-form label-width="110px">
                <el-form-item label="调整数量" required>
                    <el-input-number
                        v-model="stockBatchForm.stockDelta"
                        :min="-1000000"
                        :max="1000000"
                        controls-position="right"
                        style="width: 220px"
                    />
                </el-form-item>
                <el-form-item label="调整原因" required>
                    <el-input
                        v-model="stockBatchForm.reason"
                        type="textarea"
                        :rows="4"
                        maxlength="200"
                        show-word-limit
                        placeholder="填写盘点入库、损耗修正等具体原因"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="stockBatchVisible = false">取消</el-button>
                <el-button type="primary" :loading="batchLoading" @click="submitStockBatch">
                    确认调整
                </el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="batchResultVisible" :title="batchResultTitle" width="820px">
            <div v-if="batchResult" class="import-summary">
                <el-tag type="info">总数 {{ batchResult.totalCount }}</el-tag>
                <el-tag type="success">成功 {{ batchResult.successCount }}</el-tag>
                <el-tag :type="batchResult.failureCount > 0 ? 'danger' : 'success'">
                    失败 {{ batchResult.failureCount }}
                </el-tag>
            </div>
            <el-table
                v-if="batchResult"
                :data="batchResult.rows"
                border
                max-height="360"
                style="width: 100%; margin-top: 12px"
            >
                <el-table-column prop="id" label="商品ID" width="90" align="center" />
                <el-table-column prop="name" label="商品名称" min-width="170" show-overflow-tooltip />
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="resultStatusType(row)" size="small">
                            {{ row.success ? "成功" : "失败" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="价格变化" min-width="150" align="center">
                    <template #default="{ row }">
                        <span v-if="row.beforePrice != null || row.afterPrice != null">
                            {{ formatPrice(row.beforePrice) }} → {{ formatPrice(row.afterPrice) }}
                        </span>
                        <span v-else>—</span>
                    </template>
                </el-table-column>
                <el-table-column label="库存变化" min-width="130" align="center">
                    <template #default="{ row }">
                        <span v-if="row.beforeStock != null || row.afterStock != null">
                            {{ row.beforeStock }} → {{ row.afterStock }}
                        </span>
                        <span v-else>—</span>
                    </template>
                </el-table-column>
                <el-table-column prop="message" label="结果说明" min-width="180" show-overflow-tooltip />
            </el-table>
            <template #footer>
                <el-button type="primary" @click="batchResultVisible = false">知道了</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.app-container {
    padding: 16px;
}
.mb-4 {
    margin-bottom: 16px;
}
.toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
}
.toolbar-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}
.total-label {
    font-size: 13px;
    color: #6b7280;
}
.price-text {
    color: #e64340;
    font-weight: 600;
}
.text-gray-400 {
    color: #9ca3af;
}
.pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
}
.import-summary {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 16px;
}
.field-unit {
    margin-left: 8px;
    color: #606266;
}
</style>
