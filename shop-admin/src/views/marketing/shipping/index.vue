<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
    getShippingList,
    createShipping,
    updateShipping,
    updateShippingStatus
} from "@/api/marketing";
import type { ShippingRule } from "@/api/types";

defineOptions({ name: "MarketingShipping" });

const loading = ref(false);
const list = ref<ShippingRule[]>([]);
const total = ref(0);

async function fetchData() {
    loading.value = true;
    try {
        const res = (await getShippingList({
            pageNo: 1,
            pageSize: 50
        })) as any;
        list.value = res.list || [];
        total.value = res.total || 0;
    } finally {
        loading.value = false;
    }
}

/* ---------- 对话框 ---------- */
const dialogVisible = ref(false);
const dialogTitle = ref("新增包邮规则");
const submitting = ref(false);
const form = reactive({
    id: undefined as number | undefined,
    name: "",
    freeThresholdYuan: "",
    baseFeeYuan: "",
    status: 1
});

function resetForm() {
    form.id = undefined;
    form.name = "";
    form.freeThresholdYuan = "";
    form.baseFeeYuan = "";
    form.status = 1;
}

function yuanToCent(yuan: string): number {
    const num = parseFloat(yuan);
    return isNaN(num) ? 0 : Math.round(num * 100);
}

function openAdd() {
    resetForm();
    dialogTitle.value = "新增包邮规则";
    dialogVisible.value = true;
}

function openEdit(row: ShippingRule) {
    resetForm();
    form.id = row.id;
    form.name = row.name;
    form.freeThresholdYuan = row.freeThreshold || "";
    form.baseFeeYuan = row.baseFee || "";
    form.status = row.status;
    dialogTitle.value = "编辑包邮规则";
    dialogVisible.value = true;
}

async function handleSubmit() {
    if (!form.name.trim()) {
        ElMessage.warning("请输入规则名称");
        return;
    }
    const freeThreshold = yuanToCent(form.freeThresholdYuan);
    const baseFee = yuanToCent(form.baseFeeYuan);
    if (freeThreshold < 0) {
        ElMessage.warning("包邮门槛不能为负数");
        return;
    }
    if (baseFee < 0) {
        ElMessage.warning("基础运费不能为负数");
        return;
    }
    submitting.value = true;
    try {
        const payload: Record<string, any> = {
            name: form.name.trim(),
            freeThreshold,
            baseFee,
            status: form.status
        };
        if (form.id) {
            payload.id = form.id;
            await updateShipping(payload);
            ElMessage.success("更新成功");
        } else {
            await createShipping(payload);
            ElMessage.success("创建成功");
        }
        dialogVisible.value = false;
        fetchData();
    } finally {
        submitting.value = false;
    }
}

async function handleStatusChange(row: ShippingRule) {
    await updateShippingStatus({ id: row.id!, status: row.status });
    ElMessage.success(row.status === 1 ? "已启用" : "已禁用");
}

onMounted(fetchData);
</script>

<template>
    <div class="app-container">
        <el-card shadow="never" class="mb-4">
            <el-button type="primary" @click="openAdd">
                <el-icon class="mr-1"><i class="ep-icon-plus" /></el-icon>
                新增包邮规则
            </el-button>
        </el-card>

        <el-card shadow="never">
            <el-table :data="list" v-loading="loading" border>
                <el-table-column prop="id" label="ID" width="60" align="center" />
                <el-table-column prop="name" label="规则名称" min-width="140" show-overflow-tooltip />
                <el-table-column label="包邮门槛" width="140" align="right">
                    <template #default="{ row }">
                        ¥{{ row.freeThreshold }}
                        <span class="text-gray-400 text-xs ml-1">(满此金额包邮)</span>
                    </template>
                </el-table-column>
                <el-table-column label="基础运费" width="120" align="right">
                    <template #default="{ row }">¥{{ row.baseFee }}</template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                    <template #default="{ row }">
                        <el-switch
                            v-model="row.status"
                            :active-value="1"
                            :inactive-value="0"
                            @change="handleStatusChange(row)"
                        />
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
                <el-table-column label="操作" width="120" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
            <el-form :model="form" label-width="120px" @submit.prevent="handleSubmit">
                <el-form-item label="规则名称" required>
                    <el-input v-model="form.name" placeholder="如：默认包邮规则" maxlength="20" />
                </el-form-item>
                <el-form-item label="包邮门槛（元）">
                    <el-input v-model="form.freeThresholdYuan" placeholder="如：199" type="number" />
                    <span class="form-tip">满此金额免运费</span>
                </el-form-item>
                <el-form-item label="基础运费（元）">
                    <el-input v-model="form.baseFeeYuan" placeholder="如：10" type="number" />
                    <span class="form-tip">未达门槛时收取</span>
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
.mr-1 { margin-right: 4px; }
.text-gray-400 { color: #9ca3af; }
.text-xs { font-size: 12px; }
.ml-1 { margin-left: 4px; }
.form-tip { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
