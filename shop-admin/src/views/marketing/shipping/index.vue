<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Clock, Document, Plus, Refresh, View } from "@element-plus/icons-vue";
import {
  createShipping,
  getCurrentShipping,
  getShippingAuditList,
  getShippingList,
  updateShipping,
  updateShippingStatus
} from "@/api/marketing";
import type { MarketingShippingAuditLog, ShippingRule } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "MarketingShipping" });
const canManageMarketing = hasAnyPerms(["marketing:manage"]);

const loading = ref(false);
const list = ref<ShippingRule[]>([]);
const total = ref(0);
const currentRule = ref<ShippingRule | null>(null);

const auditVisible = ref(false);
const auditLoading = ref(false);
const auditRows = ref<MarketingShippingAuditLog[]>([]);
const auditTotal = ref(0);
const auditQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  ruleId: undefined as number | undefined
});

async function fetchData() {
  loading.value = true;
  try {
    const [page, current] = await Promise.all([
      getShippingList({ pageNo: 1, pageSize: 50 }) as any,
      getCurrentShipping() as any
    ]);
    list.value = page.list || [];
    total.value = page.total || 0;
    currentRule.value = current || null;
  } finally {
    loading.value = false;
  }
}

const currentRuleText = computed(() => {
  if (!currentRule.value) return "暂无启用且在有效期内的运费规则";
  return `${currentRule.value.name}：未满 ¥${currentRule.value.freeThreshold} 收取 ¥${currentRule.value.baseFee}`;
});

const dialogVisible = ref(false);
const dialogTitle = ref("新增运费规则");
const submitting = ref(false);
const form = reactive({
  id: undefined as number | undefined,
  name: "",
  freeThresholdYuan: "",
  baseFeeYuan: "",
  startTime: "",
  endTime: "",
  status: 1
});

function resetForm() {
  form.id = undefined;
  form.name = "";
  form.freeThresholdYuan = "";
  form.baseFeeYuan = "";
  form.startTime = "";
  form.endTime = "";
  form.status = 1;
}

function yuanToCent(yuan: string): number {
  const num = Number.parseFloat(yuan);
  return Number.isFinite(num) ? Math.round(num * 100) : 0;
}

function openAdd() {
  resetForm();
  dialogTitle.value = "新增运费规则";
  dialogVisible.value = true;
}

function openEdit(row: ShippingRule) {
  resetForm();
  form.id = row.id;
  form.name = row.name;
  form.freeThresholdYuan = row.freeThreshold || "";
  form.baseFeeYuan = row.baseFee || "";
  form.startTime = row.startTime || "";
  form.endTime = row.endTime || "";
  form.status = row.status;
  dialogTitle.value = "编辑运费规则";
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (form.name.trim().length < 2) {
    ElMessage.warning("请输入 2 个字以上的规则名称");
    return;
  }
  if (form.endTime && form.startTime && form.endTime <= form.startTime) {
    ElMessage.warning("停用时间必须晚于生效时间");
    return;
  }
  submitting.value = true;
  try {
    const payload: Record<string, unknown> = {
      name: form.name.trim(),
      freeThreshold: yuanToCent(form.freeThresholdYuan),
      baseFee: yuanToCent(form.baseFeeYuan),
      startTime: form.startTime,
      endTime: form.endTime,
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
  ElMessage.success(row.status === 1 ? "已启用" : "已停用");
  fetchData();
}

async function openAudit(row?: ShippingRule) {
  auditQuery.pageNo = 1;
  auditQuery.ruleId = row?.id;
  auditVisible.value = true;
  await fetchAudit();
}

async function fetchAudit() {
  auditLoading.value = true;
  try {
    const params: Record<string, unknown> = {
      pageNo: auditQuery.pageNo,
      pageSize: auditQuery.pageSize
    };
    if (auditQuery.ruleId) params.ruleId = auditQuery.ruleId;
    const res = (await getShippingAuditList(params)) as any;
    auditRows.value = res.list || [];
    auditTotal.value = res.total || 0;
  } finally {
    auditLoading.value = false;
  }
}

onMounted(fetchData);
</script>

<template>
  <div class="app-container">
    <section class="toolbar">
      <div class="current-rule">
        <el-icon><Clock /></el-icon>
        <span>{{ currentRuleText }}</span>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
        <el-button :icon="Document" @click="openAudit()">变更记录</el-button>
        <el-button v-if="canManageMarketing" type="primary" :icon="Plus" @click="openAdd">新增运费规则</el-button>
      </div>
    </section>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="name" label="规则名称" min-width="150" show-overflow-tooltip />
      <el-table-column label="基础运费" width="120" align="right">
        <template #default="{ row }">¥{{ row.baseFee }}</template>
      </el-table-column>
      <el-table-column label="免邮门槛" width="140" align="right">
        <template #default="{ row }">¥{{ row.freeThreshold }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="生效时间" width="170" align="center">
        <template #default="{ row }">{{ row.startTime || "立即" }}</template>
      </el-table-column>
      <el-table-column prop="endTime" label="停用时间" width="170" align="center">
        <template #default="{ row }">{{ row.endTime || "长期有效" }}</template>
      </el-table-column>
      <el-table-column label="当前" width="86" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.currentActive" type="success">生效中</el-tag>
          <el-tag v-else type="info">未生效</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
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
      <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canManageMarketing" type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="primary" link size="small" :icon="View" @click="openAudit(row)">记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      共 {{ total }} 条规则
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form :model="form" label-width="120px" @submit.prevent="handleSubmit">
        <el-form-item label="规则名称" required>
          <el-input v-model="form.name" placeholder="如：默认运费规则" maxlength="64" />
        </el-form-item>
        <el-form-item label="基础运费（元）" required>
          <el-input v-model="form.baseFeeYuan" placeholder="如：10" type="number" min="0" />
        </el-form-item>
        <el-form-item label="免邮门槛（元）" required>
          <el-input v-model="form.freeThresholdYuan" placeholder="如：199" type="number" min="0" />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示立即生效"
            class="full-field"
          />
        </el-form-item>
        <el-form-item label="停用时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空表示长期有效"
            class="full-field"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="auditVisible" title="运费规则变更记录" size="760px">
      <el-table :data="auditRows" v-loading="auditLoading" border>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="requestUri" label="接口" min-width="230" show-overflow-tooltip />
        <el-table-column prop="businessRef" label="规则ID" width="100" />
        <el-table-column label="结果" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? "成功" : "失败" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="110" show-overflow-tooltip />
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="auditQuery.pageNo"
          v-model:page-size="auditQuery.pageSize"
          :total="auditTotal"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @current-change="fetchAudit"
          @size-change="fetchAudit"
        />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.app-container {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.current-rule {
  min-height: 32px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.full-field {
  width: 100%;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  color: #606266;
}

@media (max-width: 720px) {
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .actions {
    justify-content: flex-start;
  }
}
</style>
