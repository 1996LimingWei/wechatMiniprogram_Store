<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import MaterialImagePicker from "@/components/MaterialImagePicker/index.vue";
import { http } from "@/utils/http";
defineOptions({ name: "ShopDisplaySettings" });
const form = reactive({ displayName: "", logoUrl: "", introduction: "", customerService: "" });
const busy = ref(false);
onMounted(async () => Object.assign(form, await http.get<Record<string, string>, undefined>("/admin-api/system/shop")));
async function save() {
  if (!form.displayName.trim()) { ElMessage.warning("请填写店铺展示名称"); return; }
  busy.value = true;
  try {
    await http.post<boolean, typeof form>("/admin-api/system/shop", { data: { ...form } });
    ElMessage.success("已保存，重新打开小程序后更新展示");
  } finally { busy.value = false; }
}
</script>
<template>
  <el-card>
    <template #header>店铺展示设置</template>
    <el-alert title="此处修改页面展示，不修改微信公众平台账号名称或备案信息。" :closable="false" class="mb-4" />
    <el-form label-width="110px" style="max-width: 680px">
      <el-form-item label="店铺展示名" required><el-input v-model="form.displayName" maxlength="30" show-word-limit /></el-form-item>
      <el-form-item label="店铺 Logo"><MaterialImagePicker v-model="form.logoUrl" biz-type="content" empty-text="从素材库选择店铺 Logo" /></el-form-item>
      <el-form-item label="店铺简介"><el-input v-model="form.introduction" type="textarea" maxlength="200" show-word-limit /></el-form-item>
      <el-form-item label="客服联系方式"><el-input v-model="form.customerService" maxlength="100" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="busy" @click="save">保存设置</el-button></el-form-item>
    </el-form>
  </el-card>
</template>
