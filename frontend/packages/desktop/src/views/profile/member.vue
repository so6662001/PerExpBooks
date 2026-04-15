<template>
  <div class="page-container member-page">
    <div class="page-header">
      <h2>会员管理</h2>
    </div>

    <el-card v-if="memberStatus" class="status-card">
      <div class="current-status">
        <div class="status-info">
          <h3>
            {{ memberStatus.memberStatus === 1 ? '会员' : '免费版' }}
          </h3>
          <p v-if="memberStatus.memberStatus === 1">
            到期时间：{{ memberStatus.expireTime }} (剩余 {{ memberStatus.daysLeft }} 天)
          </p>
          <p v-else>升级会员解锁更多功能</p>
        </div>
        <div class="quota-info" v-if="memberStatus.quotaInfo">
          <span>发票额度：{{ memberStatus.quotaInfo.monthlyInvoiceUsed }} / {{ memberStatus.quotaInfo.monthlyInvoiceLimit }}</span>
          <el-progress
            :percentage="memberStatus.quotaInfo.monthlyInvoiceLimit > 0 ? Math.round((memberStatus.quotaInfo.monthlyInvoiceUsed / memberStatus.quotaInfo.monthlyInvoiceLimit) * 100) : 0"
            :color="memberStatus.quotaInfo.monthlyInvoiceUsed > memberStatus.quotaInfo.monthlyInvoiceLimit * 0.8 ? '#FF9500' : '#007AFF'"
          />
        </div>
      </div>
    </el-card>

    <h3 class="section-title">选择套餐</h3>
    <el-row :gutter="16">
      <el-col :span="8" v-for="plan in plans" :key="plan.planType">
        <el-card
          :class="['plan-card', { recommended: plan.recommended }]"
          shadow="hover"
        >
          <div v-if="plan.recommended" class="recommend-badge">推荐</div>
          <h3 class="plan-name">{{ plan.name }}</h3>
          <div class="plan-price">
            <span class="price">¥{{ plan.price }}</span>
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
          <el-button
            :type="plan.recommended ? 'primary' : 'default'"
            style="width: 100%"
            @click="handleSubscribe(plan)"
          >
            {{ memberStatus?.memberStatus === 1 ? '续费/升级' : '立即开通' }}
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
  getPlans,
  getMemberStatus,
  createMemberOrder,
  type PlanVO,
  type MemberStatusVO,
} from '@qianku/shared'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const memberStatus = ref<MemberStatusVO | null>(null)
const plans = ref<PlanVO[]>([])

async function loadData() {
  try {
    const [statusData, plansData] = await Promise.all([
      getMemberStatus(),
      getPlans(),
    ])
    memberStatus.value = statusData
    plans.value = plansData
  } catch {
    // silent
  }
}

async function handleSubscribe(plan: PlanVO) {
  try {
    await createMemberOrder({
      planType: plan.planType,
      payType: 1,
    })
    ElMessage.success('订单创建成功')
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
