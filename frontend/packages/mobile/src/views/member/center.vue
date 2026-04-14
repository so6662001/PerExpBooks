<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMemberPlans, createMemberOrder, useUserStore, formatAmount } from '@qianku/shared'
import type { MemberPlanVO } from '@qianku/shared'

const router = useRouter()
const userStore = useUserStore()
const plans = ref<MemberPlanVO[]>([])
const loading = ref(true)
const ordering = ref(false)

onMounted(async () => {
  try {
    const [plansData] = await Promise.all([
      getMemberPlans(),
      userStore.fetchMemberStatus(),
    ])
    plans.value = plansData
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

async function handleSubscribe(plan: MemberPlanVO) {
  ordering.value = true
  try {
    const result = await createMemberOrder({
      planId: plan.id,
      paymentMethod: 'wechat',
    })
    if (result.paymentUrl) {
      window.location.href = result.paymentUrl
    }
  } catch (e: any) {
    showToast(e.message || '创建订单失败')
  } finally {
    ordering.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="会员中心" left-arrow @click-left="router.back()" />

    <div class="member-status card">
      <div class="status-header">
        <span class="status-icon">👑</span>
        <div class="status-info">
          <div class="status-title">
            {{ userStore.memberStatus?.isMember ? userStore.memberStatus.level + ' 会员' : '未开通会员' }}
          </div>
          <div v-if="userStore.memberStatus?.isMember" class="status-expire">
            到期时间：{{ userStore.memberStatus?.expireAt }}
          </div>
        </div>
      </div>
      <div v-if="userStore.memberStatus?.isMember" class="quota-info">
        <div class="quota-bar">
          <div
            class="quota-fill"
            :style="{ width: `${((userStore.memberStatus?.usedQuota || 0) / (userStore.memberStatus?.monthlyQuota || 1)) * 100}%` }"
          />
        </div>
        <div class="quota-text">
          本月已用 {{ userStore.memberStatus?.usedQuota }} / {{ userStore.memberStatus?.monthlyQuota }} 次
        </div>
      </div>
    </div>

    <div class="plans-section">
      <div class="section-title">选择套餐</div>
      <div class="plans-grid">
        <div
          v-for="plan in plans"
          :key="plan.id"
          class="plan-card card"
          :class="{ recommended: plan.recommended }"
        >
          <div v-if="plan.recommended" class="recommend-badge">推荐</div>
          <div class="plan-name">{{ plan.name }}</div>
          <div class="plan-price">
            <span class="price-current">{{ formatAmount(plan.price) }}</span>
            <span v-if="plan.originalPrice > plan.price" class="price-original">
              {{ formatAmount(plan.originalPrice) }}
            </span>
          </div>
          <div class="plan-duration">
            {{ plan.duration }}{{ plan.durationUnit === 'month' ? '个月' : '年' }}
          </div>
          <ul class="plan-features">
            <li v-for="(feat, i) in plan.features" :key="i">✓ {{ feat }}</li>
          </ul>
          <van-button
            :type="plan.recommended ? 'primary' : 'default'"
            block
            round
            size="small"
            :loading="ordering"
            @click="handleSubscribe(plan)"
          >
            {{ userStore.memberStatus?.isMember ? '续费' : '开通' }}
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.member-status {
  .status-header {
    display: flex;
    align-items: center;
    gap: 12px;

    .status-icon {
      font-size: 36px;
    }

    .status-info {
      .status-title {
        font-size: 18px;
        font-weight: 600;
      }

      .status-expire {
        font-size: 13px;
        color: var(--color-text-secondary);
        margin-top: 2px;
      }
    }
  }

  .quota-info {
    margin-top: 16px;

    .quota-bar {
      height: 6px;
      background: var(--color-bg);
      border-radius: 3px;
      overflow: hidden;

      .quota-fill {
        height: 100%;
        background: linear-gradient(90deg, #007AFF, #5856D6);
        border-radius: 3px;
        transition: width 0.3s;
      }
    }

    .quota-text {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 6px;
    }
  }
}

.plans-section {
  .plans-grid {
    display: grid;
    grid-template-columns: repeat(1, 1fr);
    gap: 12px;
    padding: 0 16px;
  }

  .plan-card {
    position: relative;
    text-align: center;
    padding: 24px 16px;
    margin: 0;
    border: 2px solid transparent;
    transition: border-color 0.2s;

    &.recommended {
      border-color: var(--color-primary);
    }

    .recommend-badge {
      position: absolute;
      top: -1px;
      right: 16px;
      background: var(--color-primary);
      color: white;
      font-size: 11px;
      font-weight: 600;
      padding: 2px 10px;
      border-radius: 0 0 6px 6px;
    }

    .plan-name {
      font-size: 18px;
      font-weight: 600;
    }

    .plan-price {
      margin-top: 8px;

      .price-current {
        font-size: 28px;
        font-weight: 700;
        color: var(--color-primary);
      }

      .price-original {
        font-size: 14px;
        color: var(--color-text-tertiary);
        text-decoration: line-through;
        margin-left: 6px;
      }
    }

    .plan-duration {
      font-size: 13px;
      color: var(--color-text-secondary);
      margin-top: 4px;
    }

    .plan-features {
      list-style: none;
      padding: 0;
      margin: 16px 0;
      text-align: left;

      li {
        font-size: 13px;
        color: var(--color-text-secondary);
        line-height: 2;
      }
    }
  }
}
</style>
