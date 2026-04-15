<template>
  <div class="page-container stats-monthly">
    <div class="page-header">
      <h2>月度趋势</h2>
      <el-select v-model="year" style="width: 120px" @change="loadData">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
    </div>

    <div class="chart-container">
      <div ref="chartRef" style="height: 400px" />
    </div>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>月度数据明细</span>
      </template>
      <el-table :data="monthlyData" stripe show-summary :summary-method="getSummary">
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column prop="amount" label="支出" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="reimbursed" label="已报销" align="right">
          <template #default="{ row }">{{ formatAmount(row.reimbursed) }}</template>
        </el-table-column>
        <el-table-column label="差额" align="right">
          <template #default="{ row }">
            <span :class="{ negative: row.amount - row.reimbursed > 0 }">
              {{ formatAmount(row.amount - row.reimbursed) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getMonthlyTrend, formatAmount, type MonthlyTrendVO } from '@qianku/shared'
import type { VNode } from 'vue'

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const yearOptions = computed(() =>
  Array.from({ length: 5 }, (_, i) => currentYear - i)
)

const monthlyData = ref<MonthlyTrendVO[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

async function loadData() {
  try {
    monthlyData.value = await getMonthlyTrend({ year: year.value })
  } catch {
    monthlyData.value = []
  }
  await nextTick()
  renderChart()
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const lines = params.map(
          (p: any) => `${p.marker} ${p.seriesName}: ¥${p.value.toFixed(2)}`
        )
        return `${params[0].name}<br/>${lines.join('<br/>')}`
      },
    },
    legend: { data: ['支出', '已报销'], bottom: 0 },
    grid: { left: 60, right: 20, top: 40, bottom: 50 },
    xAxis: {
      type: 'category',
      data: monthlyData.value.map(m => m.month),
      axisTick: { show: false },
    },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [
      {
        name: '支出',
        type: 'line',
        smooth: true,
        data: monthlyData.value.map(m => m.amount),
        itemStyle: { color: '#007AFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,122,255,0.2)' },
            { offset: 1, color: 'rgba(0,122,255,0.01)' },
          ]),
        },
      },
      {
        name: '已报销',
        type: 'line',
        smooth: true,
        data: monthlyData.value.map(m => m.reimbursed),
        itemStyle: { color: '#34C759' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(52,199,89,0.2)' },
            { offset: 1, color: 'rgba(52,199,89,0.01)' },
          ]),
        },
      },
    ],
  })
}

function getSummary({ columns, data }: any) {
  const sums: (string | VNode)[] = []
  columns.forEach((col: any, index: number) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const prop = col.property
    if (prop === 'amount' || prop === 'reimbursed') {
      const total = data.reduce((s: number, r: any) => s + (r[prop] || 0), 0)
      sums[index] = formatAmount(total)
    } else if (index === 3) {
      const diff = data.reduce((s: number, r: any) => s + (r.amount - r.reimbursed), 0)
      sums[index] = formatAmount(diff)
    } else {
      sums[index] = ''
    }
  })
  return sums
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => {
  chart?.dispose()
})
</script>

<style lang="scss" scoped>
.stats-monthly {
  .negative {
    color: #FF3B30;
    font-weight: 500;
  }
}
</style>
