<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Refresh, Search } from "@element-plus/icons-vue";
import { getObservabilitySummary, getOrderTrace } from "@/api/observability";
import type {
  MetricItem,
  ObservabilitySummary,
  OrderTrace,
  TraceItem
} from "@/api/observability";

defineOptions({ name: "TradeObservability" });

const loading = ref(false);
const traceLoading = ref(false);
const summary = ref<ObservabilitySummary>({
  health: [],
  metrics: [],
  alerts: [],
  jobs: []
});
const trace = ref<OrderTrace | null>(null);
const query = reactive({
  orderSn: ""
});

const allTraceLogs = computed<TraceItem[]>(() => {
  if (!trace.value) return [];
  return [
    ...trace.value.tradeLogs,
    ...trace.value.payLogs,
    ...trace.value.auditLogs
  ].sort((a, b) => a.time.localeCompare(b.time));
});

async function fetchSummary() {
  loading.value = true;
  try {
    summary.value = await getObservabilitySummary(
      query.orderSn.trim() ? { orderSn: query.orderSn.trim() } : {}
    );
    trace.value = summary.value.orderTrace ?? null;
  } finally {
    loading.value = false;
  }
}

async function searchTrace() {
  const orderSn = query.orderSn.trim();
  if (!orderSn) {
    ElMessage.warning("请输入订单号");
    return;
  }
  traceLoading.value = true;
  try {
    trace.value = await getOrderTrace(orderSn);
    if (!trace.value) {
      ElMessage.warning("未找到该订单链路");
    }
  } finally {
    traceLoading.value = false;
  }
}

function healthType(status: string) {
  return status === "UP" ? "success" : "danger";
}

function levelType(level: string) {
  const map: Record<string, "danger" | "warning" | "info" | "success"> = {
    CRITICAL: "danger",
    WARN: "warning",
    INFO: "info"
  };
  return map[level] ?? "info";
}

function jobType(status: string) {
  return status === "SUCCESS" ? "success" : status === "FAIL" ? "danger" : "info";
}

function metricType(item: MetricItem) {
  return item.value > 0 && item.level === "WARN" ? "warning" : "info";
}

onMounted(fetchSummary);
</script>

<template>
  <div class="observability-page" v-loading="loading">
    <div class="toolbar">
      <el-form :inline="true" :model="query">
        <el-form-item label="订单号">
          <el-input v-model="query.orderSn" clearable class="order-search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchTrace">追踪</el-button>
          <el-button :icon="Refresh" @click="fetchSummary">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="health-row">
      <div v-for="item in summary.health" :key="item.component" class="health-item">
        <div class="health-main">
          <span class="health-name">{{ item.component }}</span>
          <el-tag :type="healthType(item.status)" size="small">{{ item.status }}</el-tag>
        </div>
        <div class="muted-text">{{ item.message }}</div>
        <div class="muted-text">{{ item.lastCheckTime }}</div>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="item in summary.metrics" :key="item.code" class="metric-item">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">
          {{ item.value }}
          <span>{{ item.unit }}</span>
        </div>
        <el-tag :type="metricType(item)" size="small">{{ item.level }}</el-tag>
      </div>
    </div>

    <section class="section-block">
      <div class="section-title">
        <h3>当前告警</h3>
        <span class="muted-text">{{ summary.alerts.length }} 条</span>
      </div>
      <el-table :data="summary.alerts" border>
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="message" label="说明" min-width="280" show-overflow-tooltip />
        <el-table-column prop="currentValue" label="当前值" width="90" />
        <el-table-column prop="thresholdValue" label="阈值" width="90" />
        <el-table-column prop="triggerCount" label="次数" width="80" />
        <el-table-column prop="lastTriggerTime" label="最近触发" width="170" />
      </el-table>
    </section>

    <section class="section-block">
      <div class="section-title">
        <h3>定时任务</h3>
      </div>
      <el-table :data="summary.jobs" border>
        <el-table-column prop="jobName" label="任务" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="jobType(row.lastStatus)">{{ row.lastStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="processedCount" label="处理数" width="90" />
        <el-table-column prop="successCount" label="成功" width="80" />
        <el-table-column prop="failureCount" label="失败" width="80" />
        <el-table-column prop="consecutiveFailures" label="连续失败" width="100" />
        <el-table-column prop="lastMessage" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="lastRunTime" label="最近执行" width="170" />
      </el-table>
    </section>

    <section class="section-block" v-loading="traceLoading">
      <div class="section-title">
        <h3>订单链路</h3>
        <span v-if="trace" class="muted-text">
          {{ trace.orderSn }} / 用户 {{ trace.userId }}
        </span>
      </div>
      <el-descriptions v-if="trace" :column="4" border class="trace-summary">
        <el-descriptions-item label="订单ID">{{ trace.orderId }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ trace.orderStatus }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ trace.payStatus }}</el-descriptions-item>
        <el-descriptions-item label="支付单号">{{ trace.paySn || "—" }}</el-descriptions-item>
        <el-descriptions-item label="售后单号">{{ trace.afterSaleSn || "—" }}</el-descriptions-item>
        <el-descriptions-item label="退款单号">{{ trace.providerRefundNo || "—" }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="allTraceLogs" border>
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="ref" label="关联" min-width="170" />
        <el-table-column prop="message" label="说明" min-width="300" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.observability-page {
  padding: 16px;
}

.toolbar {
  margin-bottom: 12px;
}

.order-search {
  width: 260px;
}

.health-row,
.metric-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.health-row {
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.metric-grid {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.health-item,
.metric-item {
  min-height: 86px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}

.health-main,
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.health-name,
.metric-label {
  font-size: 14px;
  font-weight: 600;
}

.metric-value {
  margin: 8px 0;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.metric-value span {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.muted-text {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.section-block {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 10px;
}

.section-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.trace-summary {
  margin-bottom: 12px;
}
</style>
