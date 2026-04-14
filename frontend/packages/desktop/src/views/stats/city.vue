<template>
  <div class="page-container stats-city">
    <div class="page-header">
      <h2>城市排行</h2>
      <el-select v-model="year" style="width: 120px" @change="loadData">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <div class="chart-container">
          <h3 class="chart-title">城市费用排行</h3>
          <div ref="barChartRef" style="height: 400px" />
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-container">
          <h3 class="chart-title">城市出差占比</h3>
          <div ref="pieChartRef" style="height: 400px" />
        </div>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>城市明细</span>
      </template>
      <el-table :data="cityStats" stripe>
        <el-table-column type="index" width="60" label="排名" />
        <el-table-column prop="city" label="城市" width="150" />
        <el-table-column prop="tripCount" label="出差次数" width="100" align="center" />
        <el-table-column prop="totalDays" label="总天数" width="100" align="center" />
        <el-table-column prop="totalExpense" label="总费用" align="right" width="150">
          <template #default="{ row }">
            <span class="amount">{{ formatAmount(row.totalExpense) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="日均费用" align="right" width="130">
          <template #default="{ row }">
            {{ row.totalDays > 0 ? formatAmount(row.totalExpense / row.totalDays) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="费用占比" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.percentage" :color="'#007AFF'" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { listTrips, formatAmount, type TripVO } from '@qianku/shared'

interface CityStats {
  city: string
  tripCount: number
  totalDays: number
  totalExpense: number
  percentage: number
}

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const yearOptions = computed(() => Array.from({ length: 5 }, (_, i) => currentYear - i))

const cityStats = ref<CityStats[]>([])
const barChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const colors = ['#007AFF', '#34C759', '#FF9500', '#FF3B30', '#5856D6', '#AF52DE', '#FF2D55', '#00C7BE']

async function loadData() {
  try {
    const res = await listTrips({
      pageNum: 1,
      pageSize: 200,
      startDate: `${year.value}-01-01`,
      endDate: `${year.value}-12-31`,
    })
    const cityMap = new Map<string, { count: number; days: number; expense: number }>()
    res.list.forEach((t: TripVO) => {
      const city = t.destination
      const existing = cityMap.get(city) || { count: 0, days: 0, expense: 0 }
      existing.count++
      existing.days += t.days
      existing.expense += t.totalExpense
      cityMap.set(city, existing)
    })

    const totalExpense = Array.from(cityMap.values()).reduce((s, v) => s + v.expense, 0)
    cityStats.value = Array.from(cityMap.entries())
      .map(([city, data]) => ({
        city,
        tripCount: data.count,
        totalDays: data.days,
        totalExpense: data.expense,
        percentage: totalExpense > 0 ? Math.round((data.expense / totalExpense) * 100) : 0,
      }))
      .sort((a, b) => b.totalExpense - a.totalExpense)
  } catch {
    cityStats.value = []
  }
  await nextTick()
  renderBarChart()
  renderPieChart()
}

function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  const top10 = cityStats.value.slice(0, 10).reverse()
  barChart.setOption({
    tooltip: { trigger: 'axis', formatter: (params: any) => `${params[0].name}: ¥${params[0].value.toFixed(2)}` },
    grid: { left: 80, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    yAxis: { type: 'category', data: top10.map(c => c.city), axisTick: { show: false } },
    series: [{
      type: 'bar',
      data: top10.map((c, i) => ({
        value: c.totalExpense,
        itemStyle: { color: colors[i % colors.length], borderRadius: [0, 6, 6, 0] },
      })),
      barWidth: 22,
    }],
  })
}

function renderPieChart() {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}次 ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      center: ['50%', '42%'],
      data: cityStats.value.slice(0, 8).map((c, i) => ({
        name: c.city,
        value: c.tripCount,
        itemStyle: { color: colors[i % colors.length] },
      })),
      label: { formatter: '{b}\n{d}%' },
    }],
  })
}

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style lang="scss" scoped>
.stats-city {
  .chart-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 12px;
    color: #1d1d1f;
  }

  .amount {
    font-weight: 600;
    color: #1d1d1f;
  }
}
</style>
