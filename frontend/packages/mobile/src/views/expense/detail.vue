<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { getExpenseDetail, deleteExpense, formatAmount, getCategoryLabel, getStatusLabel, getStatusColor, formatDate } from '@qianku/shared'
import type { ExpenseVO } from '@qianku/shared'

const route = useRoute()
const router = useRouter()
const expense = ref<ExpenseVO | null>(null)
const loading = ref(true)

onMounted(async () => {
  const id = route.params.id as string
  try {
    expense.value = await getExpenseDetail(id)
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

async function handleDelete() {
  if (!expense.value) return
  try {
    await showDialog({
      title: '确认删除',
      message: '删除后不可恢复，确定要删除吗？',
      showCancelButton: true,
      confirmButtonColor: '#FF3B30',
    })
    await deleteExpense(expense.value.id)
    showToast({ message: '删除成功', type: 'success' })
    router.back()
  } catch {
    // cancelled or failed
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="费用详情" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="expense">
      <div class="amount-header">
        <div class="amount-value">{{ formatAmount(expense.amount) }}</div>
        <span class="status-tag" :class="expense.status">
          {{ getStatusLabel(expense.status) }}
        </span>
      </div>

      <div class="detail-card card">
        <div class="detail-row">
          <span class="label">费用类别</span>
          <span class="value">{{ getCategoryLabel(expense.category) }}</span>
        </div>
        <div class="detail-row">
          <span class="label">费用日期</span>
          <span class="value">{{ formatDate(expense.expenseDate) }}</span>
        </div>
        <div class="detail-row">
          <span class="label">创建时间</span>
          <span class="value">{{ formatDate(expense.createdAt, 'YYYY-MM-DD HH:mm') }}</span>
        </div>
        <div v-if="expense.description" class="detail-row">
          <span class="label">备注</span>
          <span class="value">{{ expense.description }}</span>
        </div>
      </div>

      <div v-if="expense.invoiceInfo" class="detail-card card">
        <h4 class="card-title">发票信息</h4>
        <div class="detail-row">
          <span class="label">发票号码</span>
          <span class="value">{{ expense.invoiceInfo.invoiceNo }}</span>
        </div>
        <div class="detail-row">
          <span class="label">发票类型</span>
          <span class="value">{{ expense.invoiceInfo.invoiceType }}</span>
        </div>
        <div class="detail-row">
          <span class="label">销方名称</span>
          <span class="value">{{ expense.invoiceInfo.seller }}</span>
        </div>
      </div>

      <div v-if="expense.status === 'pending'" class="action-bar">
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

.amount-header {
  text-align: center;
  padding: 32px 16px 24px;
  background: var(--color-card);

  .amount-value {
    font-size: 36px;
    font-weight: 700;
    letter-spacing: -1px;
    margin-bottom: 8px;
  }
}

.detail-card {
  .card-title {
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .detail-row {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 0.5px solid var(--color-divider);

    &:last-child { border-bottom: none; }

    .label {
      font-size: 14px;
      color: var(--color-text-secondary);
    }

    .value {
      font-size: 14px;
      font-weight: 500;
      max-width: 60%;
      text-align: right;
      word-break: break-all;
    }
  }
}

.action-bar {
  padding: 24px 16px;
  display: flex;
  gap: 12px;

  .van-button { flex: 1; }
}
</style>
