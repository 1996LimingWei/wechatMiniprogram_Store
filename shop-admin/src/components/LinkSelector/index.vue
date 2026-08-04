<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { getProductPage } from "@/api/product";
import { getTopicList } from "@/api/content";
import type { ProductSpu, ContentTopic } from "@/api/types";

defineOptions({ name: "LinkSelector" });

const props = defineProps<{
    modelValue: string;
    placeholder?: string;
}>();

const emit = defineEmits<{
    "update:modelValue": [value: string];
}>();

/* ---------- 固定页面选项 ---------- */
const FIXED_PAGES = [
    { label: "首页", value: "/pages/index/index" },
    { label: "新品上市", value: "/pages/newGoods/newGoods" },
    { label: "热销商品", value: "/pages/hotGoods/hotGoods" },
    { label: "商品分类", value: "/pages/catalog/catalog" },
    { label: "购物车", value: "/pages/cart/cart" },
    { label: "搜索页", value: "/pages/search/search" },
    { label: "专题列表", value: "/pages/topic/topic" }
];

/* ---------- 类型检测 ---------- */
type LinkType = "none" | "page" | "product" | "topic" | "custom";

const linkType = ref<LinkType>("none");
const fixedPage = ref("");
const selectedProduct = ref<ProductSpu | null>(null);
const selectedTopic = ref<ContentTopic | null>(null);
const customUrl = ref("");
const initLoading = ref(false);

/* 解析 URL → 类型 */
function parseUrl(url: string): { type: LinkType; page?: string; id?: number } {
    if (!url) return { type: "none" };
    const pageMatch = FIXED_PAGES.find(p => p.value === url);
    if (pageMatch) return { type: "page", page: url };
    const goodsMatch = url.match(/^\/pages\/goods\/goods\?id=(\d+)$/);
    if (goodsMatch) return { type: "product", id: Number(goodsMatch[1]) };
    const topicMatch = url.match(/^\/pages\/topic\/topic\?id=(\d+)$/);
    if (topicMatch) return { type: "topic", id: Number(topicMatch[1]) };
    return { type: "custom" };
}

/* 生成 URL */
function buildUrl(): string {
    switch (linkType.value) {
        case "none": return "";
        case "page": return fixedPage.value;
        case "product": return selectedProduct.value ? `/pages/goods/goods?id=${selectedProduct.value.id}` : "";
        case "topic": return selectedTopic.value ? `/pages/topic/topic?id=${selectedTopic.value.id}` : "";
        case "custom": return customUrl.value;
    }
}

/* ---------- 数据加载（带缓存） ---------- */
const allProducts = ref<ProductSpu[]>([]);
const allTopics = ref<ContentTopic[]>([]);
let productsLoaded = false;
let topicsLoaded = false;

async function loadProducts() {
    if (productsLoaded) return;
    try {
        const res = await getProductPage({ pageNo: 1, pageSize: 200, status: 1 });
        allProducts.value = (res as any)?.list || [];
        productsLoaded = true;
    } catch (e) {
        console.error("[LinkSelector] loadProducts failed:", e);
        allProducts.value = [];
    }
}

async function loadTopics() {
    if (topicsLoaded) return;
    try {
        const res = await getTopicList();
        allTopics.value = (res as ContentTopic[]) || [];
        topicsLoaded = true;
    } catch (e) {
        console.error("[LinkSelector] loadTopics failed:", e);
        allTopics.value = [];
    }
}

/* ---------- 初始化：解析 URL 并填充状态 ---------- */
async function initFromUrl(url: string) {
    initLoading.value = true;
    try {
        // 重置所有状态
        linkType.value = "none";
        fixedPage.value = "";
        selectedProduct.value = null;
        selectedTopic.value = null;
        customUrl.value = "";

        if (!url) return;
        const parsed = parseUrl(url);
        linkType.value = parsed.type;

        switch (parsed.type) {
            case "page":
                fixedPage.value = parsed.page!;
                break;
            case "product":
                if (parsed.id) {
                    await loadProducts();
                    const found = allProducts.value.find((p: any) => p.id === parsed.id);
                    selectedProduct.value = found || { id: parsed.id, name: `商品 #${parsed.id}`, price: 0 } as ProductSpu;
                }
                break;
            case "topic":
                if (parsed.id) {
                    await loadTopics();
                    const found = allTopics.value.find((t: any) => t.id === parsed.id);
                    selectedTopic.value = found || { id: parsed.id, title: `专题 #${parsed.id}` } as ContentTopic;
                }
                break;
            case "custom":
                customUrl.value = url;
                break;
        }
    } finally {
        initLoading.value = false;
    }
}

/* 监听 modelValue — 组件重建时触发（配合父级 :key） */
watch(() => props.modelValue, (val) => {
    initFromUrl(val || "");
}, { immediate: true });

/* 类型切换时重置 */
function onTypeChange() {
    fixedPage.value = "";
    selectedProduct.value = null;
    selectedTopic.value = null;
    customUrl.value = "";
    productSearch.value = "";
    topicSearch.value = "";
    emitUrl();
}

function emitUrl() {
    emit("update:modelValue", buildUrl());
}

/* ---------- 商品选择 ---------- */
const productSearch = ref("");
const productDialogVisible = ref(false);

const filteredProducts = computed(() => {
    if (!productSearch.value) return allProducts.value;
    const kw = productSearch.value.toLowerCase();
    return allProducts.value.filter(p => p.name?.toLowerCase().includes(kw));
});

function openProductPicker() {
    productDialogVisible.value = true;
    productSearch.value = "";
    loadProducts();
}

function pickProduct(p: ProductSpu) {
    selectedProduct.value = p;
    productDialogVisible.value = false;
    emitUrl();
}

function clearProduct() {
    selectedProduct.value = null;
    emitUrl();
}

/* ---------- 专题选择 ---------- */
const topicSearch = ref("");
const topicDialogVisible = ref(false);

const filteredTopics = computed(() => {
    if (!topicSearch.value) return allTopics.value;
    const kw = topicSearch.value.toLowerCase();
    return allTopics.value.filter(t => t.title?.toLowerCase().includes(kw));
});

function openTopicPicker() {
    topicDialogVisible.value = true;
    topicSearch.value = "";
    loadTopics();
}

function pickTopic(t: ContentTopic) {
    selectedTopic.value = t;
    topicDialogVisible.value = false;
    emitUrl();
}

function clearTopic() {
    selectedTopic.value = null;
    emitUrl();
}

/* ---------- 固定页面选择 ---------- */
function onFixedPageChange() {
    emitUrl();
}

/* ---------- 自定义链接 ---------- */
function onCustomUrlInput() {
    emitUrl();
}

/* ---------- 显示文本 ---------- */
const displayText = computed(() => {
    switch (linkType.value) {
        case "none": return "未设置";
        case "page": return FIXED_PAGES.find(p => p.value === fixedPage.value)?.label || fixedPage.value;
        case "product": return selectedProduct.value ? `商品: ${selectedProduct.value.name}` : "请选择商品";
        case "topic": return selectedTopic.value ? `专题: ${selectedTopic.value.title}` : "请选择专题";
        case "custom": return customUrl.value || "请输入链接";
    }
});

/* 预览 URL */
const previewUrl = computed(() => buildUrl());
</script>

<template>
    <div class="link-selector">
        <!-- 类型选择 -->
        <el-radio-group v-model="linkType" size="small" @change="onTypeChange" class="link-type-group">
            <el-radio-button value="none">不跳转</el-radio-button>
            <el-radio-button value="page">固定页面</el-radio-button>
            <el-radio-button value="product">商品</el-radio-button>
            <el-radio-button value="topic">专题</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
        </el-radio-group>

        <!-- 固定页面 -->
        <div v-if="linkType === 'page'" class="link-detail">
            <el-select v-model="fixedPage" placeholder="请选择目标页面" @change="onFixedPageChange" style="width: 100%">
                <el-option
                    v-for="p in FIXED_PAGES"
                    :key="p.value"
                    :label="p.label"
                    :value="p.value"
                />
            </el-select>
        </div>

        <!-- 商品选择 -->
        <div v-if="linkType === 'product'" class="link-detail">
            <div v-if="selectedProduct" class="selected-item">
                <el-image v-if="selectedProduct.picUrl" :src="selectedProduct.picUrl" class="selected-thumb" fit="cover" />
                <div class="selected-info">
                    <div class="selected-name">{{ selectedProduct.name }}</div>
                    <div class="selected-meta">
                        <el-tag size="small" type="warning">商品</el-tag>
                        <span class="selected-price">¥{{ ((selectedProduct.price || 0) / 100).toFixed(2) }}</span>
                        <span class="selected-id">ID: {{ selectedProduct.id }}</span>
                    </div>
                </div>
                <div class="selected-actions">
                    <el-button type="danger" link size="small" @click="clearProduct">清除</el-button>
                    <el-button type="primary" link size="small" @click="openProductPicker">重选</el-button>
                </div>
            </div>
            <el-button v-else type="primary" @click="openProductPicker">选择商品</el-button>
        </div>

        <!-- 专题选择 -->
        <div v-if="linkType === 'topic'" class="link-detail">
            <div v-if="selectedTopic" class="selected-item">
                <el-image v-if="selectedTopic.picUrl" :src="selectedTopic.picUrl" class="selected-thumb" fit="cover" />
                <div class="selected-info">
                    <div class="selected-name">{{ selectedTopic.title }}</div>
                    <div class="selected-meta">
                        <el-tag size="small" type="success">专题</el-tag>
                        <span class="selected-id">ID: {{ selectedTopic.id }}</span>
                        <span v-if="selectedTopic.subtitle" class="selected-subtitle">{{ selectedTopic.subtitle }}</span>
                    </div>
                </div>
                <div class="selected-actions">
                    <el-button type="danger" link size="small" @click="clearTopic">清除</el-button>
                    <el-button type="primary" link size="small" @click="openTopicPicker">重选</el-button>
                </div>
            </div>
            <el-button v-else type="primary" @click="openTopicPicker">选择专题</el-button>
        </div>

        <!-- 自定义链接 -->
        <div v-if="linkType === 'custom'" class="link-detail">
            <el-input
                v-model="customUrl"
                placeholder="输入小程序页面路径，如 /pages/search/search"
                @input="onCustomUrlInput"
            />
        </div>

        <!-- 预览 -->
        <div v-if="linkType !== 'none' && previewUrl" class="link-preview">
            <span class="preview-label">生成路径：</span>
            <span class="preview-value">{{ previewUrl }}</span>
        </div>

        <!-- 商品选择弹窗 -->
        <el-dialog v-model="productDialogVisible" title="选择商品" width="640px" destroy-on-close append-to-body>
            <el-input
                v-model="productSearch"
                placeholder="搜索商品名称..."
                clearable
                style="margin-bottom: 12px"
            />
            <div v-loading="initLoading" class="picker-grid">
                <div
                    v-for="p in filteredProducts"
                    :key="p.id"
                    class="picker-card"
                    :class="{ 'picker-card-active': selectedProduct?.id === p.id }"
                    @click="pickProduct(p)"
                >
                    <el-image :src="p.picUrl" style="width: 100%; height: 80px" fit="cover" />
                    <div class="picker-card-info">
                        <div class="picker-card-name">{{ p.name }}</div>
                        <div class="picker-card-price">¥{{ ((p.price || 0) / 100).toFixed(2) }}</div>
                    </div>
                </div>
                <el-empty v-if="filteredProducts.length === 0" description="暂无商品" />
            </div>
        </el-dialog>

        <!-- 专题选择弹窗 -->
        <el-dialog v-model="topicDialogVisible" title="选择专题" width="560px" destroy-on-close append-to-body>
            <el-input
                v-model="topicSearch"
                placeholder="搜索专题标题..."
                clearable
                style="margin-bottom: 12px"
            />
            <div v-loading="initLoading" class="picker-list">
                <div
                    v-for="t in filteredTopics"
                    :key="t.id"
                    class="picker-row"
                    :class="{ 'picker-row-active': selectedTopic?.id === t.id }"
                    @click="pickTopic(t)"
                >
                    <el-image v-if="t.picUrl" :src="t.picUrl" style="width: 60px; height: 40px; border-radius: 4px" fit="cover" />
                    <div class="picker-row-info">
                        <div class="picker-row-name">{{ t.title }}</div>
                        <div class="picker-row-sub">{{ t.subtitle || '—' }}</div>
                    </div>
                </div>
                <el-empty v-if="filteredTopics.length === 0" description="暂无专题" />
            </div>
        </el-dialog>
    </div>
</template>

<style scoped>
.link-selector { width: 100%; }
.link-type-group { margin-bottom: 10px; }
.link-detail { margin-bottom: 8px; }
.link-preview {
    padding: 6px 10px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 12px;
    color: #909399;
}
.preview-label { color: #606266; }
.preview-value { color: #409eff; word-break: break-all; }

.selected-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: #f0f9eb;
    border: 1px solid #e1f3d8;
    border-radius: 6px;
}
.selected-thumb { width: 48px; height: 48px; border-radius: 4px; flex-shrink: 0; }
.selected-info { flex: 1; min-width: 0; }
.selected-name { font-size: 14px; color: #303133; font-weight: 500; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.selected-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.selected-price { font-size: 13px; color: #e6a23c; font-weight: 600; }
.selected-id { font-size: 12px; color: #909399; }
.selected-subtitle { font-size: 12px; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.selected-actions { display: flex; gap: 4px; flex-shrink: 0; }

/* 商品选择网格 */
.picker-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
    max-height: 360px;
    overflow-y: auto;
}
.picker-card {
    border: 2px solid #e4e7ed;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.2s;
}
.picker-card:hover { border-color: #409eff; box-shadow: 0 2px 6px rgba(64,158,255,0.15); }
.picker-card-active { border-color: #409eff; background: #ecf5ff; }
.picker-card-info { padding: 4px 6px; }
.picker-card-name { font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.picker-card-price { font-size: 12px; color: #e6a23c; font-weight: 600; }

/* 专题选择列表 */
.picker-list {
    max-height: 360px;
    overflow-y: auto;
}
.picker-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    margin-bottom: 8px;
    cursor: pointer;
    transition: all 0.2s;
}
.picker-row:hover { border-color: #409eff; background: #f5f7fa; }
.picker-row-active { border-color: #409eff; background: #ecf5ff; }
.picker-row-info { flex: 1; min-width: 0; }
.picker-row-name { font-size: 14px; color: #303133; }
.picker-row-sub { font-size: 12px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
