<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  getReimbursementDetail,
  exportReimbursement,
  formatAmount,
  formatDate,
} from '@qianku/shared'
import { useUserStore } from '@qianku/shared'
import type { ReimbursementVO } from '@qianku/shared'

defineOptions({ name: 'ReimbursementPreview' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const detail = ref<ReimbursementVO | null>(null)
const loading = ref(true)
const exporting = ref(false)

const totalAmount = computed(() => detail.value?.totalAmount || 0)

onMounted(async () => {
  const id = route.params.id as string
  if (!userStore.userInfo) {
    await userStore.fetchProfile()
  }
  try {
    detail.value = await getReimbursementDetail(id)
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

async function handleExport(type: 'merged_pdf' | 'zip') {
  if (!detail.value) return
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
</script>

<template>
  <div class="page">
    <van-nav-bar title="报销单预览" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="detail">
      <div class="preview-container">
        <div class="preview-paper">
          <h2 class="paper-title">费用报销单</h2>

          <div class="paper-info">
            <div class="info-row">
              <span>报销标题：{{ detail.title }}</span>
            </div>
            <div class="info-row">
              <span>报销人：{{ userStore.userInfo?.nickname || '-' }}</span>
              <span>部门：{{ userStore.userInfo?.department || '-' }}</span>
            </div>
            <div class="info-row">
              <span>创建日期：{{ formatDate(detail.createdAt) }}</span>
              <span>单据编号：{{ detail.reimburseNo || String(detail.id).slice(0, 8).toUpperCase() }}</span>
            </div>
          </div>

          <table class="paper-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>费用类别</th>
                <th>日期</th>
                <th>金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(expense, index) in detail.expenses" :key="expense.id">
                <td>{{ index + 1 }}</td>
                <td>{{ expense.categoryName || expense.description || '费用' }}</td>
                <td>{{ formatDate(expense.expenseDate) }}</td>
                <td class="amount-cell">{{ formatAmount(expense.amount) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="3" class="total-label">合计</td>
                <td class="amount-cell total-amount">{{ formatAmount(totalAmount) }}</td>
              </tr>
            </tfoot>
          </table>

          <div class="paper-attachments">
            <div class="attach-title">附件清单</div>
            <div v-if="detail.expenses.length > 0" class="attach-list">
              <div v-for="(expense, index) in detail.expenses" :key="expense.id" class="attach-item">
                {{ index + 1 }}. {{ expense.categoryName || expense.description || '费用' }} -
                {{ expense.invoiceNo ? `发票号: ${expense.invoiceNo}` : '无发票' }}
              </div>
            </div>
            <div v-else class="attach-empty">无附件</div>
          </div>

          <div v-if="detail.remark" class="paper-remark">
            <span class="remark-label">备注：</span>{{ detail.remark }}
          </div>
        </div>
      </div>

      <div class="action-bar">
        <van-button
          type="primary"
          round
          :loading="exporting"
          @click="handleExport('merged_pdf')"
        >
          📄 导出合并PDF
        </van-button>
        <van-button
          plain
          type="primary"
          round
          :loading="exporting"
          @click="handleExport('zip')"
        >
          📦 导出ZIP
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

.preview-container {
  padding: 16px;
}

.preview-paper {
  background: #fff;
  border-radius: 12px;
  padding: 24px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .paper-title {
    text-align: center;
    font-size: 20px;
    font-weight: 700;
    margin-bottom: 20px;
    letter-spacing: 4px;
  }

  .paper-info {
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #eee;

    .info-row {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      color: var(--color-text-secondary);
      line-height: 2;
    }
  }

  .paper-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
    margin-bottom: 16px;

    th, td {
      border: 1px solid #e0e0e0;
      padding: 8px 6px;
      text-align: center;
    }

    th {
      background: #f5f5f5;
      font-weight: 600;
      font-size: 12px;
      color: var(--color-text-secondary);
    }

    .amount-cell {
      text-align: right;
      font-family: 'SF Mono', monospace;
    }

    .total-label {
      text-align: right;
      font-weight: 600;
    }

    .total-amount {
      font-weight: 700;
      color: var(--color-primary);
    }
  }

  .paper-attachments {
    margin-bottom: 12px;

    .attach-title {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 8px;
    }

    .attach-item {
      font-size: 12px;
      color: var(--color-text-secondary);
      line-height: 1.8;
    }

    .attach-empty {
      font-size: 12px;
      color: var(--color-text-tertiary);
    }
  }

  .paper-remark {
    font-size: 13px;
    color: var(--color-text-secondary);
    padding-top: 12px;
    border-top: 1px solid #eee;

    .remark-label {
      font-weight: 500;
      color: var(--color-text);
    }
  }
}

.action-bar {
  padding: 20px 16px;
  display: flex;
  gap: 12px;

  .van-button { flex: 1; }
}
</style>
