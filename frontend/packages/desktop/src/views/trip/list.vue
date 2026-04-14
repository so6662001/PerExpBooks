<template>
  <div class="page-container trip-list">
    <div class="page-header">
      <h2>出差管理</h2>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>新建出差
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadData">
        <el-option label="计划中" value="planned" />
        <el-option label="进行中" value="ongoing" />
        <el-option label="已完成" value="completed" />
        <el-option label="已取消" value="cancelled" />
      </el-select>
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
    </div>

    <el-table :data="trips" stripe v-loading="loading">
      <el-table-column prop="destination" label="目的地" width="150" />
      <el-table-column label="出差时间" width="220">
        <template #default="{ row }">
          {{ row.startDate }} ~ {{ row.endDate }}
        </template>
      </el-table-column>
      <el-table-column prop="days" label="天数" width="80" align="center" />
      <el-table-column prop="purpose" label="事由" min-width="200" show-overflow-tooltip />
      <el-table-column prop="totalExpense" label="费用" width="120" align="right">
        <template #default="{ row }">
          {{ formatAmount(row.totalExpense) }}
        </template>
      </el-table-column>
      <el-table-column prop="budget" label="预算" width="120" align="right">
        <template #default="{ row }">
          {{ row.budget ? formatAmount(row.budget) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="tripStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push(`/trip/${row.id}`)">
            详情
          </el-button>
          <el-button
            link
            type="success"
            size="small"
            v-if="row.status === 'ongoing'"
            @click="handleComplete(row)"
          >
            完成
          </el-button>
          <el-button
            link
            type="danger"
            size="small"
            v-if="row.status === 'planned'"
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

    <el-dialog v-model="showCreateDialog" title="新建出差" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="目的地" required>
          <el-input v-model="form.destination" placeholder="请输入出差目的地" />
        </el-form-item>
        <el-form-item label="时间范围" required>
          <el-date-picker
            v-model="formDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="事由" required>
          <el-input v-model="form.purpose" type="textarea" :rows="2" placeholder="出差事由" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="form.budget" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  listTrips,
  createTrip,
  completeTrip,
  deleteTrip,
  formatAmount,
  getStatusLabel,
  type TripVO,
  type TripQueryDTO,
  type TripCreateDTO,
} from '@qianku/shared'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const trips = ref<TripVO[]>([])
const total = ref(0)
const showCreateDialog = ref(false)
const creating = ref(false)
const dateRange = ref<[string, string] | null>(null)
const formDateRange = ref<[string, string] | null>(null)

const query = reactive<TripQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  startDate: undefined,
  endDate: undefined,
})

const form = reactive<TripCreateDTO>({
  destination: '',
  startDate: '',
  endDate: '',
  purpose: '',
  budget: undefined,
  remark: '',
})

function tripStatusType(status: string) {
  const map: Record<string, string> = {
    planned: '',
    ongoing: 'warning',
    completed: 'success',
    cancelled: 'info',
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
    const res = await listTrips(query)
    trips.value = res.list
    total.value = res.total
  } catch {
    ElMessage.error('加载出差列表失败')
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!form.destination || !formDateRange.value || !form.purpose) {
    ElMessage.warning('请填写必要信息')
    return
  }
  form.startDate = formDateRange.value[0]
  form.endDate = formDateRange.value[1]
  creating.value = true
  try {
    await createTrip(form)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    form.destination = ''
    form.purpose = ''
    form.budget = undefined
    form.remark = ''
    formDateRange.value = null
    loadData()
  } catch {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

async function handleComplete(row: TripVO) {
  try {
    await ElMessageBox.confirm('确认完成该出差记录？', '提示')
    await completeTrip(row.id)
    ElMessage.success('操作成功')
    loadData()
  } catch {
    // cancelled
  }
}

async function handleDelete(row: TripVO) {
  try {
    await ElMessageBox.confirm('确定要删除该出差记录吗？', '提示', { type: 'warning' })
    await deleteTrip(row.id)
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
.trip-list {
  .amount {
    font-weight: 600;
  }
}
</style>
