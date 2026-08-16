<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Check, Refresh, Search, View } from "@element-plus/icons-vue";
import {
  getPayExceptionPage,
  getPayOrderDetail,
  getPayOrderPage,
  handlePayException,
  syncPayOrder
} from "@/api/payment";
import type {
  AdminPayOrder,
  PayException,
  PayOrderDetail
} from "@/api/payment";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "PaymentExceptionWorkbench" });

const canSync = hasAnyPerms(["trade:manage", "trade:payment-sync"]);
const canHandle = hasAnyPerms(["trade:manage", "trade:payment-handle"]);

const activeTab = ref<"orders" | "exceptions">("exceptions");
const loading = ref(false);
const total = ref(0);
const orderRows = ref<AdminPayOrder[]>([]);
const exceptionRows = ref<PayException[]>([]);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PayOrderDetail | null>(null);

const query = reactive({
  page: 1,
  size: 10,
  paySn: "",
  orderSn: "",
  status: "all",
  reasonCode: "all",
  handled: "0",
  createTimeStart: "",
  createTimeEnd: ""
});

const reasonOptions = [
  { label: "全部", value: "all" },
  { label: "微信已支付本地未支付", value: "WECHAT_PAID_LOCAL_UNPAID" },
  { label: "本地关闭微信已支付", value: "WECHAT_PAID_LOCAL_CLOSED" },
  { label: "支付中超时", value: "PENDING_TIMEOUT" },
  { label: "回调验签失败", value: "NOTIFY_VERIFY_FAILED" },
  { label: "金额不一致", value: "AMOUNT_MISMATCH" },
  { label: "查单失败", value: "QUERY_FAILED" }
];

async function fetchData() {
  loading.value = true;
  try {
    const params = buildParams();
    if (activeTab.value === "orders") {
      const result = await getPayOrderPage(params);
      orderRows.value = result.list ?? [];
      total.value = result.total ?? 0;
    } else {
      const result = await getPayExceptionPage(params);
      exceptionRows.value = result.list ?? [];
      total.value = result.total ?? 0;
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
  if (query.paySn.trim()) params.paySn = query.paySn.trim();
  if (query.orderSn.trim()) params.orderSn = query.orderSn.trim();
  if (query.createTimeStart.trim()) {
    params.createTimeStart = query.createTimeStart.trim();
  }
  if (query.createTimeEnd.trim()) {
    params.createTimeEnd = query.createTimeEnd.trim();
  }
  if (activeTab.value === "orders" && query.status !== "all") {
    params.status = Number(query.status);
  }
  if (activeTab.value === "exceptions") {
    if (query.reasonCode !== "all") params.reasonCode = query.reasonCode;
    if (query.handled !== "all") params.handled = Number(query.handled);
  }
  return params;
}

function handleTabChange() {
  query.page = 1;
  total.value = 0;
  fetchData();
}

function resetQuery() {
  query.page = 1;
  query.paySn = "";
  query.orderSn = "";
  query.status = "all";
  query.reasonCode = "all";
  query.handled = activeTab.value === "exceptions" ? "0" : "all";
  query.createTimeStart = "";
  query.createTimeEnd = "";
  fetchData();
}

function handlePageChange(page: number) {
  query.page = page;
  fetchData();
}

async function openDetail(payOrderId?: number) {
  if (!payOrderId) {
    ElMessage.warning("缺少支付单ID");
    return;
  }
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    detail.value = await getPayOrderDetail(payOrderId);
  } finally {
    detailLoading.value = false;
  }
}

async function handleSync(row: AdminPayOrder | PayException) {
  const payOrderId = "payOrderId" in row ? row.payOrderId : row.id;
  if (!payOrderId) {
    ElMessage.warning("缺少支付单ID");
    return;
  }
  const result = await syncPayOrder(payOrderId);
  result.success ? ElMessage.success(result.message) : ElMessage.warning(result.message);
  if (detail.value?.payOrder.id === payOrderId) {
    detail.value = await getPayOrderDetail(payOrderId);
  }
  await fetchData();
}

async function handleException(row: PayException) {
  const { value } = await ElMessageBox.prompt(
    `确认处理支付异常 ${row.reasonCode}？`,
    "处理支付异常",
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
  await handlePayException(row.id, value.trim());
  ElMessage.success("支付异常已处理");
  if (detail.value?.payOrder.id === row.payOrderId) {
    detail.value = await getPayOrderDetail(row.payOrderId);
  }
  await fetchData();
}

function statusType(status?: number) {
  const types: Record<number, "warning" | "success" | "info"> = {
    0: "warning",
    1: "success",
    2: "info",
    3: "info"
  };
  return types[status ?? -1] ?? "info";
}

function handledType(handled?: number) {
  return handled === 1 ? "success" : "danger";
}

function money(value?: string | number) {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? `¥${numberValue.toFixed(2)}` : "—";
}

onMounted(fetchData);
</script>

<template>
  <div class="payment-page">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="异常列表" name="exceptions" />
      <el-tab-pane label="支付单" name="orders" />
    </el-tabs>

    <el-form :inline="true" :model="query" class="search-form">
      <el-form-item label="支付单号">
        <el-input v-model="query.paySn" clearable class="compact-search" />
      </el-form-item>
      <el-form-item label="订单号">
        <el-input v-model="query.orderSn" clearable class="compact-search" />
      </el-form-item>
      <el-form-item v-if="activeTab === 'orders'" label="支付状态">
        <el-select v-model="query.status" class="compact-search">
          <el-option label="全部" value="all" />
          <el-option label="待支付" value="0" />
          <el-option label="已支付" value="1" />
          <el-option label="已关闭" value="2" />
          <el-option label="已退款" value="3" />
        </el-select>
      </el-form-item>
      <template v-else>
        <el-form-item label="处理状态">
          <el-select v-model="query.handled" class="compact-search">
            <el-option label="全部" value="all" />
            <el-option label="待处理" value="0" />
            <el-option label="已处理" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常原因">
          <el-select v-model="query.reasonCode" class="reason-select">
            <el-option
              v-for="item in reasonOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </template>
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
        <el-button type="primary" :icon="Search" @click="fetchData">
          查询
        </el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-if="activeTab === 'exceptions'"
      v-loading="loading"
      :data="exceptionRows"
      border
    >
      <el-table-column prop="paySn" label="支付单号" min-width="180" />
      <el-table-column prop="orderSn" label="订单号" min-width="180" />
      <el-table-column prop="reason" label="异常原因" min-width="260">
        <template #default="{ row }">
          <div class="reason-cell">
            <el-tag size="small" type="danger">{{ row.reasonCode }}</el-tag>
            <span>{{ row.reason }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="本地状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.localStatus)">
            {{ row.localStatusText || "—" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="wechatTradeState" label="微信状态" width="120" />
      <el-table-column label="微信金额" width="110">
        <template #default="{ row }">{{ money(row.wechatAmount) }}</template>
      </el-table-column>
      <el-table-column label="处理状态" width="110">
        <template #default="{ row }">
          <el-tag :type="handledType(row.handled)">
            {{ row.handled === 1 ? "已处理" : "待处理" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastDetectTime" label="最近发现" width="170" />
      <el-table-column label="操作" fixed="right" width="220">
        <template #default="{ row }">
          <el-button
            text
            type="primary"
            :icon="View"
            @click="openDetail(row.payOrderId)"
          >
            详情
          </el-button>
          <el-button
            v-if="canSync && row.handled !== 1"
            text
            type="primary"
            :icon="Refresh"
            @click="handleSync(row)"
          >
            同步
          </el-button>
          <el-button
            v-if="canHandle && row.handled !== 1"
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

    <el-table v-else v-loading="loading" :data="orderRows" border>
      <el-table-column prop="paySn" label="支付单号" min-width="190" />
      <el-table-column prop="orderSn" label="订单号" min-width="180" />
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column label="支付金额" width="110">
        <template #default="{ row }">{{ money(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="channel" label="渠道" width="100" />
      <el-table-column label="本地状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="wechatTradeState" label="微信状态" width="120" />
      <el-table-column prop="lastQueryTime" label="最近查单" width="170" />
      <el-table-column prop="syncMessage" label="同步说明" min-width="180" />
      <el-table-column label="操作" fixed="right" width="170">
        <template #default="{ row }">
          <el-button
            text
            type="primary"
            :icon="View"
            @click="openDetail(row.id)"
          >
            详情
          </el-button>
          <el-button
            v-if="canSync"
            text
            type="primary"
            :icon="Refresh"
            @click="handleSync(row)"
          >
            同步
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

    <el-drawer v-model="detailVisible" title="支付单详情" size="720px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="支付单号">
              {{ detail.payOrder.paySn }}
            </el-descriptions-item>
            <el-descriptions-item label="订单号">
              {{ detail.order.orderSn }}
            </el-descriptions-item>
            <el-descriptions-item label="本地状态">
              {{ detail.payOrder.statusText }}
            </el-descriptions-item>
            <el-descriptions-item label="微信状态">
              {{ detail.payOrder.wechatTradeState || "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="支付金额">
              {{ money(detail.payOrder.amount) }}
            </el-descriptions-item>
            <el-descriptions-item label="微信金额">
              {{ detail.payOrder.wechatAmount ? money(detail.payOrder.wechatAmount) : "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="渠道交易号" :span="2">
              {{ detail.payOrder.channelTradeNo || "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="最近查单" :span="2">
              {{ detail.payOrder.lastQueryTime || "—" }}
            </el-descriptions-item>
            <el-descriptions-item label="同步说明" :span="2">
              {{ detail.payOrder.syncMessage || "—" }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="detail-actions">
            <el-button
              v-if="canSync"
              type="primary"
              :icon="Refresh"
              @click="handleSync(detail.payOrder)"
            >
              人工同步
            </el-button>
          </div>

          <h4>异常处理记录</h4>
          <el-table :data="detail.exceptions" border size="small">
            <el-table-column prop="reasonCode" label="异常编码" min-width="180" />
            <el-table-column prop="reason" label="原因" min-width="220" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="handledType(row.handled)">
                  {{ row.handled === 1 ? "已处理" : "待处理" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="handleRemark" label="处理备注" min-width="180" />
            <el-table-column prop="handleTime" label="处理时间" width="170" />
          </el-table>

          <h4>支付回调记录</h4>
          <el-table :data="detail.notifyLogs" border size="small">
            <el-table-column prop="notificationId" label="通知ID" min-width="180" />
            <el-table-column prop="eventType" label="事件" min-width="150" />
            <el-table-column prop="channelTradeNo" label="渠道交易号" min-width="170" />
            <el-table-column prop="statusText" label="状态" width="100" />
            <el-table-column prop="message" label="说明" min-width="180" />
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.payment-page {
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

.reason-select {
  width: 220px;
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
  justify-content: flex-end;
  margin: 12px 0 18px;
}

h4 {
  margin: 18px 0 10px;
  font-size: 15px;
  font-weight: 600;
}
</style>
