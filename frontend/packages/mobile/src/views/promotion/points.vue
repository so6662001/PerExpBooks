<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { getPromotionDashboard, getPointsLog, redeemPoints, formatDate } from '@qianku/shared'
import type { DashboardVO, PointsLogVO } from '@qianku/shared'

defineOptions({ name: 'PromotionPoints' })

const router = useRouter()
const dashboard = ref<DashboardVO | null>(null)
const pointRecords = ref<PointsLogVO[]>([])
const loading = ref(true)

const exchangeOptions = [
  { redeemType: '1', points: 50, reward: '7天会员', icon: '📅' },
  { redeemType: '2', points: 100, reward: '¥10优惠券', icon: '🎫' },
  { redeemType: '3', points: 200, reward: '1月会员', icon: '👑' },
  { redeemType: '4', points: 500, reward: '年度会员', icon: '💎' },
]

onMounted(async () => {
  try {
    const [dashboardData, logsData] = await Promise.all([
      getPromotionDashboard(),
      getPointsLog(),
    ])
    dashboard.value = dashboardData
    pointRecords.value = logsData
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

async function handleExchange(option: typeof exchangeOptions[0]) {
  if (!dashboard.value || dashboard.value.points < option.points) {
    showToast('积分不足')
    return
  }
  try {
    await showDialog({
      title: '确认兑换',
      message: `消耗 ${option.points} 积分兑换 ${option.reward}？`,
      showCancelButton: true,
    })
    await redeemPoints({ redeemType: option.redeemType })
    showToast({ message: '兑换成功', type: 'success' })
    const [dashboardData, logsData] = await Promise.all([
      getPromotionDashboard(),
      getPointsLog(),
    ])
    dashboard.value = dashboardData
    pointRecords.value = logsData
  } catch {
    // cancelled or failed
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="积分中心" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="points-header">
        <div class="points-label">当前积分</div>
        <div class="points-value">{{ dashboard?.points || 0 }}</div>
      </div>

      <div class="section-title">积分兑换</div>
      <div class="exchange-grid">
        <div
          v-for="option in exchangeOptions"
          :key="option.redeemType"
          class="exchange-card card"
          @click="handleExchange(option)"
        >
          <div class="exchange-icon">{{ option.icon }}</div>
          <div class="exchange-reward">{{ option.reward }}</div>
          <div class="exchange-points">{{ option.points }} 积分</div>
          <van-button
            type="primary"
            size="mini"
            round
            :disabled="!dashboard || dashboard.points < option.points"
          >
            兑换
          </van-button>
        </div>
      </div>

      <div class="section-title">积分流水</div>

      <div v-if="pointRecords.length > 0" class="records-list">
        <div v-for="record in pointRecords" :key="record.id" class="record-item card">
          <div class="record-info">
            <div class="record-desc">{{ record.remark }}</div>
            <div class="record-date">{{ formatDate(record.createdAt, 'YYYY-MM-DD HH:mm') }}</div>
          </div>
          <div class="record-points" :class="record.points > 0 ? 'positive' : 'negative'">
            {{ record.points > 0 ? '+' : '' }}{{ record.points }}
          </div>
        </div>
      </div>

      <div v-if="pointRecords.length === 0" class="empty-state">
        <div class="empty-icon">⭐</div>
        <div class="empty-text">暂无积分记录</div>
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

.points-header {
  text-align: center;
  padding: 32px 16px;
  background: linear-gradient(135deg, #FF9500, #FF6B00);
  color: white;

  .points-label {
    font-size: 14px;
    opacity: 0.85;
  }

  .points-value {
    font-size: 48px;
    font-weight: 800;
    margin-top: 4px;
    letter-spacing: -2px;
  }
}

.exchange-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 0 16px;

  .exchange-card {
    text-align: center;
    padding: 18px 12px;
    margin: 0;
    cursor: pointer;
    transition: transform 0.15s;

    &:active { transform: scale(0.97); }

    .exchange-icon {
      font-size: 32px;
      margin-bottom: 8px;
    }

    .exchange-reward {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 4px;
    }

    .exchange-points {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-bottom: 10px;
    }
  }
}

.records-list {
  .record-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 0 16px 8px;
    padding: 14px 16px;

    .record-info {
      .record-desc {
        font-size: 15px;
        font-weight: 500;
      }

      .record-date {
        font-size: 12px;
        color: var(--color-text-secondary);
        margin-top: 2px;
      }
    }

    .record-points {
      font-size: 18px;
      font-weight: 700;

      &.positive { color: #34C759; }
      &.negative { color: #FF3B30; }
    }
  }
}
</style>
