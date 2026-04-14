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
      <el-select v-model="query.categoryId" placeholder="分类" clearable style="width: 120px" @change="loadData">
        <el-option
          v-for="cat in categoryList"
          :key="cat.id"
          :label="cat.name"
          :value="cat.id"
        />
      </el-select>
      <el-select v-model="query.reimburseStatus" placeholder="状态" clearable style="width: 120px" @change="loadData">
        <el-option label="待报销" :value="0" />
        <el-option label="报销中" :value="1" />
        <el-option label="已报销" :value="2" />
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
      <el-table-column prop="categoryName" label="分类" width="100">
        <template #default="{ row }">
          <span>{{ row.categoryName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="invoiceNo" label="发票号" width="180">
        <template #default="{ row }">
          {{ row.invoiceNo || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" width="120" align="right" sortable>
        <template #default="{ row }">
          <span class="amount">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="reimburseStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="statusTagType(row.reimburseStatus)"
            size="small"
          >
            {{ getReimburseStatusLabel(row.reimburseStatus) }}
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
            :disabled="row.reimburseStatus !== 0"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
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
        <el-descriptions-item label="分类">{{ currentExpense.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatAmount(currentExpense.amount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getReimburseStatusLabel(currentExpense.reimburseStatus) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentExpense.description }}</el-descriptions-item>
        <el-descriptions-item label="发票号" :span="2">{{ currentExpense.invoiceNo || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="uploadDialogVisible" title="确认发票信息" width="500px">
      <div v-if="uploadedInvoice && !uploadedInvoice.parseSuccess" style="margin-bottom: 12px; color: #FF9500;">
        {{ uploadedInvoice.parseMessage }}
      </div>
      <el-form label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="uploadForm.categoryId">
            <el-option
              v-for="cat in categoryList"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="uploadForm.amount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="uploadForm.expenseDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUploadCreate">确认创建</el-button>
      </template>
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
  listCategories,
  formatAmount,
  getCategoryLabel,
  getCategoryIcon,
  getStatusLabel,
  type ExpenseVO,
  type ExpenseQueryDTO,
  type InvoiceUploadVO,
} from '@qianku/shared'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const expenses = ref<ExpenseVO[]>([])
const total = ref(0)
const selectedIds = ref<(number | string)[]>([])
const searchText = ref('')
const dateRange = ref<[string, string] | null>(null)
const detailVisible = ref(false)
const currentExpense = ref<ExpenseVO | null>(null)
const categoryList = ref<any[]>([])

const uploadDialogVisible = ref(false)
const uploadedInvoice = ref<InvoiceUploadVO | null>(null)
const uploadForm = reactive({
  categoryId: undefined as number | undefined,
  amount: undefined as number | undefined,
  expenseDate: '',
  description: '',
})

const query = reactive<ExpenseQueryDTO>({
  page: 1,
  pageSize: 10,
  reimburseStatus: undefined,
  categoryId: undefined,
  startDate: undefined,
  endDate: undefined,
})

function statusTagType(reimburseStatus: number) {
  if (reimburseStatus === 0) return 'warning'
  if (reimburseStatus === 1) return ''
  if (reimburseStatus === 2) return 'success'
  return 'info'
}

function getReimburseStatusLabel(reimburseStatus: number) {
  if (reimburseStatus === 0) return '待报销'
  if (reimburseStatus === 1) return '报销中'
  if (reimburseStatus === 2) return '已报销'
  return '未知'
}

onMounted(async () => {
  try {
    const cats = await listCategories()
    categoryList.value = cats as any[]
  } catch {
    // ignore
  }
  loadData()
})

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
    expenses.value = res.list || (res as any).records || []
    total.value = res.total || 0
  } catch {
    ElMessage.error('加载费用列表失败')
  } finally {
    loading.value = false
  }
}

async function handleUpload(file: UploadFile) {
  if (!file.raw) return
  try {
    ElMessage.info('正在上传发票...')
    const invoice = await uploadInvoice(file.raw)
    uploadedInvoice.value = invoice
    if (invoice.parseSuccess && invoice.amount) {
      uploadForm.amount = invoice.amount
      uploadForm.expenseDate = invoice.invoiceDate || ''
      uploadForm.description = invoice.sellerName ? `${invoice.sellerName} - 发票` : ''
    } else {
      uploadForm.amount = undefined
      uploadForm.expenseDate = ''
      uploadForm.description = ''
    }
    uploadForm.categoryId = categoryList.value.length > 0 ? categoryList.value[0].id : undefined
    uploadDialogVisible.value = true
  } catch {
    ElMessage.error('上传失败，请重试')
  }
}

async function confirmUploadCreate() {
  if (!uploadedInvoice.value || !uploadForm.categoryId || !uploadForm.amount || !uploadForm.expenseDate) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await createExpense({
      categoryId: uploadForm.categoryId,
      type: 1,
      amount: uploadForm.amount,
      invoiceNo: uploadedInvoice.value.invoiceNo || undefined,
      invoiceCode: uploadedInvoice.value.invoiceCode || undefined,
      invoiceDate: uploadedInvoice.value.invoiceDate || undefined,
      sellerName: uploadedInvoice.value.sellerName || undefined,
      buyerName: uploadedInvoice.value.buyerName || undefined,
      taxAmount: uploadedInvoice.value.taxAmount || undefined,
      fileUrl: uploadedInvoice.value.fileUrl || undefined,
      fileName: uploadedInvoice.value.fileName || undefined,
      description: uploadForm.description,
      expenseDate: uploadForm.expenseDate,
    })
    ElMessage.success('费用创建成功')
    uploadDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('创建失败')
  }
}

function handleSelectionChange(rows: ExpenseVO[]) {
  selectedIds.value = rows.filter(r => r.reimburseStatus === 0).map(r => r.id)
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
