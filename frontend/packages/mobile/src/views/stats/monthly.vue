<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMonthlyTrend, formatAmount } from '@qianku/shared'
import type { MonthlyTrendVO } from '@qianku/shared'
import * as echarts from 'echarts'

defineOptions({ name: 'StatsMonthly' })

const router = useRouter()
const loading = ref(true)
const trendData = ref<MonthlyTrendVO[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const showYearPicker = ref(false)
const yearColumns = Array.from({ length: 5 }, (_, i) => ({
  text: `${currentYear - i} 年`,
  value: currentYear - i,
}))

onMounted(() => {
  loadData()
})

onUnmounted(() => {
  chart?.dispose()
})

watch(year, () => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    trendData.value = await getMonthlyTrend({ year: year.value })
    await nextTick()
    renderChart()
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || trendData.value.length === 0) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const lines = params.map((p: any) => `${p.seriesName}: ¥${p.value.toFixed(2)}`)
        return `${params[0].name}<br/>${lines.join('<br/>')}`
      },
    },
    legend: {
      data: ['支出', '报销'],
      bottom: 0,
      textStyle: { fontSize: 12 },
    },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: trendData.value.map(d => d.month),
      axisLabel: { fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (v: number) => v >= 1000 ? `${(v / 1000).toFixed(0)}k` : String(v),
        fontSize: 11,
      },
    },
    series: [
      {
        name: '支出',
        type: 'line',
        data: trendData.value.map(d => d.expense),
        smooth: true,
        lineStyle: { color: '#007AFF', width: 2.5 },
        itemStyle: { color: '#007AFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,122,255,0.15)' },
            { offset: 1, color: 'rgba(0,122,255,0.01)' },
          ]),
        },
      },
      {
        name: '报销',
        type: 'line',
        data: trendData.value.map(d => d.reimbursed),
        smooth: true,
        lineStyle: { color: '#34C759', width: 2.5 },
        itemStyle: { color: '#34C759' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(52,199,89,0.15)' },
            { offset: 1, color: 'rgba(52,199,89,0.01)' },
          ]),
        },
      },
    ],
  })
}

function onYearConfirm({ selectedOptions }: any) {
  year.value = selectedOptions[0]?.value || currentYear
  showYearPicker.value = false
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="月度趋势" left-arrow @click-left="router.back()" />

    <div class="year-selector card">
      <van-field
        :model-value="`${year} 年`"
        is-link
        readonly
        label="年份"
        @click="showYearPicker = true"
      />
    </div>

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="chart-section card">
        <h3 class="chart-title">月度费用趋势</h3>
        <div ref="chartRef" class="chart-container" />
      </div>

      <div class="data-section">
        <div class="section-title">月度数据</div>
        <div class="data-table card">
          <div class="table-header">
            <span class="col-month">月份</span>
            <span class="col-expense">支出</span>
            <span class="col-reimbursed">报销</span>
          </div>
          <div v-for="item in trendData" :key="item.month" class="table-row">
            <span class="col-month">{{ item.month }}</span>
            <span class="col-expense">{{ formatAmount(item.expense) }}</span>
            <span class="col-reimbursed">{{ formatAmount(item.reimbursed) }}</span>
          </div>
          <div v-if="trendData.length === 0" class="table-empty">
            暂无数据
          </div>
        </div>
      </div>
    </template>

    <van-popup v-model:show="showYearPicker" position="bottom" round>
      <van-picker
        :columns="yearColumns"
        @confirm="onYearConfirm"
        @cancel="showYearPicker = false"
      />
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.year-selector {
  margin-top: 0;
}

.chart-section {
  .chart-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .chart-container {
    width: 100%;
    height: 280px;
  }
}

.data-section {
  .data-table {
    .table-header, .table-row {
      display: flex;
      padding: 10px 0;
      border-bottom: 0.5px solid var(--color-divider);
    }

    .table-header {
      font-size: 12px;
      font-weight: 600;
      color: var(--color-text-secondary);
    }

    .table-row {
      font-size: 14px;

      &:last-child { border-bottom: none; }
    }

    .col-month { flex: 1; }
    .col-expense { flex: 1; text-align: right; color: #007AFF; }
    .col-reimbursed { flex: 1; text-align: right; color: #34C759; }

    .table-empty {
      text-align: center;
      padding: 24px;
      color: var(--color-text-tertiary);
      font-size: 14px;
    }
  }
}
</style>
