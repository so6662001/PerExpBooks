<template>
  <div class="page-container stats-progress">
    <div class="page-header">
      <h2>报销进度</h2>
    </div>

    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #FF9500">{{ formatAmount(overview.pendingAmount) }}</div>
          <div class="stat-label">待报销</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #007AFF">{{ formatAmount(overview.totalExpense - overview.reimbursedAmount - overview.pendingAmount) }}</div>
          <div class="stat-label">报销中</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #34C759">{{ formatAmount(overview.reimbursedAmount) }}</div>
          <div class="stat-label">已报销</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #1d1d1f">
            {{ overview.totalExpense > 0 ? Math.round((overview.reimbursedAmount / overview.totalExpense) * 100) : 0 }}%
          </div>
          <div class="stat-label">报销率</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="chart-container">
          <h3 class="chart-title">报销状态分布</h3>
          <div ref="pieChartRef" style="height: 360px" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-container">
          <h3 class="chart-title">月度报销进度</h3>
          <div ref="barChartRef" style="height: 360px" />
        </div>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>最近报销单</span>
      </template>
      <el-table :data="reimbursements" stripe>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="itemCount" label="笔数" width="80" align="center" />
        <el-table-column prop="totalAmount" label="金额" width="130" align="right">
          <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'received' ? 'success' : row.status === 'exported' ? 'warning' : ''" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getStatsOverview,
  getMonthlyTrend,
  listReimbursements,
  formatAmount,
  formatDate,
  getStatusLabel,
  type StatsOverviewVO,
  type MonthlyTrendVO,
  type ReimbursementVO,
} from '@qianku/shared'

const overview = reactive<StatsOverviewVO>({
  totalExpense: 0,
  reimbursedAmount: 0,
  pendingAmount: 0,
  tripDays: 0,
  tripCount: 0,
  invoiceCount: 0,
})
const monthlyData = ref<MonthlyTrendVO[]>([])
const reimbursements = ref<ReimbursementVO[]>([])

const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

async function loadData() {
  try {
    const [overviewRes, monthlyRes, reimRes] = await Promise.allSettled([
      getStatsOverview(),
      getMonthlyTrend({ year: new Date().getFullYear() }),
      listReimbursements({ pageNum: 1, pageSize: 10 }),
    ])
    if (overviewRes.status === 'fulfilled') Object.assign(overview, overviewRes.value)
    if (monthlyRes.status === 'fulfilled') monthlyData.value = monthlyRes.value
    if (reimRes.status === 'fulfilled') reimbursements.value = reimRes.value.list
  } catch {
    // silent
  }
  await nextTick()
  renderPieChart()
  renderBarChart()
}

function renderPieChart() {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  const pending = overview.pendingAmount
  const reimbursing = Math.max(0, overview.totalExpense - overview.reimbursedAmount - overview.pendingAmount)
  const reimbursed = overview.reimbursedAmount

  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '42%'],
      data: [
        { name: '待报销', value: pending, itemStyle: { color: '#FF9500' } },
        { name: '报销中', value: reimbursing, itemStyle: { color: '#007AFF' } },
        { name: '已报销', value: reimbursed, itemStyle: { color: '#34C759' } },
      ],
      label: { formatter: '{b}\n¥{c}' },
    }],
  })
}

function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['支出', '已报销'], bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 50 },
    xAxis: {
      type: 'category',
      data: monthlyData.value.map(m => m.month),
    },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [
      {
        name: '支出',
        type: 'bar',
        data: monthlyData.value.map(m => m.expense),
        itemStyle: { color: '#007AFF', borderRadius: [6, 6, 0, 0] },
      },
      {
        name: '已报销',
        type: 'bar',
        data: monthlyData.value.map(m => m.reimbursed),
        itemStyle: { color: '#34C759', borderRadius: [6, 6, 0, 0] },
      },
    ],
  })
}

function handleResize() {
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style lang="scss" scoped>
.stats-progress {
  .stat-cards {
    margin-bottom: 16px;
  }

  .chart-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 12px;
    color: #1d1d1f;
  }
}
</style>
