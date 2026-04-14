<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import {
  getTripDetail,
  deleteTrip,
  listExpenses,
  formatAmount,
  getStatusLabel,
  getCategoryLabel,
  formatDate,
} from '@qianku/shared'
import type { TripVO, ExpenseVO } from '@qianku/shared'

defineOptions({ name: 'TripDetail' })

const route = useRoute()
const router = useRouter()
const trip = ref<TripVO | null>(null)
const expenses = ref<ExpenseVO[]>([])
const loading = ref(true)
const expensesLoading = ref(false)

onMounted(async () => {
  const id = route.params.id as string
  try {
    trip.value = await getTripDetail(id)
    expensesLoading.value = true
    const result = await listExpenses({ tripId: id, pageNum: 1, pageSize: 100 })
    expenses.value = result.list
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
    expensesLoading.value = false
  }
})

function goEdit() {
  if (!trip.value) return
  router.push(`/trip/edit/${trip.value.id}`)
}

async function handleDelete() {
  if (!trip.value) return
  try {
    await showDialog({
      title: '确认删除',
      message: '删除后不可恢复，确定要删除该出差记录吗？',
      showCancelButton: true,
      confirmButtonColor: '#FF3B30',
    })
    await deleteTrip(trip.value.id)
    showToast({ message: '删除成功', type: 'success' })
    router.back()
  } catch {
    // cancelled or failed
  }
}

function goExpenseDetail(id: string) {
  router.push(`/expense/${id}`)
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="出差详情" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="trip">
      <div class="trip-header">
        <div class="trip-dest">📍 {{ trip.destination }}</div>
        <span class="status-tag" :class="trip.status">
          {{ getStatusLabel(trip.status) }}
        </span>
      </div>

      <div class="detail-card card">
        <div class="detail-row">
          <span class="label">出差日期</span>
          <span class="value">{{ formatDate(trip.startDate) }} ~ {{ formatDate(trip.endDate) }}</span>
        </div>
        <div class="detail-row">
          <span class="label">出差天数</span>
          <span class="value">{{ trip.days }} 天</span>
        </div>
        <div class="detail-row">
          <span class="label">出差事由</span>
          <span class="value">{{ trip.purpose }}</span>
        </div>
        <div v-if="trip.budget" class="detail-row">
          <span class="label">预算</span>
          <span class="value">{{ formatAmount(trip.budget) }}</span>
        </div>
        <div class="detail-row">
          <span class="label">总支出</span>
          <span class="value highlight">{{ formatAmount(trip.totalExpense) }}</span>
        </div>
        <div v-if="trip.remark" class="detail-row">
          <span class="label">备注</span>
          <span class="value">{{ trip.remark }}</span>
        </div>
      </div>

      <div class="section-title">关联费用 ({{ expenses.length }})</div>

      <van-loading v-if="expensesLoading" class="page-loading" />

      <div
        v-for="expense in expenses"
        :key="expense.id"
        class="expense-item card"
        @click="goExpenseDetail(expense.id)"
      >
        <div class="expense-info">
          <span class="expense-name">{{ getCategoryLabel(expense.category) }}</span>
          <span class="expense-date">{{ formatDate(expense.expenseDate) }}</span>
        </div>
        <span class="expense-amount amount">{{ formatAmount(expense.amount) }}</span>
      </div>

      <div v-if="expenses.length === 0 && !expensesLoading" class="empty-state">
        <div class="empty-icon">📭</div>
        <div class="empty-text">暂无关联费用</div>
      </div>

      <div class="action-bar">
        <van-button plain round type="primary" @click="goEdit">
          编辑
        </van-button>
        <van-button plain round type="danger" @click="handleDelete">
          删除
        </van-button>
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.trip-header {
  text-align: center;
  padding: 28px 16px 20px;
  background: var(--color-card);

  .trip-dest {
    font-size: 22px;
    font-weight: 700;
    margin-bottom: 10px;
  }
}

.detail-card {
  .detail-row {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 0.5px solid var(--color-divider);

    &:last-child { border-bottom: none; }

    .label {
      font-size: 14px;
      color: var(--color-text-secondary);
      flex-shrink: 0;
    }

    .value {
      font-size: 14px;
      font-weight: 500;
      max-width: 60%;
      text-align: right;
      word-break: break-all;

      &.highlight {
        color: var(--color-primary);
        font-weight: 600;
      }
    }
  }
}

.expense-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 16px 6px;
  padding: 12px 16px;
  cursor: pointer;
  transition: transform 0.15s;

  &:active { transform: scale(0.98); }

  .expense-info {
    display: flex;
    flex-direction: column;

    .expense-name { font-size: 15px; font-weight: 500; }
    .expense-date { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
  }

  .expense-amount { font-size: 15px; }
}

.action-bar {
  padding: 24px 16px;
  display: flex;
  gap: 12px;

  .van-button { flex: 1; }
}
</style>
