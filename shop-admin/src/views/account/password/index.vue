<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { changeOwnPassword } from "@/api/system";
import { useUserStoreHook } from "@/store/modules/user";

defineOptions({ name: "AccountPassword" });
const formRef = ref<FormInstance>();
const saving = ref(false);
const form = reactive({ oldPassword: "", newPassword: "", confirmPassword: "" });
const rules: FormRules = {
  oldPassword: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [{ required: true, validator: (_rule, value, callback) => value && value.length >= 10 && /[A-Za-z]/.test(value) && /\d/.test(value) ? callback() : callback(new Error("密码至少 10 位且包含字母和数字")), trigger: "blur" }],
  confirmPassword: [{ required: true, validator: (_rule, value, callback) => value === form.newPassword ? callback() : callback(new Error("两次输入的新密码不一致")), trigger: "blur" }]
};
async function submit() { await formRef.value?.validate(); saving.value = true; try { await changeOwnPassword(form.oldPassword, form.newPassword); ElMessage.success("密码已修改，请重新登录"); await useUserStoreHook().logOut(); } finally { saving.value = false; } }
</script>

<template><div class="app-container password-page"><el-card shadow="never"><template #header>修改登录密码</template><el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="password-form"><el-form-item label="原密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item><el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item><el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" /></el-form-item><el-form-item><el-button type="primary" :loading="saving" @click="submit">确认修改</el-button></el-form-item></el-form></el-card></div></template>
<style scoped>.password-form { max-width: 520px; }</style>
