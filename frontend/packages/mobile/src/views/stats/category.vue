<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCategoryRatio, formatAmount } from '@qianku/shared'
import type { CategoryRatioVO } from '@qianku/shared'
import * as echarts from 'echarts'

defineOptions({ name: 'StatsCategory' })

const router = useRouter()
const loading = ref(true)
const categoryData = ref<CategoryRatioVO[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const showStartPicker = ref(false)
const showEndPicker = ref(false)
const startDate = ref('')
const endDate = ref('')

const colors = ['#007AFF', '#34C759', '#FF9500', '#FF3B30', '#5856D6', '#AF52DE', '#FF2D55']

onMounted(() => {
  loadData()
})

onUnmounted(() => {
  chart?.dispose()
})

async function loadData() {
  loading.value = true
  try {
    const params: any = {}
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    categoryData.value = await getCategoryRatio(params)
    await nextTick()
    renderChart()
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || categoryData.value.length === 0) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)',
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, fontSize: 12 },
      data: categoryData.value.map((d, i) => ({
        name: d.categoryName,
        value: d.amount,
        itemStyle: { color: colors[i % colors.length] },
      })),
    }],
  })
}

function onStartDateConfirm({ selectedValues }: any) {
  startDate.value = selectedValues.join('-')
  showStartPicker.value = false
  loadData()
}

function onEndDateConfirm({ selectedValues }: any) {
  endDate.value = selectedValues.join('-')
  showEndPicker.value = false
  loadData()
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="分类分析" left-arrow @click-left="router.back()" />

    <div class="filter-section card">
      <van-field
        v-model="startDate"
        is-link
        readonly
        label="开始日期"
        placeholder="选择开始日期"
        @click="showStartPicker = true"
      />
      <van-field
        v-model="endDate"
        is-link
        readonly
        label="结束日期"
        placeholder="选择结束日期"
        @click="showEndPicker = true"
      />
    </div>

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="chart-section card">
        <h3 class="chart-title">分类占比</h3>
        <div ref="chartRef" class="chart-container" />
      </div>

      <div class="section-title">分类明细</div>
      <div class="category-list">
        <div v-for="(item, index) in categoryData" :key="item.categoryId" class="category-item card">
          <div class="category-dot" :style="{ background: colors[index % colors.length] }" />
          <div class="category-info">
            <div class="category-name">{{ item.categoryName }}</div>
          </div>
          <div class="category-right">
            <div class="category-amount">{{ formatAmount(item.amount) }}</div>
            <div class="category-percent">{{ item.ratio.toFixed(1) }}%</div>
          </div>
        </div>
      </div>

      <div v-if="categoryData.length === 0" class="empty-state">
        <div class="empty-icon">📊</div>
        <div class="empty-text">暂无分类数据</div>
      </div>
    </template>

    <van-popup v-model:show="showStartPicker" position="bottom" round>
      <van-date-picker @confirm="onStartDateConfirm" @cancel="showStartPicker = false" />
    </van-popup>
    <van-popup v-model:show="showEndPicker" position="bottom" round>
      <van-date-picker @confirm="onEndDateConfirm" @cancel="showEndPicker = false" />
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.filter-section {
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

.category-list {
  .category-item {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 0 16px 8px;
    padding: 14px 16px;

    .category-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .category-info {
      flex: 1;

      .category-name {
        font-size: 15px;
        font-weight: 500;
      }
    }

    .category-right {
      text-align: right;

      .category-amount {
        font-size: 15px;
        font-weight: 600;
      }

      .category-percent {
        font-size: 12px;
        color: var(--color-text-secondary);
        margin-top: 2px;
      }
    }
  }
}
</style>
