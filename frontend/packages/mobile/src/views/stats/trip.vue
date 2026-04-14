<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listTrips, formatAmount, getStatusLabel, formatDate } from '@qianku/shared'
import type { TripVO } from '@qianku/shared'

defineOptions({ name: 'StatsTrip' })

const router = useRouter()
const loading = ref(true)
const trips = ref<TripVO[]>([])

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const showYearPicker = ref(false)
const yearColumns = Array.from({ length: 5 }, (_, i) => ({
  text: `${currentYear - i} 年`,
  value: currentYear - i,
}))

const totalTrips = ref(0)
const totalDays = ref(0)
const totalExpense = ref(0)

onMounted(() => {
  loadData()
})

watch(year, () => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const startDate = `${year.value}-01-01`
    const endDate = `${year.value}-12-31`
    const result = await listTrips({ pageNum: 1, pageSize: 100, startDate, endDate })
    trips.value = result.list

    totalTrips.value = trips.value.length
    totalDays.value = trips.value.reduce((sum, t) => sum + t.days, 0)
    totalExpense.value = trips.value.reduce((sum, t) => sum + t.totalExpense, 0)
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onYearConfirm({ selectedOptions }: any) {
  year.value = selectedOptions[0]?.value || currentYear
  showYearPicker.value = false
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="出差统计" left-arrow @click-left="router.back()" />

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
      <div class="summary-grid">
        <div class="summary-card card">
          <div class="summary-icon">✈️</div>
          <div class="summary-value">{{ totalTrips }}</div>
          <div class="summary-label">出差次数</div>
        </div>
        <div class="summary-card card">
          <div class="summary-icon">📅</div>
          <div class="summary-value">{{ totalDays }}</div>
          <div class="summary-label">总天数</div>
        </div>
        <div class="summary-card card">
          <div class="summary-icon">💰</div>
          <div class="summary-value">{{ formatAmount(totalExpense) }}</div>
          <div class="summary-label">总支出</div>
        </div>
      </div>

      <div class="section-title">出差记录汇总</div>

      <div v-for="trip in trips" :key="trip.id" class="trip-item card">
        <div class="trip-header">
          <span class="trip-dest">📍 {{ trip.destination }}</span>
          <span class="status-tag" :class="trip.status">
            {{ getStatusLabel(trip.status) }}
          </span>
        </div>
        <div class="trip-dates">
          {{ formatDate(trip.startDate) }} ~ {{ formatDate(trip.endDate) }} · {{ trip.days }}天
        </div>
        <div class="trip-footer">
          <span class="trip-expense">支出 {{ formatAmount(trip.totalExpense) }}</span>
          <span class="trip-count">{{ trip.expenseCount }} 笔费用</span>
        </div>
      </div>

      <div v-if="trips.length === 0" class="empty-state">
        <div class="empty-icon">✈️</div>
        <div class="empty-text">{{ year }}年暂无出差记录</div>
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px 16px;

  .summary-card {
    text-align: center;
    padding: 16px 8px;
    margin: 0;

    .summary-icon {
      font-size: 24px;
      margin-bottom: 6px;
    }

    .summary-value {
      font-size: 18px;
      font-weight: 700;
      color: var(--color-text);
    }

    .summary-label {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 4px;
    }
  }
}

.trip-item {
  margin: 0 16px 8px;
  padding: 16px;

  .trip-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .trip-dest {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .trip-dates {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 6px;
  }

  .trip-footer {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 0.5px solid var(--color-divider);
    font-size: 13px;

    .trip-expense {
      font-weight: 500;
      color: var(--color-primary);
    }

    .trip-count {
      color: var(--color-text-secondary);
    }
  }
}
</style>
