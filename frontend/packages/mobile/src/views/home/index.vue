<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getStatsOverview, getRecentExpenses, formatAmount, useUserStore } from '@qianku/shared'
import type { StatsOverviewVO, ExpenseVO } from '@qianku/shared'
import { formatDate } from '@qianku/shared'

defineOptions({ name: 'Home' })

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const stats = ref<StatsOverviewVO>({
  totalExpense: 0,
  totalReimbursed: 0,
  totalPending: 0,
  totalTripDays: 0,
  tripCount: 0,
  invoiceCount: 0,
})
const recentExpenses = ref<ExpenseVO[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const [statsData, expenses] = await Promise.all([
      getStatsOverview(),
      getRecentExpenses(5),
    ])
    stats.value = statsData
    recentExpenses.value = expenses
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }

  if (!userStore.userInfo) {
    userStore.fetchProfile()
  }
})

function goUpload() {
  router.push('/expense/upload')
}

function goCreateReimbursement() {
  router.push('/reimbursement/create')
}

function goExpenseDetail(id: number | string) {
  router.push(`/expense/${id}`)
}

function goStats() {
  router.push('/stats')
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div class="greeting">
        <span class="page-title">首页</span>
        <span class="greeting-text">{{ userStore.userInfo?.nickname || '你好' }}</span>
      </div>
    </div>

    <div class="overview-cards">
      <div class="overview-card total" @click="goStats">
        <div class="card-label">总支出</div>
        <div class="card-amount amount-large">{{ formatAmount(stats.totalExpense) }}</div>
      </div>
      <div class="overview-row">
        <div class="overview-card small">
          <div class="card-label">已报销</div>
          <div class="card-amount" style="color: var(--color-success)">
            {{ formatAmount(stats.totalReimbursed) }}
          </div>
        </div>
        <div class="overview-card small">
          <div class="card-label">待报销</div>
          <div class="card-amount" style="color: var(--color-warning)">
            {{ formatAmount(stats.totalPending) }}
          </div>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <div class="section-title">快捷操作</div>
      <div class="action-grid">
        <div class="action-item" @click="goUpload">
          <div class="action-icon" style="background: #E3F2FD">📄</div>
          <span>上传发票</span>
        </div>
        <div class="action-item" @click="router.push('/expense/upload')">
          <div class="action-icon" style="background: #FFF3E0">💰</div>
          <span>添加补贴</span>
        </div>
        <div class="action-item" @click="goCreateReimbursement">
          <div class="action-icon" style="background: #E8F5E9">📋</div>
          <span>生成报销单</span>
        </div>
        <div class="action-item" @click="router.push('/trip/create')">
          <div class="action-icon" style="background: #F3E5F5">✈️</div>
          <span>创建出差</span>
        </div>
      </div>
    </div>

    <div class="recent-section">
      <div class="section-header">
        <span class="section-title" style="padding: 0">最近费用</span>
        <span class="section-more" @click="router.push('/expense')">查看全部</span>
      </div>
      <div v-if="recentExpenses.length > 0" class="expense-list">
        <div
          v-for="expense in recentExpenses"
          :key="expense.id"
          class="expense-item card"
          @click="goExpenseDetail(expense.id)"
        >
          <div class="expense-icon">📋</div>
          <div class="expense-info">
            <div class="expense-name">{{ expense.categoryName || expense.description || '费用' }}</div>
            <div class="expense-date">{{ formatDate(expense.expenseDate) }}</div>
          </div>
          <div class="expense-right">
            <div class="expense-amount amount">{{ formatAmount(expense.amount) }}</div>
            <span class="status-tag">
              {{ expense.reimburseStatus === 0 ? '待报销' : expense.reimburseStatus === 1 ? '报销中' : '已报销' }}
            </span>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <div class="empty-icon">📭</div>
        <div class="empty-text">暂无费用记录</div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.greeting {
  display: flex;
  justify-content: space-between;
  align-items: baseline;

  .greeting-text {
    font-size: 14px;
    color: var(--color-text-secondary);
  }
}

.overview-cards {
  padding: 12px 16px 0;

  .overview-card {
    background: var(--color-card);
    border-radius: var(--radius-lg);
    padding: 20px;
    box-shadow: var(--shadow-sm);

    &.total {
      background: linear-gradient(135deg, #007AFF, #5856D6);
      color: white;
      margin-bottom: 12px;

      .card-label { opacity: 0.85; }
      .card-amount { color: white; }
    }

    .card-label {
      font-size: 13px;
      color: var(--color-text-secondary);
      margin-bottom: 4px;
    }

    .card-amount {
      font-size: 24px;
      font-weight: 700;
    }
  }

  .overview-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;

    .small {
      .card-amount { font-size: 20px; }
    }
  }
}

.quick-actions {
  padding: 16px 0;

  .action-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    padding: 0 16px;
  }

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    .action-icon {
      width: 52px;
      height: 52px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }

    span {
      font-size: 12px;
      color: var(--color-text-secondary);
    }
  }
}

.recent-section {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 16px 8px;

    .section-more {
      font-size: 13px;
      color: var(--color-primary);
      cursor: pointer;
    }
  }

  .expense-list {
    .expense-item {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0 16px 8px;
      padding: 14px;
      cursor: pointer;
      transition: transform 0.15s;

      &:active { transform: scale(0.98); }

      .expense-icon {
        font-size: 28px;
        width: 44px;
        height: 44px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-bg);
        border-radius: var(--radius-sm);
      }

      .expense-info {
        flex: 1;

        .expense-name {
          font-size: 15px;
          font-weight: 500;
        }

        .expense-date {
          font-size: 12px;
          color: var(--color-text-secondary);
          margin-top: 2px;
        }
      }

      .expense-right {
        text-align: right;

        .expense-amount {
          font-size: 16px;
          margin-bottom: 2px;
        }
      }
    }
  }
}
</style>
