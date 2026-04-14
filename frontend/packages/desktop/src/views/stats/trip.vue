<template>
  <div class="page-container stats-trip">
    <div class="page-header">
      <h2>出差统计</h2>
      <el-select v-model="year" style="width: 120px" @change="loadData">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
    </div>

    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #007AFF">{{ overview.tripCount }}</div>
          <div class="stat-label">出差次数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #5856D6">{{ overview.tripDays }}</div>
          <div class="stat-label">出差天数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #FF9500">{{ formatAmount(overview.totalExpense) }}</div>
          <div class="stat-label">总费用</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #34C759">
            {{ overview.tripCount > 0 ? formatAmount(overview.totalExpense / overview.tripCount) : '¥0.00' }}
          </div>
          <div class="stat-label">场均费用</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="chart-container">
          <h3 class="chart-title">月度出差次数</h3>
          <div ref="barChartRef" style="height: 340px" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-container">
          <h3 class="chart-title">费用/天数散点分布</h3>
          <div ref="scatterChartRef" style="height: 340px" />
        </div>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>出差记录</span>
      </template>
      <el-table :data="trips" stripe>
        <el-table-column prop="destination" label="目的地" width="150" />
        <el-table-column label="时间" width="220">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column prop="days" label="天数" width="80" align="center" />
        <el-table-column prop="purpose" label="事由" min-width="200" show-overflow-tooltip />
        <el-table-column prop="totalExpense" label="费用" width="120" align="right">
          <template #default="{ row }">{{ formatAmount(row.totalExpense) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  listTrips,
  getStatsOverview,
  formatAmount,
  getStatusLabel,
  type TripVO,
  type StatsOverviewVO,
} from '@qianku/shared'

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const yearOptions = computed(() => Array.from({ length: 5 }, (_, i) => currentYear - i))

const overview = reactive<StatsOverviewVO>({
  totalExpense: 0,
  reimbursedAmount: 0,
  pendingAmount: 0,
  tripDays: 0,
  tripCount: 0,
  invoiceCount: 0,
})
const trips = ref<TripVO[]>([])

const barChartRef = ref<HTMLElement>()
const scatterChartRef = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null
let scatterChart: echarts.ECharts | null = null

async function loadData() {
  try {
    const [overviewRes, tripsRes] = await Promise.allSettled([
      getStatsOverview({ year: year.value }),
      listTrips({ pageNum: 1, pageSize: 100, startDate: `${year.value}-01-01`, endDate: `${year.value}-12-31` }),
    ])
    if (overviewRes.status === 'fulfilled') Object.assign(overview, overviewRes.value)
    if (tripsRes.status === 'fulfilled') trips.value = tripsRes.value.list
  } catch {
    // silent
  }
  await nextTick()
  renderBarChart()
  renderScatterChart()
}

function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  const monthCounts = Array(12).fill(0)
  trips.value.forEach(t => {
    const month = new Date(t.startDate).getMonth()
    monthCounts[month]++
  })

  barChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: Array.from({ length: 12 }, (_, i) => `${i + 1}月`),
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: monthCounts.map(v => ({
        value: v,
        itemStyle: { color: '#007AFF', borderRadius: [6, 6, 0, 0] },
      })),
      barWidth: 28,
    }],
  })
}

function renderScatterChart() {
  if (!scatterChartRef.value) return
  if (!scatterChart) scatterChart = echarts.init(scatterChartRef.value)

  scatterChart.setOption({
    tooltip: {
      formatter: (params: any) =>
        `${params.data[2]}<br/>天数: ${params.data[0]}<br/>费用: ¥${params.data[1].toFixed(2)}`,
    },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'value', name: '天数', nameLocation: 'center', nameGap: 25 },
    yAxis: { type: 'value', name: '费用 (¥)', axisLabel: { formatter: '¥{value}' } },
    series: [{
      type: 'scatter',
      symbolSize: 14,
      data: trips.value.map(t => [t.days, t.totalExpense, t.destination]),
      itemStyle: { color: '#5856D6' },
    }],
  })
}

function handleResize() {
  barChart?.resize()
  scatterChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  scatterChart?.dispose()
})
</script>

<style lang="scss" scoped>
.stats-trip {
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
