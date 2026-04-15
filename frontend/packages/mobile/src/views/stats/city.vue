<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCityRanking, formatAmount } from '@qianku/shared'
import type { CityStats } from '@qianku/shared'

defineOptions({ name: 'StatsCity' })

const router = useRouter()
const loading = ref(true)
const cityData = ref<CityStats[]>([])

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

watch(year, () => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    cityData.value = await getCityRanking({ year: year.value })
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

function getRankColor(index: number): string {
  if (index === 0) return '#FF3B30'
  if (index === 1) return '#FF9500'
  if (index === 2) return '#FFCC00'
  return '#C7C7CC'
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="城市排行" left-arrow @click-left="router.back()" />

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
      <div class="city-list">
        <div v-for="(city, index) in cityData" :key="city.city" class="city-item card">
          <div class="city-rank" :style="{ background: getRankColor(index) }">
            {{ index + 1 }}
          </div>
          <div class="city-info">
            <div class="city-name">{{ city.city }}</div>
            <div class="city-count">{{ city.count }} 次出差</div>
          </div>
          <div class="city-amount">{{ formatAmount(city.amount) }}</div>
        </div>
      </div>

      <div v-if="cityData.length === 0" class="empty-state">
        <div class="empty-icon">🏙️</div>
        <div class="empty-text">{{ year }}年暂无城市数据</div>
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

.city-list {
  padding: 0 16px;

  .city-item {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    padding: 14px 16px;

    .city-rank {
      width: 28px;
      height: 28px;
      border-radius: 50%;
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
}
</style>
