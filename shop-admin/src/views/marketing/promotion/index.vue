<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    getPromotionList,
    createPromotion,
    updatePromotion,
    deletePromotion,
    updatePromotionStatus
} from "@/api/marketing";
import type { PromotionRule } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "MarketingPromotion" });
const canManageMarketing = hasAnyPerms(["marketing:manage"]);

const loading = ref(false);
const list = ref<PromotionRule[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);

async function fetchData() {
    loading.value = true;
    try {
        const res = (await getPromotionList({
            pageNo: pageNo.value,
            pageSize: pageSize.value
        })) as any;
        list.value = res.list || [];
        total.value = res.total || 0;
    } finally {
        loading.value = false;
    }
}

/* ---------- 对话框 ---------- */
const dialogVisible = ref(false);
const dialogTitle = ref("新增满减规则");
const submitting = ref(false);
const form = reactive({
    id: undefined as number | undefined,
    name: "",
    type: 1,
    thresholdYuan: "",
    discountYuan: "",
    priority: 0,
    startTime: "",
    endTime: "",
    status: 1
});

function resetForm() {
    form.id = undefined;
    form.name = "";
    form.type = 1;
    form.thresholdYuan = "";
    form.discountYuan = "";
    form.priority = 0;
    form.startTime = "";
    form.endTime = "";
    form.status = 1;
}

function yuanToCent(yuan: string): number {
    const num = parseFloat(yuan);
    return isNaN(num) ? 0 : Math.round(num * 100);
}

function openAdd() {
    resetForm();
    dialogTitle.value = "新增满减规则";
    dialogVisible.value = true;
}

function openEdit(row: PromotionRule) {
    resetForm();
    form.id = row.id;
    form.name = row.name;
    form.type = row.type;
    form.thresholdYuan = row.thresholdAmount || "";
    form.discountYuan = row.discountAmount || "";
    form.priority = row.priority;
    form.startTime = row.startTime || "";
    form.endTime = row.endTime || "";
    form.status = row.status;
    dialogTitle.value = "编辑满减规则";
    dialogVisible.value = true;
}

async function handleSubmit() {
    if (!form.name.trim()) {
        ElMessage.warning("请输入活动名称");
        return;
    }
    const thresholdAmount = yuanToCent(form.thresholdYuan);
    const discountAmount = yuanToCent(form.discountYuan);
    if (thresholdAmount <= 0) {
        ElMessage.warning("满减门槛必须大于 0");
        return;
    }
    if (discountAmount <= 0) {
        ElMessage.warning("优惠金额必须大于 0");
        return;
    }
    if (discountAmount > thresholdAmount) {
        ElMessage.warning("优惠金额不能大于满减门槛");
        return;
    }
    submitting.value = true;
    try {
        const payload: Record<string, any> = {
            name: form.name.trim(),
            type: form.type,
            thresholdAmount,
            discountAmount,
            priority: form.priority,
            startTime: form.startTime || "",
            endTime: form.endTime || "",
            status: form.status
        };
        if (form.id) {
            payload.id = form.id;
            await updatePromotion(payload);
            ElMessage.success("更新成功");
        } else {
            await createPromotion(payload);
            ElMessage.success("创建成功");
        }
        dialogVisible.value = false;
        fetchData();
    } finally {
        submitting.value = false;
    }
}

async function handleDelete(row: PromotionRule) {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, "确认删除", { type: "warning" });
    await deletePromotion({ id: row.id! });
    ElMessage.success("删除成功");
    fetchData();
}

async function handleStatusChange(row: PromotionRule) {
    await updatePromotionStatus({ id: row.id!, status: row.status });
    ElMessage.success(row.status === 1 ? "已启用" : "已禁用");
}

function handlePageChange(p: number) {
    pageNo.value = p;
    fetchData();
}

onMounted(fetchData);
</script>

<template>
    <div class="app-container">
        <el-card shadow="never" class="mb-4">
            <el-button v-if="canManageMarketing" type="primary" @click="openAdd">
                <el-icon class="mr-1"><i class="ep-icon-plus" /></el-icon>
                新增满减规则
            </el-button>
        </el-card>

        <el-card shadow="never">
            <el-table :data="list" v-loading="loading" border>
                <el-table-column prop="id" label="ID" width="60" align="center" />
                <el-table-column prop="name" label="活动名称" min-width="140" show-overflow-tooltip />
                <el-table-column label="满减门槛" width="120" align="right">
                    <template #default="{ row }">¥{{ row.thresholdAmount }}</template>
                </el-table-column>
                <el-table-column label="优惠金额" width="120" align="right">
                    <template #default="{ row }">
                        <span class="text-red">-¥{{ row.discountAmount }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="priority" label="优先级" width="80" align="center" />
                <el-table-column label="有效期" min-width="180">
                    <template #default="{ row }">
                        <span v-if="row.startTime || row.endTime">
                            {{ row.startTime || "不限" }} ~ {{ row.endTime || "不限" }}
                        </span>
                        <span v-else class="text-gray-400">长期有效</span>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-switch
                            v-model="row.status"
                            :disabled="!canManageMarketing"
                            :active-value="1"
                            :inactive-value="0"
                            @change="handleStatusChange(row)"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="canManageMarketing" type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                        <el-button v-if="canManageMarketing" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-if="total > pageSize"
                class="mt-4"
                background
                layout="total, prev, pager, next"
                :total="total"
                :page-size="pageSize"
                :current-page="pageNo"
                @current-change="handlePageChange"
            />
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
            <el-form :model="form" label-width="110px" @submit.prevent="handleSubmit">
                <el-form-item label="活动名称" required>
                    <el-input v-model="form.name" placeholder="如：满200减30" maxlength="30" />
                </el-form-item>
                <el-form-item label="满减门槛（元）" required>
                    <el-input v-model="form.thresholdYuan" placeholder="如：200" type="number" />
                </el-form-item>
                <el-form-item label="优惠金额（元）" required>
                    <el-input v-model="form.discountYuan" placeholder="如：30" type="number" />
                </el-form-item>
                <el-form-item label="优先级">
                    <el-input-number v-model="form.priority" :min="0" :max="999" controls-position="right" />
                    <span class="form-tip">数值越大越优先匹配</span>
                </el-form-item>
                <el-form-item label="开始时间">
                    <el-date-picker v-model="form.startTime" type="datetime" placeholder="留空=不限" value-format="YYYY-MM-DD HH:mm:ss" />
                </el-form-item>
                <el-form-item label="结束时间">
                    <el-date-picker v-model="form.endTime" type="datetime" placeholder="留空=不限" value-format="YYYY-MM-DD HH:mm:ss" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">启用</el-radio>
                        <el-radio :value="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.app-container { padding: 16px; }
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
.mr-1 { margin-right: 4px; }
.text-red { color: #e43d3c; }
.text-gray-400 { color: #9ca3af; }
.form-tip { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
