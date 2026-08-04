<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { QuestionFilled } from "@element-plus/icons-vue";
import {
    getBrandList,
    createBrand,
    updateBrand,
    deleteBrand
} from "@/api/content";
import type { ContentBrand } from "@/api/types";

defineOptions({ name: "ContentBrand" });

/* ---------- 数据 ---------- */
const loading = ref(false);
const list = ref<ContentBrand[]>([]);

async function fetchData() {
    loading.value = true;
    try {
        list.value = (await getBrandList()) as ContentBrand[];
    } finally {
        loading.value = false;
    }
}

/** 分 → 元 显示 */
function formatPrice(fen: number | undefined) {
    if (fen == null) return "—";
    return "¥" + (fen / 100).toFixed(2);
}

/* ---------- 对话框 ---------- */
const dialogVisible = ref(false);
const dialogTitle = ref("新增品牌");
const submitting = ref(false);
const form = reactive({
    id: undefined as number | undefined,
    name: "",
    picUrl: "",
    floorPriceYuan: 0,
    sort: 0,
    status: 1
});

function resetForm() {
    form.id = undefined;
    form.name = "";
    form.picUrl = "";
    form.floorPriceYuan = 0;
    form.sort = 0;
    form.status = 1;
}

function openAdd() {
    resetForm();
    dialogTitle.value = "新增品牌";
    dialogVisible.value = true;
}

function openEdit(row: ContentBrand) {
    resetForm();
    form.id = row.id;
    form.name = row.name;
    form.picUrl = row.picUrl || "";
    form.floorPriceYuan = row.floorPrice != null ? row.floorPrice / 100 : 0;
    form.sort = row.sort ?? 0;
    form.status = row.status ?? 1;
    dialogTitle.value = "编辑品牌";
    dialogVisible.value = true;
}

async function handleSubmit() {
    if (!form.name.trim()) {
        ElMessage.warning("请输入品牌名称");
        return;
    }
    submitting.value = true;
    try {
        const payload: ContentBrand = {
            name: form.name.trim(),
            picUrl: form.picUrl.trim(),
            floorPrice: Math.round(form.floorPriceYuan * 100),
            sort: form.sort,
            status: form.status
        };
        if (form.id) {
            (payload as any).id = form.id;
            await updateBrand(payload);
            ElMessage.success("更新成功");
        } else {
            await createBrand(payload);
            ElMessage.success("创建成功");
        }
        dialogVisible.value = false;
        fetchData();
    } finally {
        submitting.value = false;
    }
}

/* ---------- 删除 ---------- */
async function handleDelete(row: ContentBrand) {
    await ElMessageBox.confirm(
        `确定删除品牌「${row.name}」吗？`,
        "确认删除",
        { type: "warning" }
    );
    await deleteBrand(row.id!);
    ElMessage.success("删除成功");
    fetchData();
}

/* ---------- 状态切换 ---------- */
async function handleStatusChange(row: ContentBrand) {
    const payload: any = { id: row.id, status: row.status };
    await updateBrand(payload);
    ElMessage.success(row.status === 1 ? "已启用" : "已禁用");
}

onMounted(fetchData);
</script>

<template>
    <div class="app-container">
        <!-- 顶部操作栏 -->
        <el-card shadow="never" class="mb-4">
            <el-button type="primary" @click="openAdd">
                <el-icon class="mr-1"><i class="ep-icon-plus" /></el-icon>
                新增品牌
            </el-button>
        </el-card>

        <!-- 表格 -->
        <el-card shadow="never">
            <el-table :data="list" v-loading="loading" border>
                <el-table-column prop="id" label="ID" width="70" align="center" />
                <el-table-column label="品牌图片" width="120" align="center">
                    <template #default="{ row }">
                        <el-image
                            v-if="row.picUrl"
                            :src="row.picUrl"
                            style="width: 80px; height: 50px; border-radius: 4px"
                            fit="cover"
                            :preview-src-list="[row.picUrl]"
                        />
                        <span v-else class="text-gray-400">—</span>
                    </template>
                </el-table-column>
                <el-table-column prop="name" label="品牌名称" min-width="140" show-overflow-tooltip />
                <el-table-column label="起售价" width="110" align="center">
                    <template #default="{ row }">
                        <span>{{ formatPrice(row.floorPrice) }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序" width="80" align="center" />
                <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-switch
                            v-model="row.status"
                            :active-value="1"
                            :inactive-value="0"
                            @change="handleStatusChange(row)"
                        />
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
                <el-table-column label="操作" width="160" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link size="small" @click="openEdit(row)">
                            编辑
                        </el-button>
                        <el-button type="danger" link size="small" @click="handleDelete(row)">
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新增/编辑 对话框 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
            <el-form :model="form" label-width="100px" @submit.prevent="handleSubmit">
                <el-form-item label="品牌名称" required>
                    <el-input v-model="form.name" placeholder="请输入品牌名称" maxlength="50" />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        品牌图片
                        <el-tooltip content="品牌列表展示的图片，填写可访问的图片链接" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <el-input v-model="form.picUrl" placeholder="请输入品牌图片 URL（可选）" />
                </el-form-item>
                <el-form-item v-if="form.picUrl" label="图片预览">
                    <el-image
                        :src="form.picUrl"
                        style="width: 160px; height: 100px; border-radius: 4px"
                        fit="cover"
                    >
                        <template #error>
                            <div class="img-error">图片加载失败</div>
                        </template>
                    </el-image>
                </el-form-item>
                <el-form-item>
                    <template #label>
                        起售价
                        <el-tooltip content="品牌列表中展示的最低价格，以元为单位，后端存储为分" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <el-input-number
                        v-model="form.floorPriceYuan"
                        :min="0"
                        :precision="2"
                        :step="1"
                        controls-position="right"
                        style="width: 200px"
                    />
                    <span class="price-hint">元</span>
                </el-form-item>
                <el-form-item>
                    <template #label>
                        排序权重
                        <el-tooltip content="数值越大，品牌在小程序中显示越靠前" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">启用</el-radio>
                        <el-radio :value="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.app-container { padding: 16px; }
.mb-4 { margin-bottom: 16px; }
.mr-1 { margin-right: 4px; }
.text-gray-400 { color: #9ca3af; }
.tip-icon { margin-left: 4px; color: #909399; cursor: help; vertical-align: middle; }
.img-error { display: flex; align-items: center; justify-content: center; width: 160px; height: 100px; background: #f5f7fa; color: #909399; font-size: 13px; border-radius: 4px; }
.price-hint { margin-left: 8px; color: #909399; }
</style>
