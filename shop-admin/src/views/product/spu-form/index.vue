<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { QuestionFilled } from "@element-plus/icons-vue";
import { getProductDetail, saveProduct } from "@/api/product";
import { getCategoryList } from "@/api/category";
import { getSkuList } from "@/api/sku";
import { hasVisualEditorAccess } from "@/utils/auth";
import MaterialImagePicker from "@/components/MaterialImagePicker/index.vue";
import type { ProductSpu, Category, ProductSku } from "@/api/types";
import { clearPreviewDraft, notifyPreviewDataCommitted, setPreviewDraft } from "@/utils/preview-center";

defineOptions({ name: "ProductForm" });

interface SpecDimension {
  name: string;
  values: string[];
}

interface ProductFormState {
  name: string;
  type: number;
  categoryId: number | undefined;
  introduction: string;
  description: string;
  picUrl: string;
  sliderPicUrls: string[];
  detailImageUrls: string[];
  price: number;
  marketPrice: number;
  stock: number;
  sort: number;
  status: number;
  /** 单品运费（元），null 表示跟随全局运费规则 */
  freightBaseFee: number | null;
  /** 单品包邮门槛（元），null 表示跟随全局运费规则 */
  freightFreeThreshold: number | null;
}

interface ProductEditorSnapshot {
  form: ProductFormState;
  keywordTags: string[];
  multiSpecEnabled: boolean;
  specs: SpecDimension[];
  skuMatrix: ProductSku[];
  stockAdjustReason: string;
}

interface PreviewSkuItem {
  label: string;
  skuCode: string;
  price: number;
  marketPrice: number;
  stock: number;
  picUrl: string;
}

const route = useRoute();
const router = useRouter();

const spuId = computed(() => {
  const raw = route.params.id;
  return raw ? Number(raw) : undefined;
});
const isEdit = computed(() => !!spuId.value);

const categoryList = ref<Category[]>([]);
const typeOptions = [
  { label: "实物商品", value: 1 }
] as const;
const statusOptions = [
  { label: "上架", value: 1 },
  { label: "下架", value: 0 }
] as const;
const keywordOptions = [
  "陕铺",
  "滋补养生",
  "花草茶饮",
  "轻养零食",
  "节气调理",
  "送礼礼盒",
  "家庭常备",
  "办公室冲泡",
  "即食便携",
  "无糖清润"
];
const specNameOptions = ["规格", "净含量", "包装", "口味", "套餐"];
const specValuePresetMap: Record<string, string[]> = {
  规格: ["默认规格", "标准装", "体验装", "礼盒装"],
  净含量: ["60g", "100g", "120g", "250g", "500g"],
  包装: ["袋装", "盒装", "瓶装", "罐装", "礼盒装"],
  口味: ["原味", "微甜", "无糖", "清润"],
  套餐: ["单盒", "2盒装", "3盒装", "家庭装"]
};

function createEmptyFormState(): ProductFormState {
  return {
    name: "",
    type: 1,
    categoryId: undefined,
    introduction: "",
    description: "",
    picUrl: "",
    sliderPicUrls: [],
    detailImageUrls: [],
    price: 0,
    marketPrice: 0,
    stock: 0,
    sort: 0,
    status: 0,
    freightBaseFee: null,
    freightFreeThreshold: null
  };
}

function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value));
}

const loading = ref(true);
const submitting = ref(false);
const multiSpecEnabled = ref(false);
const keywordTags = ref<string[]>([]);
const previewTab = ref("card");
const savedSnapshot = ref<ProductEditorSnapshot | null>(null);
const specs = ref<SpecDimension[]>([]);
const skuMatrix = ref<ProductSku[]>([]);
const originalSkuStocks = ref(new Map<number, number>());
const stockAdjustReason = ref("");
const hasStoredSkus = ref(false);
const showSkuSection = computed(() => hasStoredSkus.value || multiSpecEnabled.value);
const form = reactive(createEmptyFormState());

const stockChanged = computed(() => {
  if (!isEdit.value) return false;
  if (!showSkuSection.value) {
    return savedSnapshot.value !== null && form.stock !== savedSnapshot.value.form.stock;
  }
  const currentIds = new Set<number>();
  for (const sku of skuMatrix.value) {
    if (!sku.id || !originalSkuStocks.value.has(sku.id)) return true;
    currentIds.add(sku.id);
    if ((originalSkuStocks.value.get(sku.id) ?? 0) !== (sku.stock ?? 0)) return true;
  }
  return currentIds.size !== originalSkuStocks.value.size;
});

const IMAGE_TAG = /<img\b[^>]*\bsrc\s*=\s*(['"])(.*?)\1[^>]*>/gi;

function parseSliderPics(raw: string): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed.filter(Boolean);
  } catch {
    /* noop */
  }
  return raw.split(",").filter(Boolean);
}

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

function parseKeywordTags(raw: string): string[] {
  if (!raw) return [];
  return raw
    .split(/[\s,，|/]+/)
    .map(item => item.trim())
    .filter(Boolean);
}

function createSnapshot(): ProductEditorSnapshot {
  return {
    form: deepClone({ ...form }),
    keywordTags: deepClone(keywordTags.value),
    multiSpecEnabled: multiSpecEnabled.value,
    specs: deepClone(specs.value),
    skuMatrix: deepClone(skuMatrix.value),
    stockAdjustReason: stockAdjustReason.value
  };
}

function applySnapshot(snapshot: ProductEditorSnapshot) {
  Object.assign(form, createEmptyFormState(), deepClone(snapshot.form));
  keywordTags.value = deepClone(snapshot.keywordTags);
  multiSpecEnabled.value = snapshot.multiSpecEnabled;
  specs.value = deepClone(snapshot.specs);
  skuMatrix.value = deepClone(snapshot.skuMatrix);
  stockAdjustReason.value = snapshot.stockAdjustReason;
}

function captureSavedSnapshot() {
  const snapshot = createSnapshot();
  savedSnapshot.value = deepClone(snapshot);
  originalSkuStocks.value = new Map(
    snapshot.skuMatrix.filter(sku => !!sku.id).map(sku => [sku.id!, sku.stock ?? 0])
  );
}

function snapshotSignature(snapshot: ProductEditorSnapshot | null): string {
  return snapshot ? JSON.stringify(snapshot) : "";
}

const hasUnsavedChanges = computed(
  () => snapshotSignature(savedSnapshot.value) !== snapshotSignature(createSnapshot())
);
const restoreButtonText = computed(() =>
  isEdit.value ? "恢复到上次保存" : "恢复空白表单"
);

function openPreviewCenter() {
  const previewUrl = router.resolve({
        path: "/visual-editor/home",
    query: { scene: "product" }
  });
  window.open(previewUrl.href, "_blank");
}

async function loadProduct() {
  if (!spuId.value) return;
  loading.value = true;
  try {
    const detail = (await getProductDetail(spuId.value)) as ProductSpu;
    Object.assign(form, createEmptyFormState(), {
      name: detail.name,
      type: detail.type ?? 1,
      categoryId: detail.categoryId,
      introduction: detail.introduction || "",
      description: stripImageTags(detail.description || ""),
      picUrl: detail.picUrl || "",
      sliderPicUrls: detail.sliderPicUrls ? parseSliderPics(detail.sliderPicUrls) : [],
      detailImageUrls: extractImageUrlsFromDescription(detail.description || ""),
      price: (detail.price ?? 0) / 100,
      marketPrice: (detail.marketPrice ?? 0) / 100,
      stock: detail.stock ?? 0,
      sort: detail.sort ?? 0,
      status: detail.status ?? 1,
      freightBaseFee:
        detail.freightBaseFee == null ? null : detail.freightBaseFee / 100,
      freightFreeThreshold:
        detail.freightFreeThreshold == null
          ? null
          : detail.freightFreeThreshold / 100
    });
    keywordTags.value = parseKeywordTags(detail.keyword || "");

    const skus = (await getSkuList(spuId.value)) as ProductSku[];
    hasStoredSkus.value = skus.length > 0;
    if (skus.length > 0) {
      const parsed = parseSpecsFromSkus(skus);
      specs.value = parsed.dims;
      skuMatrix.value = skus.map(sku => ({ ...sku }));
      multiSpecEnabled.value = parsed.dims.length > 0;
    } else {
      specs.value = [];
      skuMatrix.value = [];
      multiSpecEnabled.value = false;
    }
    stockAdjustReason.value = "";
    captureSavedSnapshot();
  } finally {
    loading.value = false;
  }
}

function parseSpecsFromSkus(skus: ProductSku[]) {
  const dimOrder: { id: number; name: string }[] = [];
  const valOrder: Map<number, string[]> = new Map();
  const seen = new Map<number, Set<string>>();

  skus.forEach(sku => {
    try {
      const props = JSON.parse(sku.properties || "[]");
      props.forEach((p: any) => {
        const dimensionId = p.specificationId ?? p.id;
        if (!valOrder.has(dimensionId)) {
          dimOrder.push({ id: dimensionId, name: p.name });
          valOrder.set(dimensionId, []);
          seen.set(dimensionId, new Set());
        }
        const set = seen.get(dimensionId)!;
        if (!set.has(p.valueName)) {
          set.add(p.valueName);
          valOrder.get(dimensionId)!.push(p.valueName);
        }
      });
    } catch {
      /* ignore malformed */
    }
  });

  return {
    dims: dimOrder.map(item => ({
      name: item.name,
      values: [...(valOrder.get(item.id) || [])]
    }))
  };
}

function generateMatrix() {
  const validSpecs = specs.value.filter(
    spec => spec.name.trim() && spec.values.filter(value => value.trim()).length > 0
  );
  if (validSpecs.length === 0) {
    skuMatrix.value = [];
    return;
  }

  const combinations: Record<string, string>[][] = validSpecs.map(spec =>
    spec.values.filter(value => value.trim()).map(value => ({ [spec.name]: value }))
  );

  let combos: Record<string, string>[] = [{}];
  combinations.forEach(group => {
    const next: Record<string, string>[] = [];
    combos.forEach(combo =>
      group.forEach(item => next.push({ ...combo, ...item }))
    );
    combos = next;
  });

  const oldMap = new Map<string, ProductSku>();
  skuMatrix.value.forEach(sku => {
    try {
      const props = JSON.parse(sku.properties || "[]");
      const key = props.map((p: any) => `${p.name}:${p.valueName}`).join(";");
      oldMap.set(key, sku);
    } catch {
      /* noop */
    }
  });

  skuMatrix.value = combos.map(combo => {
    const key = Object.entries(combo)
      .map(([name, value]) => `${name}:${value}`)
      .join(";");
    const old = oldMap.get(key);
    const props = Object.entries(combo).map(([name, valueName], index) => ({
      id: index + 1,
      valueId: validSpecs[index].values.indexOf(valueName) + 1,
      name,
      valueName
    }));
    return {
      ...(old || {}),
      spuId: spuId.value ?? 0,
      properties: JSON.stringify(props),
      skuCode: old?.skuCode || "",
      price: old?.price,
      marketPrice: old?.marketPrice,
      stock: old?.stock ?? 0,
      picUrl: old?.picUrl || ""
    } as ProductSku;
  });
}

function addSpecDimension() {
  specs.value.push({ name: "规格", values: ["默认规格"] });
}

function handleMultiSpecToggle(enabled: boolean) {
  multiSpecEnabled.value = enabled;
  if (enabled && specs.value.length === 0) {
    addSpecDimension();
    return;
  }
  if (!enabled) {
    specs.value = [];
    skuMatrix.value = [];
  }
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

function specValueOptions(name: string) {
  const key = (name || "").trim();
  return specValuePresetMap[key] || ["默认规格", "标准装", "体验装", "礼盒装"];
}

function buildSpuPayload(): ProductSpu {
  return {
    ...(isEdit.value ? { id: spuId.value } : {}),
    name: form.name.trim(),
    type: form.type,
    categoryId: form.categoryId!,
    keyword: keywordTags.value.map(item => item.trim()).filter(Boolean).join(" "),
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
    status: form.status,
    freightBaseFee:
      form.freightBaseFee == null ? null : Math.round(form.freightBaseFee * 100),
    freightFreeThreshold:
      form.freightFreeThreshold == null
        ? null
        : Math.round(form.freightFreeThreshold * 100)
  } as ProductSpu;
}

async function handleSave() {
  if (loading.value || submitting.value) return;
  if (!form.name.trim()) {
    ElMessage.warning("请输入商品名称");
    return;
  }
  if (!form.categoryId) {
    ElMessage.warning("请选择商品分类");
    return;
  }
  if (showSkuSection.value && skuMatrix.value.length === 0) {
    ElMessage.warning("请先生成 SKU 矩阵后再保存");
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
    const requestedSkus = showSkuSection.value ? skuMatrix.value : [];
    await saveProduct(payload, requestedSkus, stockAdjustReason.value.trim());
    notifyPreviewDataCommitted("product");
    captureSavedSnapshot();
    ElMessage.success(isEdit.value ? "保存成功" : "商品创建成功");
    router.push("/product/spu");
  } finally {
    submitting.value = false;
  }
}

function handleRestoreSnapshot() {
  if (!savedSnapshot.value || !hasUnsavedChanges.value) return;
  applySnapshot(savedSnapshot.value);
  ElMessage.success(isEdit.value ? "已恢复到上次保存内容" : "已恢复为空白表单");
}

function handleCancel() {
  router.push("/product/spu");
}

function getSpecColumns() {
  return specs.value.filter(
    spec => spec.name.trim() && spec.values.some(value => value.trim())
  );
}

function getSpecValue(sku: ProductSku, dimName: string) {
  try {
    const props = JSON.parse(sku.properties || "[]");
    const item = props.find((property: any) => property.name === dimName);
    return item?.valueName || "—";
  } catch {
    return "—";
  }
}

function skuPriceGet(sku: ProductSku) {
  return sku.price != null ? (sku.price / 100).toFixed(2) : "";
}

function skuPriceSet(sku: ProductSku, value: string) {
  sku.price = value ? Math.round(parseFloat(value) * 100) : undefined;
}

function skuMarketPriceGet(sku: ProductSku) {
  return sku.marketPrice != null ? (sku.marketPrice / 100).toFixed(2) : "";
}

function skuMarketPriceSet(sku: ProductSku, value: string) {
  sku.marketPrice = value ? Math.round(parseFloat(value) * 100) : undefined;
}

function parseSkuLabel(properties?: string) {
  try {
    const props = JSON.parse(properties || "[]");
    const labels = Array.isArray(props)
      ? props.map((item: any) => item?.valueName || item?.name).filter(Boolean)
      : [];
    return labels.length > 0 ? labels.join(" / ") : "默认规格";
  } catch {
    return "默认规格";
  }
}

function formatYuan(value: number) {
  return `￥${value.toFixed(2)}`;
}

const selectedCategoryName = computed(
  () => categoryList.value.find(item => item.id === form.categoryId)?.name || "未选择分类"
);
const selectedTypeLabel = computed(
  () => typeOptions.find(item => item.value === form.type)?.label || "实物商品"
);
const selectedStatusLabel = computed(
  () => statusOptions.find(item => item.value === form.status)?.label || "上架"
);
const previewImages = computed(() => {
  const sliderImages = form.sliderPicUrls.filter(Boolean);
  if (sliderImages.length > 0) return sliderImages;
  if (form.picUrl) return [form.picUrl];
  return [];
});
const previewCoverImage = computed(() => previewImages.value[0] || form.picUrl || "");
const previewDescriptionLines = computed(() => {
  const lines = form.description
    .split(/\r?\n+/)
    .map(item => item.trim())
    .filter(Boolean);
  return lines.length > 0
    ? lines
    : ["这里会实时展示商品详情文案，便于操作员边改边看小程序预呈现效果。"];
});
const previewSkuItems = computed<PreviewSkuItem[]>(() => {
  if (showSkuSection.value && skuMatrix.value.length > 0) {
    return skuMatrix.value.map(sku => ({
      label: parseSkuLabel(sku.properties),
      skuCode: sku.skuCode || "",
      price: (sku.price ?? 0) / 100,
      marketPrice: (sku.marketPrice ?? 0) / 100,
      stock: sku.stock ?? 0,
      picUrl: sku.picUrl || form.picUrl
    }));
  }
  return [{
    label: "默认规格",
    skuCode: "",
    price: form.price,
    marketPrice: form.marketPrice,
    stock: form.stock,
    picUrl: form.picUrl
  }];
});
const previewPriceText = computed(() => {
  const prices = previewSkuItems.value.map(item => item.price).filter(price => price > 0);
  if (prices.length === 0) return "￥0.00";
  const min = Math.min(...prices);
  const max = Math.max(...prices);
  return min === max ? formatYuan(min) : `${formatYuan(min)} 起`;
});
const previewMarketPriceText = computed(() => {
  const prices = previewSkuItems.value.map(item => item.marketPrice).filter(price => price > 0);
  if (prices.length === 0) return "";
  return formatYuan(Math.min(...prices));
});
const previewTotalStock = computed(() =>
  previewSkuItems.value.reduce((total, item) => total + (item.stock || 0), 0)
);
const previewSkuSummary = computed(() => previewSkuItems.value.slice(0, 4));

watch(
  [
    () => ({ ...form }),
    () => keywordTags.value.map(item => item),
    () => specs.value.map(item => ({ ...item, values: [...item.values] })),
    () => skuMatrix.value.map(item => ({ ...item })),
    () => multiSpecEnabled.value,
    selectedCategoryName
  ],
  () => {
    setPreviewDraft("product", {
      id: spuId.value,
      name: form.name,
      type: form.type,
      categoryId: form.categoryId,
      categoryName: selectedCategoryName.value,
      introduction: form.introduction,
      description: form.description,
      picUrl: form.picUrl,
      sliderPicUrls: [...form.sliderPicUrls],
      detailImageUrls: [...form.detailImageUrls],
      price: form.price,
      marketPrice: form.marketPrice,
      stock: form.stock,
      status: form.status,
      keywordTags: [...keywordTags.value],
      skuSummary: previewSkuItems.value.map(item => ({ ...item }))
    });
  },
  { deep: true, immediate: true }
);

onMounted(async () => {
  try {
    categoryList.value = (await getCategoryList()) as Category[];
    if (isEdit.value) {
      await loadProduct();
    } else {
      captureSavedSnapshot();
    }
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  clearPreviewDraft("product");
});
</script>

<template>
  <div class="app-container" v-loading="loading">
    <div class="page-header">
      <div class="header-main">
        <el-button @click="handleCancel" text>← 返回列表</el-button>
        <h3 class="page-title">{{ isEdit ? "编辑商品" : "新增商品" }}</h3>
      </div>
      <div class="header-actions">
        <el-button v-if="hasVisualEditorAccess()" plain @click="openPreviewCenter">打开可视化装修</el-button>
        <el-tag :type="hasUnsavedChanges ? 'warning' : 'success'" effect="plain">
          {{ hasUnsavedChanges ? "有未保存修改" : "已与已保存内容一致" }}
        </el-tag>
        <el-button plain :disabled="!hasUnsavedChanges" @click="handleRestoreSnapshot">
          {{ restoreButtonText }}
        </el-button>
      </div>
    </div>

    <div class="editor-shell">
      <div class="editor-main">
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
            <el-form-item label="商品类型" required>
              <el-select v-model="form.type" placeholder="请选择商品类型" style="width: 100%">
                <el-option
                  v-for="item in typeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
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
              <el-select
                v-model="keywordTags"
                multiple
                filterable
                allow-create
                default-first-option
                clearable
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择或输入搜索关键词"
                style="width: 100%"
              >
                <el-option
                  v-for="item in keywordOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
              <div class="field-tip">建议控制在 3 到 6 个词，系统会自动按空格保存。</div>
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
                  <el-input-number
                    v-model="form.sort"
                    :min="0"
                    :max="9999"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="状态">
                  <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                    <el-option
                      v-for="item in statusOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col v-if="!hasStoredSkus" :span="8">
                <el-form-item label="规格模式">
                  <el-switch
                    v-model="multiSpecEnabled"
                    inline-prompt
                    active-text="多规格"
                    inactive-text="单规格"
                    @change="handleMultiSpecToggle"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

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

        <el-card shadow="never" class="section">
          <template #header><span class="card-title">价格与库存</span></template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="8">
              <el-form-item label="售价（元）" label-position="top">
                <el-input-number
                  v-model="form.price"
                  :min="0"
                  :precision="2"
                  :step="1"
                  :disabled="showSkuSection"
                  controls-position="right"
                  style="width: 100%"
                />
                <div v-if="showSkuSection" class="field-tip">已启用规格管理时，系统将自动取最低 SKU 售价。</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="市场价（元）" label-position="top">
                <el-input-number
                  v-model="form.marketPrice"
                  :min="0"
                  :precision="2"
                  :step="1"
                  :disabled="showSkuSection"
                  controls-position="right"
                  style="width: 100%"
                />
                <div v-if="showSkuSection" class="field-tip">已启用规格管理时，系统将自动取最低 SKU 市场价。</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="库存" label-position="top">
                <el-input-number
                  v-model="form.stock"
                  :min="0"
                  :disabled="showSkuSection"
                  controls-position="right"
                  style="width: 100%"
                />
                <div v-if="showSkuSection" class="field-tip">库存由下方各 SKU 库存自动汇总。</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item
            v-if="!showSkuSection && stockChanged"
            label="调整原因"
            required
            label-width="100px"
          >
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

        <el-card shadow="never" class="section">
          <template #header><span class="card-title">运费设置</span></template>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            class="freight-tip"
            title="留空表示跟随全局运费规则（可在「营销管理 → 包邮规则」统一维护）；填写后仅本商品生效。"
          />
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12">
              <el-form-item label="单品运费（元）" label-position="top">
                <el-input-number
                  v-model="form.freightBaseFee"
                  :min="0"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                  placeholder="留空跟随全局"
                  style="width: 100%"
                />
                <div class="field-tip">买家未达包邮门槛时收取的运费</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="单品包邮门槛（元）" label-position="top">
                <el-input-number
                  v-model="form.freightFreeThreshold"
                  :min="0"
                  :precision="2"
                  :step="10"
                  controls-position="right"
                  placeholder="留空跟随全局"
                  style="width: 100%"
                />
                <div class="field-tip">单个商品金额达到该值即包邮</div>
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <el-card shadow="never" class="section">
          <template #header><span class="card-title">商品详情</span></template>
          <el-form label-width="100px">
            <el-form-item label="详情内容">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="8"
                placeholder="请输入客户可读的商品卖点、食用场景、适用人群等内容"
              />
              <div class="field-tip">这里按普通文字填写即可，无需编写 HTML。</div>
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

        <el-card v-if="showSkuSection" shadow="never" class="section">
          <template #header><span class="card-title">SKU 规格管理</span></template>

          <div v-for="(dim, di) in specs" :key="di" class="spec-dim">
            <div class="spec-dim-header">
              <el-select
                v-model="dim.name"
                filterable
                allow-create
                default-first-option
                placeholder="规格名称"
                style="width: 200px"
                @blur="generateMatrix"
              >
                <el-option
                  v-for="name in specNameOptions"
                  :key="name"
                  :label="name"
                  :value="name"
                />
              </el-select>
              <el-button type="danger" link size="small" @click="removeSpecDimension(di)">
                删除维度
              </el-button>
            </div>
            <div class="spec-values">
              <div v-for="(val, vi) in dim.values" :key="vi" class="spec-val-item">
                <el-select
                  v-model="dim.values[vi]"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="规格值"
                  style="width: 160px"
                  @blur="generateMatrix"
                >
                  <el-option
                    v-for="item in specValueOptions(dim.name)"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
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

          <el-table
            v-if="skuMatrix.length > 0"
            :data="skuMatrix"
            native-scrollbar
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
            <el-table-column label="SKU编码" width="150" align="center">
              <template #default="{ row }">
                <el-input
                  v-model="row.skuCode"
                  size="small"
                  maxlength="64"
                  placeholder="如 SKU-001"
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
      </div>

      <div class="editor-side">
        <el-card shadow="never" class="preview-panel">
          <template #header>
            <div class="preview-header">
              <div>
                <div class="card-title">实时预览看板</div>
                <div class="preview-subtitle">后台边改边看小程序前端大概呈现效果</div>
              </div>
              <el-tag :type="hasUnsavedChanges ? 'warning' : 'success'" effect="light">
                {{ hasUnsavedChanges ? "预览含未保存修改" : "预览已对齐当前保存态" }}
              </el-tag>
            </div>
          </template>

          <el-tabs v-model="previewTab" class="preview-tabs">
            <el-tab-pane label="分类页卡片" name="card">
              <div class="phone-shell">
                <div class="phone-screen">
                  <div class="miniapp-nav">
                    <span>{{ selectedCategoryName }}</span>
                    <span class="miniapp-meta">{{ selectedStatusLabel }}</span>
                  </div>
                  <div class="miniapp-search">搜索商品，共 1 款好物</div>
                  <div class="catalog-card">
                    <div class="catalog-image-wrap">
                      <img v-if="previewCoverImage" :src="previewCoverImage" alt="商品主图" class="catalog-image" />
                      <div v-else class="image-placeholder">待上传商品主图</div>
                    </div>
                    <div class="catalog-content">
                      <div class="catalog-name">{{ form.name || "这里会显示商品名称" }}</div>
                      <div class="catalog-intro">
                        {{ form.introduction || "这里会显示商品简介，让操作员更直观看到前端列表卡片效果。" }}
                      </div>
                      <div class="catalog-bottom">
                        <div>
                          <div class="catalog-price">{{ previewPriceText }}</div>
                          <div class="catalog-price-note">{{ selectedTypeLabel }}</div>
                        </div>
                        <button class="catalog-action" type="button">选</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="详情页预览" name="detail">
              <div class="phone-shell">
                <div class="phone-screen detail-screen">
                  <div class="detail-gallery">
                    <img v-if="previewCoverImage" :src="previewCoverImage" alt="商品主图" class="detail-hero" />
                    <div v-else class="image-placeholder detail-placeholder">待上传商品主图</div>
                    <div class="detail-dots">
                      <span
                        v-for="(_, index) in Math.max(previewImages.length, 1)"
                        :key="index"
                        class="detail-dot"
                        :class="{ active: index === 0 }"
                      />
                    </div>
                  </div>
                  <div class="detail-card">
                    <div class="detail-title">{{ form.name || "这里会显示商品名称" }}</div>
                    <div class="detail-intro">
                      {{ form.introduction || "这里会显示商品简介，帮助客户预判详情页首屏信息是否清楚。" }}
                    </div>
                    <div class="detail-price-row">
                      <span class="detail-price">{{ previewPriceText }}</span>
                      <span v-if="previewMarketPriceText" class="detail-market-price">{{ previewMarketPriceText }}</span>
                    </div>
                    <div class="detail-meta">
                      <span>分类：{{ selectedCategoryName }}</span>
                      <span>库存：{{ previewTotalStock }}</span>
                    </div>
                    <div class="detail-tags">
                      <span
                        v-for="tag in keywordTags.slice(0, 4)"
                        :key="tag"
                        class="detail-tag"
                      >
                        {{ tag }}
                      </span>
                      <span v-if="keywordTags.length === 0" class="detail-tag detail-tag-muted">未设置关键词</span>
                    </div>
                  </div>
                  <div class="detail-section">
                    <div class="detail-section-title">商品详情</div>
                    <p
                      v-for="(line, index) in previewDescriptionLines"
                      :key="index"
                      class="detail-paragraph"
                    >
                      {{ line }}
                    </p>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="规格价格" name="sku">
              <div class="preview-summary">
                <div class="summary-item">
                  <span class="summary-label">当前分类</span>
                  <strong>{{ selectedCategoryName }}</strong>
                </div>
                <div class="summary-item">
                  <span class="summary-label">前端售价</span>
                  <strong>{{ previewPriceText }}</strong>
                </div>
                <div class="summary-item">
                  <span class="summary-label">总库存</span>
                  <strong>{{ previewTotalStock }}</strong>
                </div>
              </div>
              <div class="preview-sku-list">
                <div v-for="(item, index) in previewSkuSummary" :key="`${item.skuCode}-${index}`" class="preview-sku-card">
                  <img v-if="item.picUrl" :src="item.picUrl" alt="SKU 图片" class="preview-sku-image" />
                  <div v-else class="preview-sku-image image-placeholder small-placeholder">待配图</div>
                  <div class="preview-sku-info">
                    <div class="preview-sku-label">{{ item.label }}</div>
                    <div class="preview-sku-code">{{ item.skuCode || "未设置 SKU 编码" }}</div>
                    <div class="preview-sku-meta">
                      <span>{{ formatYuan(item.price) }}</span>
                      <span>库存 {{ item.stock }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <el-alert
                :closable="false"
                type="info"
                show-icon
                title="这里展示的是实时预览和恢复功能。当前版本的一键恢复针对本次编辑会话，恢复到上次加载或保存时的内容。"
              />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </div>

    <div class="footer-actions">
      <el-button @click="handleCancel">取消</el-button>
      <el-button plain :disabled="!hasUnsavedChanges" @click="handleRestoreSnapshot">
        {{ restoreButtonText }}
      </el-button>
      <el-button type="primary" :disabled="!savedSnapshot" :loading="submitting" @click="handleSave">
        {{ isEdit ? "保存修改" : "创建商品" }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.app-container {
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editor-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 16px;
  align-items: start;
}

.editor-main {
  min-width: 0;
}

.editor-side {
  position: sticky;
  top: 16px;
}

.section {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}

.preview-subtitle {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.preview-panel {
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.preview-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.phone-shell {
  border-radius: 28px;
  background: #25262b;
  padding: 12px;
}

.phone-screen {
  min-height: 680px;
  border-radius: 22px;
  background: #f6f7f4;
  overflow: hidden;
}

.miniapp-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 18px 10px;
  font-size: 14px;
  font-weight: 600;
  color: #2d3a2e;
}

.miniapp-meta {
  color: #7f8d82;
  font-size: 12px;
  font-weight: 500;
}

.miniapp-search {
  margin: 0 18px 14px;
  border: 1px solid #e5eadf;
  background: #fefefc;
  color: #9ca89d;
  border-radius: 999px;
  padding: 10px 14px;
  font-size: 13px;
}

.catalog-card {
  margin: 0 18px 18px;
  background: #fefefc;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(91, 140, 90, 0.08);
}

.catalog-image-wrap {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #eef2ec;
}

.catalog-image,
.detail-hero,
.preview-sku-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.catalog-content {
  padding: 14px 16px 16px;
}

.catalog-name,
.detail-title {
  color: #2d3a2e;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.4;
}

.catalog-intro,
.detail-intro,
.detail-paragraph,
.preview-sku-code {
  margin-top: 8px;
  color: #7f8d82;
  font-size: 13px;
  line-height: 1.6;
}

.catalog-bottom {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-top: 14px;
}

.catalog-price,
.detail-price {
  color: #cf4a3e;
  font-size: 24px;
  font-weight: 700;
}

.catalog-price-note {
  margin-top: 4px;
  color: #9ca89d;
  font-size: 12px;
}

.catalog-action {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 14px;
  background: #dce8d8;
  color: #4f7e50;
  font-size: 20px;
  font-weight: 700;
}

.detail-screen {
  background: #f4f6f2;
}

.detail-gallery {
  position: relative;
  aspect-ratio: 1 / 1;
  background: #eef2ec;
}

.detail-dots {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 14px;
  display: flex;
  justify-content: center;
  gap: 6px;
}

.detail-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.5);
}

.detail-dot.active {
  width: 20px;
  background: #ffffff;
}

.detail-card,
.detail-section {
  margin: 14px;
  padding: 16px;
  border-radius: 18px;
  background: #fefefc;
}

.detail-price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-top: 12px;
}

.detail-market-price {
  color: #a0aab7;
  font-size: 14px;
  text-decoration: line-through;
}

.detail-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  color: #7f8d82;
  font-size: 12px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.detail-tag {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #e8f2e7;
  color: #4f7e50;
  font-size: 12px;
}

.detail-tag-muted {
  background: #f0f2ed;
  color: #9ca89d;
}

.detail-section-title {
  color: #2d3a2e;
  font-size: 15px;
  font-weight: 700;
}

.preview-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.summary-item {
  border-radius: 14px;
  background: #f6f7f4;
  padding: 12px;
}

.summary-label {
  display: block;
  margin-bottom: 6px;
  color: #909399;
  font-size: 12px;
}

.preview-sku-list {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.preview-sku-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  border: 1px solid #ebeef2;
  border-radius: 14px;
  padding: 10px;
  background: #fefefc;
}

.preview-sku-image {
  width: 72px;
  height: 72px;
  border-radius: 12px;
}

.preview-sku-info {
  min-width: 0;
}

.preview-sku-label {
  color: #2d3a2e;
  font-size: 14px;
  font-weight: 600;
}

.preview-sku-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  color: #5f8f74;
  font-size: 13px;
  font-weight: 600;
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
  padding: 16px 0 0;
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

.freight-tip {
  margin-bottom: 16px;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #9ca89d;
  background: linear-gradient(135deg, #eef2ec 0%, #e2e9df 100%);
  font-size: 13px;
}

.detail-placeholder {
  min-height: 100%;
}

.small-placeholder {
  font-size: 12px;
}

@media (max-width: 1280px) {
  .editor-shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .editor-side {
    position: static;
  }
}

@media (max-width: 768px) {
  .page-header,
  .header-actions,
  .header-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .preview-summary {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
