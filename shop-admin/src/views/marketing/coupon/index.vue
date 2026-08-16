<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
    getCouponTemplateList,
    createCouponTemplate,
    updateCouponTemplate,
    deleteCouponTemplate,
    updateCouponTemplateStatus,
    getCouponInstanceList
} from "@/api/marketing";
import type { CouponTemplate, CouponInstance } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "MarketingCoupon" });
const canManageMarketing = hasAnyPerms(["marketing:manage"]);

/* ---------- 列表 ---------- */
const loading = ref(false);
const list = ref<CouponTemplate[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);

async function fetchData() {
    loading.value = true;
    try {
        const res = await getCouponTemplateList({
            pageNo: pageNo.value,
            pageSize: pageSize.value
        }) as any;
        list.value = res.list || [];
        total.value = res.total || 0;
    } finally {
        loading.value = false;
    }
}

/* ---------- 对话框 ---------- */
const dialogVisible = ref(false);
const dialogTitle = ref("新增优惠券");
const submitting = ref(false);
const form = reactive({
    id: undefined as number | undefined,
    name: "",
    type: 1,
    thresholdYuan: "",
    discountYuan: "",
    totalCount: 0,
    perUserLimit: 1,
    validityType: 1,
    validStartTime: "",
    validEndTime: "",
    validDays: 30,
    status: 1
});

function resetForm() {
    form.id = undefined;
    form.name = "";
    form.type = 1;
    form.thresholdYuan = "";
    form.discountYuan = "";
    form.totalCount = 0;
    form.perUserLimit = 1;
    form.validityType = 1;
    form.validStartTime = "";
    form.validEndTime = "";
    form.validDays = 30;
    form.status = 1;
}

function yuanToCent(yuan: string): number {
    const num = parseFloat(yuan);
    return isNaN(num) ? 0 : Math.round(num * 100);
}

function openAdd() {
    resetForm();
    dialogTitle.value = "新增优惠券";
    dialogVisible.value = true;
}

function openEdit(row: CouponTemplate) {
    resetForm();
    form.id = row.id;
    form.name = row.name;
    form.type = row.type;
    form.thresholdYuan = row.thresholdAmount || "0";
    form.discountYuan = row.discountAmount || "0";
    form.totalCount = row.totalCount;
    form.perUserLimit = row.perUserLimit;
    form.validityType = row.validityType;
    form.validStartTime = row.validStartTime || "";
    form.validEndTime = row.validEndTime || "";
    form.validDays = row.validDays || 30;
    form.status = row.status;
    dialogTitle.value = "编辑优惠券";
    dialogVisible.value = true;
}

async function handleSubmit() {
    if (!form.name.trim()) {
        ElMessage.warning("请输入券名称");
        return;
    }
    const thresholdAmount = yuanToCent(form.thresholdYuan);
    const discountAmount = yuanToCent(form.discountYuan);
    if (discountAmount <= 0) {
        ElMessage.warning("优惠金额必须大于 0");
        return;
    }
    submitting.value = true;
    try {
        const payload: Record<string, any> = {
            name: form.name.trim(),
            type: form.type,
            thresholdAmount,
            discountAmount,
            totalCount: form.totalCount,
            perUserLimit: form.perUserLimit,
            validityType: form.validityType,
            validStartTime: form.validStartTime || "",
            validEndTime: form.validEndTime || "",
            validDays: form.validDays,
            status: form.status
        };
        if (form.id) {
            payload.id = form.id;
            await updateCouponTemplate(payload);
            ElMessage.success("更新成功");
        } else {
            await createCouponTemplate(payload);
            ElMessage.success("创建成功");
        }
        dialogVisible.value = false;
        fetchData();
    } finally {
        submitting.value = false;
    }
}

/* ---------- 删除 ---------- */
async function handleDelete(row: CouponTemplate) {
    await ElMessageBox.confirm(`确定删除优惠券「${row.name}」吗？`, "确认删除", {
        type: "warning"
    });
    await deleteCouponTemplate({ id: row.id! });
    ElMessage.success("删除成功");
    fetchData();
}

/* ---------- 状态切换 ---------- */
async function handleStatusChange(row: CouponTemplate) {
    await updateCouponTemplateStatus({ id: row.id!, status: row.status });
    ElMessage.success(row.status === 1 ? "已启用" : "已禁用");
}

/* ---------- 券实例查看 ---------- */
const instanceVisible = ref(false);
const instanceLoading = ref(false);
const instances = ref<CouponInstance[]>([]);
const instanceTotal = ref(0);

async function showInstances(row: CouponTemplate) {
    instanceVisible.value = true;
    instanceLoading.value = true;
    try {
        const res = await getCouponInstanceList({
            templateId: row.id,
            pageNo: 1,
            pageSize: 50
        }) as any;
        instances.value = res.list || [];
        instanceTotal.value = res.total || 0;
    } finally {
        instanceLoading.value = false;
    }
}

const statusText = (s: number) =>
    s === 0 ? "未使用" : s === 1 ? "已使用" : s === 2 ? "已过期" : "未知";
const typeText = (t: number) => (t === 1 ? "满减券" : t === 2 ? "新人券" : "未知");

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
                新增优惠券
            </el-button>
        </el-card>

        <el-card shadow="never">
            <el-table :data="list" v-loading="loading" border>
                <el-table-column prop="id" label="ID" width="60" align="center" />
                <el-table-column prop="name" label="券名称" min-width="120" show-overflow-tooltip />
                <el-table-column label="类型" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.type === 2 ? 'warning' : 'primary'" size="small">
                            {{ typeText(row.type) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="门槛" width="100" align="right">
                    <template #default="{ row }">
                        ¥{{ row.thresholdAmount }}
                    </template>
                </el-table-column>
                <el-table-column label="优惠" width="100" align="right">
                    <template #default="{ row }">
                        <span class="text-red">-¥{{ row.discountAmount }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="发行/已领" width="120" align="center">
                    <template #default="{ row }">
                        {{ row.totalCount || "∞" }} / {{ row.claimedCount || 0 }}
                    </template>
                </el-table-column>
                <el-table-column label="有效期" min-width="160">
                    <template #default="{ row }">
                        <span v-if="row.validityType === 1">
                            {{ row.validStartTime || "" }} ~ {{ row.validEndTime || "" }}
                        </span>
                        <span v-else>领取后 {{ row.validDays }} 天</span>
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
                <el-table-column label="操作" width="200" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="canManageMarketing" type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                        <el-button type="info" link size="small" @click="showInstances(row)">查看领取</el-button>
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

        <!-- 新增/编辑 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
            <el-form :model="form" label-width="110px" @submit.prevent="handleSubmit">
                <el-form-item label="券名称" required>
                    <el-input v-model="form.name" placeholder="如：满100减20" maxlength="30" />
                </el-form-item>
                <el-form-item label="券类型">
                    <el-radio-group v-model="form.type">
                        <el-radio :value="1">满减券</el-radio>
                        <el-radio :value="2">新人券</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="使用门槛（元）">
                    <el-input v-model="form.thresholdYuan" placeholder="0 表示无门槛" type="number" />
                </el-form-item>
                <el-form-item label="优惠金额（元）" required>
                    <el-input v-model="form.discountYuan" placeholder="如：20" type="number" />
                </el-form-item>
                <el-form-item label="发行总量">
                    <el-input-number v-model="form.totalCount" :min="0" controls-position="right" />
                    <span class="form-tip">0 = 不限量</span>
                </el-form-item>
                <el-form-item label="每人限领">
                    <el-input-number v-model="form.perUserLimit" :min="1" :max="99" controls-position="right" />
                </el-form-item>
                <el-form-item label="有效期类型">
                    <el-radio-group v-model="form.validityType">
                        <el-radio :value="1">固定日期</el-radio>
                        <el-radio :value="2">领取后天数</el-radio>
                    </el-radio-group>
                </el-form-item>
                <template v-if="form.validityType === 1">
                    <el-form-item label="有效期开始">
                        <el-date-picker v-model="form.validStartTime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DD HH:mm:ss" />
                    </el-form-item>
                    <el-form-item label="有效期结束">
                        <el-date-picker v-model="form.validEndTime" type="datetime" placeholder="选择结束时间" value-format="YYYY-MM-DD HH:mm:ss" />
                    </el-form-item>
                </template>
                <el-form-item v-else label="有效天数">
                    <el-input-number v-model="form.validDays" :min="1" :max="365" controls-position="right" />
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

        <!-- 券实例抽屉 -->
        <el-dialog v-model="instanceVisible" title="领取记录" width="700px">
            <el-table :data="instances" v-loading="instanceLoading" border size="small">
                <el-table-column prop="id" label="ID" width="60" />
                <el-table-column prop="userId" label="用户ID" width="80" />
                <el-table-column label="优惠" width="80">
                    <template #default="{ row }">{{ row.discountAmount ? `¥${row.discountAmount}` : "-" }}</template>
                </el-table-column>
                <el-table-column label="状态" width="80">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 0 ? 'success' : row.status === 1 ? 'info' : 'danger'" size="small">
                            {{ statusText(row.status) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="expireTime" label="过期时间" min-width="140" />
                <el-table-column prop="usedTime" label="使用时间" min-width="140">
                    <template #default="{ row }">{{ row.usedTime || "-" }}</template>
                </el-table-column>
            </el-table>
            <p class="mt-2 text-gray-500">共 {{ instanceTotal }} 条记录</p>
        </el-dialog>
    </div>
</template>

<style scoped>
.app-container { padding: 16px; }
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
.mt-2 { margin-top: 8px; }
.mr-1 { margin-right: 4px; }
.text-red { color: #e43d3c; }
.text-gray-500 { color: #9ca3af; }
.form-tip { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
