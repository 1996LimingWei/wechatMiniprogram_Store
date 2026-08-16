<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Refresh, Search, View } from "@element-plus/icons-vue";
import {
  adjustInventoryStock,
  getInventoryLogPage,
  getInventoryPage,
  reconcileInventory,
  updateInventoryWarningStock
} from "@/api/inventory";
import type {
  InventoryReconcileResult,
  InventorySku,
  InventoryStockLog
} from "@/api/inventory";
import type { PageResult } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "ProductInventory" });
const canManageProduct = hasAnyPerms(["product:manage"]);

const loading = ref(false);
const rows = ref<InventorySku[]>([]);
const total = ref(0);
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  productName: "",
  skuCode: "",
  stockStatus: "",
  lowStockOnly: false
});

async function fetchData() {
  loading.value = true;
  try {
    const params: Parameters<typeof getInventoryPage>[0] = {
      pageNo: query.pageNo,
      pageSize: query.pageSize
    };
    if (query.productName.trim()) params.productName = query.productName.trim();
    if (query.skuCode.trim()) params.skuCode = query.skuCode.trim();
    if (query.stockStatus) params.stockStatus = query.stockStatus;
    if (query.lowStockOnly) params.lowStockOnly = true;
    const result = (await getInventoryPage(params)) as PageResult<InventorySku>;
    rows.value = result.list ?? [];
    total.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  fetchData();
}

function handleReset() {
  query.productName = "";
  query.skuCode = "";
  query.stockStatus = "";
  query.lowStockOnly = false;
  query.pageNo = 1;
  fetchData();
}

function handlePageChange(page: number) {
  query.pageNo = page;
  fetchData();
}

function money(cents?: number) {
  return cents == null ? "—" : `¥${(cents / 100).toFixed(2)}`;
}

function statusType(status: string) {
  if (status === "OUT_OF_STOCK") return "danger";
  if (status === "LOW_STOCK") return "warning";
  return "success";
}

const warningVisible = ref(false);
const warningSaving = ref(false);
const warningTarget = ref<InventorySku | null>(null);
const warningForm = reactive({ warningStock: 0 });

function openWarning(row: InventorySku) {
  warningTarget.value = row;
  warningForm.warningStock = row.warningStock ?? 0;
  warningVisible.value = true;
}

async function submitWarning() {
  if (!warningTarget.value) return;
  warningSaving.value = true;
  try {
    await updateInventoryWarningStock({
      skuId: warningTarget.value.skuId,
      warningStock: warningForm.warningStock
    });
    ElMessage.success("预警库存已更新");
    warningVisible.value = false;
    fetchData();
  } finally {
    warningSaving.value = false;
  }
}

const adjustVisible = ref(false);
const adjustSaving = ref(false);
const adjustTarget = ref<InventorySku | null>(null);
const adjustForm = reactive({ changeQuantity: 0, reason: "" });

function openAdjust(row: InventorySku) {
  adjustTarget.value = row;
  adjustForm.changeQuantity = 0;
  adjustForm.reason = "";
  adjustVisible.value = true;
}

async function submitAdjust() {
  if (!adjustTarget.value) return;
  const reason = adjustForm.reason.trim();
  if (reason.length < 4 || reason.length > 200) {
    ElMessage.warning("请填写 4 至 200 个字符的调整原因");
    return;
  }
  adjustSaving.value = true;
  try {
    await adjustInventoryStock({
      skuId: adjustTarget.value.skuId,
      changeQuantity: adjustForm.changeQuantity,
      reason
    });
    ElMessage.success("库存已调整并写入流水");
    adjustVisible.value = false;
    fetchData();
  } finally {
    adjustSaving.value = false;
  }
}

const logVisible = ref(false);
const logLoading = ref(false);
const logRows = ref<InventoryStockLog[]>([]);
const logTotal = ref(0);
const logTarget = ref<InventorySku | null>(null);
const logQuery = reactive({ pageNo: 1, pageSize: 10, bizNo: "" });

async function openLogs(row: InventorySku) {
  logTarget.value = row;
  logQuery.pageNo = 1;
  logQuery.bizNo = "";
  logVisible.value = true;
  await fetchLogs();
}

async function fetchLogs() {
  if (!logTarget.value) return;
  logLoading.value = true;
  try {
    const result = await getInventoryLogPage({
      pageNo: logQuery.pageNo,
      pageSize: logQuery.pageSize,
      skuId: logTarget.value.skuId,
      bizNo: logQuery.bizNo.trim() || undefined
    });
    logRows.value = result.list ?? [];
    logTotal.value = result.total ?? 0;
  } finally {
    logLoading.value = false;
  }
}

const reconcileVisible = ref(false);
const reconcileLoading = ref(false);
const reconcileResult = ref<InventoryReconcileResult | null>(null);

async function handleReconcile() {
  reconcileLoading.value = true;
  try {
    reconcileResult.value = await reconcileInventory();
    reconcileVisible.value = true;
    if (reconcileResult.value.mismatchCount > 0) {
      ElMessage.warning("库存对账发现差异，请查看明细");
    } else {
      ElMessage.success("库存对账无异常");
    }
  } finally {
    reconcileLoading.value = false;
  }
}

onMounted(fetchData);
</script>

<template>
  <div class="app-container inventory-page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
        <el-form-item label="商品名称">
          <el-input
            v-model="query.productName"
            clearable
            placeholder="输入商品名称"
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="SKU">
          <el-input
            v-model="query.skuCode"
            clearable
            placeholder="编码或 SKU ID"
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="库存状态">
          <el-select v-model="query.stockStatus" clearable placeholder="全部" style="width: 130px">
            <el-option label="正常" value="NORMAL" />
            <el-option label="低库存" value="LOW_STOCK" />
            <el-option label="缺货" value="OUT_OF_STOCK" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.lowStockOnly">只看低库存</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button v-if="canManageProduct" :loading="reconcileLoading" @click="handleReconcile">库存对账</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="table-toolbar">共 {{ total }} 个 SKU</div>
      <el-table v-loading="loading" :data="rows" border style="width: 100%">
        <el-table-column label="商品" min-width="220" fixed="left">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image v-if="row.picUrl" :src="row.picUrl" fit="cover" class="sku-image" />
              <div>
                <strong>{{ row.productName }}</strong>
                <span>{{ row.categoryName || "未分类" }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="skuId" label="SKU ID" width="90" align="center" />
        <el-table-column label="SKU编码" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.skuCode || "—" }}</template>
        </el-table-column>
        <el-table-column prop="specName" label="规格" min-width="150" show-overflow-tooltip />
        <el-table-column label="售价" width="100" align="right">
          <template #default="{ row }">{{ money(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="availableStock" label="可售" width="80" align="center" />
        <el-table-column prop="lockedStock" label="锁定" width="80" align="center" />
        <el-table-column prop="stock" label="当前库存" width="100" align="center" />
        <el-table-column prop="warningStock" label="预警库存" width="100" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.stockStatus)" size="small">{{ row.stockStatusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManageProduct" type="primary" link size="small" @click="openAdjust(row)">调库存</el-button>
            <el-button v-if="canManageProduct" type="primary" link size="small" @click="openWarning(row)">预警</el-button>
            <el-button type="primary" link size="small" :icon="View" @click="openLogs(row)">流水</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNo"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="warningVisible" title="设置预警库存" width="460px">
      <el-form label-width="100px">
        <el-form-item label="SKU">
          <span>{{ warningTarget?.skuCode || `#${warningTarget?.skuId}` }}</span>
        </el-form-item>
        <el-form-item label="预警库存">
          <el-input-number
            v-model="warningForm.warningStock"
            :min="0"
            :max="1000000"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warningVisible = false">取消</el-button>
        <el-button type="primary" :loading="warningSaving" @click="submitWarning">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustVisible" title="人工调整库存" width="560px">
      <el-form label-width="110px">
        <el-form-item label="SKU">
          <span>{{ adjustTarget?.productName }} / {{ adjustTarget?.specName }}</span>
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ adjustTarget?.stock ?? "—" }}</span>
        </el-form-item>
        <el-form-item label="调整数量" required>
          <el-input-number
            v-model="adjustForm.changeQuantity"
            :min="-1000000"
            :max="1000000"
            controls-position="right"
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input
            v-model="adjustForm.reason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="填写盘点入库、损耗修正等具体原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustSaving" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logVisible" title="库存流水" size="min(760px, 96vw)">
      <el-form :inline="true" @submit.prevent="fetchLogs">
        <el-form-item label="业务单号">
          <el-input v-model="logQuery.bizNo" clearable placeholder="输入业务单号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchLogs">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="logLoading" :data="logRows" border>
        <el-table-column prop="bizType" label="类型" width="120" />
        <el-table-column prop="bizNo" label="业务单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="changeQuantity" label="变化" width="80" align="center" />
        <el-table-column label="库存" width="120" align="center">
          <template #default="{ row }">{{ row.beforeStock }} → {{ row.afterStock }}</template>
        </el-table-column>
        <el-table-column prop="operatorId" label="操作人" width="90" align="center" />
        <el-table-column prop="remark" label="说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" align="center" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="logQuery.pageNo"
          :page-size="logQuery.pageSize"
          :total="logTotal"
          layout="total, prev, pager, next"
          @current-change="fetchLogs"
        />
      </div>
    </el-drawer>

    <el-dialog v-model="reconcileVisible" title="库存对账结果" width="820px">
      <div v-if="reconcileResult" class="reconcile-summary">
        <el-tag type="info">SKU 总数 {{ reconcileResult.totalSkuCount }}</el-tag>
        <el-tag :type="reconcileResult.mismatchCount > 0 ? 'danger' : 'success'">
          差异 {{ reconcileResult.mismatchCount }}
        </el-tag>
      </div>
      <el-table
        v-if="reconcileResult"
        :data="reconcileResult.rows"
        border
        max-height="420"
        style="width: 100%; margin-top: 12px"
      >
        <el-table-column prop="skuId" label="SKU ID" width="90" align="center" />
        <el-table-column prop="skuCode" label="SKU编码" width="150" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="currentStock" label="当前库存" width="100" align="center" />
        <el-table-column prop="ledgerStock" label="流水库存" width="100" align="center" />
        <el-table-column prop="difference" label="差异" width="90" align="center" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="reconcileVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.inventory-page {
  padding: 16px;
}

.filter-card {
  margin-bottom: 16px;
}

.table-toolbar {
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
}

.product-cell {
  align-items: center;
  display: flex;
  gap: 10px;
}

.product-cell div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.product-cell span {
  color: #909399;
  font-size: 12px;
}

.sku-image {
  border-radius: 4px;
  height: 44px;
  width: 44px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.reconcile-summary {
  display: flex;
  gap: 8px;
}
</style>
