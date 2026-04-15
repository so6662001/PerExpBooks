<template>
  <div class="page-container stats-yearly">
    <div class="page-header">
      <h2>年度对比</h2>
      <el-select v-model="year" style="width: 120px" @change="loadData">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
    </div>

    <div class="chart-container">
      <h3 class="chart-title">{{ year }}年 vs {{ year - 1 }}年 月度支出对比</h3>
      <div ref="lineChartRef" style="height: 400px" />
    </div>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>月度对比数据</span>
      </template>
      <el-table :data="compareData" stripe>
        <el-table-column label="月份" width="100">
          <template #default="{ row }">{{ row.month }}月</template>
        </el-table-column>
        <el-table-column :label="`${year}年`" align="right">
          <template #default="{ row }">
            <span style="color: #007AFF">{{ formatAmount(row.currentYear) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="`${year - 1}年`" align="right">
          <template #default="{ row }">
            <span style="color: #FF9500">{{ formatAmount(row.lastYear) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="同比变化" align="right" width="150">
          <template #default="{ row }">
            <span v-if="row.lastYear > 0" :style="{ color: row.currentYear >= row.lastYear ? '#FF3B30' : '#34C759' }">
              {{ row.currentYear >= row.lastYear ? '+' : '' }}{{ ((row.currentYear - row.lastYear) / row.lastYear * 100).toFixed(1) }}%
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getYearlyCompare, formatAmount, type YearlyCompareVO } from '@qianku/shared'

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const yearOptions = computed(() => Array.from({ length: 5 }, (_, i) => currentYear - i))

const compareData = ref<YearlyCompareVO[]>([])
const lineChartRef = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null

async function loadData() {
  try {
    compareData.value = await getYearlyCompare({ year: year.value })
  } catch {
    compareData.value = []
  }
  await nextTick()
  renderChart()
}

function renderChart() {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)

  lineChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const lines = params.map(
          (p: any) => `${p.marker} ${p.seriesName}: ¥${p.value.toFixed(2)}`
        )
        return `${params[0].name}月<br/>${lines.join('<br/>')}`
      },
    },
    legend: { data: [`${year.value}年`, `${year.value - 1}年`], bottom: 0 },
    grid: { left: 60, right: 20, top: 40, bottom: 50 },
    xAxis: {
      type: 'category',
      data: compareData.value.map(d => `${d.month}月`),
      axisTick: { show: false },
    },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [
      {
        name: `${year.value}年`,
        type: 'line',
        smooth: true,
        data: compareData.value.map(d => d.currentYear),
        itemStyle: { color: '#007AFF' },
        lineStyle: { width: 2.5 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,122,255,0.15)' },
            { offset: 1, color: 'rgba(0,122,255,0.01)' },
          ]),
        },
      },
      {
        name: `${year.value - 1}年`,
        type: 'line',
        smooth: true,
        data: compareData.value.map(d => d.lastYear),
        itemStyle: { color: '#FF9500' },
        lineStyle: { width: 2.5, type: 'dashed' },
      },
    ],
  })
}

function handleResize() {
  lineChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
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
