<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Check, Close, Refresh, Search, View } from "@element-plus/icons-vue";
import {
  approveAfterSale,
  getAfterSalePage,
  receiveAfterSale,
  rejectAfterSale,
  syncAfterSale
} from "@/api/afterSale";
import type { AfterSale } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "AfterSaleList" });

const statusTabs = [
  { label: "全部", value: "all" },
  { label: "待审核", value: "0" },
  { label: "退款处理中", value: "4" },
  { label: "退款失败", value: "5" },
  { label: "待买家寄回", value: "6" },
  { label: "待商家收货", value: "7" },
  { label: "已退款", value: "1" },
  { label: "已拒绝", value: "2" },
  { label: "已撤销", value: "3" }
];
const canProcess = hasAnyPerms(["trade:manage", "trade:after-sale-process"]);

const loading = ref(false);
const tableData = ref<AfterSale[]>([]);
const total = ref(0);
const activeStatus = ref("all");
const query = reactive({
  page: 1,
  size: 10,
  orderId: "",
  userId: ""
});

async function fetchData() {
  loading.value = true;
  try {
    const params: Parameters<typeof getAfterSalePage>[0] = {
      page: query.page,
      size: query.size
    };
    if (activeStatus.value !== "all") {
      params.status = Number(activeStatus.value);
    }
    if (query.orderId.trim()) {
      params.orderId = Number(query.orderId.trim());
    }
    if (query.userId.trim()) {
      params.userId = Number(query.userId.trim());
    }
    const result = await getAfterSalePage(params);
    tableData.value = result.list ?? [];
    total.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleTabChange() {
  query.page = 1;
  fetchData();
}

function handleReset() {
  activeStatus.value = "all";
  query.orderId = "";
  query.userId = "";
  query.page = 1;
  fetchData();
}

function handlePageChange(page: number) {
  query.page = page;
  fetchData();
}

function money(value?: string | number) {
  const amount = Number(value ?? 0);
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : "—";
}

function statusType(status?: number) {
  const types: Record<
    number,
    "primary" | "warning" | "success" | "danger" | "info"
  > = {
    0: "warning",
    1: "success",
    2: "danger",
    3: "info",
    4: "primary",
    5: "danger",
    6: "warning",
    7: "primary"
  };
  return types[status ?? -1] ?? "info";
}

const detailVisible = ref(false);
const detail = ref<AfterSale | null>(null);
const syncSaving = ref(false);

function openDetail(row: AfterSale) {
  detail.value = row;
  detailVisible.value = true;
}

async function handleApprove(row: AfterSale) {
  await ElMessageBox.confirm(
    row.type === 2
      ? `确认审核通过售后单 ${row.afterSaleSn}？通过后等待买家寄回，收货前不会退款。`
      : `确认同意售后单 ${row.afterSaleSn} 的退款申请？退款金额为 ${money(row.refundAmount)}。`,
    row.type === 2 ? "确认通过退货申请" : "确认同意退款",
    {
      type: "warning",
      confirmButtonText: row.type === 2 ? "审核通过" : "同意退款",
      cancelButtonText: "取消"
    }
  );
  const result = await approveAfterSale(row.id);
  if (result.status === 5) {
    ElMessage.warning("退款失败，订单状态已恢复");
  } else {
    ElMessage.success(
      result.status === 4 ? "退款请求已提交渠道处理" : "退款已完成"
    );
  }
  if (detail.value?.id === row.id) detailVisible.value = false;
  await fetchData();
}

async function handleReceive(row: AfterSale) {
  const { value } = await ElMessageBox.prompt(
    `请确认已收到售后单 ${row.afterSaleSn} 的退货商品。确认后将发起 ${money(row.refundAmount)} 退款。`,
    "确认收货并退款",
    {
      confirmButtonText: "确认收货",
      cancelButtonText: "取消",
      inputPlaceholder: "质检说明（可选）",
      inputValidator: input =>
        !input || input.length <= 255 || "质检说明不能超过 255 个字符"
    }
  );
  const result = await receiveAfterSale(row.id, value || "");
  ElMessage.success(
    result.status === 4 ? "已收货，退款渠道处理中" : "已收货并完成退款"
  );
  if (detail.value?.id === row.id) detailVisible.value = false;
  await fetchData();
}

async function handleSync(row: AfterSale) {
  syncSaving.value = true;
  try {
    const result = await syncAfterSale(row.id);
    if (result.status === 5) {
      ElMessage.warning("退款失败，订单状态已恢复");
    } else {
      ElMessage.success(
        result.status === 1 ? "退款状态已同步完成" : "退款渠道仍在处理中"
      );
    }
    if (detail.value?.id === row.id) detail.value = result;
    await fetchData();
  } finally {
    syncSaving.value = false;
  }
}

const rejectVisible = ref(false);
const rejectSaving = ref(false);
const rejectFormRef = ref<FormInstance>();
const rejectTarget = ref<AfterSale | null>(null);
const rejectForm = reactive({ reason: "" });
const rejectRules: FormRules = {
  reason: [
    { required: true, message: "请填写拒绝原因", trigger: "blur" },
    {
      min: 4,
      max: 200,
      message: "拒绝原因长度应为 4 至 200 个字符",
      trigger: "blur"
    }
  ]
};

function openReject(row: AfterSale) {
  rejectTarget.value = row;
  rejectForm.reason = "";
  rejectVisible.value = true;
}

async function submitReject() {
  await rejectFormRef.value?.validate();
  if (!rejectTarget.value) return;
  rejectSaving.value = true;
  try {
    await rejectAfterSale(rejectTarget.value.id, rejectForm.reason.trim());
    ElMessage.success("售后申请已拒绝");
    if (detail.value?.id === rejectTarget.value.id) detailVisible.value = false;
    rejectVisible.value = false;
    await fetchData();
  } finally {
    rejectSaving.value = false;
  }
}

onMounted(fetchData);

function refundDescription(row: AfterSale) {
  return [
    row.refundMessage,
    row.providerRefundNo ? `渠道退款单号：${row.providerRefundNo}` : "",
    row.refundAttemptCount ? `自动处理次数：${row.refundAttemptCount}` : "",
    row.refundNextAttemptTime ? `下次重试：${row.refundNextAttemptTime}` : "",
    row.refundLastError ? `最近错误：${row.refundLastError}` : "",
    row.refundTime ? `退款时间：${row.refundTime}` : ""
  ]
    .filter(Boolean)
    .join("；");
}
</script>

<template>
  <div class="app-container after-sale-page">
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <div>
          <strong>售后状态</strong>
          <span>仅“待审核”的申请可执行同意或拒绝</span>
        </div>
        <el-form :inline="true" class="filter-form" @submit.prevent="fetchData">
          <el-form-item label="订单 ID">
            <el-input
              v-model="query.orderId"
              placeholder="输入订单 ID"
              clearable
              class="filter-input"
              @keyup.enter="handleTabChange"
            />
          </el-form-item>
          <el-form-item label="用户 ID">
            <el-input
              v-model="query.userId"
              placeholder="输入用户 ID"
              clearable
              class="filter-input"
              @keyup.enter="handleTabChange"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleTabChange"
              >搜索</el-button
            >
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="tab in statusTabs"
          :key="tab.value"
          :label="tab.label"
          :name="tab.value"
        />
      </el-tabs>

      <div class="table-toolbar">共 {{ total }} 笔售后申请</div>
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        class="after-sale-table"
      >
        <el-table-column
          prop="afterSaleSn"
          label="售后单号"
          min-width="190"
          fixed="left"
        />
        <el-table-column label="关联订单" min-width="190">
          <template #default="{ row }">{{
            row.orderSn || `订单 #${row.orderId}`
          }}</template>
        </el-table-column>
        <el-table-column label="用户" width="120" align="center">
          <template #default="{ row }">用户 #{{ row.userId }}</template>
        </el-table-column>
        <el-table-column label="售后类型" width="110" align="center">
          <template #default="{ row }">{{
            row.typeText || (row.type === 1 ? "仅退款" : "退货退款")
          }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="120" align="right">
          <template #default="{ row }"
            ><strong class="amount-text">{{
              money(row.refundAmount)
            }}</strong></template
          >
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="applyTime"
          label="申请时间"
          width="170"
          align="center"
        />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)"
              >详情</el-button
            >
            <el-button
              v-if="canProcess && row.status === 0"
              type="success"
              link
              :icon="Check"
              @click="handleApprove(row)"
              >同意</el-button
            >
            <el-button
              v-if="canProcess && row.status === 0"
              type="danger"
              link
              :icon="Close"
              @click="openReject(row)"
              >拒绝</el-button
            >
            <el-button
              v-if="canProcess && row.status === 7"
              type="success"
              link
              :icon="Check"
              @click="handleReceive(row)"
              >确认收货</el-button
            >
            <el-button
              v-if="canProcess && row.status === 4"
              type="primary"
              link
              :loading="syncSaving"
              :icon="Refresh"
              @click="handleSync(row)"
              >同步</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handleReset"
        />
      </div>
    </el-card>

    <el-drawer
      v-model="detailVisible"
      title="售后详情"
      size="min(560px, 96vw)"
      destroy-on-close
    >
      <template v-if="detail">
        <div class="detail-heading">
          <div>
            <span>售后单号</span>
            <strong>{{ detail.afterSaleSn }}</strong>
          </div>
          <el-tag :type="statusType(detail.status)" size="large">{{
            detail.statusText
          }}</el-tag>
        </div>

        <section class="detail-section">
          <h4>申请信息</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="关联订单">{{
              detail.orderSn || `订单 #${detail.orderId}`
            }}</el-descriptions-item>
            <el-descriptions-item label="申请用户"
              >用户 #{{ detail.userId }}</el-descriptions-item
            >
            <el-descriptions-item label="售后类型">{{
              detail.typeText || (detail.type === 1 ? "仅退款" : "退货退款")
            }}</el-descriptions-item>
            <el-descriptions-item label="退款金额"
              ><strong class="amount-text">{{
                money(detail.refundAmount)
              }}</strong></el-descriptions-item
            >
            <el-descriptions-item label="申请时间">{{
              detail.applyTime || "—"
            }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section v-if="detail.items?.length" class="detail-section">
          <h4>售后商品</h4>
          <el-table :data="detail.items" border size="small">
            <el-table-column prop="goodsName" label="商品" min-width="160" />
            <el-table-column prop="specName" label="规格" min-width="100" />
            <el-table-column prop="applyCount" label="数量" width="70" align="center" />
            <el-table-column prop="refundAmount" label="退款金额" width="110" align="right">
              <template #default="{ row }">{{ money(row.refundAmount) }}</template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="detail.returnNo || detail.status === 6" class="detail-section">
          <h4>退货履约</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="寄回期限">{{ detail.returnDeadline || "—" }}</el-descriptions-item>
            <el-descriptions-item label="退货物流">{{ detail.returnNo ? `${detail.returnCompany} ${detail.returnNo}` : "等待买家寄回" }}</el-descriptions-item>
            <el-descriptions-item label="寄回时间">{{ detail.returnTime || "—" }}</el-descriptions-item>
            <el-descriptions-item label="收货时间">{{ detail.receiveTime || "—" }}</el-descriptions-item>
            <el-descriptions-item label="质检说明">{{ detail.receiveRemark || "—" }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <h4>申请说明</h4>
          <dl class="reason-list">
            <div>
              <dt>申请原因</dt>
              <dd>{{ detail.reason || "未填写" }}</dd>
            </div>
            <div>
              <dt>补充说明</dt>
              <dd>{{ detail.applyRemark || "未填写" }}</dd>
            </div>
          </dl>
        </section>

        <section
          v-if="detail.status === 2"
          class="detail-section reject-section"
        >
          <h4>拒绝信息</h4>
          <dl class="reason-list">
            <div>
              <dt>拒绝原因</dt>
              <dd>{{ detail.rejectReason || "—" }}</dd>
            </div>
            <div>
              <dt>拒绝时间</dt>
              <dd>{{ detail.rejectTime || "—" }}</dd>
            </div>
          </dl>
        </section>

        <section
          v-if="detail.status === 1 || detail.status === 4"
          class="detail-section"
        >
          <h4>退款结果</h4>
          <el-alert
            :title="detail.status === 1 ? '退款已完成' : '退款渠道处理中'"
            :description="
              refundDescription(detail) ||
              `审核时间：${detail.auditTime || '—'}`
            "
            :type="detail.status === 1 ? 'success' : 'info'"
            :closable="false"
            show-icon
          />
        </section>

        <div v-if="canProcess && detail.status === 0" class="drawer-actions">
          <el-button type="danger" :icon="Close" @click="openReject(detail)"
            >拒绝退款</el-button
          >
          <el-button type="success" :icon="Check" @click="handleApprove(detail)"
            >同意退款</el-button
          >
        </div>
        <div v-if="canProcess && detail.status === 7" class="drawer-actions">
          <el-button type="success" :icon="Check" @click="handleReceive(detail)">确认收货并退款</el-button>
        </div>
        <div v-else-if="canProcess && detail.status === 4" class="drawer-actions">
          <el-button
            type="primary"
            :icon="Refresh"
            :loading="syncSaving"
            @click="handleSync(detail)"
            >同步退款状态</el-button
          >
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="rejectVisible"
      title="拒绝退款申请"
      width="min(520px, 92vw)"
      destroy-on-close
    >
      <el-alert
        v-if="rejectTarget"
        :title="`售后单号：${rejectTarget.afterSaleSn}`"
        :description="`退款金额：${money(rejectTarget.refundAmount)}`"
        type="warning"
        :closable="false"
        class="dialog-alert"
      />
      <el-form
        ref="rejectFormRef"
        :model="rejectForm"
        :rules="rejectRules"
        label-width="90px"
      >
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请说明拒绝退款的具体原因，该内容将展示给用户"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSaving" @click="submitReject"
          >确认拒绝</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.after-sale-page {
  min-width: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row,
.detail-heading,
.drawer-actions {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-input {
  width: 140px;
}

.filter-row > div,
.detail-heading > div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.filter-row span,
.detail-heading span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.table-toolbar {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 4px 0 12px;
}

.after-sale-table {
  width: 100%;
}

.amount-text {
  color: var(--el-color-danger);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.detail-heading {
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
  margin-bottom: 24px;
  padding: 16px;
}

.detail-section {
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 24px;
  padding-bottom: 24px;
}

.detail-section h4 {
  font-size: 15px;
  margin: 0 0 14px;
}

.reason-list {
  margin: 0;
}

.reason-list > div {
  display: grid;
  gap: 16px;
  grid-template-columns: 82px 1fr;
  padding: 10px 0;
}

.reason-list dt {
  color: var(--el-text-color-secondary);
}

.reason-list dd {
  line-height: 1.6;
  margin: 0;
  overflow-wrap: anywhere;
}

.reject-section {
  border-left: 3px solid var(--el-color-danger);
  padding-left: 14px;
}

.drawer-actions {
  border-top: 1px solid var(--el-border-color);
  justify-content: flex-end;
  padding-top: 18px;
}

.dialog-alert {
  margin-bottom: 20px;
}

@media (max-width: 640px) {
  .filter-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .filter-form,
  .filter-form :deep(.el-form-item),
  .filter-input {
    width: 100%;
  }

  .reason-list > div {
    gap: 6px;
    grid-template-columns: 1fr;
  }
}
</style>
