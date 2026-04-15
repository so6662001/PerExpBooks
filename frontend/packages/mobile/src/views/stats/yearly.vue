<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getYearlyCompare, formatAmount } from '@qianku/shared'
import type { YearlyCompareVO } from '@qianku/shared'
import * as echarts from 'echarts'

defineOptions({ name: 'StatsYearly' })

const router = useRouter()
const loading = ref(true)
const compareData = ref<YearlyCompareVO[]>([])
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
    compareData.value = await getYearlyCompare({ year: year.value })
    await nextTick()
    renderChart()
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || compareData.value.length === 0) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const lines = params.map((p: any) => `${p.seriesName}: ¥${p.value.toFixed(2)}`)
        return `${params[0].name}月<br/>${lines.join('<br/>')}`
      },
    },
    legend: {
      data: [`${year.value}年`, `${year.value - 1}年`],
      bottom: 0,
      textStyle: { fontSize: 12 },
    },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: compareData.value.map(d => `${d.month}月`),
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
        name: `${year.value}年`,
        type: 'line',
        data: compareData.value.map(d => d.currentYear),
        smooth: true,
        lineStyle: { color: '#007AFF', width: 2.5 },
        itemStyle: { color: '#007AFF' },
      },
      {
        name: `${year.value - 1}年`,
        type: 'line',
        data: compareData.value.map(d => d.lastYear),
        smooth: true,
        lineStyle: { color: '#FF9500', width: 2.5, type: 'dashed' },
        itemStyle: { color: '#FF9500' },
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
    <van-nav-bar title="年度对比" left-arrow @click-left="router.back()" />

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
        <h3 class="chart-title">{{ year }}年 vs {{ year - 1 }}年</h3>
        <div ref="chartRef" class="chart-container" />
      </div>

      <div class="data-section">
        <div class="section-title">月度对比数据</div>
        <div class="data-table card">
          <div class="table-header">
            <span class="col">月份</span>
            <span class="col">{{ year }}年</span>
            <span class="col">{{ year - 1 }}年</span>
          </div>
          <div v-for="item in compareData" :key="item.month" class="table-row">
            <span class="col">{{ item.month }}月</span>
            <span class="col current">{{ formatAmount(item.currentYear) }}</span>
            <span class="col last">{{ formatAmount(item.lastYear) }}</span>
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

    .col { flex: 1; text-align: center; }
    .current { color: #007AFF; }
    .last { color: #FF9500; }
  }
}
</style>
