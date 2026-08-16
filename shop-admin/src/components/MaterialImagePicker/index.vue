<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { UploadRequestOptions } from "element-plus";
import Sortable from "sortablejs";
import { Close, Picture, Rank, Search, UploadFilled } from "@element-plus/icons-vue";
import { getMaterialPage, uploadMaterial } from "@/api/material";
import type { MaterialAsset } from "@/api/types";

defineOptions({ name: "MaterialImagePicker" });

const props = withDefaults(defineProps<{
  modelValue?: string | string[];
  multiple?: boolean;
  bizType?: string;
  max?: number;
  emptyText?: string;
}>(), {
  multiple: false,
  bizType: "product",
  max: 10,
  emptyText: "未选择图片"
});

const emit = defineEmits<{
  "update:modelValue": [value: string | string[]];
}>();

type UploadError = Parameters<NonNullable<UploadRequestOptions["onError"]>>[0];

const uploading = ref(false);
const dialogVisible = ref(false);
const dialogLoading = ref(false);
const materialList = ref<MaterialAsset[]>([]);
const materialTotal = ref(0);
const selectedListRef = ref<HTMLElement>();
let sortable: Sortable | undefined;

const query = reactive({
  pageNo: 1,
  pageSize: 12,
  keyword: ""
});

const selectedUrls = computed(() => {
  if (props.multiple) {
    return Array.isArray(props.modelValue) ? props.modelValue.filter(Boolean) : [];
  }
  return typeof props.modelValue === "string" && props.modelValue ? [props.modelValue] : [];
});

function updateUrls(urls: string[]) {
  const normalized = urls.filter(Boolean);
  emit("update:modelValue", props.multiple ? normalized.slice(0, props.max) : (normalized[0] || ""));
}

function addUrl(url: string) {
  if (!url) return;
  if (!props.multiple) {
    updateUrls([url]);
    dialogVisible.value = false;
    return;
  }
  const next = selectedUrls.value.includes(url)
    ? selectedUrls.value
    : selectedUrls.value.concat(url);
  if (next.length > props.max) {
    ElMessage.warning(`最多选择 ${props.max} 张图片`);
    return;
  }
  updateUrls(next);
}

function removeUrl(index: number) {
  const next = selectedUrls.value.slice();
  next.splice(index, 1);
  updateUrls(next);
}

function clearSingle() {
  updateUrls([]);
}

async function fetchMaterials() {
  dialogLoading.value = true;
  try {
    const page = await getMaterialPage({
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      bizType: props.bizType,
      keyword: query.keyword.trim() || undefined
    });
    materialList.value = page.list;
    materialTotal.value = page.total;
  } finally {
    dialogLoading.value = false;
  }
}

function openDialog() {
  query.pageNo = 1;
  dialogVisible.value = true;
  fetchMaterials();
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
    const asset = await uploadMaterial(options.file as File, props.bizType);
    addUrl(asset.url);
    ElMessage.success("上传成功");
    options.onSuccess?.({});
    fetchMaterials();
  } catch (error) {
    ElMessage.error(extractErrorMessage(error));
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

function extractErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) return error.message;
  if (typeof error === "string" && error.trim()) return error;
  return "上传失败，请稍后重试";
}

function initSortable() {
  if (!props.multiple || !selectedListRef.value || sortable) return;
  sortable = Sortable.create(selectedListRef.value, {
    animation: 150,
    handle: ".drag-handle",
    onEnd(event) {
      if (event.oldIndex == null || event.newIndex == null || event.oldIndex === event.newIndex) return;
      const next = selectedUrls.value.slice();
      const [moved] = next.splice(event.oldIndex, 1);
      next.splice(event.newIndex, 0, moved);
      updateUrls(next);
    }
  });
}

onMounted(() => nextTick(initSortable));
onBeforeUnmount(() => {
  sortable?.destroy();
  sortable = undefined;
});

watch(() => props.multiple, () => {
  sortable?.destroy();
  sortable = undefined;
  nextTick(initSortable);
});
</script>

<template>
  <div class="material-picker">
    <div v-if="multiple" ref="selectedListRef" class="selected-list">
      <div v-for="(url, index) in selectedUrls" :key="url" class="selected-item">
        <el-image class="image-thumb" :src="url" fit="cover" :preview-src-list="[url]" preview-teleported>
          <template #error>
            <div class="image-error">
              <el-icon><Picture /></el-icon>
            </div>
          </template>
        </el-image>
        <el-button class="drag-handle" text :icon="Rank" />
        <el-button class="remove-btn" type="danger" circle size="small" :icon="Close" @click="removeUrl(index)" />
      </div>
      <div v-if="selectedUrls.length === 0" class="empty-block">{{ emptyText }}</div>
    </div>

    <div v-else class="single-box">
      <el-image v-if="selectedUrls[0]" class="single-thumb" :src="selectedUrls[0]" fit="cover" :preview-src-list="[selectedUrls[0]]" preview-teleported>
        <template #error>
          <div class="single-error">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
      </el-image>
      <div v-else class="single-empty">{{ emptyText }}</div>
      <el-button v-if="selectedUrls[0]" type="danger" link @click="clearSingle">清空</el-button>
    </div>

    <div class="actions">
      <el-upload
        :show-file-list="false"
        :before-upload="beforeUpload"
        :http-request="handleUpload"
        accept="image/jpeg,image/png,image/webp"
      >
        <el-button :icon="UploadFilled" :loading="uploading">上传图片</el-button>
      </el-upload>
      <el-button :icon="Search" @click="openDialog">选择素材</el-button>
    </div>

    <el-dialog v-model="dialogVisible" title="选择素材" width="820px" destroy-on-close>
      <div class="dialog-toolbar">
        <el-input
          v-model="query.keyword"
          clearable
          maxlength="80"
          placeholder="按文件名或 URL 搜索"
          style="width: 260px"
          @keyup.enter="fetchMaterials"
        />
        <el-button type="primary" :icon="Search" @click="fetchMaterials">查询</el-button>
      </div>

      <div v-loading="dialogLoading" class="material-grid">
        <button
          v-for="item in materialList"
          :key="item.id"
          type="button"
          class="material-card"
          :class="{ selected: selectedUrls.includes(item.url) }"
          @click="addUrl(item.url)"
        >
          <el-image class="material-image" :src="item.url" fit="cover" />
          <span class="material-name">{{ item.fileName }}</span>
        </button>
        <el-empty v-if="!dialogLoading && materialList.length === 0" description="暂无素材" />
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next"
          :total="materialTotal"
          @size-change="fetchMaterials"
          @current-change="fetchMaterials"
        />
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.material-picker {
  width: 100%;
}
.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 88px;
}
.selected-item {
  position: relative;
  width: 88px;
  height: 88px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #f5f7fa;
}
.image-thumb,
.image-error {
  width: 88px;
  height: 88px;
}
.image-error,
.single-error,
.single-empty,
.empty-block {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f5f7fa;
}
.drag-handle {
  position: absolute;
  left: 4px;
  bottom: 4px;
  color: #ffffff;
  background: rgba(0, 0, 0, 0.45);
}
.remove-btn {
  position: absolute;
  right: 4px;
  top: 4px;
}
.empty-block {
  width: 160px;
  height: 88px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  font-size: 13px;
}
.single-box {
  display: flex;
  align-items: center;
  gap: 12px;
}
.single-thumb,
.single-error,
.single-empty {
  width: 120px;
  height: 90px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.dialog-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
  gap: 12px;
  min-height: 260px;
}
.material-card {
  padding: 0;
  overflow: hidden;
  text-align: left;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.material-card.selected {
  border-color: #409eff;
  box-shadow: 0 0 0 1px #409eff inset;
}
.material-image {
  width: 100%;
  height: 96px;
  display: block;
}
.material-name {
  display: block;
  padding: 8px;
  color: #606266;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
