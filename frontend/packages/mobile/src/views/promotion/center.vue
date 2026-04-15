<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  getPromotionDashboard,
  getInviteCode,
  getInviteRecords,
  recordDailyShare,
  getPoster,
  generateCustomCard,
  formatAmount,
  formatDate,
  copyToClipboard,
} from '@qianku/shared'
import type { DashboardVO, InviteCodeVO, InviteRecordVO } from '@qianku/shared'

const router = useRouter()
const dashboard = ref<DashboardVO | null>(null)
const codeInfo = ref<InviteCodeVO | null>(null)
const records = ref<InviteRecordVO[]>([])
const loading = ref(true)
const showShareGuide = ref(false)
const shareGuideMessage = ref('')

onMounted(async () => {
  try {
    const [dashboardData, codeData, recordData] = await Promise.all([
      getPromotionDashboard(),
      getInviteCode(),
      getInviteRecords(),
    ])
    dashboard.value = dashboardData
    codeInfo.value = codeData
    records.value = recordData
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

const levelName = (level: number) => {
  const names: Record<number, string> = { 1: '新手', 2: '银牌', 3: '金牌', 4: '钻石' }
  return names[level] || '新手'
}

async function copyInviteLink() {
  if (!codeInfo.value) return
  try {
    await copyToClipboard(codeInfo.value.inviteLink)
    showToast({ message: '邀请链接已复制', type: 'success' })
  } catch {
    showToast('复制失败')
  }
}

async function generatePoster() {
  try {
    const result = await getPoster({ type: 'invite' })
    showToast({ message: '海报已生成', type: 'success' })
  } catch (e: any) {
    showToast(e.message || '生成失败')
  }
}

async function recordDailyShareAndTrack() {
  try {
    await recordDailyShare()
    showToast({ message: '分享成功 +2积分', type: 'success' })
    const data = await getPromotionDashboard()
    dashboard.value = data
  } catch (e: any) {
    showToast(e.message || '分享失败')
  }
}

async function generateCard(cardType: string) {
  try {
    await generateCustomCard({ cardType })
    showToast({ message: '卡片已生成', type: 'success' })
  } catch (e: any) {
    showToast(e.message || '生成失败')
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="推广中心" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="dashboard">
      <div class="level-card">
        <div class="level-title">
          {{ dashboard.levelInfo.levelName }} Lv.{{ dashboard.levelInfo.level }}
        </div>
        <div class="level-progress" v-if="dashboard.levelInfo.nextLevelInviteRequired">
          <van-progress
            :percentage="Math.min(100, Math.round(dashboard.levelInfo.paidInviteCount / dashboard.levelInfo.nextLevelInviteRequired * 100))"
            stroke-width="8"
            color="#FFD700"
          />
          <div class="progress-text">
            {{ dashboard.levelInfo.paidInviteCount }}/{{ dashboard.levelInfo.nextLevelInviteRequired }} 人 → {{ dashboard.levelInfo.nextLevelName }}
          </div>
        </div>
      </div>

      <div class="data-grid">
        <div class="data-item card">
          <div class="data-value">¥{{ formatAmount(dashboard.totalCommission) }}</div>
          <div class="data-label">累计收益</div>
        </div>
        <div class="data-item card">
          <div class="data-value">¥{{ formatAmount(dashboard.availableBalance) }}</div>
          <div class="data-label">可提现</div>
        </div>
        <div class="data-item card">
          <div class="data-value">{{ dashboard.points }}</div>
          <div class="data-label">积分</div>
        </div>
        <div class="data-item card">
          <div class="data-value">{{ dashboard.totalInvite }}人</div>
          <div class="data-label">已邀请</div>
        </div>
      </div>

      <div class="actions card">
        <van-button type="primary" round block @click="copyInviteLink">复制邀请链接</van-button>
        <van-button round block @click="generatePoster" style="margin-top: 8px">生成海报</van-button>
        <van-button round block @click="recordDailyShareAndTrack" style="margin-top: 8px" color="#FF9500">
          每日分享+2积分
        </van-button>
      </div>

      <div class="social-cards">
        <div class="section-title">社交货币卡片</div>
        <div class="cards-grid">
          <div class="social-card card" @click="generateCard('achievement')">
            <div class="card-icon">🏆</div>
            <div class="card-name">出差战绩卡</div>
          </div>
          <div class="social-card card" @click="generateCard('savings')">
            <div class="card-icon">📊</div>
            <div class="card-name">效率对比卡</div>
          </div>
          <div class="social-card card" @click="generateCard('monthly_report')">
            <div class="card-icon">📅</div>
            <div class="card-name">月度报告卡</div>
          </div>
        </div>
      </div>

      <van-cell-group title="邀请记录" v-if="records.length > 0">
        <van-cell
          v-for="r in records"
          :key="r.inviteeId"
          :title="r.inviteeNickname || r.inviteePhone || '用户'"
          :label="formatDate(r.createdAt)"
        >
          <template #value>
            <van-tag :type="r.inviteeStatus === 1 ? 'success' : 'default'">
              {{ r.inviteeStatus === 1 ? '已付费' : '试用中' }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>

      <div v-if="records.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">📢</div>
        <div class="empty-text">暂无邀请记录</div>
      </div>

      <div class="bottom-actions">
        <van-button type="primary" block round @click="$router.push('/promotion/withdraw')">
          提现 (可提现¥{{ formatAmount(dashboard.availableBalance) }})
        </van-button>
        <van-button block round @click="$router.push('/promotion/points')" style="margin-top: 10px">
          积分商城 ({{ dashboard.points }}积分)
        </van-button>
      </div>
    </template>

    <van-popup v-model:show="showShareGuide" position="center" round style="padding: 24px; width: 80%">
      <div style="text-align: center">
        <div style="font-size: 16px; font-weight: 600; margin-bottom: 12px">分享引导</div>
        <div style="font-size: 14px; color: #666">{{ shareGuideMessage }}</div>
        <van-button type="primary" round block style="margin-top: 16px" @click="showShareGuide = false">
          知道了
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.level-card {
  text-align: center;
  padding: 24px 16px;
  background: linear-gradient(135deg, #007AFF, #5856D6);
  color: white;

  .level-title {
    font-size: 24px;
    font-weight: 700;
  }

  .level-progress {
    margin-top: 12px;
    padding: 0 20px;

    .progress-text {
      font-size: 12px;
      opacity: 0.85;
      margin-top: 6px;
    }
  }
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 12px 16px;

  .data-item {
    text-align: center;
    padding: 16px 8px;
    margin: 0;

    .data-value {
      font-size: 20px;
      font-weight: 700;
      color: var(--color-text);
    }

    .data-label {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 4px;
    }
  }
}

.actions {
  padding: 16px;
}

.social-cards {
  padding: 0 16px 16px;

  .cards-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .social-card {
    text-align: center;
    padding: 16px 8px;
    margin: 0;
    cursor: pointer;
    transition: transform 0.15s;

    &:active { transform: scale(0.95); }

    .card-icon { font-size: 28px; margin-bottom: 6px; }
    .card-name { font-size: 12px; color: var(--color-text-secondary); }
  }
}

.bottom-actions {
  padding: 16px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom));
}
</style>
