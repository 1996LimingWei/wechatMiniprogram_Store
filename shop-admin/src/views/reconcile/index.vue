<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import dayjs from "dayjs";
import { ElMessage, ElMessageBox } from "element-plus";
import { Check, Download, Refresh, Search, View } from "@element-plus/icons-vue";
import {
  exportReconcile,
  getReconcileBatchDetail,
  getReconcileBatchPage,
  getReconcileDifferencePage,
  handleReconcileDifference,
  runReconcile
} from "@/api/reconcile";
import type {
  ReconcileBatch,
  ReconcileDetail,
  ReconcileDifference
} from "@/api/reconcile";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "ReconcileWorkbench" });

const canTrigger = hasAnyPerms(["trade:manage", "trade:reconcile-trigger"]);
const canExport = hasAnyPerms(["trade:manage", "trade:reconcile-export"]);
const canHandle = hasAnyPerms(["trade:manage", "trade:reconcile-handle"]);

const loading = ref(false);
const running = ref(false);
const detailLoading = ref(false);
const diffLoading = ref(false);
const rows = ref<ReconcileBatch[]>([]);
const differences = ref<ReconcileDifference[]>([]);
const total = ref(0);
const diffTotal = ref(0);
const selectedBatch = ref<ReconcileBatch | null>(null);
const detail = ref<ReconcileDetail | null>(null);

const query = reactive({
  page: 1,
  size: 10,
  dateStart: "",
  dateEnd: "",
  status: "all"
});

const diffQuery = reactive({
  page: 1,
  size: 10,
  diffType: "all",
  handled: "all"
});

const runDate = ref(dayjs().subtract(1, "day").format("YYYY-MM-DD"));

const diffTypeOptions = [
  { label: "全部", value: "all" },
  { label: "平账", value: "BALANCED" },
  { label: "本地多", value: "LOCAL_MORE" },
  { label: "微信多", value: "WECHAT_MORE" },
  { label: "金额不一致", value: "AMOUNT_MISMATCH" },
  { label: "状态不一致", value: "STATUS_MISMATCH" },
  { label: "缺少关联订单", value: "MISSING_ORDER" }
];

async function fetchData() {
  loading.value = true;
  try {
    const result = await getReconcileBatchPage(buildParams());
    rows.value = result.list ?? [];
    total.value = result.total ?? 0;
    if (!selectedBatch.value && rows.value.length > 0) {
      await selectBatch(rows.value[0]);
    }
  } finally {
    loading.value = false;
  }
}

function buildParams() {
  const params: Record<string, unknown> = {
    page: query.page,
    size: query.size
  };
  if (query.dateStart) params.dateStart = query.dateStart;
  if (query.dateEnd) params.dateEnd = query.dateEnd;
  if (query.status !== "all") params.status = Number(query.status);
  return params;
}

function buildDiffParams() {
  const params: Record<string, unknown> = {
    page: diffQuery.page,
    size: diffQuery.size,
    batchId: selectedBatch.value?.id
  };
  if (diffQuery.diffType !== "all") params.diffType = diffQuery.diffType;
  if (diffQuery.handled !== "all") params.handled = Number(diffQuery.handled);
  return params;
}

async function selectBatch(row: ReconcileBatch) {
  selectedBatch.value = row;
  detailLoading.value = true;
  try {
    detail.value = await getReconcileBatchDetail(row.id);
    diffQuery.page = 1;
    await fetchDifferences();
  } finally {
    detailLoading.value = false;
  }
}

async function fetchDifferences() {
  if (!selectedBatch.value) {
    differences.value = [];
    diffTotal.value = 0;
    return;
  }
  diffLoading.value = true;
  try {
    const result = await getReconcileDifferencePage(buildDiffParams());
    differences.value = result.list ?? [];
    diffTotal.value = result.total ?? 0;
  } finally {
    diffLoading.value = false;
  }
}

function resetQuery() {
  query.page = 1;
  query.dateStart = "";
  query.dateEnd = "";
  query.status = "all";
  fetchData();
}

function handlePageChange(page: number) {
  query.page = page;
  fetchData();
}

function handleDiffPageChange(page: number) {
  diffQuery.page = page;
  fetchDifferences();
}

async function handleRun() {
  if (!runDate.value) {
    ElMessage.warning("请选择对账日期");
    return;
  }
  await ElMessageBox.confirm(
    `确认生成 ${runDate.value} 的日终对账结果？`,
    "手动对账",
    { type: "warning", confirmButtonText: "生成", cancelButtonText: "取消" }
  );
  running.value = true;
  try {
    const result = await runReconcile(runDate.value);
    ElMessage.success("日终对账已生成");
    selectedBatch.value = result.batch;
    detail.value = result;
    await fetchData();
    await selectBatch(result.batch);
  } finally {
    running.value = false;
  }
}

async function handleExport(row?: ReconcileBatch | null) {
  const batch = row ?? selectedBatch.value;
  if (!batch) {
    ElMessage.warning("请先选择对账批次");
    return;
  }
  const blob = await exportReconcile(batch.id);
  downloadBlob(blob, `日终对账-${batch.reconcileDate}.csv`);
}

async function handleDifference(row: ReconcileDifference) {
  const { value } = await ElMessageBox.prompt(
    `确认处理差异 ${row.diffTypeText}？`,
    "处理对账差异",
    {
      confirmButtonText: "确认处理",
      cancelButtonText: "取消",
      inputPlaceholder: "填写处理说明",
      inputValidator: input =>
        !!input && input.trim().length >= 4 && input.trim().length <= 200
          ? true
          : "处理说明长度应为 4 至 200 个字符"
    }
  );
  await handleReconcileDifference(row.id, value.trim());
  ElMessage.success("对账差异已处理");
  await fetchDifferences();
  if (selectedBatch.value) {
    detail.value = await getReconcileBatchDetail(selectedBatch.value.id);
  }
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function money(value?: string | number) {
  const amount = Number(value ?? 0);
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : "—";
}

function statusType(status?: number) {
  const types: Record<number, "warning" | "success" | "danger" | "info"> = {
    0: "warning",
    1: "success",
    2: "danger"
  };
  return types[status ?? -1] ?? "info";
}

function diffTypeColor(type: string) {
  const types: Record<string, "success" | "warning" | "danger" | "primary" | "info"> = {
    BALANCED: "success",
    LOCAL_MORE: "warning",
    WECHAT_MORE: "warning",
    AMOUNT_MISMATCH: "danger",
    STATUS_MISMATCH: "danger",
    MISSING_ORDER: "danger"
  };
  return types[type] ?? "info";
}

onMounted(fetchData);
</script>

<template>
  <div class="reconcile-page">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="开始日期">
        <el-date-picker
          v-model="query.dateStart"
          value-format="YYYY-MM-DD"
          type="date"
          class="date-search"
        />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker
          v-model="query.dateEnd"
          value-format="YYYY-MM-DD"
          type="date"
          class="date-search"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" class="compact-search">
          <el-option label="全部" value="all" />
          <el-option label="处理中" value="0" />
          <el-option label="完成" value="1" />
          <el-option label="失败" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="fetchData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
      <el-form-item v-if="canTrigger" label="对账日期">
        <el-date-picker
          v-model="runDate"
          value-format="YYYY-MM-DD"
          type="date"
          class="date-search"
        />
        <el-button
          type="success"
          :icon="Refresh"
          :loading="running"
          @click="handleRun"
        >
          生成对账
        </el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="rows"
      border
      highlight-current-row
      @row-click="selectBatch"
    >
      <el-table-column prop="reconcileDate" label="对账日期" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="localPayCount" label="本地支付" width="90" />
      <el-table-column label="本地支付金额" width="120" align="right">
        <template #default="{ row }">{{ money(row.localPayAmount) }}</template>
      </el-table-column>
      <el-table-column prop="localRefundCount" label="本地退款" width="90" />
      <el-table-column label="本地退款金额" width="120" align="right">
        <template #default="{ row }">{{ money(row.localRefundAmount) }}</template>
      </el-table-column>
      <el-table-column label="本地净收入" width="120" align="right">
        <template #default="{ row }">{{ money(row.localNetAmount) }}</template>
      </el-table-column>
      <el-table-column prop="differenceCount" label="差异" width="80" />
      <el-table-column prop="source" label="来源" width="150" />
      <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
      <el-table-column label="操作" fixed="right" width="170">
        <template #default="{ row }">
          <el-button text type="primary" :icon="View" @click.stop="selectBatch(row)">详情</el-button>
          <el-button
            v-if="canExport"
            text
            type="primary"
            :icon="Download"
            @click.stop="handleExport(row)"
          >
            导出
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      layout="total, prev, pager, next"
      :current-page="query.page"
      :page-size="query.size"
      :total="total"
      @current-change="handlePageChange"
    />

    <div v-if="detail?.batch" v-loading="detailLoading" class="detail-panel">
      <div class="panel-title">
        <h3>{{ detail.batch.reconcileDate }} 对账汇总</h3>
        <el-button
          v-if="canExport"
          type="primary"
          :icon="Download"
          @click="handleExport(detail.batch)"
        >
          导出结果
        </el-button>
      </div>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="本地支付">
          {{ detail.batch.localPayCount }} / {{ money(detail.batch.localPayAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="本地退款">
          {{ detail.batch.localRefundCount }} / {{ money(detail.batch.localRefundAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="本地净收入">
          {{ money(detail.batch.localNetAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="差异数量">
          {{ detail.batch.differenceCount }}
        </el-descriptions-item>
        <el-descriptions-item label="微信支付">
          {{ detail.batch.wechatPayCount }} / {{ money(detail.batch.wechatPayAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="微信退款">
          {{ detail.batch.wechatRefundCount }} / {{ money(detail.batch.wechatRefundAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="微信净收入">
          {{ money(detail.batch.wechatNetAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="手续费">
          {{ detail.batch.feeAmount ? money(detail.batch.feeAmount) : "—" }}
        </el-descriptions-item>
        <el-descriptions-item label="交易账单" :span="2">
          <el-link v-if="detail.batch.tradeBillUrl" :href="detail.batch.tradeBillUrl" target="_blank">
            微信交易账单
          </el-link>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="资金账单" :span="2">
          <el-link v-if="detail.batch.fundBillUrl" :href="detail.batch.fundBillUrl" target="_blank">
            微信资金账单
          </el-link>
          <span v-else>—</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="diff-toolbar">
        <el-select v-model="diffQuery.diffType" class="compact-search" @change="fetchDifferences">
          <el-option
            v-for="item in diffTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="diffQuery.handled" class="compact-search" @change="fetchDifferences">
          <el-option label="全部处理状态" value="all" />
          <el-option label="待处理" value="0" />
          <el-option label="已处理" value="1" />
        </el-select>
      </div>

      <el-table v-loading="diffLoading" :data="differences" border>
        <el-table-column label="差异类型" width="130">
          <template #default="{ row }">
            <el-tag :type="diffTypeColor(row.diffType)">{{ row.diffTypeText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="businessTypeText" label="业务" width="80" />
        <el-table-column prop="businessSn" label="业务单号" min-width="170" />
        <el-table-column prop="orderSn" label="订单号" min-width="160" />
        <el-table-column label="本地金额" width="110" align="right">
          <template #default="{ row }">{{ money(row.localAmount) }}</template>
        </el-table-column>
        <el-table-column label="渠道金额" width="110" align="right">
          <template #default="{ row }">{{ money(row.channelAmount) }}</template>
        </el-table-column>
        <el-table-column prop="localStatus" label="本地状态" width="120" />
        <el-table-column prop="channelStatus" label="渠道状态" width="130" />
        <el-table-column prop="reason" label="原因" min-width="220" />
        <el-table-column label="处理" width="95">
          <template #default="{ row }">
            <el-tag :type="row.handled === 1 ? 'success' : 'danger'">
              {{ row.handled === 1 ? "已处理" : "待处理" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="110">
          <template #default="{ row }">
            <el-button
              v-if="canHandle && row.handled !== 1"
              text
              type="success"
              :icon="Check"
              @click="handleDifference(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next"
        :current-page="diffQuery.page"
        :page-size="diffQuery.size"
        :total="diffTotal"
        @current-change="handleDiffPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.reconcile-page {
  padding: 16px;
}

.search-form {
  margin-bottom: 12px;
}

.compact-search {
  width: 160px;
}

.date-search {
  width: 150px;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-panel {
  margin-top: 18px;
}

.panel-title,
.diff-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.diff-toolbar {
  justify-content: flex-start;
  margin-top: 16px;
}

h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
</style>
