<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  getNotifyConfig,
  getNotifyLogs,
  saveNotifyConfig,
  sendTestNotifyMail,
  type NotifyConfig,
  type NotifyLog
} from "@/api/notify";

defineOptions({ name: "ShopNotifySettings" });

const loading = ref(false);
const saving = ref(false);
const testing = ref(false);

const form = reactive<NotifyConfig>({
  enabled: 0,
  mailEnabled: 1,
  mailRecipients: "",
  smtpHost: "",
  smtpPort: 465,
  smtpUsername: "",
  smtpPassword: "",
  smtpPasswordConfigured: false,
  mailFrom: "",
  sslEnabled: 1
});

const logs = ref<NotifyLog[]>([]);
const logTotal = ref(0);
const logLoading = ref(false);
const logQuery = reactive({ pageNo: 1, pageSize: 10 });

async function fetchConfig() {
  loading.value = true;
  try {
    const data = await getNotifyConfig();
    Object.assign(form, data);
  } finally {
    loading.value = false;
  }
}

async function fetchLogs() {
  logLoading.value = true;
  try {
    const result = await getNotifyLogs(logQuery);
    logs.value = result.list ?? [];
    logTotal.value = result.total ?? 0;
  } finally {
    logLoading.value = false;
  }
}

async function handleSave() {
  saving.value = true;
  try {
    const data = await saveNotifyConfig({
      enabled: form.enabled,
      mailEnabled: form.mailEnabled,
      mailRecipients: form.mailRecipients,
      smtpHost: form.smtpHost,
      smtpPort: form.smtpPort,
      smtpUsername: form.smtpUsername,
      smtpPassword: form.smtpPassword,
      mailFrom: form.mailFrom,
      sslEnabled: form.sslEnabled
    });
    Object.assign(form, data);
    ElMessage.success("通知设置已保存");
  } finally {
    saving.value = false;
  }
}

async function handleTest() {
  testing.value = true;
  try {
    await sendTestNotifyMail();
    ElMessage.success("测试邮件已发送，请检查收件箱");
    logQuery.pageNo = 1;
    await fetchLogs();
  } finally {
    testing.value = false;
  }
}

onMounted(async () => {
  await fetchConfig();
  await fetchLogs();
});
</script>

<template>
  <div class="notify-page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>新订单通知</h3>
            <p>客户在小程序支付成功后，立即推送提醒商家备货发货。</p>
          </div>
          <el-button type="primary" :loading="saving" @click="handleSave"
            >保存设置</el-button
          >
        </div>
      </template>

      <el-form label-width="130px" class="notify-form">
        <el-form-item label="开启通知">
          <el-switch
            v-model="form.enabled"
            :active-value="1"
            :inactive-value="0"
            active-text="开启"
            inactive-text="关闭"
          />
          <span class="form-tip">关闭后不再推送任何新订单提醒</span>
        </el-form-item>

        <el-form-item label="通知方式">
          <el-checkbox v-model="form.mailEnabled" :true-value="1" :false-value="0">
            邮件
          </el-checkbox>
          <span class="form-tip">当前版本支持邮件通知，后续可扩展短信等渠道</span>
        </el-form-item>

        <el-form-item label="收件人邮箱">
          <el-input
            v-model="form.mailRecipients"
            type="textarea"
            :rows="2"
            maxlength="1024"
            placeholder="多个邮箱用英文逗号分隔，例如：boss@shop.com,warehouse@shop.com"
          />
        </el-form-item>

        <el-divider content-position="left">邮件服务器（SMTP）</el-divider>

        <el-form-item label="SMTP 服务器">
          <el-input v-model="form.smtpHost" placeholder="例如：smtp.qq.com" />
        </el-form-item>
        <el-form-item label="SMTP 端口">
          <el-input-number v-model="form.smtpPort" :min="1" :max="65535" />
          <span class="form-tip">SSL 常用 465，STARTTLS 常用 587</span>
        </el-form-item>
        <el-form-item label="登录账号">
          <el-input v-model="form.smtpUsername" placeholder="通常是完整邮箱地址" />
        </el-form-item>
        <el-form-item label="授权码">
          <el-input
            v-model="form.smtpPassword"
            type="password"
            show-password
            placeholder="邮箱 SMTP 授权码，不是登录密码"
          />
          <span v-if="form.smtpPasswordConfigured" class="form-tip">
            已配置，保留 ****** 表示不修改
          </span>
        </el-form-item>
        <el-form-item label="发件人地址">
          <el-input v-model="form.mailFrom" placeholder="留空默认使用登录账号" />
        </el-form-item>
        <el-form-item label="连接方式">
          <el-radio-group v-model="form.sslEnabled">
            <el-radio-button :value="1">SSL 直连（465）</el-radio-button>
            <el-radio-button :value="0">STARTTLS（587/25）</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button @click="handleSave" :loading="saving">保存设置</el-button>
          <el-button
            type="success"
            plain
            :loading="testing"
            :disabled="!form.smtpHost"
            @click="handleTest"
            >发送测试邮件</el-button
          >
          <span class="form-tip">测试邮件使用已保存的配置，请先保存再测试</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="log-card">
      <template #header>
        <div class="card-header">
          <h3>最近推送记录</h3>
          <el-button :loading="logLoading" @click="fetchLogs">刷新</el-button>
        </div>
      </template>
      <el-table native-scrollbar v-loading="logLoading" :data="logs" border>
        <el-table-column prop="orderSn" label="订单号" min-width="180" />
        <el-table-column prop="channel" label="渠道" width="90" align="center" />
        <el-table-column prop="receiver" label="接收方" min-width="220" show-overflow-tooltip />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? "成功" : "失败" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发送时间" width="180" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="logQuery.pageNo"
          v-model:page-size="logQuery.pageSize"
          :total="logTotal"
          layout="total, prev, pager, next"
          @current-change="fetchLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.notify-page {
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-header h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.card-header p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.notify-form {
  max-width: 760px;
}

.form-tip {
  margin-left: 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.log-card {
  margin-top: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
