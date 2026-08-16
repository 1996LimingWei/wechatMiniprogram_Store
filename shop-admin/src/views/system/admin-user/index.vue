<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Lock, Plus, Refresh, Search } from "@element-plus/icons-vue";
import {
  forceLogoutAdminUser,
  getAdminUserPage,
  getSystemRoles,
  resetAdminUserPassword,
  saveAdminUser,
  setAdminUserStatus,
  unlockAdminUser,
  type AdminUser,
  type SystemRole
} from "@/api/system";

defineOptions({ name: "SystemAdminUser" });

const loading = ref(false);
const total = ref(0);
const rows = ref<AdminUser[]>([]);
const roles = ref<SystemRole[]>([]);
const query = reactive({ pageNo: 1, pageSize: 10, username: "", nickname: "", status: "" });

async function fetchData() {
  loading.value = true;
  try {
    const result = await getAdminUserPage({
      ...query,
      status: query.status === "" ? undefined : Number(query.status)
    });
    rows.value = result.list ?? [];
    total.value = result.total ?? 0;
  } finally {
    loading.value = false;
  }
}

function search() { query.pageNo = 1; fetchData(); }
function reset() { query.username = ""; query.nickname = ""; query.status = ""; search(); }

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ id: undefined as number | undefined, username: "", password: "", nickname: "", avatar: "", roleIds: [] as number[] });
const rules: FormRules = {
  username: [{ required: true, message: "请输入账号", trigger: "blur" }, { pattern: /^[A-Za-z][A-Za-z0-9_.-]{2,63}$/, message: "账号须以字母开头，长度 3 至 64 位", trigger: "blur" }],
  password: [{ validator: (_rule, value, callback) => !form.id && (!value || value.length < 10 || !/[A-Za-z]/.test(value) || !/\d/.test(value)) ? callback(new Error("密码至少 10 位且包含字母和数字")) : callback(), trigger: "blur" }],
  nickname: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  roleIds: [{ type: "array", required: true, min: 1, message: "至少选择一个角色", trigger: "change" }]
};

function openCreate() {
  Object.assign(form, { id: undefined, username: "", password: "", nickname: "", avatar: "", roleIds: [] });
  dialogVisible.value = true;
}
function openEdit(row: AdminUser) {
  Object.assign(form, { id: row.id, username: row.username, password: "", nickname: row.nickname, avatar: row.avatar ?? "", roleIds: [...row.roleIds] });
  dialogVisible.value = true;
}
async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await saveAdminUser({ ...form, password: form.id ? undefined : form.password });
    ElMessage.success(form.id ? "账号已更新，原登录会话已失效" : "管理员账号已创建");
    dialogVisible.value = false;
    await fetchData();
  } finally { saving.value = false; }
}
async function toggleStatus(row: AdminUser) {
  const nextStatus = row.status === 1 ? 0 : 1;
  await ElMessageBox.confirm(`确认${nextStatus ? "启用" : "停用"}账号 ${row.username}？${nextStatus ? "" : "停用将立即注销该账号。"}`, "确认操作", { type: "warning" });
  await setAdminUserStatus(row.id, nextStatus);
  ElMessage.success(nextStatus ? "账号已启用" : "账号已停用并注销");
  await fetchData();
}
async function unlock(row: AdminUser) { await unlockAdminUser(row.id); ElMessage.success("账号已解除锁定"); await fetchData(); }
async function resetPassword(row: AdminUser) {
  const { value } = await ElMessageBox.prompt(`为 ${row.username} 设置新密码`, "重置密码", { inputType: "password", inputPlaceholder: "至少 10 位，包含字母和数字", inputValidator: value => value && value.length >= 10 && /[A-Za-z]/.test(value) && /\d/.test(value) || "密码至少 10 位且包含字母和数字" });
  await resetAdminUserPassword(row.id, value);
  ElMessage.success("密码已重置，原会话已失效");
}
async function forceLogout(row: AdminUser) { await forceLogoutAdminUser(row.id); ElMessage.success("已注销该账号全部会话"); }

onMounted(async () => { roles.value = await getSystemRoles(); await fetchData(); });
</script>

<template>
  <div class="app-container system-page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent="search">
        <el-form-item label="账号"><el-input v-model="query.username" clearable placeholder="账号" @keyup.enter="search" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="query.nickname" clearable placeholder="姓名" @keyup.enter="search" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部" class="status-select"><el-option label="启用" value="1" /><el-option label="停用" value="0" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="search">查询</el-button><el-button :icon="Refresh" @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <div class="toolbar"><el-button type="primary" :icon="Plus" @click="openCreate">新增管理员</el-button><span>共 {{ total }} 个账号</span></div>
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="username" label="账号" min-width="150" />
        <el-table-column prop="nickname" label="姓名" min-width="130" />
        <el-table-column label="角色" min-width="190"><template #default="{ row }"><el-tag v-for="role in row.roleNames" :key="role" class="role-tag" effect="plain">{{ role }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? "启用" : "停用" }}</el-tag></template></el-table-column>
        <el-table-column label="登录安全" min-width="180"><template #default="{ row }"><span v-if="row.lockedUntil" class="danger">已锁定至 {{ row.lockedUntil }}</span><span v-else>失败 {{ row.failedLoginCount }} 次</span></template></el-table-column>
        <el-table-column prop="lastLoginTime" label="最近登录" width="170" />
        <el-table-column label="操作" width="280" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link :type="row.status ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status ? "停用" : "启用" }}</el-button><el-button link :icon="Lock" @click="unlock(row)">解锁</el-button><el-dropdown><el-button link>更多</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="resetPassword(row)">重置密码</el-dropdown-item><el-dropdown-item @click="forceLogout(row)">强制注销</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total" layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" @current-change="fetchData" @size-change="search" /></div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑管理员' : '新增管理员'" width="min(620px, 94vw)" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px"><el-form-item label="账号" prop="username"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item><el-form-item v-if="!form.id" label="初始密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item><el-form-item label="姓名" prop="nickname"><el-input v-model="form.nickname" /></el-form-item><el-form-item label="头像地址"><el-input v-model="form.avatar" placeholder="HTTPS 地址或站内静态资源" /></el-form-item><el-form-item label="角色" prop="roleIds"><el-checkbox-group v-model="form.roleIds"><el-checkbox v-for="role in roles.filter(item => item.status === 1)" :key="role.id" :label="role.id">{{ role.name }}</el-checkbox></el-checkbox-group></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-card { margin-bottom: 16px; } .toolbar, .pagination { display: flex; align-items: center; justify-content: space-between; } .toolbar { margin-bottom: 16px; } .toolbar span { color: var(--el-text-color-secondary); font-size: 14px; } .pagination { justify-content: flex-end; margin-top: 18px; } .status-select { width: 110px; } .role-tag { margin: 2px 4px 2px 0; } .danger { color: var(--el-color-danger); } @media (max-width: 640px) { .filter-card :deep(.el-form-item), .filter-card :deep(.el-input), .filter-card :deep(.el-select) { width: 100%; } }
</style>
