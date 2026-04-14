<template>
  <div class="page-container reimburse-create">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-button @click="$router.back()" circle>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>创建报销单</h2>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="14">
        <el-card>
          <template #header>
            <span>选择待报销费用</span>
          </template>
          <el-table
            ref="tableRef"
            :data="pendingExpenses"
            v-loading="loadingExpenses"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column prop="expenseDate" label="日期" width="110" />
            <el-table-column prop="category" label="分类" width="90">
              <template #default="{ row }">
                {{ getCategoryLabel(row.category) }}
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
            <el-table-column prop="amount" label="金额" width="110" align="right">
              <template #default="{ row }">
                {{ formatAmount(row.amount) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card>
          <template #header>
            <span>报销单信息</span>
          </template>
          <el-form :model="form" label-width="80px">
            <el-form-item label="标题" required>
              <el-input v-model="form.title" placeholder="如：2024年3月差旅报销" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息" />
            </el-form-item>
          </el-form>

          <el-divider />

          <div class="summary">
            <div class="summary-row">
              <span>已选费用</span>
              <span>{{ selectedExpenses.length }} 笔</span>
            </div>
            <div class="summary-row total">
              <span>合计金额</span>
              <span class="total-amount">{{ formatAmount(totalAmount) }}</span>
            </div>
          </div>

          <el-button
            type="primary"
            style="width: 100%; margin-top: 20px"
            size="large"
            :disabled="selectedExpenses.length === 0 || !form.title"
            :loading="submitting"
            @click="handleSubmit"
          >
            生成报销单
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getPendingExpenses,
  createReimbursement,
  formatAmount,
  getCategoryLabel,
  type ExpenseVO,
} from '@qianku/shared'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loadingExpenses = ref(false)
const submitting = ref(false)
const pendingExpenses = ref<ExpenseVO[]>([])
const selectedExpenses = ref<ExpenseVO[]>([])
const tableRef = ref()

const form = ref({
  title: '',
  remark: '',
})

const totalAmount = computed(() =>
  selectedExpenses.value.reduce((sum, e) => sum + e.amount, 0)
)

function handleSelectionChange(rows: ExpenseVO[]) {
  selectedExpenses.value = rows
}

async function loadPendingExpenses() {
  loadingExpenses.value = true
  try {
    const res = await getPendingExpenses()
    pendingExpenses.value = res as ExpenseVO[]

    await nextTick()
    const preSelectedIds = route.query.ids?.toString().split(',') || []
    if (preSelectedIds.length > 0 && tableRef.value) {
      pendingExpenses.value.forEach(row => {
        if (preSelectedIds.includes(row.id)) {
          tableRef.value.toggleRowSelection(row, true)
        }
      })
    }
  } catch {
    ElMessage.error('加载待报销费用失败')
  } finally {
    loadingExpenses.value = false
  }
}

async function handleSubmit() {
  if (!form.value.title) {
    ElMessage.warning('请输入报销单标题')
    return
  }
  if (selectedExpenses.value.length === 0) {
    ElMessage.warning('请选择至少一笔费用')
    return
  }
  submitting.value = true
  try {
    const res = await createReimbursement({
      title: form.value.title,
      remark: form.value.remark,
      expenseIds: selectedExpenses.value.map(e => e.id),
    })
    ElMessage.success('报销单创建成功')
    router.push(`/reimbursement/${res.id}`)
  } catch {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPendingExpenses()
})
</script>

<style lang="scss" scoped>
.reimburse-create {
  .summary {
    .summary-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 0;
      font-size: 14px;
      color: #86868b;

      &.total {
        font-size: 16px;
        font-weight: 600;
        color: #1d1d1f;
        border-top: 1px solid #f0f0f0;
        padding-top: 12px;
        margin-top: 4px;
      }

      .total-amount {
        font-size: 24px;
        color: #007AFF;
      }
    }
  }
}
</style>
