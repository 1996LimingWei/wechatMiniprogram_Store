<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { Refresh, Search } from "@element-plus/icons-vue";
import {
  getLoginAuditPage,
  getOperationAuditPage,
  type LoginAuditLog,
  type OperationAuditLog
} from "@/api/system";

defineOptions({ name: "SystemAudit" });

const tab = ref("operation");
const loading = ref(false);
const loginRows = ref<LoginAuditLog[]>([]);
const operationRows = ref<OperationAuditLog[]>([]);
const loginTotal = ref(0);
const operationTotal = ref(0);
const loginQuery = reactive({ pageNo: 1, pageSize: 10, username: "", success: "" });
const operationQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  username: "",
  requestUri: "",
  businessRef: "",
  operationType: "",
  highRisk: "",
  success: ""
});

async function fetchLogin() {
  loading.value = true;
  try {
    const result = await getLoginAuditPage({
      ...loginQuery,
      success: loginQuery.success === "" ? undefined : Number(loginQuery.success)
    });
    loginRows.value = result.list ?? [];
    loginTotal.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

async function fetchOperation() {
  loading.value = true;
  try {
    const result = await getOperationAuditPage({
      ...operationQuery,
      highRisk: operationQuery.highRisk === "" ? undefined : Number(operationQuery.highRisk),
      success: operationQuery.success === "" ? undefined : Number(operationQuery.success)
    });
    operationRows.value = result.list ?? [];
    operationTotal.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function searchLogin() {
  loginQuery.pageNo = 1;
  fetchLogin();
}

function resetLogin() {
  loginQuery.username = "";
  loginQuery.success = "";
  searchLogin();
}

function searchOperation() {
  operationQuery.pageNo = 1;
  fetchOperation();
}

function resetOperation() {
  operationQuery.username = "";
  operationQuery.requestUri = "";
  operationQuery.businessRef = "";
  operationQuery.operationType = "";
  operationQuery.highRisk = "";
  operationQuery.success = "";
  searchOperation();
}

function tabChange() {
  tab.value === "login" ? fetchLogin() : fetchOperation();
}

onMounted(fetchOperation);
</script>

<template>
  <div class="app-container audit-page">
    <el-card shadow="never">
      <el-tabs v-model="tab" @tab-change="tabChange">
        <el-tab-pane label="操作日志" name="operation">
          <el-form :inline="true" @submit.prevent="searchOperation">
            <el-form-item label="账号">
              <el-input v-model="operationQuery.username" clearable placeholder="账号" />
            </el-form-item>
            <el-form-item label="操作类型">
              <el-input v-model="operationQuery.operationType" clearable placeholder="例如 批量调价" />
            </el-form-item>
            <el-form-item label="接口路径">
              <el-input v-model="operationQuery.requestUri" clearable placeholder="例如 /trade/order" />
            </el-form-item>
            <el-form-item label="业务编号">
              <el-input v-model="operationQuery.businessRef" clearable placeholder="订单、售后或商品编号" />
            </el-form-item>
            <el-form-item label="风险">
              <el-select v-model="operationQuery.highRisk" clearable placeholder="全部" class="short-select">
                <el-option label="高风险" value="1" />
                <el-option label="普通" value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="operationQuery.success" clearable placeholder="全部" class="short-select">
                <el-option label="成功" value="1" />
                <el-option label="失败" value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="searchOperation">查询</el-button>
              <el-button :icon="Refresh" @click="resetOperation">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="loading" :data="operationRows" border>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="snapshot-grid">
                  <span>角色快照</span>
                  <pre>{{ row.adminRoleCodes || "—" }}</pre>
                  <span>User-Agent</span>
                  <pre>{{ row.userAgent || "—" }}</pre>
                  <span>变更前</span>
                  <pre>{{ row.beforeSnapshot || "—" }}</pre>
                  <span>变更后</span>
                  <pre>{{ row.afterSnapshot || "—" }}</pre>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column prop="username" label="操作人" min-width="120" />
            <el-table-column label="风险" width="86" align="center">
              <template #default="{ row }">
                <el-tag :type="row.highRisk ? 'danger' : 'info'" effect="plain">
                  {{ row.highRisk ? "高风险" : "普通" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operationType" label="操作类型" min-width="130" />
            <el-table-column prop="method" label="方法" width="78" />
            <el-table-column prop="requestUri" label="接口路径" min-width="230" show-overflow-tooltip />
            <el-table-column prop="businessRef" label="业务编号" min-width="140" />
            <el-table-column label="结果" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">
                  {{ row.success ? "成功" : "失败" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="durationMs" label="耗时" width="85" align="right">
              <template #default="{ row }">{{ row.durationMs }} ms</template>
            </el-table-column>
            <el-table-column prop="message" label="说明" min-width="130" show-overflow-tooltip />
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="operationQuery.pageNo"
              v-model:page-size="operationQuery.pageSize"
              :total="operationTotal"
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50]"
              @current-change="fetchOperation"
              @size-change="searchOperation"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="登录日志" name="login">
          <el-form :inline="true" @submit.prevent="searchLogin">
            <el-form-item label="账号">
              <el-input v-model="loginQuery.username" clearable placeholder="账号" />
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="loginQuery.success" clearable placeholder="全部" class="short-select">
                <el-option label="成功" value="1" />
                <el-option label="失败" value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="searchLogin">查询</el-button>
              <el-button :icon="Refresh" @click="resetLogin">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table v-loading="loading" :data="loginRows" border>
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column prop="username" label="账号" min-width="130" />
            <el-table-column prop="nickname" label="姓名" min-width="120" />
            <el-table-column prop="ip" label="IP" min-width="130" />
            <el-table-column label="结果" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">
                  {{ row.success ? "成功" : "失败" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="说明" min-width="160" />
            <el-table-column prop="userAgent" label="客户端" min-width="260" show-overflow-tooltip />
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="loginQuery.pageNo"
              v-model:page-size="loginQuery.pageSize"
              :total="loginTotal"
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50]"
              @current-change="fetchLogin"
              @size-change="searchLogin"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.short-select {
  width: 100px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  gap: 10px 14px;
  padding: 8px 20px;
}

.snapshot-grid span {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}

.snapshot-grid pre {
  min-height: 30px;
  max-height: 160px;
  padding: 8px 10px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
