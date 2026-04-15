<template>
  <div class="page-container reimburse-detail">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-button @click="$router.back()" circle>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>报销单详情</h2>
      </div>
      <div class="header-actions" v-if="detail">
        <el-button @click="handleExport('report_only')">
          <el-icon><Document /></el-icon>仅报销单PDF
        </el-button>
        <el-button @click="handleExport('merged_pdf')">
          <el-icon><Download /></el-icon>导出PDF
        </el-button>
        <el-button @click="handleExport('zip')">
          <el-icon><FolderOpened /></el-icon>导出ZIP
        </el-button>
        <el-button @click="showEmailDialog = true">
          <el-icon><Message /></el-icon>发送到邮箱
        </el-button>
        <el-button
          type="success"
          v-if="detail.reimburseStatus === 1"
          @click="handleReceived"
        >
          确认收款
        </el-button>
        <el-button
          type="danger"
          plain
          v-if="detail.reimburseStatus !== 2"
          @click="handleCancel"
        >
          取消报销单
        </el-button>
      </div>
    </div>

    <el-skeleton :loading="loading" :rows="10" animated>
      <template #default>
        <el-row :gutter="20" v-if="detail">
          <el-col :span="16">
            <el-card>
              <template #header>
                <span>报销单信息</span>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
                <el-descriptions-item label="金额">
                  <span class="amount">{{ formatAmount(detail.totalAmount) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="笔数">{{ detail.itemCount }} 笔</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="statusType(detail.reimburseStatus)">{{ getStatusLabel(detail.reimburseStatus) }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">
                  {{ formatDate(detail.createdAt, 'YYYY-MM-DD HH:mm') }}
                </el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
              </el-descriptions>
            </el-card>

            <el-card style="margin-top: 16px">
              <template #header>
                <span>费用明细</span>
              </template>
              <el-table :data="detail.expenses" stripe>
                <el-table-column type="index" width="50" />
                <el-table-column prop="expenseDate" label="日期" width="120" />
                <el-table-column prop="category" label="分类" width="100">
                  <template #default="{ row }">
                    {{ getCategoryIcon(row.category) }} {{ getCategoryLabel(row.category) }}
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
                <el-table-column prop="invoiceInfo.invoiceNo" label="发票号" width="160">
                  <template #default="{ row }">
                    {{ row.invoiceInfo?.invoiceNo || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="amount" label="金额" width="120" align="right">
                  <template #default="{ row }">
                    {{ formatAmount(row.amount) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card>
              <template #header>
                <span>费用分布</span>
              </template>
              <div ref="chartRef" style="height: 280px" />
            </el-card>

            <el-card style="margin-top: 16px">
              <template #header>
                <span>时间线</span>
              </template>
              <el-timeline>
                <el-timeline-item timestamp="创建" placement="top" color="#007AFF">
                  报销单创建于 {{ formatDate(detail.createdAt, 'YYYY-MM-DD HH:mm') }}
                </el-timeline-item>
                <el-timeline-item
                  v-if="detail.exportedAt"
                  timestamp="导出"
                  placement="top"
                  color="#FF9500"
                >
                  已导出于 {{ formatDate(detail.exportedAt, 'YYYY-MM-DD HH:mm') }}
                </el-timeline-item>
                <el-timeline-item
                  v-if="detail.reimburseStatus === 2"
                  timestamp="收款"
                  placement="top"
                  color="#34C759"
                >
                  已确认收款
                </el-timeline-item>
              </el-timeline>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </el-skeleton>

    <el-dialog v-model="showEmailDialog" title="发送到邮箱" width="440px">
      <el-form label-width="80px">
        <el-form-item label="邮箱">
          <el-input v-model="emailInput" placeholder="请输入接收邮箱" />
        </el-form-item>
        <el-form-item label="附件类型">
          <el-radio-group v-model="emailAttachType">
            <el-radio :value="1">合并PDF (推荐)</el-radio>
            <el-radio :value="2">ZIP压缩包</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEmailDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!emailInput" @click="handleSendEmail">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import {
  getReimbursementDetail,
  exportReimbursement,
  markReceived,
  cancelReimbursement,
  sendEmail,
  formatAmount,
  formatDate,
  getCategoryLabel,
  getCategoryIcon,
  getStatusLabel,
  type ReimbursementVO,
} from '@qianku/shared'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Tracker } from '@qianku/shared/analytics'

const route = useRoute()
const loading = ref(true)
const detail = ref<ReimbursementVO | null>(null)
const chartRef = ref<HTMLElement>()
const showEmailDialog = ref(false)
const emailInput = ref('')
const emailAttachType = ref(1)
let chart: echarts.ECharts | null = null

function statusType(status: number) {
  const map: Record<number, string> = { 0: '', 1: 'warning', 2: 'success' }
  return (map[status] ?? 'info') as any
}

async function loadData() {
  loading.value = true
  try {
    detail.value = await getReimbursementDetail(route.params.id as string)
    await nextTick()
    renderChart()
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value || !detail.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  const catMap: Record<string, number> = {}
  detail.value.expenses.forEach(e => {
    const label = getCategoryLabel(e.category)
    catMap[label] = (catMap[label] || 0) + e.amount
  })

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      data: Object.entries(catMap).map(([name, value]) => ({ name, value })),
      label: { formatter: '{b}\n{d}%', fontSize: 11 },
    }],
  })
}

async function handleExport(type: 'merged_pdf' | 'zip' | 'report_only') {
  if (!detail.value) return
  try {
    const res = await exportReimbursement(detail.value.id, { type })
    if (res.url) window.open(res.url, '_blank')
    if (type === 'merged_pdf') {
      try { Tracker.getInstance().track('reimburse_export_merged_pdf') } catch {}
    } else if (type === 'zip') {
      try { Tracker.getInstance().track('reimburse_export_zip') } catch {}
    }
    ElMessage.success('导出成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  }
}

async function handleSendEmail() {
  if (!detail.value || !emailInput.value) return
  try {
    await sendEmail(detail.value.id, {
      email: emailInput.value,
      attachType: emailAttachType.value,
    })
    try { Tracker.getInstance().track('reimburse_send_email') } catch {}
    ElMessage.success('已发送至邮箱')
    showEmailDialog.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  }
}

async function handleReceived() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('确认已收到报销款项？', '提示')
    await markReceived(detail.value.id)
    try { Tracker.getInstance().track('reimburse_confirm_received') } catch {}
    ElMessage.success('已确认收款')
    loadData()
  } catch {
    // cancelled
  }
}

async function handleCancel() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('确认取消此报销单？关联的费用将恢复为待报销状态。', '取消报销单')
    await cancelReimbursement(detail.value.id)
    ElMessage.success('已取消报销单')
    loadData()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => {
  chart?.dispose()
})
</script>

<style lang="scss" scoped>
.reimburse-detail {
  .header-actions {
    display: flex;
    gap: 8px;
  }

  .amount {
    font-size: 18px;
    font-weight: 700;
    color: #007AFF;
  }
}
</style>
