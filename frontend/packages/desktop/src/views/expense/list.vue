<template>
  <div class="page-container expense-list">
    <div class="page-header">
      <h2>费用列表</h2>
      <div class="header-actions">
        <el-button type="primary" @click="$router.push('/expense/upload')">
          <el-icon><Upload /></el-icon>上传发票
        </el-button>
        <el-button
          :disabled="selectedIds.length === 0"
          @click="batchCreateReimbursement"
        >
          批量创建报销单 ({{ selectedIds.length }})
        </el-button>
      </div>
    </div>

    <el-upload
      class="drag-upload"
      drag
      action=""
      :auto-upload="false"
      :show-file-list="false"
      accept=".jpg,.jpeg,.png,.pdf"
      multiple
      @change="handleUpload"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">拖拽发票图片到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">支持 JPG/PNG/PDF 格式</div>
      </template>
    </el-upload>

    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="loadData"
      />
      <el-select v-model="query.category" placeholder="分类" clearable style="width: 120px" @change="loadData">
        <el-option label="交通" value="transport" />
        <el-option label="住宿" value="accommodation" />
        <el-option label="餐饮" value="meal" />
        <el-option label="办公" value="office" />
        <el-option label="通讯" value="communication" />
        <el-option label="补贴" value="subsidy" />
        <el-option label="其他" value="other" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadData">
        <el-option label="待报销" value="pending" />
        <el-option label="报销中" value="reimbursing" />
        <el-option label="已报销" value="reimbursed" />
      </el-select>
      <el-input
        v-model="searchText"
        placeholder="搜索描述..."
        clearable
        style="width: 200px"
        @clear="loadData"
        @keyup.enter="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-table
      :data="expenses"
      stripe
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="expenseDate" label="日期" width="120" sortable />
      <el-table-column prop="category" label="分类" width="100">
        <template #default="{ row }">
          <span>{{ getCategoryIcon(row.category) }} {{ getCategoryLabel(row.category) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="invoiceInfo.invoiceNo" label="发票号" width="180">
        <template #default="{ row }">
          {{ row.invoiceInfo?.invoiceNo || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" width="120" align="right" sortable>
        <template #default="{ row }">
          <span class="amount">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="statusTagType(row.status)"
            size="small"
          >
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button
            link
            type="danger"
            size="small"
            :disabled="row.status !== 'pending'"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="loadData"
      @current-change="loadData"
    />

    <el-dialog v-model="detailVisible" title="费用详情" width="600px">
      <el-descriptions :column="2" border v-if="currentExpense">
        <el-descriptions-item label="日期">{{ currentExpense.expenseDate }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ getCategoryLabel(currentExpense.category) }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatAmount(currentExpense.amount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusLabel(currentExpense.status) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentExpense.description }}</el-descriptions-item>
        <el-descriptions-item label="发票号" :span="2">{{ currentExpense.invoiceInfo?.invoiceNo || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  listExpenses,
  deleteExpense,
  uploadInvoice,
  createExpense,
  formatAmount,
  getCategoryLabel,
  getCategoryIcon,
  getStatusLabel,
  type ExpenseVO,
  type ExpenseQueryDTO,
} from '@qianku/shared'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const expenses = ref<ExpenseVO[]>([])
const total = ref(0)
const selectedIds = ref<string[]>([])
const searchText = ref('')
const dateRange = ref<[string, string] | null>(null)
const detailVisible = ref(false)
const currentExpense = ref<ExpenseVO | null>(null)

const query = reactive<ExpenseQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  category: undefined,
  startDate: undefined,
  endDate: undefined,
})

function statusTagType(status: string) {
  const map: Record<string, string> = {
    pending: 'warning',
    reimbursing: '',
    reimbursed: 'success',
  }
  return (map[status] || 'info') as any
}

async function loadData() {
  loading.value = true
  if (dateRange.value) {
    query.startDate = dateRange.value[0]
    query.endDate = dateRange.value[1]
  } else {
    query.startDate = undefined
    query.endDate = undefined
  }
  try {
    const res = await listExpenses(query)
    expenses.value = res.list
    total.value = res.total
  } catch {
    ElMessage.error('加载费用列表失败')
  } finally {
    loading.value = false
  }
}

async function handleUpload(file: UploadFile) {
  if (!file.raw) return
  try {
    ElMessage.info('正在识别发票...')
    const invoice = await uploadInvoice(file.raw)
    await createExpense({
      invoiceId: invoice.invoiceId,
      category: 'other',
      amount: invoice.amount,
      description: `${invoice.seller} - ${invoice.items?.[0]?.name || '费用'}`,
      expenseDate: invoice.invoiceDate,
    })
    ElMessage.success('发票上传并创建费用成功')
    loadData()
  } catch {
    ElMessage.error('上传失败，请重试')
  }
}

function handleSelectionChange(rows: ExpenseVO[]) {
  selectedIds.value = rows.filter(r => r.status === 'pending').map(r => r.id)
}

function batchCreateReimbursement() {
  router.push({
    path: '/reimbursement/create',
    query: { ids: selectedIds.value.join(',') },
  })
}

function viewDetail(row: ExpenseVO) {
  currentExpense.value = row
  detailVisible.value = true
}

async function handleDelete(row: ExpenseVO) {
  try {
    await ElMessageBox.confirm('确定要删除该费用记录吗？', '提示', {
      type: 'warning',
    })
    await deleteExpense(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.expense-list {
  .header-actions {
    display: flex;
    gap: 8px;
  }

  .drag-upload {
    margin-bottom: 16px;

    :deep(.el-upload-dragger) {
      padding: 20px;
      border-radius: 12px;
      border: 2px dashed #e5e5ea;
      background: #fafafa;
      transition: all 0.3s;

      &:hover {
        border-color: #007AFF;
        background: #f0f7ff;
      }
    }
  }

  .amount {
    font-weight: 600;
    color: #1d1d1f;
  }
}
</style>
