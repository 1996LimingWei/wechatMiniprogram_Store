<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getMemberPage, getMemberDetail, updateMember } from "@/api/member";
import type { MemberUser, MemberUserDetail } from "@/api/types";

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
const detail = ref<MemberUserDetail | null>(null);

async function openDetail(row: MemberUser) {
    drawerVisible.value = true;
    drawerLoading.value = true;
    detail.value = null;
    isEditing.value = false;
    try {
        detail.value = (await getMemberDetail(row.id!)) as MemberUserDetail;
    } finally {
        drawerLoading.value = false;
    }
}

/* ---------- 编辑模式 ---------- */
const isEditing = ref(false);
const editSaving = ref(false);
const editForm = reactive({
    nickname: "",
    mobile: "",
    avatar: "",
    status: 1 as number
});

function startEdit() {
    if (!detail.value) return;
    editForm.nickname = detail.value.nickname || "";
    editForm.mobile = detail.value.mobile || "";
    editForm.avatar = detail.value.avatar || "";
    editForm.status = detail.value.status ?? 1;
    isEditing.value = true;
}

function cancelEdit() {
    isEditing.value = false;
}

async function saveEdit() {
    await ElMessageBox.confirm(
        "修改会员信息可能影响小程序端用户展示和关联数据，请确认操作。\n\n• 修改昵称/头像将直接影响小程序端显示\n• 禁用会员将导致其无法登录和下单",
        "⚠️ 操作风险提示",
        {
            type: "warning",
            confirmButtonText: "确认修改",
            cancelButtonText: "取消",
            distinguishCancelAndClose: true
        }
    );
    editSaving.value = true;
    try {
        await updateMember({
            id: detail.value!.id!,
            nickname: editForm.nickname.trim(),
            mobile: editForm.mobile.trim(),
            avatar: editForm.avatar.trim(),
            status: editForm.status
        });
        ElMessage.success("会员信息已更新");
        isEditing.value = false;
        // 刷新详情
        const refreshed = (await getMemberDetail(detail.value!.id!)) as MemberUserDetail;
        detail.value = refreshed;
        // 刷新列表
        fetchData();
    } finally {
        editSaving.value = false;
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

/* ---------- 订单状态 ---------- */
function orderStatusLabel(s?: number) {
    const map: Record<number, string> = {
        0: "待付款",
        1: "已付款",
        2: "已发货",
        3: "已完成",
        4: "已关闭",
        5: "已取消"
    };
    return map[s ?? -1] || "未知";
}

function formatPrice(cents?: number) {
    if (cents == null) return "—";
    return `¥${(cents / 100).toFixed(2)}`;
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

            <el-table
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
            size="480px"
            direction="rtl"
        >
            <div v-loading="drawerLoading">
                <template v-if="detail">
                    <!-- 基础信息 -->
                    <div class="detail-section">
                        <div class="detail-header">
                            基础信息
                            <el-button
                                v-if="!isEditing"
                                type="primary"
                                link
                                size="small"
                                style="margin-left: auto"
                                @click="startEdit"
                            >
                                编辑
                            </el-button>
                            <template v-else>
                                <el-button type="primary" size="small" :loading="editSaving" @click="saveEdit">保存</el-button>
                                <el-button size="small" @click="cancelEdit">取消</el-button>
                            </template>
                        </div>

                        <!-- 查看模式 -->
                        <template v-if="!isEditing">
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
                        </template>

                        <!-- 编辑模式 -->
                        <template v-else>
                            <el-form label-width="80px" class="edit-form">
                                <el-form-item label="头像 URL">
                                    <el-input v-model="editForm.avatar" placeholder="输入头像图片 URL" />
                                </el-form-item>
                                <el-form-item v-if="editForm.avatar" label="头像预览">
                                    <el-avatar :size="48" :src="editForm.avatar" />
                                </el-form-item>
                                <el-form-item label="昵称">
                                    <el-input v-model="editForm.nickname" placeholder="输入昵称" maxlength="30" />
                                </el-form-item>
                                <el-form-item label="手机号">
                                    <el-input v-model="editForm.mobile" placeholder="输入手机号" maxlength="11" />
                                </el-form-item>
                                <el-form-item label="状态">
                                    <el-radio-group v-model="editForm.status">
                                        <el-radio :value="1">正常</el-radio>
                                        <el-radio :value="0">禁用</el-radio>
                                    </el-radio-group>
                                </el-form-item>
                            </el-form>
                            <el-alert
                                title="修改会员信息将直接影响小程序端用户展示，禁用会员将无法登录和下单。"
                                type="warning"
                                :closable="false"
                                show-icon
                                style="margin-top: 8px"
                            />
                        </template>
                    </div>

                    <!-- 数据概览 -->
                    <div class="detail-section">
                        <div class="detail-header">数据概览</div>
                        <div class="detail-stats">
                            <div class="detail-stat-item">
                                <span class="detail-stat-num">{{ detail.orderCount ?? 0 }}</span>
                                <span class="detail-stat-label">订单数</span>
                            </div>
                            <div class="detail-stat-item">
                                <span class="detail-stat-num">{{ detail.collectCount ?? 0 }}</span>
                                <span class="detail-stat-label">收藏数</span>
                            </div>
                            <div class="detail-stat-item">
                                <span class="detail-stat-num">{{ detail.commentCount ?? 0 }}</span>
                                <span class="detail-stat-label">评论数</span>
                            </div>
                        </div>
                    </div>

                    <!-- 收货地址 -->
                    <div class="detail-section">
                        <div class="detail-header">收货地址</div>
                        <template v-if="detail.addresses && detail.addresses.length > 0">
                            <div
                                v-for="addr in detail.addresses"
                                :key="addr.id"
                                class="detail-addr-item"
                            >
                                <div class="detail-addr-top">
                                    <span>{{ addr.userName }}</span>
                                    <span class="detail-addr-tel">{{ addr.telNumber }}</span>
                                    <el-tag v-if="addr.isDefault === 1" size="small" type="warning">默认</el-tag>
                                </div>
                                <div class="detail-addr-detail">
                                    {{ addr.fullRegion }} {{ addr.detailInfo }}
                                </div>
                            </div>
                        </template>
                        <el-empty v-else description="暂无收货地址" :image-size="60" />
                    </div>

                    <!-- 最近订单 -->
                    <div class="detail-section">
                        <div class="detail-header">最近订单</div>
                        <template v-if="detail.recentOrders && detail.recentOrders.length > 0">
                            <div
                                v-for="order in detail.recentOrders"
                                :key="order.id"
                                class="detail-order-item"
                            >
                                <div class="detail-order-top">
                                    <span class="detail-order-sn">{{ order.orderSn }}</span>
                                    <el-tag size="small">{{ orderStatusLabel(order.status) }}</el-tag>
                                </div>
                                <div class="detail-order-bottom">
                                    <span class="detail-order-price">{{ formatPrice(order.actualPrice) }}</span>
                                    <span class="detail-order-time">{{ order.createTime }}</span>
                                </div>
                            </div>
                        </template>
                        <el-empty v-else description="暂无订单" :image-size="60" />
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

/* 数据概览 */
.detail-stats {
    display: flex;
    gap: 16px;
}
.detail-stat-item {
    flex: 1;
    text-align: center;
    padding: 12px 0;
    background: #f5f7fa;
    border-radius: 8px;
}
.detail-stat-num {
    display: block;
    font-size: 20px;
    font-weight: 700;
    color: #303133;
}
.detail-stat-label {
    display: block;
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
}

/* 地址 */
.detail-addr-item {
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
}
.detail-addr-item:last-child { border-bottom: none; }
.detail-addr-top {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #303133;
}
.detail-addr-tel {
    color: #909399;
}
.detail-addr-detail {
    font-size: 13px;
    color: #606266;
    margin-top: 4px;
}

/* 订单 */
.detail-order-item {
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
}
.detail-order-item:last-child { border-bottom: none; }
.detail-order-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}
.detail-order-sn {
    font-size: 13px;
    color: #303133;
    font-family: monospace;
}
.detail-order-bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 6px;
}
.detail-order-price {
    font-size: 14px;
    font-weight: 600;
    color: #e64340;
}
.detail-order-time {
    font-size: 12px;
    color: #909399;
}

/* 编辑表单 */
.edit-form {
    margin-top: 4px;
}
</style>
