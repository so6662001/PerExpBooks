<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getPendingExpenses, createReimbursement, formatAmount, formatDate, get } from '@qianku/shared'
import type { ExpenseVO } from '@qianku/shared'

const router = useRouter()
const pendingExpenses = ref<ExpenseVO[]>([])
const selectedIds = ref<number[]>([])
const title = ref('')
const remark = ref('')
const loading = ref(true)
const creating = ref(false)
const showShareGuide = ref(false)
const shareGuideMessage = ref('')

const totalAmount = computed(() =>
  pendingExpenses.value
    .filter((e) => selectedIds.value.includes(e.id))
    .reduce((sum, e) => sum + e.amount, 0),
)

const selectedCount = computed(() => selectedIds.value.length)

const invoiceFileCount = computed(() =>
  pendingExpenses.value
    .filter((e) => selectedIds.value.includes(e.id) && e.fileUrl)
    .length,
)

const canSubmit = computed(() =>
  selectedIds.value.length > 0 && title.value.trim() && !creating.value,
)

onMounted(async () => {
  try {
    const result = await getPendingExpenses()
    pendingExpenses.value = result as any as ExpenseVO[]
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function toggleSelect(id: number) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function toggleAll() {
  if (selectedIds.value.length === pendingExpenses.value.length) {
    selectedIds.value = []
  } else {
    selectedIds.value = pendingExpenses.value.map((e) => e.id)
  }
}

async function handleCreate() {
  if (!canSubmit.value) return
  creating.value = true
  try {
    await createReimbursement({
      title: title.value,
      remark: remark.value,
      expenseIds: selectedIds.value,
    })
    showToast({ message: '报销单创建成功', type: 'success' })
    try {
      const triggerResult = await get('/trigger/check', { params: { scene: 'REIMBURSEMENT_CREATED' } })
      if (triggerResult?.show) {
        showShareGuide.value = true
        shareGuideMessage.value = triggerResult.message
        return
      }
    } catch { /* ignore */ }
    router.back()
  } catch (e: any) {
    showToast(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="创建报销单" left-arrow @click-left="router.back()" />

    <div class="form-card card">
      <van-field
        v-model="title"
        label="报销标题"
        placeholder="例：3月北京出差报销"
        required
      />
      <van-field
        v-model="remark"
        label="备注"
        placeholder="可选填写备注"
        type="textarea"
        rows="2"
        autosize
      />
    </div>

    <div class="expense-section">
      <div class="section-header">
        <span class="section-title" style="padding: 0">待报销费用</span>
        <span class="select-all" @click="toggleAll">
          {{ selectedIds.length === pendingExpenses.length ? '取消全选' : '全选' }}
        </span>
      </div>

      <van-loading v-if="loading" class="page-loading" />

      <div v-for="expense in pendingExpenses" :key="expense.id" class="expense-check-item card" @click="toggleSelect(expense.id)">
        <van-checkbox
          :model-value="selectedIds.includes(expense.id)"
          checked-color="#007AFF"
          @click.stop
          @update:model-value="toggleSelect(expense.id)"
        />
        <div class="check-item-info">
          <div class="check-item-name">{{ expense.categoryName || expense.description || '费用' }}</div>
          <div class="check-item-date">
            {{ formatDate(expense.expenseDate) }}
            <span v-if="expense.fileUrl" style="color:#34C759; margin-left: 6px">有发票原件</span>
            <span v-else style="color:#8E8E93; margin-left: 6px">无发票</span>
          </div>
        </div>
        <div class="check-item-amount amount">{{ formatAmount(expense.amount) }}</div>
      </div>

      <div v-if="pendingExpenses.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">📭</div>
        <div class="empty-text">暂无待报销费用</div>
      </div>
    </div>

    <div class="submit-bar">
      <div class="summary">
        <span>已选 {{ selectedCount }} 项，含 {{ invoiceFileCount }} 张发票原件</span>
        <span class="total-amount amount">合计 {{ formatAmount(totalAmount) }}</span>
      </div>
      <van-button
        type="primary"
        block
        round
        size="large"
        :disabled="!canSubmit"
        :loading="creating"
        @click="handleCreate"
      >
        生成报销单
      </van-button>
    </div>

    <van-popup v-model:show="showShareGuide" position="center" round style="padding: 24px; width: 80%">
      <div style="text-align: center">
        <div style="font-size: 16px; font-weight: 600; margin-bottom: 12px">分享给好友</div>
        <div style="font-size: 14px; color: #666">{{ shareGuideMessage }}</div>
        <van-button type="primary" round block style="margin-top: 16px" @click="showShareGuide = false; router.back()">
          知道了
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.form-card {
  margin-top: 12px;
}

.expense-section {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 16px 8px;

    .select-all {
      font-size: 13px;
      color: var(--color-primary);
      cursor: pointer;
    }
  }
}

.expense-check-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 16px 8px;
  padding: 14px;
  cursor: pointer;

  .check-item-info {
    flex: 1;

    .check-item-name {
      font-size: 15px;
      font-weight: 500;
    }

    .check-item-date {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 2px;
    }
  }

  .check-item-amount {
    font-size: 16px;
  }
}

.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--color-card);
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  border-top: 0.5px solid var(--color-border);
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);

  .summary {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;
    font-size: 14px;

    .total-amount {
      font-size: 18px;
      font-weight: 700;
      color: var(--color-primary);
    }
  }
}
</style>
