<template>
  <div class="page-container promotion-page">
    <div class="page-header">
      <h2>推广中心</h2>
    </div>

    <el-row :gutter="16" class="stat-cards" v-if="dashboard">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #007AFF">{{ dashboard.totalInvite }}</div>
          <div class="stat-label">邀请人数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #34C759">{{ formatAmount(dashboard.totalCommission) }}</div>
          <div class="stat-label">累计收益</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #FF9500">{{ formatAmount(dashboard.availableBalance) }}</div>
          <div class="stat-label">可提现</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value" style="color: #5856D6">{{ dashboard.levelInfo?.levelName }} Lv.{{ dashboard.levelInfo?.level }}</div>
          <div class="stat-label">推广等级</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>推广链接</span>
          </template>
          <div class="promo-section">
            <p>邀请码：<strong>{{ codeInfo?.inviteCode || '-' }}</strong></p>
            <el-input v-model="inviteLink" readonly style="margin: 12px 0">
              <template #append>
                <el-button @click="handleCopy">复制链接</el-button>
              </template>
            </el-input>
            <p class="tip">分享链接给好友，好友注册并购买会员后，您将获得佣金奖励。</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>收益概览</span>
          </template>
          <div ref="chartRef" style="height: 250px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>邀请记录</span>
      </template>
      <el-table :data="records" stripe v-loading="loadingRecords">
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.inviteeAvatarUrl">
                {{ row.inviteeNickname?.charAt(0) }}
              </el-avatar>
              <span>{{ row.inviteeNickname || '用户' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="80">
          <template #default="{ row }">
            {{ row.level === 1 ? '直邀' : '二级' }}
          </template>
        </el-table-column>
        <el-table-column prop="inviteeStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.inviteeStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.inviteeStatus === 1 ? '已付费' : '试用中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt, 'YYYY-MM-DD HH:mm') }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getPromotionDashboard,
  getInviteCode,
  getInviteRecords,
  formatAmount,
  formatDate,
  copyToClipboard,
  type DashboardVO,
  type InviteCodeVO,
  type InviteRecordVO,
} from '@qianku/shared'
import { ElMessage } from 'element-plus'

const dashboard = ref<DashboardVO | null>(null)
const codeInfo = ref<InviteCodeVO | null>(null)
const inviteLink = ref('')
const records = ref<InviteRecordVO[]>([])
const loadingRecords = ref(false)
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

async function loadData() {
  try {
    const [dashboardData, codeData] = await Promise.all([
      getPromotionDashboard(),
      getInviteCode(),
    ])
    dashboard.value = dashboardData
    codeInfo.value = codeData
    inviteLink.value = codeData.inviteLink
    await nextTick()
    renderChart()
  } catch {
    // silent
  }
}

async function loadRecords() {
  loadingRecords.value = true
  try {
    const res = await getInviteRecords()
    records.value = res
  } catch {
    records.value = []
  } finally {
    loadingRecords.value = false
  }
}

function renderChart() {
  if (!chartRef.value || !dashboard.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '60%'],
      data: [
        { name: '可提现', value: dashboard.value.availableBalance, itemStyle: { color: '#34C759' } },
        { name: '冻结中', value: dashboard.value.frozenBalance, itemStyle: { color: '#FF9500' } },
        { name: '已提现', value: Math.max(0, dashboard.value.totalCommission - dashboard.value.availableBalance - dashboard.value.frozenBalance), itemStyle: { color: '#007AFF' } },
      ],
      label: { formatter: '{b}\n¥{c}' },
    }],
  })
}

async function handleCopy() {
  try {
    await copyToClipboard(inviteLink.value)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

onMounted(() => {
  loadData()
  loadRecords()
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => {
  chart?.dispose()
})
</script>

<style lang="scss" scoped>
.promotion-page {
  .stat-cards {
    margin-bottom: 16px;
  }

  .promo-section {
    .tip {
      font-size: 13px;
      color: #86868b;
      line-height: 1.6;
    }
  }

  .user-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .commission {
    color: #34C759;
    font-weight: 600;
  }
}
</style>
