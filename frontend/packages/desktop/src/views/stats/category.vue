<template>
  <div class="page-container stats-category">
    <div class="page-header">
      <h2>分类分析</h2>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="loadData"
      />
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="chart-container">
          <h3 class="chart-title">分类占比</h3>
          <div ref="pieChartRef" style="height: 380px" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-container">
          <h3 class="chart-title">分类对比</h3>
          <div ref="barChartRef" style="height: 380px" />
        </div>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>分类排行</span>
      </template>
      <el-table :data="categoryData" stripe>
        <el-table-column type="index" width="60" label="排名" />
        <el-table-column prop="categoryName" label="分类名" width="150" />
        <el-table-column prop="amount" label="金额" width="150" align="right" sortable>
          <template #default="{ row }">
            <span class="amount">{{ formatAmount(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ratio" label="占比" width="200">
          <template #default="{ row }">
            <el-progress :percentage="row.ratio" :color="'#007AFF'" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getCategoryRatio,
  formatAmount,
  type CategoryRatioVO,
} from '@qianku/shared'

const dateRange = ref<[string, string] | null>(null)
const categoryData = ref<CategoryRatioVO[]>([])
const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

const colors = ['#007AFF', '#34C759', '#FF9500', '#FF3B30', '#5856D6', '#AF52DE', '#FF2D55']

async function loadData() {
  const params: any = {}
  if (dateRange.value) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  try {
    categoryData.value = await getCategoryRatio(params)
  } catch {
    categoryData.value = []
  }
  await nextTick()
  renderPieChart()
  renderBarChart()
}

function renderPieChart() {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      label: { formatter: '{b}\n{d}%' },
      data: categoryData.value.map((c, i) => ({
        name: c.categoryName,
        value: c.amount,
        itemStyle: { color: colors[i % colors.length] },
      })),
    }],
  })
}

function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  const labels = categoryData.value.map(c => c.categoryName)
  barChart.setOption({
    tooltip: { trigger: 'axis', formatter: (params: any) => `${params[0].name}: ¥${params[0].value.toFixed(2)}` },
    grid: { left: 80, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    yAxis: { type: 'category', data: labels.reverse(), axisTick: { show: false } },
    series: [{
      type: 'bar',
      data: [...categoryData.value].reverse().map((c, i) => ({
        value: c.amount,
        itemStyle: { color: colors[(categoryData.value.length - 1 - i) % colors.length], borderRadius: [0, 6, 6, 0] },
      })),
      barWidth: 24,
    }],
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
.stats-category {
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
