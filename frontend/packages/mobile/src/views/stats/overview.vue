<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getStatsOverview, getMonthlyTrend, getCategoryRatio, formatAmount } from '@qianku/shared'
import type { StatsOverviewVO, MonthlyTrendVO, CategoryRatioVO } from '@qianku/shared'
import * as echarts from 'echarts'

const router = useRouter()
const overview = ref<StatsOverviewVO>({
  totalExpense: 0,
  totalReimbursed: 0,
  totalPending: 0,
  totalTripDays: 0,
  tripCount: 0,
  invoiceCount: 0,
})
const trendData = ref<MonthlyTrendVO[]>([])
const categoryData = ref<CategoryRatioVO[]>([])
const loading = ref(true)

const trendChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null

const statCards = [
  { key: 'totalExpense', label: '总支出', icon: '💰', format: true },
  { key: 'totalReimbursed', label: '已报销', icon: '✅', format: true },
  { key: 'totalPending', label: '待报销', icon: '⏳', format: true },
  { key: 'totalTripDays', label: '出差天数', icon: '📅', format: false },
  { key: 'tripCount', label: '出差次数', icon: '✈️', format: false },
  { key: 'invoiceCount', label: '发票数', icon: '📄', format: false },
]

const reportLinks = [
  { label: '月度趋势', path: '/stats/monthly', icon: '📈' },
  { label: '分类分析', path: '/stats/category', icon: '📊' },
  { label: '出差统计', path: '/stats/trip', icon: '✈️' },
  { label: '报销进度', path: '/stats/progress', icon: '📋' },
  { label: '年度对比', path: '/stats/yearly', icon: '📅' },
  { label: '城市排行', path: '/stats/city', icon: '🏙️' },
  { label: '费用日历', path: '/stats/calendar', icon: '🗓️' },
]

onMounted(async () => {
  try {
    const currentYear = new Date().getFullYear()
    const [overviewData, trend, category] = await Promise.all([
      getStatsOverview(),
      getMonthlyTrend({ year: currentYear }),
      getCategoryRatio(),
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
        data: trendData.value.map((d) => d.amount),
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
        name: d.categoryName,
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

    <div class="report-section card">
      <h3 class="chart-title">详细报表</h3>
      <div class="report-grid">
        <div
          v-for="link in reportLinks"
          :key="link.path"
          class="report-item"
          @click="router.push(link.path)"
        >
          <span class="report-icon">{{ link.icon }}</span>
          <span class="report-label">{{ link.label }}</span>
          <van-icon name="arrow" class="report-arrow" />
        </div>
      </div>
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

.report-section {
  .chart-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .report-grid {
    .report-item {
      display: flex;
      align-items: center;
      padding: 14px 0;
      border-bottom: 0.5px solid var(--color-divider);
      cursor: pointer;

      &:last-child {
        border-bottom: none;
      }

      .report-icon {
        font-size: 20px;
        margin-right: 12px;
      }

      .report-label {
        flex: 1;
        font-size: 15px;
      }

      .report-arrow {
        color: var(--color-text-tertiary);
      }
    }
  }
}
</style>
