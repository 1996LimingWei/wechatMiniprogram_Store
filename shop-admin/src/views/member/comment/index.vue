<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getCommentPage, updateCommentStatus } from "@/api/member";
import type { ProductComment } from "@/api/types";

defineOptions({ name: "CommentList" });

/* ---------- 查询 ---------- */
const loading = ref(false);
const tableData = ref<ProductComment[]>([]);
const total = ref(0);
const query = reactive({
    pageNo: 1,
    pageSize: 10,
    status: undefined as number | undefined
});

async function fetchData() {
    loading.value = true;
    try {
        const params: any = {
            pageNo: query.pageNo,
            pageSize: query.pageSize
        };
        if (query.status != null) params.status = query.status;

        const res = (await getCommentPage(params)) as any;
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
    query.status = undefined;
    query.pageNo = 1;
    fetchData();
}

function handlePageChange(page: number) {
    query.pageNo = page;
    fetchData();
}

/* ---------- 状态操作 ---------- */
async function handleApprove(row: ProductComment) {
    await ElMessageBox.confirm("确定审核通过该评论吗？", "确认审核", { type: "info" });
    await updateCommentStatus(row.id!, 1);
    row.status = 1;
    ElMessage.success("已审核通过");
}

async function handleHide(row: ProductComment) {
    await ElMessageBox.confirm("确定隐藏该评论吗？隐藏后前端将不再展示。", "确认隐藏", { type: "warning" });
    await updateCommentStatus(row.id!, 0);
    row.status = 0;
    ElMessage.success("已隐藏");
}

/* ---------- 状态标签 ---------- */
function statusLabel(s?: number) {
    if (s === 1) return "显示";
    if (s === 0) return "隐藏";
    return "未知";
}

function statusType(s?: number) {
    if (s === 1) return "success";
    if (s === 0) return "info";
    return "info";
}

onMounted(fetchData);
</script>

<template>
    <div class="app-container">
        <!-- 筛选栏 -->
        <el-card shadow="never" class="mb-4">
            <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
                <el-form-item label="状态">
                    <el-select
                        v-model="query.status"
                        placeholder="全部"
                        clearable
                        style="width: 120px"
                    >
                        <el-option label="显示" :value="1" />
                        <el-option label="隐藏" :value="0" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                    <el-button @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 表格 -->
        <el-card shadow="never">
            <div class="toolbar">
                <span class="total-label">共 {{ total }} 条评论</span>
            </div>

            <el-table
                :data="tableData"
                v-loading="loading"
                border
                style="width: 100%; margin-top: 12px"
            >
                <el-table-column prop="userNickname" label="用户" width="120" show-overflow-tooltip />
                <el-table-column prop="spuName" label="商品" min-width="150" show-overflow-tooltip />
                <el-table-column prop="content" label="评论内容" min-width="200" show-overflow-tooltip />
                <el-table-column label="状态" width="90" align="center">
                    <template #default="{ row }">
                        <el-tag :type="statusType(row.status)" size="small">
                            {{ statusLabel(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="评论时间" width="170" align="center" />
                <el-table-column label="操作" width="160" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button
                            v-if="row.status !== 1"
                            type="success"
                            link
                            size="small"
                            @click="handleApprove(row)"
                        >
                            审核通过
                        </el-button>
                        <el-button
                            v-if="row.status !== 0"
                            type="warning"
                            link
                            size="small"
                            @click="handleHide(row)"
                        >
                            隐藏
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
.app-container { padding: 16px; }
.mb-4 { margin-bottom: 16px; }
.toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
}
.total-label {
    font-size: 13px;
    color: #6b7280;
}
.pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
}
</style>
