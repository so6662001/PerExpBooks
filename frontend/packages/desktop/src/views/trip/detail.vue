<template>
  <div class="page-container trip-detail">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-button @click="$router.back()" circle>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>出差详情</h2>
      </div>
      <el-tag v-if="trip" :type="tripStatusType(trip.status)" size="large">
        {{ getStatusLabel(trip.status) }}
      </el-tag>
    </div>

    <el-skeleton :loading="loading" :rows="8" animated>
      <template #default>
        <el-row :gutter="20" v-if="trip">
          <el-col :span="16">
            <el-card>
              <template #header>
                <span>基本信息</span>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="目的地">{{ trip.destination }}</el-descriptions-item>
                <el-descriptions-item label="天数">{{ trip.days }} 天</el-descriptions-item>
                <el-descriptions-item label="开始日期">{{ trip.startDate }}</el-descriptions-item>
                <el-descriptions-item label="结束日期">{{ trip.endDate }}</el-descriptions-item>
                <el-descriptions-item label="事由" :span="2">{{ trip.purpose }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ trip.remark || '-' }}</el-descriptions-item>
              </el-descriptions>
            </el-card>

            <el-card style="margin-top: 16px">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <span>关联费用 ({{ expenses.length }} 笔)</span>
                  <el-button type="primary" size="small" @click="$router.push(`/expense/list?tripId=${trip.id}`)">
                    查看全部
                  </el-button>
                </div>
              </template>
              <el-table :data="expenses" stripe>
                <el-table-column prop="expenseDate" label="日期" width="120" />
                <el-table-column prop="category" label="分类" width="100">
                  <template #default="{ row }">
                    {{ getCategoryLabel(row.category) }}
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
                <el-table-column prop="amount" label="金额" width="120" align="right">
                  <template #default="{ row }">
                    {{ formatAmount(row.amount) }}
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'reimbursed' ? 'success' : 'warning'" size="small">
                      {{ getStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card>
              <template #header>
                <span>费用汇总</span>
              </template>
              <div class="summary-items">
                <div class="summary-item">
                  <span class="label">总费用</span>
                  <span class="value">{{ formatAmount(trip.totalExpense) }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">预算</span>
                  <span class="value">{{ trip.budget ? formatAmount(trip.budget) : '-' }}</span>
                </div>
                <div class="summary-item" v-if="trip.budget">
                  <span class="label">预算使用</span>
                  <el-progress
                    :percentage="Math.min(100, Math.round((trip.totalExpense / trip.budget) * 100))"
                    :color="trip.totalExpense > trip.budget ? '#FF3B30' : '#34C759'"
                  />
                </div>
                <div class="summary-item">
                  <span class="label">费用笔数</span>
                  <span class="value">{{ trip.expenseCount }}</span>
                </div>
              </div>
            </el-card>

            <el-card style="margin-top: 16px">
              <template #header>
                <span>费用分布</span>
              </template>
              <div ref="chartRef" style="height: 250px" />
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import {
  getTripDetail,
  listExpenses,
  formatAmount,
  getCategoryLabel,
  getStatusLabel,
  type TripVO,
  type ExpenseVO,
} from '@qianku/shared'

const route = useRoute()
const loading = ref(true)
const trip = ref<TripVO | null>(null)
const expenses = ref<ExpenseVO[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function tripStatusType(status: string) {
  const map: Record<string, string> = {
    planned: '',
    ongoing: 'warning',
    completed: 'success',
    cancelled: 'info',
  }
  return (map[status] || 'info') as any
}

async function loadData() {
  const id = route.params.id as string
  loading.value = true
  try {
    const [tripRes, expenseRes] = await Promise.all([
      getTripDetail(id),
      listExpenses({ tripId: id, pageNum: 1, pageSize: 100 }),
    ])
    trip.value = tripRes
    expenses.value = expenseRes.list

    await nextTick()
    renderChart()
  } catch {
    // silent
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || expenses.value.length === 0) return
  if (!chart) chart = echarts.init(chartRef.value)

  const categoryMap: Record<string, number> = {}
  expenses.value.forEach(e => {
    const label = getCategoryLabel(e.category)
    categoryMap[label] = (categoryMap[label] || 0) + e.amount
  })

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      data: Object.entries(categoryMap).map(([name, value]) => ({ name, value })),
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
    }],
  })
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
.trip-detail {
  .summary-items {
    .summary-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .label {
        font-size: 14px;
        color: #86868b;
      }

      .value {
        font-size: 16px;
        font-weight: 600;
        color: #1d1d1f;
      }
    }
  }
}
</style>
