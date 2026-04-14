<template>
  <div class="page-container member-page">
    <div class="page-header">
      <h2>会员管理</h2>
    </div>

    <el-card v-if="memberStatus" class="status-card">
      <div class="current-status">
        <div class="status-info">
          <h3>
            {{ memberStatus.isMember ? memberStatus.level + ' 会员' : '免费版' }}
          </h3>
          <p v-if="memberStatus.isMember">
            到期时间：{{ memberStatus.expireAt }} (剩余 {{ memberStatus.remainingDays }} 天)
          </p>
          <p v-else>升级会员解锁更多功能</p>
        </div>
        <div class="quota-info" v-if="memberStatus.isMember">
          <span>本月额度：{{ memberStatus.usedQuota }} / {{ memberStatus.monthlyQuota }}</span>
          <el-progress
            :percentage="Math.round((memberStatus.usedQuota / memberStatus.monthlyQuota) * 100)"
            :color="memberStatus.usedQuota > memberStatus.monthlyQuota * 0.8 ? '#FF9500' : '#007AFF'"
          />
        </div>
      </div>
    </el-card>

    <h3 class="section-title">选择套餐</h3>
    <el-row :gutter="16">
      <el-col :span="8" v-for="plan in plans" :key="plan.id">
        <el-card
          :class="['plan-card', { recommended: plan.recommended }]"
          shadow="hover"
        >
          <div v-if="plan.recommended" class="recommend-badge">推荐</div>
          <h3 class="plan-name">{{ plan.name }}</h3>
          <div class="plan-price">
            <span class="price">¥{{ plan.price }}</span>
            <span class="unit">/{{ plan.durationUnit === 'month' ? '月' : '年' }}</span>
          </div>
          <div v-if="plan.originalPrice > plan.price" class="original-price">
            原价 ¥{{ plan.originalPrice }}
          </div>
          <ul class="plan-features">
            <li v-for="(feature, i) in plan.features" :key="i">
              <el-icon color="#34C759"><Check /></el-icon>
              {{ feature }}
            </li>
          </ul>
          <div class="plan-quota">每月 {{ plan.monthlyQuota }} 次发票识别</div>
          <el-button
            :type="plan.recommended ? 'primary' : 'default'"
            style="width: 100%"
            @click="handleSubscribe(plan)"
          >
            {{ memberStatus?.isMember ? '续费/升级' : '立即开通' }}
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  useUserStore,
  getMemberPlans,
  createMemberOrder,
  type MemberPlanVO,
  type MemberStatusVO,
} from '@qianku/shared'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const memberStatus = ref<MemberStatusVO | null>(null)
const plans = ref<MemberPlanVO[]>([])

async function loadData() {
  try {
    await userStore.fetchMemberStatus()
    memberStatus.value = userStore.memberStatus
    plans.value = await getMemberPlans()
  } catch {
    // silent
  }
}

async function handleSubscribe(plan: MemberPlanVO) {
  try {
    const order = await createMemberOrder({
      planId: plan.id,
      paymentMethod: 'wechat',
    })
    if (order.paymentUrl) {
      window.open(order.paymentUrl, '_blank')
    }
    ElMessage.info('请在弹出的页面中完成支付')
  } catch {
    ElMessage.error('创建订单失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.member-page {
  .status-card {
    margin-bottom: 24px;

    .current-status {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .status-info {
        h3 {
          font-size: 20px;
          font-weight: 600;
          margin-bottom: 4px;
        }

        p {
          color: #86868b;
          font-size: 14px;
        }
      }

      .quota-info {
        width: 240px;

        span {
          font-size: 13px;
          color: #86868b;
          display: block;
          margin-bottom: 8px;
        }
      }
    }
  }

  .section-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 16px;
  }

  .plan-card {
    text-align: center;
    padding: 8px;
    position: relative;
    overflow: hidden;

    &.recommended {
      border: 2px solid #007AFF;
    }

    .recommend-badge {
      position: absolute;
      top: 12px;
      right: -28px;
      background: #007AFF;
      color: #fff;
      font-size: 12px;
      padding: 2px 32px;
      transform: rotate(45deg);
    }

    .plan-name {
      font-size: 18px;
      font-weight: 600;
      margin-bottom: 12px;
    }

    .plan-price {
      margin-bottom: 4px;

      .price {
        font-size: 36px;
        font-weight: 700;
        color: #007AFF;
      }

      .unit {
        font-size: 14px;
        color: #86868b;
      }
    }

    .original-price {
      font-size: 13px;
      color: #c7c7cc;
      text-decoration: line-through;
      margin-bottom: 16px;
    }

    .plan-features {
      list-style: none;
      padding: 0;
      margin: 16px 0;
      text-align: left;

      li {
        padding: 6px 0;
        font-size: 14px;
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .plan-quota {
      font-size: 13px;
      color: #86868b;
      margin-bottom: 16px;
    }
  }
}
</style>
