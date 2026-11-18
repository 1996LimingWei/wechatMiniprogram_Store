<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { getMemberPage, getMemberDetail } from "@/api/member";
import type { MemberUser } from "@/api/types";

defineOptions({ name: "MemberList" });

/* ---------- 查询 ---------- */
const loading = ref(false);
const tableData = ref<MemberUser[]>([]);
const total = ref(0);
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  nickname: "",
  mobile: ""
});

async function fetchData() {
  loading.value = true;
  try {
    const params: any = {
      pageNo: query.pageNo,
      pageSize: query.pageSize
    };
    if (query.nickname.trim()) params.nickname = query.nickname.trim();
    if (query.mobile.trim()) params.mobile = query.mobile.trim();

    const res = (await getMemberPage(params)) as any;
    tableData.value = res.list ?? [];
    total.value = res.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  fetchData();
}

function handleReset() {
  query.nickname = "";
  query.mobile = "";
  query.pageNo = 1;
  fetchData();
}

function handlePageChange(page: number) {
  query.pageNo = page;
  fetchData();
}

/* ---------- 详情抽屉 ---------- */
const drawerVisible = ref(false);
const drawerLoading = ref(false);
const detail = ref<MemberUser | null>(null);

async function openDetail(row: MemberUser) {
  drawerVisible.value = true;
  drawerLoading.value = true;
  detail.value = null;
  try {
    detail.value = (await getMemberDetail(row.id!)) as MemberUser;
  } finally {
    drawerLoading.value = false;
  }
}

/* ---------- 状态标签 ---------- */
function statusLabel(s?: number) {
  if (s === 1) return "正常";
  if (s === 0) return "禁用";
  return "未知";
}

function statusType(s?: number) {
  if (s === 1) return "success";
  if (s === 0) return "danger";
  return "info";
}

onMounted(fetchData);
</script>

<template>
  <div class="app-container">
    <!-- 筛选栏 -->
    <el-card shadow="never" class="mb-4">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
        <el-form-item label="昵称">
          <el-input
            v-model="query.nickname"
            placeholder="输入昵称搜索"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="query.mobile"
            placeholder="输入手机号搜索"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <div class="toolbar">
        <span class="total-label">共 {{ total }} 位会员</span>
      </div>

      <el-table native-scrollbar
        :data="tableData"
        v-loading="loading"
        border
        style="width: 100%; margin-top: 12px; cursor: pointer"
        @row-click="openDetail"
      >
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="40" :src="row.avatar" />
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="mobile" label="手机号" width="140" align="center">
          <template #default="{ row }">
            {{ row.mobile || "—" }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" align="center" />
      </el-table>

      <!-- 分页 -->
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

    <!-- 会员详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="会员详情"
      size="420px"
      direction="rtl"
    >
      <div v-loading="drawerLoading">
        <template v-if="detail">
          <div class="detail-section">
            <div class="detail-header">基础信息</div>
            <div class="detail-avatar-row">
              <el-avatar :size="64" :src="detail.avatar" />
              <div class="detail-avatar-info">
                <div class="detail-nickname">{{ detail.nickname || "未设置昵称" }}</div>
                <div class="detail-mobile">{{ detail.mobile || "未绑定手机号" }}</div>
              </div>
              <el-tag :type="statusType(detail.status)" size="small" style="margin-left: auto">
                {{ statusLabel(detail.status) }}
              </el-tag>
            </div>
            <div class="detail-meta">
              注册时间：{{ detail.createTime || "—" }}
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.app-container { padding: 16px; }
.mb-4 { margin-bottom: 16px; }
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.total-label {
  font-size: 13px;
  color: #6b7280;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 详情抽屉 */
.detail-section {
  margin-bottom: 24px;
  padding: 0 4px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.detail-avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.detail-avatar-info { flex: 1; }
.detail-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.detail-mobile {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.detail-meta {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}
</style>
