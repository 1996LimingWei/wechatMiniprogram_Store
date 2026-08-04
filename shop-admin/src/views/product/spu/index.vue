<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    getProductPage,
    updateProduct,
    deleteProduct
} from "@/api/product";
import { getCategoryList } from "@/api/category";
import type { ProductSpu, Category } from "@/api/types";

defineOptions({ name: "ProductList" });

const router = useRouter();

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
const query = reactive({
    pageNo: 1,
    pageSize: 10,
    name: "",
    categoryId: undefined as number | undefined,
    status: undefined as number | undefined
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
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 操作栏 + 表格 -->
        <el-card shadow="never">
            <div class="toolbar">
                <el-button type="primary" @click="goCreate">
                    新增商品
                </el-button>
                <span class="total-label">共 {{ total }} 件商品</span>
            </div>

            <el-table
                :data="tableData"
                v-loading="loading"
                border
                style="width: 100%; margin-top: 12px"
            >
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
                            :type="row.status === 1 ? 'warning' : 'success'"
                            link
                            size="small"
                            @click="handleStatusChange(row)"
                        >
                            {{ row.status === 1 ? "下架" : "上架" }}
                        </el-button>
                        <el-button
                            type="primary"
                            link
                            size="small"
                            @click="goEdit(row)"
                        >
                            编辑
                        </el-button>
                        <el-button
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
</style>
