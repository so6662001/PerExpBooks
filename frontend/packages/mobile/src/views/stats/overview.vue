<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getStatsOverview, getMonthlyTrend, getCategoryStats, formatAmount } from '@qianku/shared'
import type { StatsOverviewVO, MonthlyTrendVO, CategoryStatsVO } from '@qianku/shared'
import * as echarts from 'echarts'

const router = useRouter()
const overview = ref<StatsOverviewVO>({
  totalExpense: 0,
  reimbursedAmount: 0,
  pendingAmount: 0,
  tripDays: 0,
  tripCount: 0,
  invoiceCount: 0,
})
const trendData = ref<MonthlyTrendVO[]>([])
const categoryData = ref<CategoryStatsVO[]>([])
const loading = ref(true)

const trendChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null

const statCards = [
  { key: 'totalExpense', label: '总支出', icon: '💰', format: true },
  { key: 'reimbursedAmount', label: '已报销', icon: '✅', format: true },
  { key: 'pendingAmount', label: '待报销', icon: '⏳', format: true },
  { key: 'tripDays', label: '出差天数', icon: '📅', format: false },
  { key: 'tripCount', label: '出差次数', icon: '✈️', format: false },
  { key: 'invoiceCount', label: '发票数', icon: '📄', format: false },
]

onMounted(async () => {
  try {
    const [overviewData, trend, category] = await Promise.all([
      getStatsOverview(),
      getMonthlyTrend(),
      getCategoryStats(),
    ])
    overview.value = overviewData
    trendData.value = trend
    categoryData.value = category
    await nextTick()
    renderTrendChart()
    renderCategoryChart()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  trendChart?.dispose()
  categoryChart?.dispose()
})

function renderTrendChart() {
  if (!trendChartRef.value || trendData.value.length === 0) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trendData.value.map((d) => d.month),
      axisLabel: { fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: (v: number) => v >= 1000 ? `${(v / 1000).toFixed(0)}k` : v, fontSize: 11 },
    },
    series: [
      {
        name: '支出',
        type: 'line',
        data: trendData.value.map((d) => d.expense),
        smooth: true,
        lineStyle: { color: '#007AFF', width: 2 },
        itemStyle: { color: '#007AFF' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0,122,255,0.15)' },
          { offset: 1, color: 'rgba(0,122,255,0.01)' },
        ]) },
      },
      {
        name: '报销',
        type: 'line',
        data: trendData.value.map((d) => d.reimbursed),
        smooth: true,
        lineStyle: { color: '#34C759', width: 2 },
        itemStyle: { color: '#34C759' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(52,199,89,0.15)' },
          { offset: 1, color: 'rgba(52,199,89,0.01)' },
        ]) },
      },
    ],
  })
}

function renderCategoryChart() {
  if (!categoryChartRef.value || categoryData.value.length === 0) return
  categoryChart = echarts.init(categoryChartRef.value)
  const colors = ['#007AFF', '#34C759', '#FF9500', '#FF3B30', '#5856D6', '#AF52DE', '#FF2D55']
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, fontSize: 12 },
      data: categoryData.value.map((d, i) => ({
        name: d.category,
        value: d.amount,
        itemStyle: { color: colors[i % colors.length] },
      })),
    }],
  })
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="统计报表" left-arrow @click-left="router.back()" />

    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-card card">
        <div class="stat-icon">{{ card.icon }}</div>
        <div class="stat-value">
          {{ card.format ? formatAmount((overview as any)[card.key]) : (overview as any)[card.key] }}
        </div>
        <div class="stat-label">{{ card.label }}</div>
      </div>
    </div>

    <div class="chart-section card">
      <h3 class="chart-title">月度趋势</h3>
      <div ref="trendChartRef" class="chart-container" />
    </div>

    <div class="chart-section card">
      <h3 class="chart-title">分类占比</h3>
      <div ref="categoryChartRef" class="chart-container" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px 16px;

  .stat-card {
    text-align: center;
    padding: 14px 8px;
    margin: 0;

    .stat-icon {
      font-size: 24px;
      margin-bottom: 6px;
    }

    .stat-value {
      font-size: 16px;
      font-weight: 700;
      color: var(--color-text);
    }

    .stat-label {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 4px;
    }
  }
}

.chart-section {
  .chart-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .chart-container {
    width: 100%;
    height: 260px;
  }
}
</style>
