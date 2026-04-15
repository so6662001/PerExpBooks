<template>
  <div class="page-container stats-progress">
    <div class="page-header">
      <h2>报销进度</h2>
      <el-select v-model="year" style="width: 120px" @change="loadData">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
    </div>

    <div class="chart-container">
      <h3 class="chart-title">月度报销进度</h3>
      <div ref="barChartRef" style="height: 400px" />
    </div>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>报销进度明细</span>
      </template>
      <el-table :data="progressData" stripe>
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column prop="submitted" label="已提交" align="right">
          <template #default="{ row }">
            <span style="color: #FF9500">{{ formatAmount(row.submitted) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="received" label="已收款" align="right">
          <template #default="{ row }">
            <span style="color: #34C759">{{ formatAmount(row.received) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pending" label="待处理" align="right">
          <template #default="{ row }">
            <span style="color: #C7C7CC">{{ formatAmount(row.pending) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getReimburseProgress,
  formatAmount,
  type ReimburseProgressVO,
} from '@qianku/shared'

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const yearOptions = computed(() => Array.from({ length: 5 }, (_, i) => currentYear - i))

const progressData = ref<ReimburseProgressVO[]>([])
const barChartRef = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null

async function loadData() {
  try {
    progressData.value = await getReimburseProgress({ year: year.value })
  } catch {
    progressData.value = []
  }
  await nextTick()
  renderBarChart()
}

function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const lines = params.map(
          (p: any) => `${p.marker} ${p.seriesName}: ¥${p.value.toFixed(2)}`
        )
        return `${params[0].name}<br/>${lines.join('<br/>')}`
      },
    },
    legend: { data: ['已提交', '已收款', '待处理'], bottom: 0 },
    grid: { left: 60, right: 20, top: 20, bottom: 50 },
    xAxis: {
      type: 'category',
      data: progressData.value.map(d => d.month),
      axisTick: { show: false },
    },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [
      {
        name: '已收款',
        type: 'bar',
        stack: 'total',
        data: progressData.value.map(d => d.received),
        itemStyle: { color: '#34C759' },
      },
      {
        name: '待处理',
        type: 'bar',
        stack: 'total',
        data: progressData.value.map(d => d.pending),
        itemStyle: { color: '#C7C7CC' },
      },
      {
        name: '已提交',
        type: 'bar',
        stack: 'total',
        data: progressData.value.map(d => d.submitted),
        itemStyle: { color: '#FF9500' },
      },
    ],
  })
}

function handleResize() {
  barChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
})
</script>

<style lang="scss" scoped>
.stats-progress {
  .chart-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 12px;
    color: #1d1d1f;
  }
}
</style>
