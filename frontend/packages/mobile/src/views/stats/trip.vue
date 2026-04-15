<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getTripSummary, formatAmount } from '@qianku/shared'
import type { TripSummaryVO } from '@qianku/shared'

defineOptions({ name: 'StatsTrip' })

const router = useRouter()
const loading = ref(true)

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const showYearPicker = ref(false)
const yearColumns = Array.from({ length: 5 }, (_, i) => ({
  text: `${currentYear - i} 年`,
  value: currentYear - i,
}))

const summary = ref<TripSummaryVO>({
  tripCount: 0,
  totalDays: 0,
  totalSubsidy: 0,
  cityDistribution: [],
})

onMounted(() => {
  loadData()
})

watch(year, () => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    summary.value = await getTripSummary({ year: year.value })
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
          <div class="summary-value">{{ summary.tripCount }}</div>
          <div class="summary-label">出差次数</div>
        </div>
        <div class="summary-card card">
          <div class="summary-icon">📅</div>
          <div class="summary-value">{{ summary.totalDays }}</div>
          <div class="summary-label">总天数</div>
        </div>
        <div class="summary-card card">
          <div class="summary-icon">💰</div>
          <div class="summary-value">{{ formatAmount(summary.totalSubsidy) }}</div>
          <div class="summary-label">总补贴</div>
        </div>
      </div>

      <div class="section-title">城市分布</div>

      <div v-for="(city, index) in summary.cityDistribution" :key="city.city" class="city-item card">
        <div class="city-rank">{{ index + 1 }}</div>
        <div class="city-info">
          <div class="city-name">📍 {{ city.city }}</div>
          <div class="city-count">{{ city.count }} 次出差</div>
        </div>
        <div class="city-amount">{{ formatAmount(city.amount) }}</div>
      </div>

      <div v-if="summary.cityDistribution.length === 0" class="empty-state">
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

.city-item {
  display: flex;
  align-items: center;
  margin: 0 16px 8px;
  padding: 14px 16px;

  .city-rank {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--color-primary);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    margin-right: 12px;
    flex-shrink: 0;
  }

  .city-info {
    flex: 1;

    .city-name {
      font-size: 15px;
      font-weight: 500;
    }

    .city-count {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 2px;
    }
  }

  .city-amount {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-primary);
  }
}
</style>
