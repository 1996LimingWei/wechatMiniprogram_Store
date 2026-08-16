<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { QuestionFilled } from "@element-plus/icons-vue";
import {
    getCategoryList,
    createCategory,
    updateCategory,
    updateCategoryStatus,
    deleteCategory
} from "@/api/category";
import MaterialImagePicker from "@/components/MaterialImagePicker/index.vue";
import type { Category } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "ProductCategory" });
const canManageProduct = hasAnyPerms(["product:manage"]);

/* ---------- 数据 ---------- */
const loading = ref(false);
const flatList = ref<Category[]>([]);

/** 将扁平列表构建为树形结构 */
const treeData = computed(() => {
    const map = new Map<number, any>();
    const roots: any[] = [];
    flatList.value.forEach(c => {
        map.set(c.id!, { ...c, children: [] });
    });
    flatList.value.forEach(c => {
        const node = map.get(c.id!)!;
        if (c.parentId === 0) {
            roots.push(node);
        } else {
            map.get(c.parentId)?.children.push(node);
        }
    });
    return roots;
});

/** 仅顶级分类（供「新增子分类」下拉使用） */
const topLevel = computed(() =>
    flatList.value.filter(c => c.parentId === 0)
);

async function fetchData() {
    loading.value = true;
    try {
        flatList.value = (await getCategoryList()) as Category[];
    } finally {
        loading.value = false;
    }
}

/* ---------- 对话框 ---------- */
const dialogVisible = ref(false);
const dialogTitle = ref("新增分类");
const submitting = ref(false);
const form = reactive({
    id: undefined as number | undefined,
    parentId: 0,
    name: "",
    icon: "",
    sort: 0,
    status: 1
});

function resetForm() {
    form.id = undefined;
    form.parentId = 0;
    form.name = "";
    form.icon = "";
    form.sort = 0;
    form.status = 1;
}

/** 新增顶级分类 */
function openAddRoot() {
    resetForm();
    form.parentId = 0;
    dialogTitle.value = "新增顶级分类";
    dialogVisible.value = true;
}

/** 新增子分类 */
function openAddChild(row: Category) {
    resetForm();
    form.parentId = row.id!;
    dialogTitle.value = "新增子分类";
    dialogVisible.value = true;
}

/** 编辑 */
function openEdit(row: Category) {
    resetForm();
    form.id = row.id;
    form.parentId = row.parentId;
    form.name = row.name;
    form.icon = row.icon || "";
    form.sort = row.sort ?? 0;
    form.status = row.status ?? 1;
    dialogTitle.value = "编辑分类";
    dialogVisible.value = true;
}

async function handleSubmit() {
    if (!form.name.trim()) {
        ElMessage.warning("请输入分类名称");
        return;
    }
    submitting.value = true;
    try {
        const payload: Category = {
            parentId: form.parentId,
            name: form.name.trim(),
            icon: form.icon || undefined,
            sort: form.sort,
            status: form.status
        };
        if (form.id) {
            (payload as any).id = form.id;
            await updateCategory(payload);
            ElMessage.success("更新成功");
        } else {
            await createCategory(payload);
            ElMessage.success("创建成功");
        }
        dialogVisible.value = false;
        fetchData();
    } finally {
        submitting.value = false;
    }
}

/* ---------- 删除 ---------- */
async function handleDelete(row: Category) {
    const hasChildren = flatList.value.some(c => c.parentId === row.id);
    if (hasChildren) {
        ElMessage.warning("该分类下有子分类，请先删除子分类");
        return;
    }
    await ElMessageBox.confirm(
        `确定删除分类「${row.name}」吗？`,
        "确认删除",
        { type: "warning" }
    );
    await deleteCategory(row.id!);
    ElMessage.success("删除成功");
    fetchData();
}

/* ---------- 状态切换 ---------- */
async function handleStatusChange(row: Category) {
    const prevStatus = row.status === 1 ? 0 : 1;
    try {
        await updateCategoryStatus(row.id!, row.status);
        ElMessage.success(row.status === 1 ? "已启用" : "已禁用");
    } catch {
        row.status = prevStatus;
    }
}

onMounted(fetchData);
</script>

<template>
    <div class="app-container">
        <!-- 顶部操作栏 -->
        <el-card shadow="never" class="mb-4">
            <el-button v-if="canManageProduct" type="primary" @click="openAddRoot">
                <el-icon class="mr-1"><i class="ep-icon-plus" /></el-icon>
                新增顶级分类
            </el-button>
        </el-card>

        <!-- 树形表格 -->
        <el-card shadow="never">
            <el-table
                :data="treeData"
                row-key="id"
                v-loading="loading"
                :tree-props="{ children: 'children' }"
                default-expand-all
                border
            >
                <el-table-column prop="name" label="分类名称" min-width="200" />
                <el-table-column prop="id" label="ID" width="80" align="center" />
                <el-table-column prop="icon" label="图标" width="100" align="center">
                    <template #default="{ row }">
                        <el-image
                            v-if="row.icon"
                            :src="row.icon"
                            style="width: 32px; height: 32px; border-radius: 4px"
                            fit="cover"
                        />
                        <span v-else class="text-gray-400">—</span>
                    </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序" width="80" align="center" />
                <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-switch
                            v-model="row.status"
                            :disabled="!canManageProduct"
                            :active-value="1"
                            :inactive-value="0"
                            @change="handleStatusChange(row)"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="240" align="center">
                    <template #default="{ row }">
                        <el-button
                            v-if="canManageProduct && row.parentId === 0"
                            type="primary"
                            link
                            size="small"
                            @click="openAddChild(row)"
                        >
                            新增子分类
                        </el-button>
                        <el-button
                            v-if="canManageProduct"
                            type="primary"
                            link
                            size="small"
                            @click="openEdit(row)"
                        >
                            编辑
                        </el-button>
                        <el-button
                            v-if="canManageProduct"
                            type="danger"
                            link
                            size="small"
                            @click="handleDelete(row)"
                        >
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新增/编辑 对话框 -->
        <el-dialog
            v-model="dialogVisible"
            :title="dialogTitle"
            width="500px"
            destroy-on-close
        >
            <el-form
                :model="form"
                label-width="100px"
                @submit.prevent="handleSubmit"
            >
                <el-form-item label="上级分类">
                    <el-select
                        v-model="form.parentId"
                        placeholder="无（顶级分类）"
                        clearable
                        style="width: 100%"
                    >
                        <el-option :value="0" label="无（顶级分类）" />
                        <el-option
                            v-for="cat in topLevel"
                            :key="cat.id"
                            :value="cat.id!"
                            :label="cat.name"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="分类名称" required>
                    <el-input
                        v-model="form.name"
                        placeholder="请输入分类名称"
                        maxlength="20"
                    />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        分类图标
                        <el-tooltip content="分类列表中展示的小图标，可上传新图或选择素材库图片" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <MaterialImagePicker v-model="form.icon" biz-type="product" empty-text="请选择分类图标" />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        排序权重
                        <el-tooltip content="数值越大，分类在小程序中显示越靠前。例如 200 排在 100 前面" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <el-input-number
                        v-model="form.sort"
                        :min="0"
                        :max="9999"
                        controls-position="right"
                    />
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
                <el-button
                    type="primary"
                    :loading="submitting"
                    @click="handleSubmit"
                >
                    确定
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.app-container {
    padding: 16px;
}
.mb-4 {
    margin-bottom: 16px;
}
.mr-1 {
    margin-right: 4px;
}
.text-gray-400 {
    color: #9ca3af;
}
.tip-icon {
    margin-left: 4px;
    color: #909399;
    cursor: help;
    vertical-align: middle;
}
</style>
