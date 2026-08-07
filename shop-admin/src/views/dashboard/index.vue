<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from "vue";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import {
  getDashboardSummary,
  getOrderTrend,
  getOrderStatusDistribution,
  getTopProducts,
  getRecentOrders
} from "@/api/dashboard";
import type {
  DashboardSummary,
  OrderTrend,
  OrderStatusItem,
  TopProduct,
  DashboardRecentOrder
} from "@/api/types";
import echarts from "@/plugins/echarts";

defineOptions({ name: "Dashboard" });

/* ========== 数据 ========== */
const loading = ref(true);
const summary = ref<DashboardSummary>({
  todayOrderCount: 0,
  todaySalesAmount: 0,
  productCount: 0,
  memberCount: 0
});
const recentOrders = ref<DashboardRecentOrder[]>([]);

/* 趋势天数切换 */
const trendDays = ref(7);

/* 订单状态映射 */
type TagType = "success" | "warning" | "info" | "danger" | "primary";
const orderStatusMap: Record<number, { label: string; type: TagType }> = {
  0: { label: "待付款", type: "warning" },
  1: { label: "待发货", type: "primary" },
  2: { label: "待收货", type: "primary" },
  3: { label: "已完成", type: "success" },
  4: { label: "已取消", type: "info" },
  5: { label: "退款中", type: "danger" }
};

/* ========== 图表实例 ========== */
const trendChartRef = ref<HTMLElement>();
const pieChartRef = ref<HTMLElement>();
const barChartRef = ref<HTMLElement>();
let trendChart: echarts.ECharts | null = null;
let pieChart: echarts.ECharts | null = null;
let barChart: echarts.ECharts | null = null;

/* ========== 指标卡片配置 ========== */
const statCards = [
  {
    key: "todayOrderCount",
    title: "今日订单",
    icon: "ep/shopping-cart",
    color: "#409eff",
    bg: "#ecf5ff",
    format: (v: number) => String(v)
  },
  {
    key: "todaySalesAmount",
    title: "今日销售额",
    icon: "ep/money",
    color: "#e6a23c",
    bg: "#fdf6ec",
    prefix: "¥",
    format: (v: number) => (v / 100).toFixed(2)
  },
  {
    key: "productCount",
    title: "上架商品",
    icon: "ep/goods",
    color: "#67c23a",
    bg: "#f0f9eb",
    format: (v: number) => String(v)
  },
  {
    key: "memberCount",
    title: "会员总数",
    icon: "ep/user",
    color: "#909399",
    bg: "#f4f4f5",
    format: (v: number) => String(v)
  }
];

/* ========== 数据加载 ========== */
async function fetchAll() {
  loading.value = true;
  try {
    const [sumData, trendData, statusData, topData, orderData] =
      await Promise.all([
        getDashboardSummary(),
        getOrderTrend(trendDays.value),
        getOrderStatusDistribution(),
        getTopProducts(10),
        getRecentOrders()
      ]);
    summary.value = sumData as DashboardSummary;
    recentOrders.value = (orderData as DashboardRecentOrder[]) ?? [];

    await nextTick();
    renderTrendChart(trendData as OrderTrend[]);
    renderPieChart(statusData as OrderStatusItem[]);
    renderBarChart(topData as TopProduct[]);
  } catch (e) {
    console.error("Dashboard fetch error", e);
  } finally {
    loading.value = false;
  }
}

/* ========== 图表渲染 ========== */
function renderTrendChart(data: OrderTrend[]) {
  if (!trendChartRef.value) return;
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value);
  }
  const dates = data.map(d => d.date.substring(5)); // MM-DD
  const orderCounts = data.map(d => d.orderCount);
  const salesAmounts = data.map(d => +(d.salesAmount / 100).toFixed(2));

  trendChart.setOption({
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "cross" }
    },
    legend: { data: ["订单量", "销售额(元)"], bottom: 0 },
    grid: { left: 48, right: 48, top: 16, bottom: 40 },
    xAxis: {
      type: "category",
      data: dates,
      boundaryGap: false,
      axisLabel: { fontSize: 11 }
    },
    yAxis: [
      {
        type: "value",
        name: "订单量",
        position: "left",
        axisLabel: { fontSize: 11 }
      },
      {
        type: "value",
        name: "销售额(元)",
        position: "right",
        axisLabel: { fontSize: 11 }
      }
    ],
    series: [
      {
        name: "订单量",
        type: "line",
        smooth: true,
        data: orderCounts,
        itemStyle: { color: "#409eff" },
        areaStyle: {
          color: {
            type: "linear",
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(64,158,255,0.25)" },
              { offset: 1, color: "rgba(64,158,255,0.02)" }
            ]
          }
        }
      },
      {
        name: "销售额(元)",
        type: "line",
        smooth: true,
        yAxisIndex: 1,
        data: salesAmounts,
        itemStyle: { color: "#e6a23c" },
        areaStyle: {
          color: {
            type: "linear",
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(230,162,60,0.25)" },
              { offset: 1, color: "rgba(230,162,60,0.02)" }
            ]
          }
        }
      }
    ]
  });
}

function renderPieChart(data: OrderStatusItem[]) {
  if (!pieChartRef.value) return;
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value);
  }
  const colors = [
    "#e6a23c", "#409eff", "#67c23a", "#909399", "#f56c6c", "#b37feb"
  ];
  pieChart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
    legend: { orient: "vertical", right: 8, top: "center", itemGap: 12 },
    color: colors,
    series: [
      {
        type: "pie",
        radius: ["40%", "68%"],
        center: ["35%", "50%"],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: "bold" }
        },
        data: data.map((d, i) => ({
          name: d.name,
          value: d.value,
          itemStyle: { color: colors[i % colors.length] }
        }))
      }
    ]
  });
}

function renderBarChart(data: TopProduct[]) {
  if (!barChartRef.value) return;
  if (!barChart) {
    barChart = echarts.init(barChartRef.value);
  }
  // 倒序以便从下到上显示
  const reversed = [...data].reverse();
  const names = reversed.map(d =>
    d.name.length > 10 ? d.name.substring(0, 10) + "…" : d.name
  );
  const counts = reversed.map(d => d.sales_count);

  barChart.setOption({
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      formatter: (params: any) => {
        const p = Array.isArray(params) ? params[0] : params;
        const item = reversed[p.dataIndex];
        return `${item.name}<br/>销量: ${item.sales_count}<br/>销售额: ¥${(item.sales_amount / 100).toFixed(2)}`;
      }
    },
    grid: { left: 120, right: 40, top: 8, bottom: 8 },
    xAxis: {
      type: "value",
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: "category",
      data: names,
      axisLabel: { fontSize: 11, width: 100, overflow: "truncate" }
    },
    series: [
      {
        type: "bar",
        data: counts,
        barMaxWidth: 20,
        itemStyle: {
          color: {
            type: "linear",
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: "#409eff" },
              { offset: 1, color: "#67c23a" }
            ]
          },
          borderRadius: [0, 4, 4, 0]
        },
        label: {
          show: true,
          position: "right",
          fontSize: 11,
          color: "#606266"
        }
      }
    ]
  });
}

/* ========== 窗口自适应 ========== */
function handleResize() {
  trendChart?.resize();
  pieChart?.resize();
  barChart?.resize();
}

/* ========== 趋势天数切换 ========== */
async function onTrendDaysChange() {
  try {
    const trendData = (await getOrderTrend(trendDays.value)) as OrderTrend[];
    renderTrendChart(trendData);
  } catch (e) {
    console.error("trend fetch error", e);
  }
}

/* ========== 格式化 ========== */
function formatPrice(v: number) {
  return "¥" + (v / 100).toFixed(2);
}
function getStatusLabel(s: number) {
  return orderStatusMap[s]?.label ?? "未知";
}
function getStatusType(s: number): TagType {
  return orderStatusMap[s]?.type ?? "info";
}

/* ========== 生命周期 ========== */
onMounted(() => {
  fetchAll();
  window.addEventListener("resize", handleResize);
});
onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  trendChart?.dispose();
  pieChart?.dispose();
  barChart?.dispose();
});
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.key">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card-inner">
            <div
              class="stat-icon"
              :style="{ background: card.bg, color: card.color }"
            >
              <component :is="useRenderIcon(card.icon)" />
            </div>
            <div class="stat-info">
              <div class="stat-value">
                <span v-if="card.prefix" class="stat-prefix">{{
                  card.prefix
                }}</span>
                {{
                  card.format(
                    (summary as any)[card.key] ?? 0
                  )
                }}
              </div>
              <div class="stat-label">{{ card.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行：趋势 + 状态分布 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">订单趋势</span>
              <el-radio-group
                v-model="trendDays"
                size="small"
                @change="onTrendDaysChange"
              >
                <el-radio-button :value="7">近 7 天</el-radio-button>
                <el-radio-button :value="30">近 30 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">订单状态分布</span>
          </template>
          <div ref="pieChartRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行：热销 TOP 10 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">热销商品 TOP 10</span>
          </template>
          <div ref="barChartRef" class="chart-box bar-chart" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近订单表格 -->
    <el-row class="chart-row">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">最近订单</span>
          </template>
          <el-table
            :data="recentOrders"
            stripe
            size="small"
            style="width: 100%"
          >
            <el-table-column prop="order_sn" label="订单号" min-width="180" />
            <el-table-column prop="consignee" label="收货人" width="100" />
            <el-table-column label="商品数" width="80" align="center">
              <template #default="{ row }">{{ row.item_count }}</template>
            </el-table-column>
            <el-table-column label="实付金额" width="120" align="right">
              <template #default="{ row }">{{
                formatPrice(row.actual_price)
              }}</template>
            </el-table-column>
            <el-table-column label="订单状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">{{
                  getStatusLabel(row.status)
                }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="支付状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.pay_status === 1 ? 'success' : 'warning'"
                  size="small"
                  >{{ row.pay_status === 1 ? "已支付" : "未支付" }}</el-tag
                >
              </template>
            </el-table-column>
            <el-table-column label="下单时间" min-width="170">
              <template #default="{ row }">{{ row.create_time }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 16px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.3;
}

.stat-prefix {
  font-size: 16px;
  font-weight: 500;
  margin-right: 2px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.chart-row {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart-box {
  width: 100%;
  height: 320px;
}

.chart-box.bar-chart {
  height: 400px;
}
</style>
