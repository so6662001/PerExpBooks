<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getPromotionInfo, getInviteRecords, formatAmount, copyToClipboard, formatDate } from '@qianku/shared'
import type { PromotionInfoVO, InviteRecordVO } from '@qianku/shared'

const router = useRouter()
const info = ref<PromotionInfoVO | null>(null)
const records = ref<InviteRecordVO[]>([])
const loading = ref(true)
const loadingRecords = ref(false)
const finished = ref(false)
const pageNum = ref(1)

onMounted(async () => {
  try {
    const [infoData, recordData] = await Promise.all([
      getPromotionInfo(),
      getInviteRecords({ pageNum: 1, pageSize: 20 }),
    ])
    info.value = infoData
    records.value = recordData.list
    if (records.value.length >= recordData.total) finished.value = true
    pageNum.value = 2
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

async function loadMoreRecords() {
  loadingRecords.value = true
  try {
    const result = await getInviteRecords({ pageNum: pageNum.value, pageSize: 20 })
    records.value.push(...result.list)
    if (records.value.length >= result.total) finished.value = true
    pageNum.value++
  } catch {
    finished.value = true
  } finally {
    loadingRecords.value = false
  }
}

async function copyCode() {
  if (!info.value) return
  try {
    await copyToClipboard(info.value.inviteCode)
    showToast({ message: '邀请码已复制', type: 'success' })
  } catch {
    showToast('复制失败')
  }
}

async function copyLink() {
  if (!info.value) return
  try {
    await copyToClipboard(info.value.inviteLink)
    showToast({ message: '邀请链接已复制', type: 'success' })
  } catch {
    showToast('复制失败')
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="推广中心" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="info">
      <div class="promo-header">
        <div class="promo-level">
          <span class="level-badge">{{ info.level }}</span>
          <span class="commission-rate">佣金比例 {{ (info.commissionRate * 100).toFixed(0) }}%</span>
        </div>
      </div>

      <div class="earnings-grid">
        <div class="earning-card card">
          <div class="earning-value">{{ formatAmount(info.totalEarnings) }}</div>
          <div class="earning-label">累计收益</div>
        </div>
        <div class="earning-card card">
          <div class="earning-value">{{ formatAmount(info.monthlyEarnings) }}</div>
          <div class="earning-label">本月收益</div>
        </div>
        <div class="earning-card card">
          <div class="earning-value">{{ info.inviteCount }}</div>
          <div class="earning-label">邀请人数</div>
        </div>
        <div class="earning-card card">
          <div class="earning-value">{{ info.pointsBalance }}</div>
          <div class="earning-label">积分余额</div>
        </div>
      </div>

      <div class="invite-section card">
        <h3 class="invite-title">邀请好友</h3>
        <div class="invite-item">
          <span class="invite-label">邀请码</span>
          <div class="invite-value">
            <span class="code-text">{{ info.inviteCode }}</span>
            <van-button type="primary" size="mini" round @click="copyCode">
              复制
            </van-button>
          </div>
        </div>
        <div class="invite-item">
          <span class="invite-label">邀请链接</span>
          <div class="invite-value">
            <span class="link-text">{{ info.inviteLink }}</span>
            <van-button type="primary" size="mini" round @click="copyLink">
              复制
            </van-button>
          </div>
        </div>
      </div>

      <div class="records-section">
        <div class="section-title">邀请记录</div>
        <van-list
          v-model:loading="loadingRecords"
          :finished="finished"
          finished-text="没有更多了"
          @load="loadMoreRecords"
        >
          <div
            v-for="record in records"
            :key="record.id"
            class="record-item card"
          >
            <van-image
              round
              width="40"
              height="40"
              :src="record.inviteeAvatar"
              fit="cover"
            >
              <template #error>
                <div class="avatar-mini">{{ (record.inviteeNickname || '?')[0] }}</div>
              </template>
            </van-image>
            <div class="record-info">
              <div class="record-name">{{ record.inviteeNickname }}</div>
              <div class="record-date">{{ formatDate(record.createdAt) }}</div>
            </div>
            <div class="record-right">
              <div class="record-commission">+{{ formatAmount(record.commission) }}</div>
              <span class="record-status" :class="record.status">
                {{ record.status === 'subscribed' ? '已订阅' : '已注册' }}
              </span>
            </div>
          </div>

          <div v-if="records.length === 0 && !loadingRecords" class="empty-state">
            <div class="empty-icon">📢</div>
            <div class="empty-text">暂无邀请记录</div>
          </div>
        </van-list>
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

.promo-header {
  text-align: center;
  padding: 24px 16px;
  background: linear-gradient(135deg, #007AFF, #5856D6);
  color: white;

  .level-badge {
    font-size: 24px;
    font-weight: 700;
  }

  .commission-rate {
    display: block;
    font-size: 14px;
    opacity: 0.85;
    margin-top: 6px;
  }
}

.earnings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 12px 16px;

  .earning-card {
    text-align: center;
    padding: 16px 8px;
    margin: 0;

    .earning-value {
      font-size: 20px;
      font-weight: 700;
      color: var(--color-text);
    }

    .earning-label {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 4px;
    }
  }
}

.invite-section {
  .invite-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .invite-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 0.5px solid var(--color-divider);

    &:last-child { border-bottom: none; }

    .invite-label {
      font-size: 14px;
      color: var(--color-text-secondary);
    }

    .invite-value {
      display: flex;
      align-items: center;
      gap: 8px;

      .code-text {
        font-size: 16px;
        font-weight: 600;
        font-family: monospace;
      }

      .link-text {
        font-size: 12px;
        color: var(--color-text-secondary);
        max-width: 140px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

.records-section {
  .record-item {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 0 16px 6px;
    padding: 12px;

    .avatar-mini {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: var(--color-primary);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      font-weight: 600;
    }

    .record-info {
      flex: 1;

      .record-name { font-size: 15px; font-weight: 500; }
      .record-date { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
    }

    .record-right {
      text-align: right;

      .record-commission {
        font-size: 15px;
        font-weight: 600;
        color: var(--color-warning);
      }

      .record-status {
        font-size: 11px;

        &.subscribed { color: var(--color-success); }
        &.registered { color: var(--color-text-secondary); }
      }
    }
  }
}
</style>
