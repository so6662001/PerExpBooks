<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listReimbursements, formatAmount, getStatusLabel, formatDate } from '@qianku/shared'
import type { ReimbursementVO, ReimbursementStatus } from '@qianku/shared'

defineOptions({ name: 'ReimbursementList' })

const router = useRouter()
const activeTab = ref<string>('generated')
const list = ref<ReimbursementVO[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNum = ref(1)

const tabs = [
  { name: 'generated', title: '已生成' },
  { name: 'exported', title: '已导出' },
  { name: 'received', title: '已收款' },
]

async function loadData(isRefresh = false) {
  if (isRefresh) {
    pageNum.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const result = await listReimbursements({
      pageNum: pageNum.value,
      pageSize: 20,
      status: activeTab.value as ReimbursementStatus,
    })
    if (isRefresh) {
      list.value = result.list
    } else {
      list.value.push(...result.list)
    }
    if (list.value.length >= result.total) finished.value = true
    pageNum.value++
  } catch {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function onRefresh() {
  refreshing.value = true
  loadData(true)
}

watch(activeTab, () => loadData(true))
onMounted(() => loadData(true))

function goDetail(id: string) {
  router.push(`/reimbursement/${id}`)
}

function goCreate() {
  router.push('/reimbursement/create')
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <span class="page-title">报销</span>
      <van-icon name="add-o" size="24" color="#007AFF" @click="goCreate" />
    </div>

    <van-tabs v-model:active="activeTab" sticky offset-top="56" shrink>
      <van-tab v-for="tab in tabs" :key="tab.name" :name="tab.name" :title="tab.title" />
    </van-tabs>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadData"
      >
        <div
          v-for="item in list"
          :key="item.id"
          class="reimb-card card"
          @click="goDetail(item.id)"
        >
          <div class="reimb-header">
            <span class="reimb-title">{{ item.title }}</span>
            <span class="status-tag" :class="item.status">
              {{ getStatusLabel(item.status) }}
            </span>
          </div>
          <div class="reimb-body">
            <div class="reimb-amount amount">{{ formatAmount(item.totalAmount) }}</div>
            <div class="reimb-meta">
              <span>{{ item.itemCount }} 项费用</span>
              <span>{{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
        </div>

        <div v-if="list.length === 0 && !loading" class="empty-state">
          <div class="empty-icon">📋</div>
          <div class="empty-text">暂无报销单</div>
          <van-button type="primary" size="small" round @click="goCreate" style="margin-top: 16px">
            创建报销单
          </van-button>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style lang="scss" scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reimb-card {
  margin: 0 16px 8px;
  padding: 16px;
  cursor: pointer;
  transition: transform 0.15s;

  &:active { transform: scale(0.98); }

  .reimb-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .reimb-title {
      font-size: 16px;
      font-weight: 600;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .reimb-body {
    margin-top: 10px;

    .reimb-amount {
      font-size: 24px;
      font-weight: 700;
      color: var(--color-text);
    }

    .reimb-meta {
      display: flex;
      gap: 16px;
      margin-top: 6px;
      font-size: 13px;
      color: var(--color-text-secondary);
    }
  }
}
</style>
