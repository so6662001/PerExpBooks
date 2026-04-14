<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listExpenses, formatAmount, getCategoryIcon, getCategoryLabel, getStatusLabel, getStatusColor, formatDate } from '@qianku/shared'
import type { ExpenseVO, ExpenseStatus } from '@qianku/shared'

defineOptions({ name: 'ExpenseList' })

const router = useRouter()
const activeTab = ref<string>('all')
const expenses = ref<ExpenseVO[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)
const pageSize = 20

const tabs = [
  { name: 'all', title: '全部' },
  { name: 'pending', title: '待报销' },
  { name: 'reimbursed', title: '已报销' },
]

async function loadData(isRefresh = false) {
  if (isRefresh) {
    pageNum.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize }
    if (activeTab.value !== 'all') {
      params.status = activeTab.value as ExpenseStatus
    }
    const result = await listExpenses(params)
    if (isRefresh) {
      expenses.value = result.list
    } else {
      expenses.value.push(...result.list)
    }
    if (expenses.value.length >= result.total) {
      finished.value = true
    }
    pageNum.value++
  } catch (e) {
    console.error(e)
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

watch(activeTab, () => {
  loadData(true)
})

onMounted(() => {
  loadData(true)
})

function goUpload() {
  router.push('/expense/upload')
}

function goDetail(id: string) {
  router.push(`/expense/${id}`)
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <span class="page-title">费用</span>
      <van-icon name="add-o" size="24" color="#007AFF" @click="goUpload" />
    </div>

    <van-tabs v-model:active="activeTab" sticky offset-top="56" shrink>
      <van-tab v-for="tab in tabs" :key="tab.name" :name="tab.name" :title="tab.title" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadData"
      >
        <div
          v-for="expense in expenses"
          :key="expense.id"
          class="expense-card card"
          @click="goDetail(expense.id)"
        >
          <div class="expense-left">
            <div class="expense-icon">{{ getCategoryIcon(expense.category) }}</div>
          </div>
          <div class="expense-center">
            <div class="expense-category">{{ getCategoryLabel(expense.category) }}</div>
            <div class="expense-desc">{{ expense.description || '无备注' }}</div>
            <div class="expense-date">{{ formatDate(expense.expenseDate) }}</div>
          </div>
          <div class="expense-right">
            <div class="expense-amount amount">{{ formatAmount(expense.amount) }}</div>
            <span class="status-tag" :class="expense.status">
              {{ getStatusLabel(expense.status) }}
            </span>
          </div>
        </div>

        <div v-if="expenses.length === 0 && !loading" class="empty-state">
          <div class="empty-icon">📭</div>
          <div class="empty-text">暂无费用记录</div>
          <van-button type="primary" size="small" round @click="goUpload" style="margin-top: 16px">
            上传发票
          </van-button>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style lang="scss" scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.expense-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 16px 8px;
  padding: 14px;
  cursor: pointer;
  transition: transform 0.15s;

  &:active { transform: scale(0.98); }

  .expense-left {
    .expense-icon {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      background: var(--color-bg);
      border-radius: var(--radius-sm);
    }
  }

  .expense-center {
    flex: 1;

    .expense-category {
      font-size: 15px;
      font-weight: 500;
    }

    .expense-desc {
      font-size: 13px;
      color: var(--color-text-secondary);
      margin-top: 2px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 150px;
    }

    .expense-date {
      font-size: 12px;
      color: var(--color-text-tertiary);
      margin-top: 2px;
    }
  }

  .expense-right {
    text-align: right;

    .expense-amount {
      font-size: 16px;
      margin-bottom: 4px;
    }
  }
}
</style>
