<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { UploadRequestOptions } from "element-plus";
import { CopyDocument, Delete, Picture, Refresh, Search, UploadFilled, View } from "@element-plus/icons-vue";
import {
  deleteMaterial,
  getMaterialPage,
  getMaterialReferences,
  uploadMaterial
} from "@/api/material";
import type { MaterialAsset } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "ProductMaterial" });
const canManageMaterial = hasAnyPerms(["material:manage"]);

type UploadError = Parameters<NonNullable<UploadRequestOptions["onError"]>>[0];

const loading = ref(false);
const uploading = ref(false);
const list = ref<MaterialAsset[]>([]);
const total = ref(0);
const uploadBizType = ref("product");
const referencesVisible = ref(false);
const references = ref<string[]>([]);
const currentAsset = ref<MaterialAsset>();

const query = reactive({
  pageNo: 1,
  pageSize: 12,
  bizType: "",
  keyword: "",
  createdBy: undefined as number | undefined,
  timeRange: [] as string[]
});

const bizTypeOptions = [
  { label: "商品素材", value: "product" },
  { label: "内容素材", value: "content" },
  { label: "通用素材", value: "common" }
];

async function fetchData() {
  loading.value = true;
  try {
    const page = await getMaterialPage({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      bizType: query.bizType || undefined,
      keyword: query.keyword.trim() || undefined,
      createdBy: query.createdBy,
      startTime: query.timeRange?.[0],
      endTime: query.timeRange?.[1]
    });
    list.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  fetchData();
}

function handleReset() {
  query.bizType = "";
  query.keyword = "";
  query.createdBy = undefined;
  query.timeRange = [];
  query.pageNo = 1;
  fetchData();
}

function beforeUpload(file: File) {
  const allowedTypes = ["image/jpeg", "image/png", "image/webp"];
  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning("仅支持 JPG、PNG、WebP 图片");
    return false;
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning("图片不能超过 5MB");
    return false;
  }
  return true;
}

async function handleUpload(options: UploadRequestOptions) {
  uploading.value = true;
  try {
    await uploadMaterial(options.file as File, uploadBizType.value);
    ElMessage.success("上传成功");
    options.onSuccess?.({});
    fetchData();
  } catch (error) {
    const uploadError = Object.assign(error instanceof Error ? error : new Error("上传失败"), {
      status: 0,
      method: "post",
      url: "/admin-api/material/upload"
    }) as UploadError;
    options.onError?.(uploadError);
  } finally {
    uploading.value = false;
  }
}

async function copyUrl(row: MaterialAsset) {
  await navigator.clipboard.writeText(row.url);
  ElMessage.success("图片 URL 已复制");
}

async function showReferences(row: MaterialAsset) {
  currentAsset.value = row;
  references.value = await getMaterialReferences(row.id);
  referencesVisible.value = true;
  if (references.value.length === 0) {
    ElMessage.success("当前素材未被引用");
    fetchData();
  }
}

async function handleDelete(row: MaterialAsset) {
  if (row.referenceCount > 0) {
    await showReferences(row);
    return;
  }
  await ElMessageBox.confirm(`确定删除素材「${row.fileName}」吗？`, "确认删除", { type: "warning" });
  await deleteMaterial(row.id);
  ElMessage.success("删除成功");
  fetchData();
}

function formatFileSize(size: number) {
  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(2)} MB`;
  }
  return `${Math.max(1, Math.round(size / 1024))} KB`;
}

function formatImageSize(row: MaterialAsset) {
  if (!row.width || !row.height) {
    return "—";
  }
  return `${row.width} x ${row.height}`;
}

onMounted(fetchData);
</script>

<template>
  <div class="app-container">
    <el-card shadow="never" class="mb-4">
      <div class="toolbar">
        <el-form :inline="true" :model="query" class="query-form" @submit.prevent="handleSearch">
          <el-form-item label="业务类型">
            <el-select v-model="query.bizType" clearable placeholder="全部类型" style="width: 140px">
              <el-option
                v-for="item in bizTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input
              v-model="query.keyword"
              clearable
              maxlength="80"
              placeholder="文件名或 URL"
              style="width: 240px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="上传人">
            <el-input-number
              v-model="query.createdBy"
              :min="1"
              :max="999999999"
              controls-position="right"
              placeholder="管理员ID"
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item label="上传时间">
            <el-date-picker
              v-model="query.timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              range-separator="至"
              style="width: 360px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div v-if="canManageMaterial" class="upload-box">
          <el-select v-model="uploadBizType" style="width: 120px">
            <el-option
              v-for="item in bizTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleUpload"
            accept="image/jpeg,image/png,image/webp"
          >
            <el-button type="primary" :icon="UploadFilled" :loading="uploading">上传图片</el-button>
          </el-upload>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" border>
        <el-table-column label="图片" width="120" align="center">
          <template #default="{ row }">
            <el-image
              class="thumb"
              :src="row.url"
              fit="cover"
              :preview-src-list="[row.url]"
              preview-teleported
            >
              <template #error>
                <div class="thumb-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bizType" label="业务类型" width="100" align="center" />
        <el-table-column label="尺寸" width="110" align="center">
          <template #default="{ row }">{{ formatImageSize(row) }}</template>
        </el-table-column>
        <el-table-column label="大小" width="100" align="center">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="referenceCount" label="引用数" width="90" align="center" />
        <el-table-column prop="createTime" label="上传时间" width="170" align="center" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :icon="CopyDocument" @click="copyUrl(row)">
              复制
            </el-button>
            <el-button type="primary" link size="small" :icon="View" @click="showReferences(row)">
              引用
            </el-button>
            <el-button v-if="canManageMaterial" type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :page-sizes="[12, 24, 48, 96]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="referencesVisible" title="素材引用" width="560px">
      <div v-if="currentAsset" class="asset-summary">
        <el-image class="summary-thumb" :src="currentAsset.url" fit="cover" />
        <div class="summary-main">
          <div class="summary-title">{{ currentAsset.fileName }}</div>
          <div class="summary-url">{{ currentAsset.url }}</div>
        </div>
      </div>
      <el-empty v-if="references.length === 0" description="当前素材未被引用，可以删除" />
      <el-scrollbar v-else max-height="300px">
        <el-timeline>
          <el-timeline-item v-for="item in references" :key="item">
            {{ item }}
          </el-timeline-item>
        </el-timeline>
      </el-scrollbar>
      <template #footer>
        <el-button @click="referencesVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.app-container { padding: 16px; }
.mb-4 { margin-bottom: 16px; }
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.query-form { flex: 1; }
.upload-box {
  display: flex;
  align-items: center;
  gap: 8px;
}
.thumb {
  width: 72px;
  height: 72px;
  border-radius: 4px;
}
.thumb-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  color: #909399;
  background: #f5f7fa;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.asset-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.summary-thumb {
  width: 64px;
  height: 64px;
  border-radius: 4px;
  flex: 0 0 auto;
}
.summary-main {
  min-width: 0;
  flex: 1;
}
.summary-title {
  font-weight: 600;
  color: #303133;
}
.summary-url {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  word-break: break-all;
}
@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
  }
  .upload-box {
    width: 100%;
  }
}
</style>
