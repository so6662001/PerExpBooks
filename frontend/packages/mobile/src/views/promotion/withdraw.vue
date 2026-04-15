<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  getWithdrawalBalance,
  applyWithdrawal,
  getWithdrawalRecords,
  formatAmount,
  formatDate,
} from '@qianku/shared'
import type { BalanceVO, WithdrawalVO } from '@qianku/shared'

defineOptions({ name: 'PromotionWithdraw' })

const router = useRouter()
const balance = ref<BalanceVO | null>(null)
const records = ref<WithdrawalVO[]>([])
const loading = ref(true)
const submitting = ref(false)

const amount = ref<number | undefined>(undefined)
const withdrawType = ref(1)

const methods = [
  { value: 1, label: '微信', icon: '💬' },
  { value: 2, label: '支付宝', icon: '🔵' },
]

const canSubmit = computed(() =>
  amount.value && amount.value >= 50 && balance.value && amount.value <= balance.value.availableBalance,
)

onMounted(async () => {
  try {
    const [balanceData, recordData] = await Promise.all([
      getWithdrawalBalance(),
      getWithdrawalRecords(),
    ])
    balance.value = balanceData
    records.value = recordData
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

function getStatusLabel(status: number) {
  const map: Record<number, string> = {
    0: '处理中',
    1: '已完成',
    2: '已拒绝',
  }
  return map[status] || '未知'
}

function getStatusColor(status: number) {
  const map: Record<number, string> = {
    0: '#FF9500',
    1: '#34C759',
    2: '#FF3B30',
  }
  return map[status] || '#999'
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await applyWithdrawal({
      amount: amount.value!,
      withdrawType: withdrawType.value,
    })
    showToast({ message: '申请已提交', type: 'success' })
    amount.value = undefined
    const [balanceData, recordData] = await Promise.all([
      getWithdrawalBalance(),
      getWithdrawalRecords(),
    ])
    balance.value = balanceData
    records.value = recordData
  } catch (e: any) {
    showToast(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="提现" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="balance-card card">
        <div class="balance-label">可提现余额</div>
        <div class="balance-value">{{ formatAmount(balance?.availableBalance || 0) }}</div>
      </div>

      <div class="form-section card">
        <van-field
          v-model.number="amount"
          label="提现金额"
          type="number"
          placeholder="最低50元"
          required
        >
          <template #left-icon>
            <span style="font-size: 16px; color: #007AFF">¥</span>
          </template>
        </van-field>

        <div class="method-section">
          <div class="method-label">提现方式</div>
          <div class="method-list">
            <div
              v-for="m in methods"
              :key="m.value"
              class="method-item"
              :class="{ active: withdrawType === m.value }"
              @click="withdrawType = m.value"
            >
              <span class="method-icon">{{ m.icon }}</span>
              <span class="method-name">{{ m.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="submit-bar">
        <van-button
          type="primary"
          block
          round
          size="large"
          :disabled="!canSubmit"
          :loading="submitting"
          @click="handleSubmit"
        >
          申请提现
        </van-button>
      </div>

      <div class="section-title">提现记录</div>

      <div v-for="record in records" :key="record.id" class="record-item card">
        <div class="record-info">
          <div class="record-amount">{{ formatAmount(record.amount) }}</div>
          <div class="record-date">{{ formatDate(record.createdAt, 'YYYY-MM-DD HH:mm') }}</div>
        </div>
        <span class="record-status" :style="{ color: getStatusColor(record.status) }">
          {{ getStatusLabel(record.status) }}
        </span>
      </div>

      <div v-if="records.length === 0" class="empty-state">
        <div class="empty-icon">💰</div>
        <div class="empty-text">暂无提现记录</div>
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

.balance-card {
  text-align: center;
  padding: 28px 16px;
  background: linear-gradient(135deg, #007AFF, #5856D6);
  color: white;
  margin: 0;
  border-radius: 0;

  .balance-label {
    font-size: 14px;
    opacity: 0.85;
  }

  .balance-value {
    font-size: 36px;
    font-weight: 700;
    margin-top: 8px;
    letter-spacing: -1px;
  }
}

.form-section {
  .method-section {
    padding: 12px 16px;

    .method-label {
      font-size: 14px;
      color: var(--color-text-secondary);
      margin-bottom: 10px;
    }

    .method-list {
      display: flex;
      gap: 12px;
    }

    .method-item {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 12px;
      border-radius: 10px;
      border: 2px solid var(--color-border);
      cursor: pointer;
      transition: all 0.2s;

      &.active {
        border-color: var(--color-primary);
        background: #007AFF08;
      }

      .method-icon { font-size: 20px; }
      .method-name { font-size: 14px; font-weight: 500; }
    }
  }
}

.submit-bar {
  padding: 20px 16px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 16px 8px;
  padding: 14px 16px;

  .record-info {
    .record-amount {
      font-size: 16px;
      font-weight: 600;
    }

    .record-date {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 2px;
    }
  }

  .record-status {
    font-size: 13px;
    font-weight: 500;
  }
}
</style>
