<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listTrips, formatAmount, getStatusLabel, getStatusColor, formatDate } from '@qianku/shared'
import type { TripVO } from '@qianku/shared'

const router = useRouter()
const trips = ref<TripVO[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)

async function loadData(isRefresh = false) {
  if (isRefresh) {
    pageNum.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const result = await listTrips({ pageNum: pageNum.value, pageSize: 20 })
    if (isRefresh) {
      trips.value = result.list
    } else {
      trips.value.push(...result.list)
    }
    if (trips.value.length >= result.total) finished.value = true
    pageNum.value++
  } catch {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onRefresh() {
  refreshing.value = true
  loadData(true)
}

onMounted(() => loadData(true))
</script>

<template>
  <div class="page">
    <van-nav-bar title="出差管理" left-arrow @click-left="router.back()">
      <template #right>
        <van-icon name="add-o" size="22" @click="router.push('/trip/create')" />
      </template>
    </van-nav-bar>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadData"
      >
        <div
          v-for="trip in trips"
          :key="trip.id"
          class="trip-card card"
        >
          <div class="trip-header">
            <span class="trip-dest">📍 {{ trip.destination }}</span>
            <span class="status-tag" :class="trip.status">
              {{ getStatusLabel(trip.status) }}
            </span>
          </div>
          <div class="trip-dates">
            {{ formatDate(trip.startDate) }} ~ {{ formatDate(trip.endDate) }} · {{ trip.days }}天
          </div>
          <div class="trip-purpose">{{ trip.purpose }}</div>
          <div class="trip-footer">
            <span class="trip-expense">支出 {{ formatAmount(trip.totalExpense) }}</span>
            <span class="trip-count">{{ trip.expenseCount }} 笔费用</span>
          </div>
        </div>

        <div v-if="trips.length === 0 && !loading" class="empty-state">
          <div class="empty-icon">✈️</div>
          <div class="empty-text">暂无出差记录</div>
          <van-button type="primary" size="small" round @click="router.push('/trip/create')" style="margin-top: 16px">
            创建出差
          </van-button>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style lang="scss" scoped>
.trip-card {
  margin: 0 16px 8px;
  padding: 16px;
  cursor: pointer;

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

  .trip-purpose {
    font-size: 14px;
    color: var(--color-text);
    margin-top: 4px;
  }

  .trip-footer {
    display: flex;
    justify-content: space-between;
    margin-top: 12px;
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
