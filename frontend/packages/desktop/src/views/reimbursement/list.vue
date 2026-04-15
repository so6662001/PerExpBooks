<template>
  <div class="page-container reimburse-list">
    <div class="page-header">
      <h2>报销单列表</h2>
      <el-button type="primary" @click="$router.push('/reimbursement/create')">
        <el-icon><Plus /></el-icon>创建报销单
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.reimburseStatus" placeholder="状态" clearable style="width: 120px" @change="loadData">
        <el-option label="已生成" :value="0" />
        <el-option label="已导出" :value="1" />
        <el-option label="已收款" :value="2" />
      </el-select>
    </div>

    <el-table :data="reimbursements" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="笔数" width="80" align="center" />
      <el-table-column prop="totalAmount" label="金额" width="130" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatAmount(row.totalAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="reimburseStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="reimStatusType(row.reimburseStatus)" size="small">
            {{ getStatusLabel(row.reimburseStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatDate(row.createdAt, 'YYYY-MM-DD HH:mm') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push(`/reimbursement/${row.id}`)">
            详情
          </el-button>
          <el-button link type="primary" size="small" @click="handleExport(row, 'merged_pdf')">
            导出PDF
          </el-button>
          <el-button
            link
            type="success"
            size="small"
            v-if="row.reimburseStatus === 1"
            @click="handleReceived(row)"
          >
            确认收款
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  listReimbursements,
  exportReimbursement,
  markReceived,
  formatAmount,
  formatDate,
  getStatusLabel,
  type ReimbursementVO,
  type ReimbursementQueryDTO,
} from '@qianku/shared'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const reimbursements = ref<ReimbursementVO[]>([])
const total = ref(0)

const query = reactive<ReimbursementQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  reimburseStatus: undefined,
})

function reimStatusType(status: number) {
  const map: Record<number, string> = {
    0: '',
    1: 'warning',
    2: 'success',
  }
  return (map[status] ?? 'info') as any
}

async function loadData() {
  loading.value = true
  try {
    const res = await listReimbursements(query)
    reimbursements.value = res.list
    total.value = res.total
  } catch {
    ElMessage.error('加载列表失败')
  } finally {
    loading.value = false
  }
}

async function handleExport(row: ReimbursementVO, type: 'merged_pdf' | 'zip') {
  try {
    const res = await exportReimbursement(row.id, { type })
    if (res.url) {
      window.open(res.url, '_blank')
    }
    ElMessage.success('导出成功')
    loadData()
  } catch {
    ElMessage.error('导出失败')
  }
}

async function handleReceived(row: ReimbursementVO) {
  try {
    await ElMessageBox.confirm('确认已收到报销款项？', '提示')
    await markReceived(row.id)
    ElMessage.success('已确认收款')
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
.reimburse-list {
  .amount {
    font-weight: 600;
    color: #1d1d1f;
  }
}
</style>
