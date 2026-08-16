<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Plus, Refresh } from "@element-plus/icons-vue";
import { getSystemPermissions, getSystemRoles, saveSystemRole, setSystemRoleStatus, type SystemPermission, type SystemRole } from "@/api/system";

defineOptions({ name: "SystemRole" });
const loading = ref(false);
const roles = ref<SystemRole[]>([]);
const permissions = ref<SystemPermission[]>([]);
const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ id: undefined as number | undefined, code: "", name: "", permissionIds: [] as number[] });
const rules: FormRules = { code: [{ required: true, message: "请输入角色编码", trigger: "blur" }, { pattern: /^[A-Z][A-Z0-9_]{2,63}$/, message: "仅大写字母、数字和下划线，长度 3 至 64 位", trigger: "blur" }], name: [{ required: true, message: "请输入角色名称", trigger: "blur" }], permissionIds: [{ type: "array", required: true, min: 1, message: "至少选择一个权限", trigger: "change" }] };
const groupedPermissions = computed(() => {
  const groups = new Map<string, SystemPermission[]>();
  for (const permission of permissions.value) {
    const prefix = permission.code.split(":")[0];
    groups.set(prefix, [...(groups.get(prefix) ?? []), permission]);
  }
  return [...groups.entries()];
});
async function fetchData() { loading.value = true; try { [roles.value, permissions.value] = await Promise.all([getSystemRoles(), getSystemPermissions()]); } finally { loading.value = false; } }
function openCreate() { Object.assign(form, { id: undefined, code: "", name: "", permissionIds: [] }); dialogVisible.value = true; }
function openEdit(row: SystemRole) { Object.assign(form, { id: row.id, code: row.code, name: row.name, permissionIds: [...row.permissionIds] }); dialogVisible.value = true; }
async function submit() { await formRef.value?.validate(); saving.value = true; try { await saveSystemRole({ ...form, code: form.id ? undefined : form.code }); ElMessage.success(form.id ? "角色权限已更新，相关会话已失效" : "角色已创建"); dialogVisible.value = false; await fetchData(); } finally { saving.value = false; } }
async function toggleStatus(row: SystemRole) { const status = row.status ? 0 : 1; await ElMessageBox.confirm(`确认${status ? "启用" : "停用"}角色“${row.name}”？${status ? "" : "停用后关联账号将立即失效。"}`, "确认操作", { type: "warning" }); await setSystemRoleStatus(row.id, status); ElMessage.success(status ? "角色已启用" : "角色已停用"); await fetchData(); }
onMounted(fetchData);
</script>

<template>
  <div class="app-container role-page">
    <el-card shadow="never">
      <div class="toolbar"><el-button type="primary" :icon="Plus" @click="openCreate">新增角色</el-button><el-button :icon="Refresh" circle title="刷新" @click="fetchData" /></div>
      <el-table v-loading="loading" :data="roles" border>
        <el-table-column prop="name" label="角色名称" min-width="150" />
        <el-table-column prop="code" label="角色编码" min-width="190" />
        <el-table-column label="已配置权限" min-width="330"><template #default="{ row }"><el-tag v-for="permission in row.permissionCodes" :key="permission" class="permission-tag" effect="plain">{{ permission }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? "启用" : "停用" }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button :disabled="row.code === 'SUPER_ADMIN'" link type="primary" @click="openEdit(row)">编辑</el-button><el-button :disabled="row.code === 'SUPER_ADMIN'" link :type="row.status ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status ? "停用" : "启用" }}</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="min(760px, 94vw)" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px"><el-form-item label="角色编码" prop="code"><el-input v-model="form.code" :disabled="!!form.id" placeholder="例如 ORDER_OPERATOR" /></el-form-item><el-form-item label="角色名称" prop="name"><el-input v-model="form.name" /></el-form-item><el-form-item label="接口权限" prop="permissionIds"><el-checkbox-group v-model="form.permissionIds" class="permission-groups"><section v-for="[group, items] in groupedPermissions" :key="group"><strong>{{ group }}</strong><el-checkbox v-for="permission in items" :key="permission.id" :label="permission.id">{{ permission.name }} <span>{{ permission.httpMethod }} {{ permission.pathPattern }}</span></el-checkbox></section></el-checkbox-group></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; } .permission-tag { margin: 2px 4px 2px 0; } .permission-groups { display: grid; gap: 14px; width: 100%; } .permission-groups section { background: var(--el-fill-color-lighter); padding: 10px 12px; } .permission-groups strong { display: block; margin-bottom: 7px; text-transform: uppercase; } .permission-groups :deep(.el-checkbox) { display: flex; height: auto; line-height: 1.5; margin: 6px 18px 6px 0; white-space: normal; } .permission-groups span { color: var(--el-text-color-secondary); font-size: 12px; }
</style>
