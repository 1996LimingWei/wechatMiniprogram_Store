<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import dayjs from "dayjs";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Download, Printer, Refresh, Search, Upload, Van, View } from "@element-plus/icons-vue";
import {
  downloadBatchShipTemplate,
  exportOrders,
  getDeliveryNote,
  getOrderDetail,
  getOrderLogistics,
  getOrderPage,
  getPickingList,
  importBatchShip,
  shipOrder,
  updateOrderRemark
} from "@/api/order";
import type {
  BatchShipResult,
  DeliveryNote,
  LogisticsTrace,
  PickingList,
  TradeLogistics,
  TradeOrder,
  TradeOrderDetail,
  TradeOrderItem
} from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "OrderList" });

const statusTabs = [
  { label: "全部", value: "all" },
  { label: "待发货", value: "1" },
  { label: "待收货", value: "2" },
  { label: "已完成", value: "3" },
  { label: "已取消", value: "4" },
  { label: "退款中", value: "5" }
];

const loading = ref(false);
const tableData = ref<TradeOrder[]>([]);
const total = ref(0);
const activeStatus = ref("all");
const createTimeRange = ref<[Date, Date] | null>(null);
const query = reactive({
  page: 1,
  size: 10,
  orderSn: "",
  userId: "",
  mobile: "",
  payStatus: "all"
});

function buildQueryParams() {
  const params: Parameters<typeof getOrderPage>[0] = {
    page: query.page,
    size: query.size
  };
    if (activeStatus.value !== "all") {
      params.status = Number(activeStatus.value);
    }
    if (query.orderSn.trim()) {
      params.orderSn = query.orderSn.trim();
    }
    if (query.userId.trim()) {
      params.userId = Number(query.userId.trim());
    }
    if (query.mobile.trim()) {
      params.mobile = query.mobile.trim();
    }
    if (query.payStatus !== "all") {
      params.payStatus = Number(query.payStatus);
    }
    if (createTimeRange.value) {
      params.createTimeStart = dayjs(createTimeRange.value[0]).format(
        "YYYY-MM-DD HH:mm:ss"
      );
      params.createTimeEnd = dayjs(createTimeRange.value[1]).format(
        "YYYY-MM-DD HH:mm:ss"
      );
    }
  return params;
}

async function fetchData() {
  loading.value = true;
  try {
    const params = buildQueryParams();
    const result = await getOrderPage(params);
    tableData.value = result.list ?? [];
    total.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  fetchData();
}

function handleReset() {
  query.orderSn = "";
  query.userId = "";
  query.mobile = "";
  query.payStatus = "all";
  createTimeRange.value = null;
  activeStatus.value = "all";
  query.page = 1;
  fetchData();
}

function handleTabChange() {
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

function orderStatusType(status?: number, payStatus?: number) {
  if (payStatus === 2) return "success";
  const types: Record<
    number,
    "primary" | "success" | "warning" | "info" | "danger"
  > = {
    0: "warning",
    1: "primary",
    2: "warning",
    3: "success",
    4: "info",
    5: "danger"
  };
  return types[status ?? -1] ?? "info";
}

function goodsSummary(goods: TradeOrderItem[] = []) {
  if (!goods.length) return "暂无商品";
  const first = goods[0];
  const totalCount = goods.reduce((sum, item) => sum + (item.number || 0), 0);
  return `${first.goodsName}${goods.length > 1 ? ` 等 ${goods.length} 种` : ""}，共 ${totalCount} 件`;
}

const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<TradeOrderDetail | null>(null);
const adminRemark = ref("");
const remarkSaving = ref(false);

async function loadDetail(orderId: number) {
  detailLoading.value = true;
  try {
    detail.value = await getOrderDetail(orderId);
    adminRemark.value = detail.value.orderInfo.adminRemark || "";
  } finally {
    detailLoading.value = false;
  }
}

async function openDetail(row: TradeOrder) {
  detailVisible.value = true;
  detail.value = null;
  await loadDetail(row.id);
}

const detailOrder = computed(() => detail.value?.orderInfo);
const detailGoods = computed(() => detail.value?.orderGoods ?? []);
const selectedRows = ref<TradeOrder[]>([]);

function goodsSubtotal(item: TradeOrderItem) {
  return money(Number(item.retailPrice || 0) * (item.number || 0));
}

const shipVisible = ref(false);
const shipSaving = ref(false);
const shipFormRef = ref<FormInstance>();
const shipForm = reactive({
  orderId: 0,
  orderSn: "",
  logisticsCompany: "顺丰速运",
  logisticsCode: "shunfeng",
  logisticsNo: ""
});
const shipRules: FormRules = {
  logisticsCode: [
    { required: true, message: "请选择物流公司", trigger: "change" }
  ],
  logisticsNo: [
    { required: true, message: "请输入物流单号", trigger: "blur" },
    {
      min: 6,
      max: 32,
      message: "物流单号长度应为 6 至 32 个字符",
      trigger: "blur"
    }
  ]
};
const logisticsCompanies = [
  { name: "顺丰速运", code: "shunfeng" },
  { name: "中通快递", code: "zhongtong" },
  { name: "圆通速递", code: "yuantong" },
  { name: "韵达快递", code: "yunda" },
  { name: "极兔速递", code: "jtexpress" },
  { name: "申通快递", code: "shentong" },
  { name: "京东物流", code: "jd" },
  { name: "邮政 EMS", code: "ems" }
];
const canShip = hasAnyPerms(["trade:manage", "trade:order-ship"]);
const canReadLogistics = hasAnyPerms(["trade:manage", "trade:logistics-read"]);
const canExportOrder = hasAnyPerms(["trade:manage", "trade:order-export"]);
const canBatchShip = hasAnyPerms(["trade:manage", "trade:order-batch-ship"]);
const canRemark = hasAnyPerms(["trade:manage", "trade:order-remark"]);
const canPrintDelivery = hasAnyPerms(["trade:manage", "trade:order-delivery-note"]);
const canPrintPicking = hasAnyPerms(["trade:manage", "trade:order-picking-list"]);

function handleLogisticsCompanyChange(code: string) {
  const company = logisticsCompanies.find(item => item.code === code);
  shipForm.logisticsCompany = company?.name ?? "";
}

function openShip(row: TradeOrder) {
  shipForm.orderId = row.id;
  shipForm.orderSn = row.orderSn;
  shipForm.logisticsCompany = "顺丰速运";
  shipForm.logisticsCode = "shunfeng";
  shipForm.logisticsNo = "";
  shipVisible.value = true;
}

function handleSelectionChange(rows: TradeOrder[]) {
  selectedRows.value = rows;
}

async function submitShip() {
  await shipFormRef.value?.validate();
  await ElMessageBox.confirm(
    `确认订单 ${shipForm.orderSn} 已交由${shipForm.logisticsCompany}发出？`,
    "确认发货",
    { type: "warning", confirmButtonText: "确认发货" }
  );
  shipSaving.value = true;
  try {
    await shipOrder({
      orderId: shipForm.orderId,
      logisticsCompany: shipForm.logisticsCompany,
      logisticsCode: shipForm.logisticsCode,
      logisticsNo: shipForm.logisticsNo.trim()
    });
    ElMessage.success("发货成功，订单已变为待收货");
    shipVisible.value = false;
    await fetchData();
    if (detailVisible.value && detailOrder.value?.id === shipForm.orderId) {
      await loadDetail(shipForm.orderId);
    }
  } finally {
    shipSaving.value = false;
  }
}

const logisticsVisible = ref(false);
const logisticsLoading = ref(false);
const logistics = ref<TradeLogistics | null>(null);
const logisticsOrderSn = ref("");

async function openLogistics(row: TradeOrder) {
  logisticsVisible.value = true;
  logisticsLoading.value = true;
  logistics.value = null;
  logisticsOrderSn.value = row.orderSn;
  try {
    logistics.value = await getOrderLogistics(row.id);
  } finally {
    logisticsLoading.value = false;
  }
}

function traceType(_trace: LogisticsTrace, index: number) {
  return index === 0 ? "primary" : "info";
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

async function handleExport() {
  const blob = await exportOrders(buildQueryParams());
  downloadBlob(blob, `订单导出-${dayjs().format("YYYYMMDDHHmmss")}.csv`);
}

async function handleDownloadTemplate() {
  const blob = await downloadBatchShipTemplate();
  downloadBlob(blob, "批量发货模板.csv");
}

const batchVisible = ref(false);
const batchSubmitting = ref(false);
const batchContent = ref("");
const batchFileName = ref("");
const batchResult = ref<BatchShipResult | null>(null);

function openBatchShip() {
  batchVisible.value = true;
  batchContent.value = "";
  batchFileName.value = "";
  batchResult.value = null;
}

function handleBatchFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  batchFileName.value = file.name;
  const reader = new FileReader();
  reader.onload = () => {
    batchContent.value = String(reader.result || "");
  };
  reader.readAsText(file, "utf-8");
}

async function submitBatchShip(dryRun = false) {
  if (!batchContent.value.trim()) {
    ElMessage.warning("请先选择批量发货 CSV 文件");
    return;
  }
  batchSubmitting.value = true;
  try {
    batchResult.value = await importBatchShip({
      content: batchContent.value,
      dryRun
    });
    ElMessage.success(dryRun ? "预校验完成" : "批量发货完成");
    if (!dryRun) await fetchData();
  } finally {
    batchSubmitting.value = false;
  }
}

async function saveRemark() {
  if (!detailOrder.value) return;
  remarkSaving.value = true;
  try {
    await updateOrderRemark({
      orderId: detailOrder.value.id,
      remark: adminRemark.value
    });
    ElMessage.success("内部备注已保存");
    await loadDetail(detailOrder.value.id);
  } finally {
    remarkSaving.value = false;
  }
}

function openPrintWindow(title: string, html: string) {
  const win = window.open("", "_blank", "width=960,height=720");
  if (!win) return;
  win.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>${title}</title><style>
    body{font-family:Arial,"Microsoft YaHei",sans-serif;color:#1f2937;padding:24px}
    h1{font-size:22px;margin:0 0 16px}.meta{display:grid;grid-template-columns:120px 1fr;gap:8px 14px;margin-bottom:18px}
    table{width:100%;border-collapse:collapse}th,td{border:1px solid #d1d5db;padding:8px;font-size:13px;text-align:left}
    th{background:#f3f4f6}.right{text-align:right}.remark{margin-top:16px;white-space:pre-wrap}
    @media print{button{display:none}body{padding:0}}
  </style></head><body><button onclick="window.print()">打印</button>${html}</body></html>`);
  win.document.close();
}

function escapeHtml(value?: string | number) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

async function printDelivery(row?: TradeOrder) {
  const orderId = row?.id || detailOrder.value?.id;
  if (!orderId) return;
  const note = await getDeliveryNote(orderId);
  openPrintWindow("发货单", deliveryNoteHtml(note));
}

async function printPicking() {
  const ids = selectedRows.value.map(item => item.id);
  if (!ids.length) {
    ElMessage.warning("请先勾选要打印拣货单的订单");
    return;
  }
  const list = await getPickingList(ids);
  openPrintWindow("拣货单", pickingListHtml(list));
}

function deliveryNoteHtml(note: DeliveryNote) {
  const rows = note.items.map(item => `<tr><td>${escapeHtml(item.goodsName)}</td><td>${escapeHtml(item.specName || "默认规格")}</td><td>${escapeHtml(item.skuId)}</td><td class="right">${escapeHtml(item.count)}</td><td class="right">¥${escapeHtml(item.retailPrice)}</td><td class="right">¥${escapeHtml(item.totalPrice)}</td></tr>`).join("");
  return `<h1>发货单</h1><div class="meta"><strong>订单号</strong><span>${escapeHtml(note.orderSn)}</span><strong>收货人</strong><span>${escapeHtml(note.consignee)} ${escapeHtml(note.mobile)}</span><strong>收货地址</strong><span>${escapeHtml(note.fullAddress)}</span><strong>物流</strong><span>${escapeHtml(note.logisticsCompany || "")} ${escapeHtml(note.logisticsNo || "")}</span><strong>实付金额</strong><span>¥${escapeHtml(note.actualPrice)}</span></div><table><thead><tr><th>商品</th><th>规格</th><th>SKU</th><th>数量</th><th>单价</th><th>小计</th></tr></thead><tbody>${rows}</tbody></table><div class="remark"><strong>内部备注：</strong>${escapeHtml(note.adminRemark || "")}</div>`;
}

function pickingListHtml(list: PickingList) {
  const rows = list.items.map(item => `<tr><td>${escapeHtml(item.goodsName)}</td><td>${escapeHtml(item.specName || "默认规格")}</td><td>${escapeHtml(item.skuId)}</td><td class="right">${escapeHtml(item.count)}</td><td>${escapeHtml(item.orderSns.join(", "))}</td></tr>`).join("");
  return `<h1>拣货单</h1><div class="meta"><strong>订单数</strong><span>${escapeHtml(list.orderCount)}</span><strong>商品件数</strong><span>${escapeHtml(list.itemCount)}</span></div><table><thead><tr><th>商品</th><th>规格</th><th>SKU</th><th>应拣数量</th><th>订单号</th></tr></thead><tbody>${rows}</tbody></table>`;
}

onMounted(fetchData);
</script>

<template>
  <div class="app-container order-page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="订单号">
          <el-input
            v-model="query.orderSn"
            placeholder="输入完整订单号"
            clearable
            class="order-search"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input
            v-model="query.userId"
            placeholder="输入用户 ID"
            clearable
            class="compact-search"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="query.mobile"
            placeholder="输入手机号前缀"
            clearable
            class="mobile-search"
            maxlength="11"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="query.payStatus" class="compact-search">
            <el-option label="全部" value="all" />
            <el-option label="未支付" value="0" />
            <el-option label="已支付" value="1" />
            <el-option label="已退款" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="createTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="time-picker"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch"
            >搜索</el-button
          >
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button v-if="canExportOrder" :icon="Download" @click="handleExport">导出</el-button>
          <el-button v-if="canBatchShip" :icon="Upload" @click="openBatchShip"
            >批量发货</el-button
          >
        </el-form-item>
      </el-form>
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

      <div class="table-toolbar">
        <span>共 {{ total }} 笔订单</span>
        <el-button
          v-if="canPrintPicking"
          :icon="Printer"
          :disabled="!selectedRows.length"
          @click="printPicking"
          >打印拣货单</el-button
        >
      </div>
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        class="order-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" fixed="left" />
        <el-table-column
          prop="orderSn"
          label="订单号"
          min-width="190"
          fixed="left"
        />
        <el-table-column label="用户信息" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <strong>{{ row.consignee || `用户 #${row.userId}` }}</strong>
              <span>{{ row.mobile || `用户 ID：${row.userId}` }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品摘要" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">{{
            goodsSummary(row.goodsList)
          }}</template>
        </el-table-column>
        <el-table-column label="实付金额" width="120" align="right">
          <template #default="{ row }">
            <strong class="amount-text">{{ money(row.actualPrice) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="orderStatusType(row.status, row.payStatus)"
              effect="light"
            >
              {{ row.orderStatusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="addTime"
          label="下单时间"
          width="170"
          align="center"
        />
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)"
              >详情</el-button
            >
            <el-button
              v-if="canShip && row.handleOption?.ship"
              type="success"
              link
              :icon="Van"
              @click="openShip(row)"
              >发货</el-button
            >
            <el-button
              v-if="canReadLogistics && row.handleOption?.logistics"
              type="primary"
              link
              :icon="Van"
              @click="openLogistics(row)"
              >物流</el-button
            >
            <el-button
              v-if="canPrintDelivery"
              type="primary"
              link
              :icon="Printer"
              @click="printDelivery(row)"
              >发货单</el-button
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
          @size-change="handleSearch"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" size="min(920px, 96vw)" destroy-on-close>
      <template #header>
        <div class="drawer-header">
          <div>
            <h3>订单详情</h3>
            <span>{{ detailOrder?.orderSn }}</span>
          </div>
          <div v-if="detailOrder" class="drawer-actions">
            <el-button
              v-if="canShip && detail?.handleOption?.ship"
              type="success"
              :icon="Van"
              @click="openShip(detailOrder)"
              >发货</el-button
            >
            <el-button
              v-if="canReadLogistics && detail?.handleOption?.logistics"
              :icon="Van"
              @click="openLogistics(detailOrder)"
              >查看物流</el-button
            >
            <el-button v-if="canPrintDelivery" :icon="Printer" @click="printDelivery(detailOrder)"
              >打印发货单</el-button
            >
          </div>
        </div>
      </template>

      <div v-loading="detailLoading" class="drawer-content">
        <template v-if="detail && detailOrder">
          <section class="detail-section">
            <h4>基础信息</h4>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="订单状态">
                <el-tag
                  :type="
                    orderStatusType(detailOrder.status, detailOrder.payStatus)
                  "
                >
                  {{ detailOrder.orderStatusText }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="用户 ID">{{
                detailOrder.userId
              }}</el-descriptions-item>
              <el-descriptions-item label="下单时间">{{
                detailOrder.addTime || "—"
              }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{
                detailOrder.payTime || "—"
              }}</el-descriptions-item>
              <el-descriptions-item label="关闭时间">{{
                detailOrder.closeTime || "—"
              }}</el-descriptions-item>
              <el-descriptions-item label="关闭原因">{{
                detailOrder.closeReason || "—"
              }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-section">
            <h4>收货信息</h4>
            <div class="address-block">
              <strong>{{ detailOrder.consignee || "—" }}</strong>
              <span>{{ detailOrder.mobile || "—" }}</span>
              <p>{{ detailOrder.fullRegion }} {{ detailOrder.address }}</p>
            </div>
          </section>

          <section class="detail-section">
            <h4>商品明细</h4>
            <el-table :data="detailGoods" border>
              <el-table-column label="商品" min-width="260">
                <template #default="{ row }">
                  <div class="goods-cell">
                    <el-image
                      :src="row.listPicUrl"
                      fit="cover"
                      class="goods-image"
                    />
                    <div>
                      <strong>{{ row.goodsName }}</strong>
                      <span>{{
                        row.goodsSpecifitionNameValue || "默认规格"
                      }}</span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="单价" width="110" align="right">
                <template #default="{ row }">{{
                  money(row.retailPrice)
                }}</template>
              </el-table-column>
              <el-table-column
                prop="number"
                label="数量"
                width="80"
                align="center"
              />
              <el-table-column label="小计" width="120" align="right">
                <template #default="{ row }"
                  ><strong>{{ goodsSubtotal(row) }}</strong></template
                >
              </el-table-column>
            </el-table>
          </section>

          <section class="detail-section amount-section">
            <h4>金额汇总</h4>
            <div class="amount-grid">
              <span>商品金额</span
              ><strong>{{ money(detailOrder.goodsPrice) }}</strong>
              <span>运费</span
              ><strong>{{ money(detailOrder.freightPrice) }}</strong>
              <span>优惠</span
              ><strong>-{{ money(detailOrder.couponPrice) }}</strong>
              <span class="total-label">实付金额</span
              ><strong class="total-value">{{
                money(detailOrder.actualPrice)
              }}</strong>
            </div>
          </section>

          <section class="detail-section">
            <h4>支付信息</h4>
            <el-descriptions
              v-if="detail.payOrder.hasPayOrder"
              :column="3"
              border
            >
              <el-descriptions-item label="支付单号">{{
                detail.payOrder.paySn
              }}</el-descriptions-item>
              <el-descriptions-item label="支付渠道">{{
                detail.payOrder.channel || "—"
              }}</el-descriptions-item>
              <el-descriptions-item label="支付状态">{{
                detail.payOrder.statusText
              }}</el-descriptions-item>
              <el-descriptions-item label="支付金额">{{
                money(detail.payOrder.amount)
              }}</el-descriptions-item>
              <el-descriptions-item label="支付时间" :span="2">{{
                detail.payOrder.payTime || "—"
              }}</el-descriptions-item>
            </el-descriptions>
            <el-empty v-else description="暂无支付单" :image-size="64" />
          </section>

          <section v-if="canRemark" class="detail-section">
            <h4>内部备注</h4>
            <el-input
              v-model="adminRemark"
              type="textarea"
              maxlength="200"
              show-word-limit
              :rows="3"
              placeholder="客服内部备注，不会展示给小程序用户"
            />
            <div class="remark-actions">
              <el-button
                type="primary"
                :loading="remarkSaving"
                @click="saveRemark"
                >保存备注</el-button
              >
            </div>
          </section>

          <section class="detail-section">
            <h4>订单状态时间线</h4>
            <el-timeline class="order-timeline">
              <el-timeline-item
                v-for="log in detail.orderLogs"
                :key="log.id"
                :timestamp="log.createTime"
                placement="top"
              >
                <strong>{{ log.actionText || log.action }}</strong>
                <p>
                  {{
                    log.remark ||
                    `${log.fromStatusText || ""} → ${log.toStatusText || ""}`
                  }}
                </p>
              </el-timeline-item>
            </el-timeline>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog
      v-model="shipVisible"
      title="订单发货"
      width="min(520px, 92vw)"
      destroy-on-close
    >
      <el-alert
        :title="`订单号：${shipForm.orderSn}`"
        type="info"
        :closable="false"
        class="dialog-alert"
      />
      <el-form
        ref="shipFormRef"
        :model="shipForm"
        :rules="shipRules"
        label-width="90px"
      >
        <el-form-item label="物流公司" prop="logisticsCode">
          <el-select
            v-model="shipForm.logisticsCode"
            filterable
            style="width: 100%"
            @change="handleLogisticsCompanyChange"
          >
            <el-option
              v-for="company in logisticsCompanies"
              :key="company.code"
              :label="company.name"
              :value="company.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号" prop="logisticsNo">
          <el-input
            v-model="shipForm.logisticsNo"
            maxlength="32"
            show-word-limit
            placeholder="请输入物流单号"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSaving" @click="submitShip"
          >确认发货</el-button
        >
      </template>
    </el-dialog>

    <el-dialog
      v-model="logisticsVisible"
      title="物流详情"
      width="min(620px, 92vw)"
      destroy-on-close
    >
      <div v-loading="logisticsLoading" class="logistics-content">
        <template v-if="logistics?.hasLogistics">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{
              logisticsOrderSn
            }}</el-descriptions-item>
            <el-descriptions-item label="物流公司">{{
              logistics.logisticsCompany
            }}</el-descriptions-item>
            <el-descriptions-item label="物流单号">{{
              logistics.logisticsNo
            }}</el-descriptions-item>
            <el-descriptions-item label="发货时间">{{
              logistics.deliveryTime
            }}</el-descriptions-item>
          </el-descriptions>
          <el-timeline class="logistics-timeline">
            <el-timeline-item
              v-for="(trace, index) in logistics.traces"
              :key="`${trace.time}-${index}`"
              :timestamp="trace.time"
              :type="traceType(trace, index)"
              >{{ trace.text }}</el-timeline-item
            >
          </el-timeline>
          <el-alert
            v-if="logistics.queryMessage && !logistics.traces?.length"
            :title="logistics.queryMessage"
            type="warning"
            :closable="false"
            show-icon
          />
        </template>
        <el-empty v-else description="该订单暂无物流信息" :image-size="72" />
      </div>
    </el-dialog>

    <el-dialog
      v-model="batchVisible"
      title="批量发货"
      width="min(760px, 94vw)"
      destroy-on-close
    >
      <div class="batch-tools">
        <el-button :icon="Download" @click="handleDownloadTemplate"
          >下载模板</el-button
        >
        <label class="file-button">
          <input type="file" accept=".csv,text/csv" @change="handleBatchFileChange" />
          <span>{{ batchFileName || "选择 CSV 文件" }}</span>
        </label>
        <el-button :loading="batchSubmitting" @click="submitBatchShip(true)"
          >预校验</el-button
        >
        <el-button
          type="primary"
          :loading="batchSubmitting"
          @click="submitBatchShip(false)"
          >确认导入发货</el-button
        >
      </div>
      <el-table
        v-if="batchResult"
        :data="batchResult.rows"
        border
        class="batch-result"
      >
        <el-table-column prop="rowNo" label="行号" width="80" align="center" />
        <el-table-column prop="orderSn" label="订单号" min-width="180" />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'">{{
              row.success ? "成功" : "失败"
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="message"
          label="说明"
          min-width="220"
          show-overflow-tooltip
        />
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-page {
  min-width: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.order-search {
  width: 220px;
}

.compact-search {
  width: 130px;
}

.mobile-search {
  width: 160px;
}

.time-picker {
  width: 370px;
}

.table-toolbar {
  align-items: center;
  color: var(--el-text-color-secondary);
  display: flex;
  font-size: 14px;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 12px;
}

.order-table {
  width: 100%;
}

.user-cell,
.goods-cell > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.user-cell span,
.goods-cell span,
.drawer-header span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.amount-text,
.total-value {
  color: var(--el-color-danger);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.drawer-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.drawer-header h3 {
  font-size: 18px;
  margin: 0 0 4px;
}

.drawer-actions {
  display: flex;
  gap: 8px;
  padding-right: 16px;
}

.drawer-content {
  min-height: 240px;
}

.detail-section {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 0 0 24px;
  margin-bottom: 24px;
}

.detail-section:last-child {
  border-bottom: 0;
}

.detail-section h4 {
  font-size: 15px;
  margin: 0 0 14px;
}

.address-block {
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
  padding: 14px 16px;
}

.address-block span {
  color: var(--el-text-color-secondary);
  margin-left: 16px;
}

.address-block p {
  margin: 8px 0 0;
}

.goods-cell {
  align-items: center;
  display: flex;
  gap: 12px;
}

.goods-image {
  border-radius: 4px;
  flex: 0 0 52px;
  height: 52px;
  width: 52px;
}

.amount-grid {
  display: grid;
  gap: 10px 18px;
  grid-template-columns: 1fr 120px;
  margin-left: auto;
  max-width: 320px;
  text-align: right;
}

.amount-grid .total-label,
.amount-grid .total-value {
  border-top: 1px solid var(--el-border-color);
  font-size: 16px;
  padding-top: 10px;
}

.order-timeline,
.logistics-timeline {
  padding: 8px 0 0 8px;
}

.order-timeline p {
  color: var(--el-text-color-secondary);
  margin: 6px 0 0;
}

.dialog-alert {
  margin-bottom: 20px;
}

.remark-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.batch-tools {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.file-button {
  align-items: center;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  display: inline-flex;
  min-height: 32px;
  max-width: 260px;
  padding: 0 12px;
}

.file-button input {
  display: none;
}

.file-button span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-result {
  margin-top: 8px;
}

.logistics-content {
  min-height: 160px;
}

.logistics-timeline {
  margin-top: 24px;
}

@media (max-width: 900px) {
  .filter-card :deep(.el-form) {
    display: grid;
  }

  .filter-card :deep(.el-form-item) {
    margin-bottom: 12px;
    margin-right: 0;
  }

  .order-search,
  .compact-search,
  .mobile-search,
  .time-picker {
    width: 100%;
  }

  .drawer-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .drawer-actions {
    padding-right: 0;
  }
}
</style>
