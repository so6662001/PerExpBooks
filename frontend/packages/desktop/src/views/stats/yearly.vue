<template>
  <div class="page-container stats-yearly">
    <div class="page-header">
      <h2>年度对比</h2>
    </div>

    <div class="chart-container">
      <h3 class="chart-title">年度支出对比</h3>
      <div ref="barChartRef" style="height: 400px" />
    </div>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="chart-container">
          <h3 class="chart-title">年度月均对比</h3>
          <div ref="lineChartRef" style="height: 340px" />
        </div>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>年度汇总</span>
          </template>
          <el-table :data="yearlyStats" stripe>
            <el-table-column prop="year" label="年份" width="80" />
            <el-table-column prop="totalExpense" label="总支出" align="right">
              <template #default="{ row }">{{ formatAmount(row.totalExpense) }}</template>
            </el-table-column>
            <el-table-column prop="reimbursed" label="已报销" align="right">
              <template #default="{ row }">{{ formatAmount(row.reimbursed) }}</template>
            </el-table-column>
            <el-table-column prop="tripCount" label="出差" width="80" align="center" />
            <el-table-column label="报销率" width="100" align="center">
              <template #default="{ row }">
                {{ row.totalExpense > 0 ? Math.round((row.reimbursed / row.totalExpense) * 100) : 0 }}%
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStatsOverview, getMonthlyTrend, formatAmount, type StatsOverviewVO, type MonthlyTrendVO } from '@qianku/shared'

interface YearlyStats {
  year: number
  totalExpense: number
  reimbursed: number
  tripCount: number
  monthlyData: MonthlyTrendVO[]
}

const yearlyStats = ref<YearlyStats[]>([])
const barChartRef = ref<HTMLElement>()
const lineChartRef = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

const currentYear = new Date().getFullYear()
const years = [currentYear, currentYear - 1, currentYear - 2]

async function loadData() {
  const results: YearlyStats[] = []
  for (const y of years) {
    try {
      const [ov, monthly] = await Promise.allSettled([
        getStatsOverview({ year: y }),
        getMonthlyTrend({ year: y }),
      ])
      results.push({
        year: y,
        totalExpense: ov.status === 'fulfilled' ? (ov.value as StatsOverviewVO).totalExpense : 0,
        reimbursed: ov.status === 'fulfilled' ? (ov.value as StatsOverviewVO).reimbursedAmount : 0,
        tripCount: ov.status === 'fulfilled' ? (ov.value as StatsOverviewVO).tripCount : 0,
        monthlyData: monthly.status === 'fulfilled' ? (monthly.value as MonthlyTrendVO[]) : [],
      })
    } catch {
      results.push({ year: y, totalExpense: 0, reimbursed: 0, tripCount: 0, monthlyData: [] })
    }
  }
  yearlyStats.value = results
  await nextTick()
  renderBarChart()
  renderLineChart()
}

function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  const colors = ['#007AFF', '#34C759', '#FF9500']
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['总支出', '已报销'], bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 50 },
    xAxis: { type: 'category', data: yearlyStats.value.map(y => `${y.year}年`) },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [
      {
        name: '总支出',
        type: 'bar',
        data: yearlyStats.value.map((y, i) => ({
          value: y.totalExpense,
          itemStyle: { color: colors[i], borderRadius: [6, 6, 0, 0] },
        })),
        barWidth: 40,
      },
      {
        name: '已报销',
        type: 'bar',
        data: yearlyStats.value.map(y => ({
          value: y.reimbursed,
          itemStyle: { color: '#34C759', borderRadius: [6, 6, 0, 0] },
        })),
        barWidth: 40,
      },
    ],
  })
}

function renderLineChart() {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)

  const months = Array.from({ length: 12 }, (_, i) => `${i + 1}月`)
  const colors = ['#007AFF', '#FF9500', '#5856D6']

  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: yearlyStats.value.map(y => `${y.year}年`), bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 50 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: yearlyStats.value.map((y, i) => ({
      name: `${y.year}年`,
      type: 'line',
      smooth: true,
      data: months.map((_, mi) => {
        const md = y.monthlyData.find(m => {
          const mMonth = parseInt(m.month.split('-')[1] || m.month)
          return mMonth === mi + 1
        })
        return md ? md.expense : 0
      }),
      itemStyle: { color: colors[i] },
    })),
  })
}

function handleResize() {
  barChart?.resize()
  lineChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  lineChart?.dispose()
})
</script>

<style lang="scss" scoped>
.stats-yearly {
  .chart-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 12px;
    color: #1d1d1f;
  }
}
</style>
