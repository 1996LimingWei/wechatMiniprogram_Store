<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { QuestionFilled } from "@element-plus/icons-vue";
import {
    getTopicList,
    createTopic,
    updateTopic,
    deleteTopic,
    getTopicProducts,
    setTopicProducts
} from "@/api/content";
import { getProductPage } from "@/api/product";
import type { ContentTopic, ProductSpu } from "@/api/types";

defineOptions({ name: "ContentTopic" });

/* ---------- 数据 ---------- */
const loading = ref(false);
const list = ref<ContentTopic[]>([]);

async function fetchData() {
    loading.value = true;
    try {
        list.value = (await getTopicList()) as ContentTopic[];
    } finally {
        loading.value = false;
    }
}

/* ---------- 对话框 ---------- */
const dialogVisible = ref(false);
const dialogTitle = ref("新增专题");
const submitting = ref(false);
const form = reactive({
    id: undefined as number | undefined,
    title: "",
    subtitle: "",
    picUrl: "",
    priceInfo: "",
    sort: 0,
    status: 1
});

function resetForm() {
    form.id = undefined;
    form.title = "";
    form.subtitle = "";
    form.picUrl = "";
    form.priceInfo = "";
    form.sort = 0;
    form.status = 1;
}

function openAdd() {
    resetForm();
    dialogTitle.value = "新增专题";
    dialogVisible.value = true;
}

function openEdit(row: ContentTopic) {
    resetForm();
    form.id = row.id;
    form.title = row.title;
    form.subtitle = row.subtitle || "";
    form.picUrl = row.picUrl || "";
    form.priceInfo = row.priceInfo || "";
    form.sort = row.sort ?? 0;
    form.status = row.status ?? 1;
    dialogTitle.value = "编辑专题";
    dialogVisible.value = true;
}

async function handleSubmit() {
    if (!form.title.trim()) {
        ElMessage.warning("请输入专题标题");
        return;
    }
    submitting.value = true;
    try {
        const payload: ContentTopic = {
            title: form.title.trim(),
            subtitle: form.subtitle.trim(),
            picUrl: form.picUrl.trim(),
            priceInfo: form.priceInfo.trim(),
            sort: form.sort,
            status: form.status
        };
        if (form.id) {
            (payload as any).id = form.id;
            await updateTopic(payload);
            ElMessage.success("更新成功");
        } else {
            await createTopic(payload);
            ElMessage.success("创建成功");
        }
        dialogVisible.value = false;
        fetchData();
    } finally {
        submitting.value = false;
    }
}

/* ---------- 删除 ---------- */
async function handleDelete(row: ContentTopic) {
    await ElMessageBox.confirm(
        `确定删除专题「${row.title}」吗？`,
        "确认删除",
        { type: "warning" }
    );
    await deleteTopic(row.id!);
    ElMessage.success("删除成功");
    fetchData();
}

/* ---------- 状态切换 ---------- */
async function handleStatusChange(row: ContentTopic) {
    const payload: any = { id: row.id, status: row.status };
    await updateTopic(payload);
    ElMessage.success(row.status === 1 ? "已启用" : "已禁用");
}

/* ---------- 关联商品 ---------- */
const productDialogVisible = ref(false);
const productDialogLoading = ref(false);
const currentTopicId = ref<number>(0);
const currentTopicTitle = ref("");

// 所有可选商品
const allProducts = ref<ProductSpu[]>([]);
// 已选中的商品ID列表
const selectedSpuIds = ref<number[]>([]);

async function openProductDialog(row: ContentTopic) {
    currentTopicId.value = row.id!;
    currentTopicTitle.value = row.title;
    productDialogLoading.value = true;
    productDialogVisible.value = true;

    try {
        // 并行加载：当前关联 + 所有商品
        const [currentIds, productPage] = await Promise.all([
            getTopicProducts(row.id!) as Promise<number[]>,
            getProductPage({ pageNo: 1, pageSize: 200, status: 1 }) as Promise<any>
        ]);
        selectedSpuIds.value = currentIds || [];
        allProducts.value = productPage?.list || [];
    } finally {
        productDialogLoading.value = false;
    }
}

function isSelected(spuId: number) {
    return selectedSpuIds.value.includes(spuId);
}

function toggleProduct(spuId: number) {
    const idx = selectedSpuIds.value.indexOf(spuId);
    if (idx > -1) {
        selectedSpuIds.value.splice(idx, 1);
    } else {
        selectedSpuIds.value.push(spuId);
    }
}

async function handleSaveProducts() {
    productDialogLoading.value = true;
    try {
        await setTopicProducts({
            topicId: currentTopicId.value,
            spuIds: selectedSpuIds.value
        });
        ElMessage.success("关联商品已保存");
        productDialogVisible.value = false;
    } finally {
        productDialogLoading.value = false;
    }
}

function formatPrice(fen: number | undefined) {
    if (fen == null) return "—";
    return "¥" + (fen / 100).toFixed(2);
}

onMounted(fetchData);
</script>

<template>
    <div class="app-container">
        <!-- 顶部操作栏 -->
        <el-card shadow="never" class="mb-4">
            <el-button type="primary" @click="openAdd">
                <el-icon class="mr-1"><i class="ep-icon-plus" /></el-icon>
                新增专题
            </el-button>
        </el-card>

        <!-- 表格 -->
        <el-card shadow="never">
            <el-table :data="list" v-loading="loading" border>
                <el-table-column prop="id" label="ID" width="70" align="center" />
                <el-table-column label="场景图片" width="130" align="center">
                    <template #default="{ row }">
                        <el-image
                            v-if="row.picUrl"
                            :src="row.picUrl"
                            style="width: 90px; height: 60px; border-radius: 4px"
                            fit="cover"
                            :preview-src-list="[row.picUrl]"
                        />
                        <span v-else class="text-gray-400">—</span>
                    </template>
                </el-table-column>
                <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
                <el-table-column prop="subtitle" label="副标题" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span>{{ row.subtitle || '—' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="priceInfo" label="价格说明" width="100" align="center">
                    <template #default="{ row }">
                        <span>{{ row.priceInfo || '—' }}</span>
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
                <el-table-column label="操作" width="230" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button type="success" link size="small" @click="openProductDialog(row)">
                            关联商品
                        </el-button>
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
                <el-form-item label="专题标题" required>
                    <el-input v-model="form.title" placeholder="请输入专题标题" maxlength="50" />
                </el-form-item>
                <el-form-item label="副标题">
                    <el-input v-model="form.subtitle" placeholder="请输入副标题（可选）" maxlength="100" />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        场景图片
                        <el-tooltip content="专题列表中展示的场景图，填写可访问的图片链接" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <el-input v-model="form.picUrl" placeholder="请输入场景图片 URL（可选）" />
                </el-form-item>
                <el-form-item v-if="form.picUrl" label="图片预览">
                    <el-image
                        :src="form.picUrl"
                        style="width: 180px; height: 120px; border-radius: 4px"
                        fit="cover"
                    >
                        <template #error>
                            <div class="img-error">图片加载失败</div>
                        </template>
                    </el-image>
                </el-form-item>
                <el-form-item>
                    <template #label>
                        价格说明
                        <el-tooltip content="专题卡片上展示的起始价格文字，如 49.9" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <el-input v-model="form.priceInfo" placeholder="如 49.9（可选）" maxlength="30" />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        排序权重
                        <el-tooltip content="数值越大，专题在小程序中显示越靠前" placement="top">
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

        <!-- 关联商品 对话框 -->
        <el-dialog
            v-model="productDialogVisible"
            :title="`关联商品 — ${currentTopicTitle}`"
            width="700px"
            destroy-on-close
        >
            <div v-loading="productDialogLoading">
                <p class="product-hint">点击商品卡片选中或取消，已选 <strong>{{ selectedSpuIds.length }}</strong> 件商品</p>
                <div class="product-grid">
                    <div
                        v-for="product in allProducts"
                        :key="product.id"
                        class="product-card"
                        :class="{ selected: isSelected(product.id!) }"
                        @click="toggleProduct(product.id!)"
                    >
                        <el-image
                            :src="product.picUrl"
                            style="width: 100%; height: 100px"
                            fit="cover"
                        />
                        <div class="product-card-info">
                            <div class="product-card-name">{{ product.name }}</div>
                            <div class="product-card-price">{{ formatPrice(product.price) }}</div>
                        </div>
                        <div class="product-card-check" v-if="isSelected(product.id!)">✓</div>
                    </div>
                </div>
                <el-empty v-if="allProducts.length === 0" description="暂无商品" />
            </div>
            <template #footer>
                <el-button @click="productDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="productDialogLoading" @click="handleSaveProducts">保存关联</el-button>
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
.img-error { display: flex; align-items: center; justify-content: center; width: 180px; height: 120px; background: #f5f7fa; color: #909399; font-size: 13px; border-radius: 4px; }

/* 关联商品 */
.product-hint { margin-bottom: 12px; color: #606266; font-size: 14px; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; max-height: 420px; overflow-y: auto; }
.product-card { position: relative; border: 2px solid #e4e7ed; border-radius: 8px; overflow: hidden; cursor: pointer; transition: all 0.2s; }
.product-card:hover { border-color: #67c23a; box-shadow: 0 2px 8px rgba(103,194,58,0.15); }
.product-card.selected { border-color: #67c23a; background: #f0f9eb; }
.product-card-check { position: absolute; top: 6px; right: 6px; width: 22px; height: 22px; border-radius: 50%; background: #67c23a; color: #fff; font-size: 13px; display: flex; align-items: center; justify-content: center; font-weight: bold; }
.product-card-info { padding: 6px 8px; }
.product-card-name { font-size: 12px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-card-price { font-size: 13px; color: #e6a23c; font-weight: 600; margin-top: 2px; }
</style>
