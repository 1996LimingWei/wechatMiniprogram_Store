<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { QuestionFilled } from "@element-plus/icons-vue";
import {
    getProductDetail,
    saveProduct
} from "@/api/product";
import { getCategoryList } from "@/api/category";
import { getSkuList } from "@/api/sku";
import MaterialImagePicker from "@/components/MaterialImagePicker/index.vue";
import type { ProductSpu, Category, ProductSku } from "@/api/types";

defineOptions({ name: "ProductForm" });

const route = useRoute();
const router = useRouter();

/* ---------- 路由参数 ---------- */
const spuId = computed(() => {
    const raw = route.params.id;
    return raw ? Number(raw) : undefined;
});
const isEdit = computed(() => !!spuId.value);

/* ---------- 分类数据 ---------- */
const categoryList = ref<Category[]>([]);

/* ---------- 表单数据 ---------- */
const loading = ref(false);
const submitting = ref(false);
const form = reactive({
    name: "",
    categoryId: undefined as number | undefined,
    keyword: "",
    introduction: "",
    description: "",
    picUrl: "",
    sliderPicUrls: [] as string[],
    detailImageUrls: [] as string[],
    price: 0,
    marketPrice: 0,
    stock: 0,
    sort: 0,
    status: 1
});

/* ---------- SKU 规格 ---------- */
interface SpecDimension {
    name: string;
    values: string[];
}

const specs = ref<SpecDimension[]>([]);
const skuMatrix = ref<ProductSku[]>([]);
const originalSkuStocks = ref(new Map<number, number>());
const stockAdjustReason = ref("");

const stockChanged = computed(() => {
    if (!isEdit.value) return false;
    const currentIds = new Set<number>();
    for (const sku of skuMatrix.value) {
        if (!sku.id || !originalSkuStocks.value.has(sku.id)) return true;
        currentIds.add(sku.id);
        if ((originalSkuStocks.value.get(sku.id) ?? 0) !== (sku.stock ?? 0)) return true;
    }
    return currentIds.size !== originalSkuStocks.value.size;
});

/** 解析轮播图 URL（兼容 JSON 数组字符串和逗号分隔） */
function parseSliderPics(raw: string): string[] {
    if (!raw) return [];
    try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) return parsed.filter(Boolean);
    } catch {
        /* not JSON */
    }
    return raw.split(",").filter(Boolean);
}

const IMAGE_TAG = /<img\b[^>]*\bsrc\s*=\s*(['"])(.*?)\1[^>]*>/gi;

function extractImageUrlsFromDescription(description: string): string[] {
    const urls: string[] = [];
    let match: RegExpExecArray | null;
    IMAGE_TAG.lastIndex = 0;
    while ((match = IMAGE_TAG.exec(description)) !== null) {
        if (match[2]) urls.push(match[2]);
    }
    return urls;
}

function stripImageTags(description: string): string {
    IMAGE_TAG.lastIndex = 0;
    return description.replace(IMAGE_TAG, "").trim();
}

function escapeHtmlAttr(value: string): string {
    return value
        .replace(/&/g, "&amp;")
        .replace(/"/g, "&quot;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function buildDescriptionHtml(): string {
    const content = form.description.trim();
    const images = form.detailImageUrls
        .filter(Boolean)
        .map(url => `<p><img src="${escapeHtmlAttr(url)}" /></p>`)
        .join("\n");
    return [content, images].filter(Boolean).join("\n");
}

/** 加载商品详情 */
async function loadProduct() {
    if (!spuId.value) return;
    loading.value = true;
    try {
        const detail = (await getProductDetail(spuId.value)) as ProductSpu;
        form.name = detail.name;
        form.categoryId = detail.categoryId;
        form.keyword = detail.keyword || "";
        form.introduction = detail.introduction || "";
        form.detailImageUrls = extractImageUrlsFromDescription(detail.description || "");
        form.description = stripImageTags(detail.description || "");
        form.picUrl = detail.picUrl || "";
        form.sliderPicUrls = detail.sliderPicUrls
            ? parseSliderPics(detail.sliderPicUrls)
            : [];
        form.price = (detail.price ?? 0) / 100;
        form.marketPrice = (detail.marketPrice ?? 0) / 100;
        form.stock = detail.stock ?? 0;
        form.sort = detail.sort ?? 0;
        form.status = detail.status ?? 1;

        // 加载已有 SKU
        const skus = (await getSkuList(spuId.value)) as ProductSku[];
        if (skus.length > 0) {
            const parsed = parseSpecsFromSkus(skus);
            specs.value = parsed.dims;
            skuMatrix.value = skus.map(s => ({ ...s }));
            originalSkuStocks.value = new Map(
                skus.filter(s => !!s.id).map(s => [s.id!, s.stock ?? 0])
            );
        }
    } finally {
        loading.value = false;
    }
}

/* ---------- 规格解析 ---------- */
function parseSpecsFromSkus(skus: ProductSku[]) {
    const dimOrder: { id: number; name: string }[] = [];
    const valOrder: Map<number, string[]> = new Map();
    const seen = new Map<number, Set<string>>();

    skus.forEach(sku => {
        try {
            const props = JSON.parse(sku.properties || "[]");
            props.forEach((p: any) => {
                if (!valOrder.has(p.specificationId)) {
                    dimOrder.push({ id: p.specificationId, name: p.name });
                    valOrder.set(p.specificationId, []);
                    seen.set(p.specificationId, new Set());
                }
                const set = seen.get(p.specificationId)!;
                if (!set.has(p.valueName)) {
                    set.add(p.valueName);
                    valOrder.get(p.specificationId)!.push(p.valueName);
                }
            });
        } catch {
            /* ignore malformed */
        }
    });

    return {
        dims: dimOrder.map(d => ({
            name: d.name,
            values: [...(valOrder.get(d.id) || [])]
        }))
    };
}

/** 笛卡尔积生成 SKU 矩阵 */
function generateMatrix() {
    const validSpecs = specs.value.filter(
        s => s.name.trim() && s.values.filter(v => v.trim()).length > 0
    );
    if (validSpecs.length === 0) {
        skuMatrix.value = [];
        return;
    }

    const combinations: Record<string, string>[][] = validSpecs.map(s =>
        s.values.filter(v => v.trim()).map(v => ({ [s.name]: v }))
    );

    let combos: Record<string, string>[] = [{}];
    combinations.forEach(group => {
        const next: Record<string, string>[] = [];
        combos.forEach(c =>
            group.forEach(g => next.push({ ...c, ...g }))
        );
        combos = next;
    });

    const oldMap = new Map<string, ProductSku>();
    skuMatrix.value.forEach(sku => {
        try {
            const props = JSON.parse(sku.properties || "[]");
            const key = props
                .map((p: any) => `${p.name}:${p.valueName}`)
                .join(";");
            oldMap.set(key, sku);
        } catch {
            /* skip */
        }
    });

    skuMatrix.value = combos.map(combo => {
        const key = Object.entries(combo)
            .map(([k, v]) => `${k}:${v}`)
            .join(";");
        const old = oldMap.get(key);
        const props = Object.entries(combo).map(([name, valueName], i) => ({
            id: i + 1,
            valueId: validSpecs[i].values.indexOf(valueName) + 1,
            name,
            valueName
        }));
        return {
            ...(old || {}),
            spuId: spuId.value ?? 0,
            properties: JSON.stringify(props),
            price: old?.price,
            marketPrice: old?.marketPrice,
            stock: old?.stock ?? 0,
            picUrl: old?.picUrl || ""
        } as ProductSku;
    });
}

/* ---------- 规格维度操作 ---------- */
function addSpecDimension() {
    specs.value.push({ name: "", values: [""] });
}

function removeSpecDimension(index: number) {
    specs.value.splice(index, 1);
    generateMatrix();
}

function addSpecValue(dimIndex: number) {
    specs.value[dimIndex].values.push("");
}

function removeSpecValue(dimIndex: number, valIndex: number) {
    specs.value[dimIndex].values.splice(valIndex, 1);
    generateMatrix();
}

/* ---------- 保存 ---------- */
function buildSpuPayload(): ProductSpu {
    return {
        ...(isEdit.value ? { id: spuId.value } : {}),
        name: form.name.trim(),
        categoryId: form.categoryId,
        keyword: form.keyword.trim(),
        introduction: form.introduction.trim(),
        description: buildDescriptionHtml(),
        picUrl: form.picUrl,
        sliderPicUrls: form.sliderPicUrls.filter(Boolean).length > 0
            ? JSON.stringify(form.sliderPicUrls.filter(Boolean))
            : "",
        price: Math.round(form.price * 100),
        marketPrice: Math.round(form.marketPrice * 100),
        stock: form.stock,
        sort: form.sort,
        status: form.status
    } as ProductSpu;
}

async function handleSave() {
    if (!form.name.trim()) {
        ElMessage.warning("请输入商品名称");
        return;
    }
    if (!form.categoryId) {
        ElMessage.warning("请选择商品分类");
        return;
    }
    if (stockChanged.value) {
        const reason = stockAdjustReason.value.trim();
        if (reason.length < 4 || reason.length > 200) {
            ElMessage.warning("库存发生变化，请填写 4 至 200 个字符的调整原因");
            return;
        }
    }

    submitting.value = true;
    try {
        const payload = buildSpuPayload();

        await saveProduct(payload, skuMatrix.value, stockAdjustReason.value.trim());
        ElMessage.success(isEdit.value ? "保存成功" : "商品创建成功");
        router.push("/product/spu");
    } finally {
        submitting.value = false;
    }
}

function handleCancel() {
    router.push("/product/spu");
}

/* ---------- SKU 矩阵列头 ---------- */
function getSpecColumns() {
    return specs.value.filter(
        s => s.name.trim() && s.values.some(v => v.trim())
    );
}

/** 从 properties JSON 中提取某维度的值 */
function getSpecValue(sku: ProductSku, dimName: string) {
    try {
        const props = JSON.parse(sku.properties || "[]");
        const p = props.find((x: any) => x.name === dimName);
        return p?.valueName || "—";
    } catch {
        return "—";
    }
}

/** SKU 价格（分→元）双向绑定 */
function skuPriceGet(sku: ProductSku) {
    return sku.price != null ? (sku.price / 100).toFixed(2) : "";
}

function skuPriceSet(sku: ProductSku, val: string) {
    sku.price = val ? Math.round(parseFloat(val) * 100) : undefined;
}

function skuMarketPriceGet(sku: ProductSku) {
    return sku.marketPrice != null ? (sku.marketPrice / 100).toFixed(2) : "";
}

function skuMarketPriceSet(sku: ProductSku, val: string) {
    sku.marketPrice = val ? Math.round(parseFloat(val) * 100) : undefined;
}

onMounted(async () => {
    categoryList.value = (await getCategoryList()) as Category[];
    if (isEdit.value) {
        loadProduct();
    }
});
</script>

<template>
    <div class="app-container" v-loading="loading">
        <!-- 返回 -->
        <div class="page-header">
            <el-button @click="handleCancel" text>← 返回列表</el-button>
            <h3 style="margin: 0">{{ isEdit ? "编辑商品" : "新增商品" }}</h3>
        </div>

        <!-- 1. 基础信息 -->
        <el-card shadow="never" class="section">
            <template #header><span class="card-title">基础信息</span></template>
            <el-form label-width="100px">
                <el-form-item label="商品名称" required>
                    <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="100" />
                </el-form-item>
                <el-form-item label="商品分类" required>
                    <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                        <el-option
                            v-for="cat in categoryList"
                            :key="cat.id"
                            :label="cat.name"
                            :value="cat.id!"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <template #label>
                        关键词
                        <el-tooltip content="用于小程序内搜索匹配，多个关键词用空格分隔" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <el-input v-model="form.keyword" placeholder="搜索关键词（可选）" />
                </el-form-item>
                <el-form-item label="简介">
                    <el-input v-model="form.introduction" type="textarea" :rows="2" placeholder="商品简介" />
                </el-form-item>
                <el-row :gutter="16">
                    <el-col :span="8">
                        <el-form-item>
                        <template #label>
                            排序
                            <el-tooltip content="数值越大，商品在小程序列表中显示越靠前" placement="top">
                                <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                            </el-tooltip>
                        </template>
                            <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width: 100%" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="状态">
                            <el-radio-group v-model="form.status">
                                <el-radio :value="1">上架</el-radio>
                                <el-radio :value="0">下架</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
        </el-card>

        <!-- 2. 图片 -->
        <el-card shadow="never" class="section">
            <template #header><span class="card-title">图片设置</span></template>
            <el-form label-width="100px">
                <el-form-item>
                    <template #label>
                        主图
                        <el-tooltip content="商品列表和详情页展示的主图，可上传新图或选择素材库图片" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <MaterialImagePicker v-model="form.picUrl" biz-type="product" empty-text="请选择商品主图" />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        轮播图
                        <el-tooltip content="商品详情页顶部滑动图片，支持多图上传、选择、排序和删除" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <MaterialImagePicker
                        v-model="form.sliderPicUrls"
                        multiple
                        biz-type="product"
                        :max="10"
                        empty-text="请选择轮播图"
                    />
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 3. 价格与库存 -->
        <el-card shadow="never" class="section">
            <template #header><span class="card-title">价格与库存</span></template>
            <el-row :gutter="16">
                <el-col :span="8">
                    <el-form-item label="售价（元）" label-width="100px">
                        <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" controls-position="right" style="width: 100%" />
                    </el-form-item>
                </el-col>
                <el-col :span="8">
                    <el-form-item label="市场价（元）" label-width="100px">
                        <el-input-number v-model="form.marketPrice" :min="0" :precision="2" :step="1" controls-position="right" style="width: 100%" />
                    </el-form-item>
                </el-col>
                <el-col :span="8">
                    <el-form-item label="库存" label-width="100px">
                        <el-input-number v-model="form.stock" :min="0" :disabled="isEdit" controls-position="right" style="width: 100%" />
                        <div v-if="isEdit" class="field-tip">库存由下方各 SKU 库存自动汇总</div>
                    </el-form-item>
                </el-col>
            </el-row>
        </el-card>

        <!-- 4. 商品详情 -->
        <el-card shadow="never" class="section">
            <template #header><span class="card-title">商品详情（HTML）</span></template>
            <el-form label-width="100px">
                <el-form-item label="详情内容">
                    <el-input
                        v-model="form.description"
                        type="textarea"
                        :rows="8"
                        placeholder="输入商品详情，支持 HTML 标签"
                    />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        详情图
                        <el-tooltip content="商品详情图会保存为 HTML 图片标签并参与素材引用保护" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <MaterialImagePicker
                        v-model="form.detailImageUrls"
                        multiple
                        biz-type="product"
                        :max="20"
                        empty-text="请选择详情图"
                    />
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 5. SKU 规格管理（仅编辑模式） -->
        <el-card v-if="isEdit" shadow="never" class="section">
            <template #header><span class="card-title">SKU 规格管理</span></template>

            <!-- 规格维度定义 -->
            <div v-for="(dim, di) in specs" :key="di" class="spec-dim">
                <div class="spec-dim-header">
                    <el-input
                        v-model="dim.name"
                        placeholder="规格名称（如：颜色、尺寸）"
                        style="width: 200px"
                        @blur="generateMatrix"
                    />
                    <el-button type="danger" link size="small" @click="removeSpecDimension(di)">
                        删除维度
                    </el-button>
                </div>
                <div class="spec-values">
                    <div v-for="(val, vi) in dim.values" :key="vi" class="spec-val-item">
                        <el-input
                            v-model="dim.values[vi]"
                            placeholder="规格值"
                            style="width: 160px"
                            @blur="generateMatrix"
                        />
                        <el-button type="danger" link size="small" @click="removeSpecValue(di, vi)">×</el-button>
                    </div>
                    <el-button type="primary" link @click="addSpecValue(di)">+ 添加值</el-button>
                </div>
            </div>
            <el-button type="primary" plain class="mt-3" @click="addSpecDimension">
                + 新增规格维度
            </el-button>
            <el-button type="success" plain class="mt-3" @click="generateMatrix" :disabled="specs.length === 0">
                生成 SKU 矩阵
            </el-button>

            <!-- SKU 矩阵表格 -->
            <el-table
                v-if="skuMatrix.length > 0"
                :data="skuMatrix"
                border
                style="width: 100%; margin-top: 16px"
                size="small"
            >
                <el-table-column
                    v-for="dim in getSpecColumns()"
                    :key="dim.name"
                    :label="dim.name"
                    width="120"
                    align="center"
                >
                    <template #default="{ row }">
                        {{ getSpecValue(row, dim.name) }}
                    </template>
                </el-table-column>
                <el-table-column label="价格（元）" width="130" align="center">
                    <template #default="{ row }">
                        <el-input
                            :model-value="skuPriceGet(row)"
                            @update:model-value="skuPriceSet(row, $event)"
                            size="small"
                            placeholder="0.00"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="市场价（元）" width="130" align="center">
                    <template #default="{ row }">
                        <el-input
                            :model-value="skuMarketPriceGet(row)"
                            @update:model-value="skuMarketPriceSet(row, $event)"
                            size="small"
                            placeholder="0.00"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="库存" width="110" align="center">
                    <template #default="{ row }">
                        <el-input-number
                            v-model="row.stock"
                            :min="0"
                            size="small"
                            controls-position="right"
                            style="width: 90px"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="图片" min-width="220">
                    <template #default="{ row }">
                        <MaterialImagePicker v-model="row.picUrl" biz-type="product" empty-text="可选" />
                    </template>
                </el-table-column>
            </el-table>
            <el-alert
                v-if="stockChanged"
                title="检测到 SKU 库存变化，本次保存将写入库存审计流水"
                type="warning"
                :closable="false"
                show-icon
                class="stock-audit-alert"
            />
            <el-form-item v-if="stockChanged" label="调整原因" required label-width="100px">
                <el-input
                    v-model="stockAdjustReason"
                    type="textarea"
                    :rows="3"
                    maxlength="200"
                    show-word-limit
                    placeholder="填写盘点入库、损耗修正等具体原因"
                />
            </el-form-item>
        </el-card>

        <!-- 底部按钮 -->
        <div class="footer-actions">
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSave">
                {{ isEdit ? "保存修改" : "创建商品" }}
            </el-button>
        </div>
    </div>
</template>

<style scoped>
.app-container {
    padding: 16px;
    max-width: 1100px;
}
.page-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
}
.section {
    margin-bottom: 16px;
}
.card-title {
    font-weight: 600;
    font-size: 15px;
}
.spec-dim {
    background: #f9fafb;
    border-radius: 8px;
    padding: 12px 16px;
    margin-bottom: 12px;
}
.spec-dim-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}
.spec-values {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding-left: 16px;
}
.spec-val-item {
    display: flex;
    align-items: center;
    gap: 4px;
}
.mt-3 {
    margin-top: 12px;
}
.footer-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding: 16px 0;
}
.tip-icon {
    margin-left: 4px;
    color: #909399;
    cursor: help;
    vertical-align: middle;
}
.stock-audit-alert {
    margin: 16px 0;
}
.field-tip {
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
    line-height: 18px;
}
</style>
