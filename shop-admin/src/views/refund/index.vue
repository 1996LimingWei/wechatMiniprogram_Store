<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Check, Refresh, Search, View } from "@element-plus/icons-vue";
import {
  getRefundDetail,
  getRefundPage,
  handleRefundException,
  retryRefund,
  syncRefund
} from "@/api/refund";
import type {
  RefundWorkbenchDetail,
  RefundWorkbenchItem
} from "@/api/refund";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "RefundExceptionWorkbench" });

const canSync = hasAnyPerms(["trade:manage", "trade:refund-sync"]);
const canRetry = hasAnyPerms(["trade:manage", "trade:refund-retry"]);
const canHandle = hasAnyPerms(["trade:manage", "trade:refund-handle"]);

const loading = ref(false);
const rows = ref<RefundWorkbenchItem[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<RefundWorkbenchDetail | null>(null);

const query = reactive({
  page: 1,
  size: 10,
  refundSn: "",
  afterSaleSn: "",
  orderSn: "",
  status: "all",
  exceptionOnly: "1",
  createTimeStart: "",
  createTimeEnd: ""
});

async function fetchData() {
  loading.value = true;
  try {
    const result = await getRefundPage(buildParams());
    rows.value = result.list ?? [];
    total.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function buildParams() {
  const params: Record<string, unknown> = {
    page: query.page,
    size: query.size,
    exceptionOnly: Number(query.exceptionOnly)
  };
  if (query.refundSn.trim()) params.refundSn = query.refundSn.trim();
  if (query.afterSaleSn.trim()) params.afterSaleSn = query.afterSaleSn.trim();
  if (query.orderSn.trim()) params.orderSn = query.orderSn.trim();
  if (query.status !== "all") params.status = Number(query.status);
  if (query.createTimeStart.trim()) params.createTimeStart = query.createTimeStart.trim();
  if (query.createTimeEnd.trim()) params.createTimeEnd = query.createTimeEnd.trim();
  return params;
}

function resetQuery() {
  query.page = 1;
  query.refundSn = "";
  query.afterSaleSn = "";
  query.orderSn = "";
  query.status = "all";
  query.exceptionOnly = "1";
  query.createTimeStart = "";
  query.createTimeEnd = "";
  fetchData();
}

function handlePageChange(page: number) {
  query.page = page;
  fetchData();
}

async function openDetail(row: RefundWorkbenchItem) {
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getRefundDetail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

async function handleSync(row: RefundWorkbenchItem) {
  detail.value = await syncRefund(row.id);
  ElMessage.success("退款状态已同步");
  await fetchData();
}

async function handleRetry(row: RefundWorkbenchItem) {
  await ElMessageBox.confirm(
    `确认重试退款单 ${row.refundSn}？`,
    "重试退款",
    { type: "warning", confirmButtonText: "重试", cancelButtonText: "取消" }
  );
  detail.value = await retryRefund(row.id);
  ElMessage.success("退款重试已提交");
  await fetchData();
}

async function handleException(row: RefundWorkbenchItem) {
  const { value } = await ElMessageBox.prompt(
    `确认处理退款异常 ${row.refundExceptionCode}？`,
    "处理退款异常",
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
  await handleRefundException(row.id, value.trim());
  ElMessage.success("退款异常已处理");
  await fetchData();
}

function statusType(status?: number) {
  const types: Record<number, "warning" | "success" | "danger" | "primary" | "info"> = {
    1: "success",
    4: "primary",
    5: "danger",
    6: "warning",
    7: "warning"
  };
  return types[status ?? -1] ?? "info";
}

function handledType(handled?: number) {
  return handled === 1 ? "success" : "danger";
}

function money(value?: string | number) {
  const amount = Number(value ?? 0);
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : "—";
}

onMounted(fetchData);
</script>

<template>
  <div class="refund-page">
    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="退款单号">
        <el-input v-model="query.refundSn" clearable class="compact-search" />
      </el-form-item>
      <el-form-item label="售后单号">
        <el-input v-model="query.afterSaleSn" clearable class="compact-search" />
      </el-form-item>
      <el-form-item label="订单号">
        <el-input v-model="query.orderSn" clearable class="compact-search" />
      </el-form-item>
      <el-form-item label="退款状态">
        <el-select v-model="query.status" class="compact-search">
          <el-option label="全部" value="all" />
          <el-option label="退款处理中" value="4" />
          <el-option label="退款失败" value="5" />
          <el-option label="已退款" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="列表范围">
        <el-select v-model="query.exceptionOnly" class="compact-search">
          <el-option label="仅异常" value="1" />
          <el-option label="全部退款" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间">
        <el-input
          v-model="query.createTimeStart"
          placeholder="yyyy-MM-dd HH:mm:ss"
          clearable
          class="time-search"
        />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-input
          v-model="query.createTimeEnd"
          placeholder="yyyy-MM-dd HH:mm:ss"
          clearable
          class="time-search"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="fetchData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="refundSn" label="退款单号" min-width="180" />
      <el-table-column prop="afterSaleSn" label="售后单号" min-width="180" />
      <el-table-column prop="orderSn" label="订单号" min-width="180" />
      <el-table-column label="退款金额" width="110" align="right">
        <template #default="{ row }">{{ money(row.refundAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="refundAttemptCount" label="次数" width="80" />
      <el-table-column prop="refundNextAttemptTime" label="下次执行" width="170" />
      <el-table-column label="异常原因" min-width="260">
        <template #default="{ row }">
          <div v-if="row.refundExceptionCode" class="reason-cell">
            <el-tag size="small" type="danger">{{ row.refundExceptionCode }}</el-tag>
            <span>{{ row.refundExceptionMessage }}</span>
          </div>
          <span v-else>{{ row.refundLastError || row.refundMessage || "—" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="处理" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.refundExceptionCode" :type="handledType(row.refundHandled)">
            {{ row.refundHandled === 1 ? "已处理" : "待处理" }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="240">
        <template #default="{ row }">
          <el-button text type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canSync" text type="primary" :icon="Refresh" @click="handleSync(row)">
            同步
          </el-button>
          <el-button
            v-if="canRetry && row.canRetry"
            text
            type="warning"
            :icon="Refresh"
            @click="handleRetry(row)"
          >
            重试
          </el-button>
          <el-button
            v-if="canHandle && row.refundExceptionCode && row.refundHandled !== 1"
            text
            type="success"
            :icon="Check"
            @click="handleException(row)"
          >
            处理
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

    <el-drawer v-model="detailVisible" title="退款单详情" size="720px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="退款单号">
              {{ detail.refund.refundSn }}
            </el-descriptions-item>
            <el-descriptions-item label="售后单号">
              {{ detail.refund.afterSaleSn }}
            </el-descriptions-item>
            <el-descriptions-item label="订单号">
              {{ detail.refund.orderSn }}
            </el-descriptions-item>
            <el-descriptions-item label="渠道">
              {{ detail.refund.refundProvider || "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="退款金额">
              {{ money(detail.refund.refundAmount) }}
            </el-descriptions-item>
            <el-descriptions-item label="支付金额">
              {{ money(detail.refund.payAmount) }}
            </el-descriptions-item>
            <el-descriptions-item label="退款状态">
              {{ detail.refund.statusText }}
            </el-descriptions-item>
            <el-descriptions-item label="渠道状态">
              {{ detail.refund.refundChannelState || "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="异常编码" :span="2">
              {{ detail.refund.refundExceptionCode || "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="异常说明" :span="2">
              {{ detail.refund.refundExceptionMessage || detail.refund.refundLastError || "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="处理备注" :span="2">
              {{ detail.refund.refundHandleRemark || "—" }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="detail-actions">
            <el-button v-if="canSync" type="primary" :icon="Refresh" @click="handleSync(detail.refund)">
              人工同步
            </el-button>
            <el-button
              v-if="canRetry && detail.refund.canRetry"
              type="warning"
              :icon="Refresh"
              @click="handleRetry(detail.refund)"
            >
              人工重试
            </el-button>
          </div>

          <h4>任务记录</h4>
          <el-table :data="detail.taskRecords" border size="small">
            <el-table-column prop="refundAttemptCount" label="尝试次数" width="100" />
            <el-table-column prop="lastAttemptTime" label="最近执行" width="170" />
            <el-table-column prop="nextAttemptTime" label="下次执行" width="170" />
            <el-table-column prop="lastError" label="最近错误" min-width="220" />
          </el-table>

          <h4>回调记录</h4>
          <el-empty
            v-if="detail.callbackRecords.length === 0"
            description="暂无退款回调记录"
            :image-size="64"
          />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.refund-page {
  padding: 16px;
}

.search-form {
  margin-bottom: 12px;
}

.compact-search {
  width: 160px;
}

.time-search {
  width: 190px;
}

.reason-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin: 12px 0 18px;
}

h4 {
  margin: 18px 0 10px;
  font-size: 15px;
  font-weight: 600;
}
</style>
