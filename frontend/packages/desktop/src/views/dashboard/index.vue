<template>
  <div class="page-container dashboard">
    <div class="page-header">
      <h2>仪表盘</h2>
      <el-date-picker
        v-model="selectedYear"
        type="year"
        placeholder="选择年份"
        format="YYYY"
        value-format="YYYY"
        style="width: 120px"
        @change="loadData"
      />
    </div>

    <el-row :gutter="16" class="stat-cards">
      <el-col :span="4" v-for="item in statCards" :key="item.label">
        <div class="stat-card">
          <div class="stat-value" :style="{ color: item.color }">
            {{ item.prefix }}{{ item.value }}
          </div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="14">
        <div class="chart-container">
          <h3 class="chart-title">费用月度趋势</h3>
          <div ref="trendChartRef" class="chart-box" />
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-container">
          <h3 class="chart-title">分类占比</h3>
          <div ref="categoryChartRef" class="chart-box" />
        </div>
      </el-col>
    </el-row>

    <div class="chart-container" style="margin-top: 16px">
      <h3 class="chart-title">待报销提醒</h3>
      <el-table :data="pendingExpenses" stripe style="width: 100%">
        <el-table-column prop="expenseDate" label="日期" width="120" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            {{ getCategoryLabel(row.category) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">{{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'pending' ? 'warning' : 'info'" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getStatsOverview,
  getMonthlyTrend,
  getCategoryStats,
  getRecentExpenses,
  formatAmount,
  getCategoryLabel,
  getStatusLabel,
  type StatsOverviewVO,
  type MonthlyTrendVO,
  type CategoryStatsVO,
  type ExpenseVO,
} from '@qianku/shared'

const selectedYear = ref(String(new Date().getFullYear()))
const overview = ref<StatsOverviewVO | null>(null)
const monthlyData = ref<MonthlyTrendVO[]>([])
const categoryData = ref<CategoryStatsVO[]>([])
const pendingExpenses = ref<ExpenseVO[]>([])

const trendChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null

const statCards = computed(() => [
  { label: '总支出', value: formatAmount(overview.value?.totalExpense || 0), color: '#1d1d1f', prefix: '' },
  { label: '已报销', value: formatAmount(overview.value?.reimbursedAmount || 0), color: '#34C759', prefix: '' },
  { label: '待报销', value: formatAmount(overview.value?.pendingAmount || 0), color: '#FF9500', prefix: '' },
  { label: '出差天数', value: overview.value?.tripDays || 0, color: '#007AFF', prefix: '' },
  { label: '出差次数', value: overview.value?.tripCount || 0, color: '#5856D6', prefix: '' },
  { label: '发票数', value: overview.value?.invoiceCount || 0, color: '#FF3B30', prefix: '' },
])

async function loadData() {
  const year = Number(selectedYear.value)
  try {
    const [overviewRes, trendRes, categoryRes, recentRes] = await Promise.allSettled([
      getStatsOverview({ year }),
      getMonthlyTrend({ year }),
      getCategoryStats({ year }),
      getRecentExpenses(10),
    ])
    if (overviewRes.status === 'fulfilled') overview.value = overviewRes.value
    if (trendRes.status === 'fulfilled') monthlyData.value = trendRes.value
    if (categoryRes.status === 'fulfilled') categoryData.value = categoryRes.value
    if (recentRes.status === 'fulfilled') {
      pendingExpenses.value = recentRes.value.filter(e => e.status === 'pending')
    }
  } catch {
    // silent
  }
  await nextTick()
  renderTrendChart()
  renderCategoryChart()
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const months = monthlyData.value.map(m => m.month)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['支出', '已报销'], bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: months, axisTick: { show: false } },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [
      {
        name: '支出',
        type: 'line',
        smooth: true,
        data: monthlyData.value.map(m => m.expense),
        itemStyle: { color: '#007AFF' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0,122,255,0.15)' },
          { offset: 1, color: 'rgba(0,122,255,0.01)' },
        ])},
      },
      {
        name: '已报销',
        type: 'line',
        smooth: true,
        data: monthlyData.value.map(m => m.reimbursed),
        itemStyle: { color: '#34C759' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(52,199,89,0.15)' },
          { offset: 1, color: 'rgba(52,199,89,0.01)' },
        ])},
      },
    ],
  })
}

function renderCategoryChart() {
  if (!categoryChartRef.value) return
  if (!categoryChart) {
    categoryChart = echarts.init(categoryChartRef.value)
  }
  const colors = ['#007AFF', '#34C759', '#FF9500', '#FF3B30', '#5856D6', '#AF52DE', '#FF2D55']
  categoryChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
    },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      label: { show: true, formatter: '{b}\n{d}%' },
      data: categoryData.value.map((c, i) => ({
        name: getCategoryLabel(c.category),
        value: c.amount,
        itemStyle: { color: colors[i % colors.length] },
      })),
    }],
  })
}

function handleResize() {
  trendChart?.resize()
  categoryChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  categoryChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard {
  .stat-cards {
    margin-bottom: 16px;
  }

  .chart-row {
    margin-bottom: 0;
  }

  .chart-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 16px;
    color: #1d1d1f;
  }

  .chart-box {
    height: 320px;
  }

  .amount {
    font-weight: 600;
    color: #1d1d1f;
  }
}
</style>
