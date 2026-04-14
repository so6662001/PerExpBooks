<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import {
  getReimbursementDetail,
  exportReimbursement,
  markReceived,
  formatAmount,
  getCategoryLabel,
  getStatusLabel,
  formatDate,
} from '@qianku/shared'
import type { ReimbursementVO, ExportOptions } from '@qianku/shared'

const route = useRoute()
const router = useRouter()
const detail = ref<ReimbursementVO | null>(null)
const loading = ref(true)
const exporting = ref(false)
const showEmailDialog = ref(false)
const emailInput = ref('')

onMounted(async () => {
  const id = route.params.id as string
  try {
    detail.value = await getReimbursementDetail(id)
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

async function handleExport(type: ExportOptions['type']) {
  if (!detail.value) return
  if (type === 'email') {
    showEmailDialog.value = true
    return
  }
  exporting.value = true
  try {
    const result = await exportReimbursement(detail.value.id, { type })
    if (result.url) {
      window.open(result.url, '_blank')
    }
    showToast({ message: '导出成功', type: 'success' })
  } catch (e: any) {
    showToast(e.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

async function handleSendEmail() {
  if (!detail.value || !emailInput.value) return
  exporting.value = true
  try {
    await exportReimbursement(detail.value.id, { type: 'email', email: emailInput.value })
    showToast({ message: '已发送至邮箱', type: 'success' })
    showEmailDialog.value = false
  } catch (e: any) {
    showToast(e.message || '发送失败')
  } finally {
    exporting.value = false
  }
}

async function handleMarkReceived() {
  if (!detail.value) return
  try {
    await showDialog({
      title: '确认收款',
      message: '确认已收到报销款项？',
      showCancelButton: true,
    })
    await markReceived(detail.value.id)
    detail.value.status = 'received'
    showToast({ message: '已标记收款', type: 'success' })
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="报销单详情" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="detail">
      <div class="detail-header">
        <div class="detail-title">{{ detail.title }}</div>
        <div class="detail-amount amount-large">{{ formatAmount(detail.totalAmount) }}</div>
        <span class="status-tag" :class="detail.status">
          {{ getStatusLabel(detail.status) }}
        </span>
      </div>

      <div class="info-card card">
        <div class="info-row">
          <span class="label">费用项数</span>
          <span class="value">{{ detail.itemCount }} 项</span>
        </div>
        <div class="info-row">
          <span class="label">创建时间</span>
          <span class="value">{{ formatDate(detail.createdAt, 'YYYY-MM-DD HH:mm') }}</span>
        </div>
        <div v-if="detail.remark" class="info-row">
          <span class="label">备注</span>
          <span class="value">{{ detail.remark }}</span>
        </div>
      </div>

      <div class="section-title">费用明细</div>
      <div
        v-for="expense in detail.expenses"
        :key="expense.id"
        class="expense-item card"
      >
        <div class="expense-info">
          <span class="expense-name">{{ getCategoryLabel(expense.category) }}</span>
          <span class="expense-date">{{ formatDate(expense.expenseDate) }}</span>
        </div>
        <span class="expense-amount amount">{{ formatAmount(expense.amount) }}</span>
      </div>

      <div class="section-title">导出操作</div>
      <div class="export-grid">
        <div class="export-btn card" @click="handleExport('merged_pdf')">
          <div class="export-icon">📄</div>
          <span>合并PDF</span>
        </div>
        <div class="export-btn card" @click="handleExport('zip')">
          <div class="export-icon">📦</div>
          <span>ZIP打包</span>
        </div>
        <div class="export-btn card" @click="handleExport('report_only')">
          <div class="export-icon">📋</div>
          <span>仅报销单</span>
        </div>
        <div class="export-btn card" @click="handleExport('email')">
          <div class="export-icon">📧</div>
          <span>发送邮箱</span>
        </div>
      </div>

      <div v-if="detail.status === 'exported'" class="action-bar">
        <van-button type="primary" block round @click="handleMarkReceived">
          确认收款
        </van-button>
      </div>
    </template>

    <van-dialog
      v-model:show="showEmailDialog"
      title="发送到邮箱"
      show-cancel-button
      :before-close="() => true"
      @confirm="handleSendEmail"
    >
      <div style="padding: 16px">
        <van-field
          v-model="emailInput"
          label="邮箱"
          placeholder="请输入接收邮箱"
          type="text"
        />
      </div>
    </van-dialog>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.detail-header {
  text-align: center;
  padding: 24px 16px;
  background: var(--color-card);

  .detail-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .detail-amount {
    margin-bottom: 8px;
  }
}

.info-card {
  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 0.5px solid var(--color-divider);

    &:last-child { border-bottom: none; }

    .label { font-size: 14px; color: var(--color-text-secondary); }
    .value { font-size: 14px; font-weight: 500; }
  }
}

.expense-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 16px 6px;
  padding: 12px 16px;

  .expense-info {
    display: flex;
    flex-direction: column;

    .expense-name { font-size: 15px; font-weight: 500; }
    .expense-date { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
  }

  .expense-amount { font-size: 15px; }
}

.export-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 0 16px;

  .export-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 16px 8px;
    margin: 0;
    cursor: pointer;
    transition: transform 0.15s;

    &:active { transform: scale(0.95); }

    .export-icon { font-size: 28px; }
    span { font-size: 12px; color: var(--color-text-secondary); }
  }
}

.action-bar {
  padding: 24px 16px;
}
</style>
