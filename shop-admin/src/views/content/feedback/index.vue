<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getFeedbackDetail, getFeedbackPage, handleFeedback } from "@/api/feedback";
import type { UserFeedback } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "ContentFeedback" });
const canManageFeedback = hasAnyPerms(["feedback:manage"]);
const loading = ref(false);
const tableData = ref<UserFeedback[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const detail = ref<UserFeedback | null>(null);
const query = reactive({ pageNo: 1, pageSize: 10, status: undefined as number | undefined, type: undefined as number | undefined });

async function fetchData() {
    loading.value = true;
    try {
        const result = await getFeedbackPage(query);
        tableData.value = result.list ?? [];
        total.value = result.total ?? 0;
    } finally { loading.value = false; }
}
function search() { query.pageNo = 1; fetchData(); }
function reset() { query.status = undefined; query.type = undefined; search(); }
async function viewFeedback(row: UserFeedback) {
    detail.value = await getFeedbackDetail(row.id);
    detailVisible.value = true;
}
async function processFeedback(row: UserFeedback, status: number) {
    const title = status === 1 ? "标记处理中" : "完成反馈";
    const { value } = await ElMessageBox.prompt("填写处理备注", title, {
        inputValue: row.handleRemark || "", inputPlaceholder: "处理结论或跟进说明",
        inputValidator: value => value && value.trim().length <= 500 || "处理备注为 1 至 500 个字符"
    });
    await handleFeedback({ id: row.id, status, handleRemark: value.trim() });
    ElMessage.success(status === 1 ? "已标记为处理中" : "已标记为已完成");
    fetchData();
}
function statusType(status: number) { return status === 2 ? "success" : status === 1 ? "warning" : "info"; }
onMounted(fetchData);
</script>

<template>
    <div class="app-container">
        <el-card shadow="never" class="filter-card">
            <el-form :inline="true" :model="query" @submit.prevent="search">
                <el-form-item label="反馈类型"><el-select v-model="query.type" clearable placeholder="全部" class="select-field"><el-option v-for="(label, value) in { 1: '商品相关', 2: '物流状况', 3: '客户服务', 4: '优惠活动', 5: '功能异常', 6: '产品建议', 7: '其他' }" :key="value" :label="label" :value="Number(value)" /></el-select></el-form-item>
                <el-form-item label="处理状态"><el-select v-model="query.status" clearable placeholder="全部" class="select-field"><el-option label="待处理" :value="0" /><el-option label="处理中" :value="1" /><el-option label="已完成" :value="2" /></el-select></el-form-item>
                <el-form-item><el-button type="primary" @click="search">搜索</el-button><el-button @click="reset">重置</el-button></el-form-item>
            </el-form>
        </el-card>
        <el-card shadow="never">
            <el-table v-loading="loading" :data="tableData" border>
                <el-table-column prop="typeName" label="类型" width="110" /><el-table-column prop="content" label="反馈内容" min-width="260" show-overflow-tooltip /><el-table-column prop="userNickname" label="提交用户" width="130" show-overflow-tooltip /><el-table-column prop="mobile" label="联系电话" width="130" />
                <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.statusName }}</el-tag></template></el-table-column>
                <el-table-column prop="handleRemark" label="处理备注" min-width="180" show-overflow-tooltip /><el-table-column prop="createTime" label="提交时间" width="170" />
                <el-table-column label="操作" width="170" fixed="right" align="center"><template #default="{ row }"><el-button type="primary" link @click="viewFeedback(row)">详情</el-button><el-button v-if="canManageFeedback && row.status === 0" type="warning" link @click="processFeedback(row, 1)">处理中</el-button><el-button v-if="canManageFeedback && row.status !== 2" type="success" link @click="processFeedback(row, 2)">完成</el-button></template></el-table-column>
            </el-table>
            <div class="pagination"><el-pagination v-model:current-page="query.pageNo" :page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" /></div>
        </el-card>
        <el-drawer v-model="detailVisible" title="反馈详情" size="440px">
            <el-descriptions v-if="detail" :column="1" border>
                <el-descriptions-item label="反馈类型">{{ detail.typeName }}</el-descriptions-item>
                <el-descriptions-item label="提交用户">{{ detail.userNickname || '已注销用户' }}</el-descriptions-item>
                <el-descriptions-item label="联系电话">{{ detail.mobile || '未提供' }}</el-descriptions-item>
                <el-descriptions-item label="处理状态"><el-tag :type="statusType(detail.status)">{{ detail.statusName }}</el-tag></el-descriptions-item>
                <el-descriptions-item label="反馈内容">{{ detail.content }}</el-descriptions-item>
                <el-descriptions-item label="处理备注">{{ detail.handleRemark || '暂无' }}</el-descriptions-item>
                <el-descriptions-item label="提交时间">{{ detail.createTime }}</el-descriptions-item>
                <el-descriptions-item label="处理时间">{{ detail.handleTime || '暂无' }}</el-descriptions-item>
            </el-descriptions>
        </el-drawer>
    </div>
</template>

<style scoped>
.app-container { padding: 16px; }.filter-card { margin-bottom: 16px; }.select-field { width: 140px; }.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
