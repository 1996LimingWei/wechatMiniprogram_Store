<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { QuestionFilled } from "@element-plus/icons-vue";
import {
    getChannelList,
    createChannel,
    updateChannel,
    updateChannelStatus,
    deleteChannel
} from "@/api/content";
import LinkSelector from "@/components/LinkSelector/index.vue";
import type { ContentChannel } from "@/api/types";
import { hasAnyPerms } from "@/utils/auth";

defineOptions({ name: "ContentChannel" });
const canManageContent = hasAnyPerms(["content:manage"]);

/* ---------- 数据 ---------- */
const loading = ref(false);
const list = ref<ContentChannel[]>([]);

async function fetchData() {
    loading.value = true;
    try {
        list.value = (await getChannelList()) as ContentChannel[];
    } finally {
        loading.value = false;
    }
}

/* ---------- 对话框 ---------- */
const dialogVisible = ref(false);
const dialogTitle = ref("新增频道");
const submitting = ref(false);
const dialogKey = ref(0); // 每次打开弹窗递增，强制重建子组件
const form = reactive({
    id: undefined as number | undefined,
    name: "",
    iconUrl: "",
    url: "",
    sort: 0,
    status: 1
});

function resetForm() {
    form.id = undefined;
    form.name = "";
    form.iconUrl = "";
    form.url = "";
    form.sort = 0;
    form.status = 1;
}

function openAdd() {
    resetForm();
    dialogKey.value++;
    dialogTitle.value = "新增频道";
    dialogVisible.value = true;
}

function openEdit(row: ContentChannel) {
    resetForm();
    dialogKey.value++;
    form.id = row.id;
    form.name = row.name;
    form.iconUrl = row.iconUrl || "";
    form.url = row.url || "";
    form.sort = row.sort ?? 0;
    form.status = row.status ?? 1;
    dialogTitle.value = "编辑频道";
    dialogVisible.value = true;
}

async function handleSubmit() {
    if (!form.name.trim()) {
        ElMessage.warning("请输入频道名称");
        return;
    }
    submitting.value = true;
    try {
        const payload: ContentChannel = {
            name: form.name.trim(),
            iconUrl: form.iconUrl.trim(),
            url: form.url.trim(),
            sort: form.sort,
            status: form.status
        };
        if (form.id) {
            (payload as any).id = form.id;
            await updateChannel(payload);
            ElMessage.success("更新成功");
        } else {
            await createChannel(payload);
            ElMessage.success("创建成功");
        }
        dialogVisible.value = false;
        fetchData();
    } finally {
        submitting.value = false;
    }
}

/* ---------- 删除 ---------- */
async function handleDelete(row: ContentChannel) {
    await ElMessageBox.confirm(
        `确定删除频道「${row.name}」吗？`,
        "确认删除",
        { type: "warning" }
    );
    await deleteChannel(row.id!);
    ElMessage.success("删除成功");
    fetchData();
}

/* ---------- 状态切换 ---------- */
async function handleStatusChange(row: ContentChannel) {
    const prevStatus = row.status === 1 ? 0 : 1;
    try {
        await updateChannelStatus(row.id!, row.status);
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
            <el-button v-if="canManageContent" type="primary" @click="openAdd">
                <el-icon class="mr-1"><i class="ep-icon-plus" /></el-icon>
                新增频道
            </el-button>
        </el-card>

        <!-- 表格 -->
        <el-card shadow="never">
            <el-table :data="list" v-loading="loading" border>
                <el-table-column prop="id" label="ID" width="70" align="center" />
                <el-table-column label="图标" width="100" align="center">
                    <template #default="{ row }">
                        <el-image
                            v-if="row.iconUrl"
                            :src="row.iconUrl"
                            style="width: 40px; height: 40px; border-radius: 8px"
                            fit="cover"
                        />
                        <span v-else class="text-gray-400">—</span>
                    </template>
                </el-table-column>
                <el-table-column prop="name" label="频道名称" min-width="120" show-overflow-tooltip />
                <el-table-column prop="url" label="跳转目标" min-width="200" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span>{{ row.url || '—' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="sort" label="排序" width="80" align="center" />
                <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-switch
                            v-model="row.status"
                            :disabled="!canManageContent"
                            :active-value="1"
                            :inactive-value="0"
                            @change="handleStatusChange(row)"
                        />
                    </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
                <el-table-column label="操作" width="160" align="center" fixed="right">
                    <template #default="{ row }">
                        <el-button v-if="canManageContent" type="primary" link size="small" @click="openEdit(row)">
                            编辑
                        </el-button>
                        <el-button v-if="canManageContent" type="danger" link size="small" @click="handleDelete(row)">
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新增/编辑 对话框 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
            <el-form :model="form" label-width="100px" @submit.prevent="handleSubmit">
                <el-form-item label="频道名称" required>
                    <el-input v-model="form.name" placeholder="请输入频道名称" maxlength="20" />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        图标 URL
                        <el-tooltip content="首页金刚位展示的小图标，建议正方形图片，填写可访问的图片链接" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <el-input v-model="form.iconUrl" placeholder="请输入图标 URL（可选）" />
                </el-form-item>
                <el-form-item v-if="form.iconUrl" label="图标预览">
                    <el-image
                        :src="form.iconUrl"
                        style="width: 48px; height: 48px; border-radius: 8px"
                        fit="cover"
                    >
                        <template #error>
                            <div class="icon-error">失败</div>
                        </template>
                    </el-image>
                </el-form-item>
                <el-form-item>
                    <template #label>
                        跳转目标
                        <el-tooltip content="选择用户点击频道入口后的跳转目标，支持选择商品、专题、固定页面或自定义链接" placement="top">
                            <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                        </el-tooltip>
                    </template>
                    <LinkSelector :key="dialogKey" v-model="form.url" />
                </el-form-item>
                <el-form-item>
                    <template #label>
                        排序权重
                        <el-tooltip content="数值越大，频道在首页金刚位中显示越靠前" placement="top">
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
.icon-error { display: flex; align-items: center; justify-content: center; width: 48px; height: 48px; background: #f5f7fa; color: #909399; font-size: 11px; border-radius: 8px; }
</style>
