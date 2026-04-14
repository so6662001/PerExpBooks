<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  getPromotionInfo,
  createWithdrawal,
  getWithdrawalRecords,
  formatAmount,
  formatDate,
} from '@qianku/shared'
import type { PromotionInfoVO, WithdrawalVO } from '@qianku/shared'

defineOptions({ name: 'PromotionWithdraw' })

const router = useRouter()
const info = ref<PromotionInfoVO | null>(null)
const records = ref<WithdrawalVO[]>([])
const loading = ref(true)
const submitting = ref(false)

const amount = ref<number | undefined>(undefined)
const method = ref<'wechat' | 'alipay'>('wechat')

const methods = [
  { value: 'wechat', label: '微信', icon: '💬' },
  { value: 'alipay', label: '支付宝', icon: '🔵' },
]

const canSubmit = computed(() =>
  amount.value && amount.value >= 50 && info.value && amount.value <= info.value.totalEarnings,
)

onMounted(async () => {
  try {
    const [infoData, recordData] = await Promise.all([
      getPromotionInfo(),
      getWithdrawalRecords({ pageNum: 1, pageSize: 50 }),
    ])
    info.value = infoData
    records.value = recordData.list
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '审核中',
    processing: '处理中',
    completed: '已到账',
    rejected: '已拒绝',
  }
  return map[status] || status
}

function getStatusColor(status: string) {
  const map: Record<string, string> = {
    pending: '#FF9500',
    processing: '#007AFF',
    completed: '#34C759',
    rejected: '#FF3B30',
  }
  return map[status] || '#999'
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await createWithdrawal({
      amount: amount.value!,
      method: method.value,
      account: '',
      accountName: '',
    })
    showToast({ message: '申请已提交', type: 'success' })
    amount.value = undefined
    const recordData = await getWithdrawalRecords({ pageNum: 1, pageSize: 50 })
    records.value = recordData.list
    const infoData = await getPromotionInfo()
    info.value = infoData
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
        <div class="balance-value">{{ formatAmount(info?.totalEarnings || 0) }}</div>
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
              :class="{ active: method === m.value }"
              @click="method = m.value as 'wechat' | 'alipay'"
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
