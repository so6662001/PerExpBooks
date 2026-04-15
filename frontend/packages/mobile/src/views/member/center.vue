<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getPlans, getMemberStatus, createMemberOrder, getAvailableCoupons, useUserStore, formatAmount } from '@qianku/shared'
import type { PlanVO, MemberStatusVO, CouponVO, CreateOrderDTO } from '@qianku/shared'

const router = useRouter()
const userStore = useUserStore()

const plans = ref<PlanVO[]>([])
const status = ref<MemberStatusVO | null>(null)
const coupons = ref<CouponVO[]>([])
const loading = ref(true)
const ordering = ref(false)

const selectedPlan = ref<number>(2)
const selectedCoupon = ref<CouponVO | null>(null)
const showCouponPicker = ref(false)
const payType = ref<number>(1)
const teamMemberCount = ref<number>(5)

const currentPlan = computed(() => plans.value.find(p => p.planType === selectedPlan.value))

const finalAmount = computed(() => {
  if (!currentPlan.value) return 0
  let price = currentPlan.value.price
  if (selectedPlan.value === 3) {
    price = price * teamMemberCount.value
  }
  if (selectedCoupon.value) {
    price = Math.max(0, price - selectedCoupon.value.discountValue)
  }
  return price
})

const quota = computed(() => {
  if (!status.value?.quotaInfo) {
    return { invoiceUsed: 0, invoiceLimit: 5, reimburseUsed: 0, reimburseLimit: 2 }
  }
  const q = status.value.quotaInfo
  return {
    invoiceUsed: q.monthlyInvoiceUsed,
    invoiceLimit: q.monthlyInvoiceLimit,
    reimburseUsed: q.monthlyReimburseUsed,
    reimburseLimit: q.monthlyReimburseLimit,
  }
})

watch(selectedPlan, async (val) => {
  selectedCoupon.value = null
  try {
    coupons.value = await getAvailableCoupons({ planType: val })
  } catch (e) {
    console.error(e)
  }
})

onMounted(async () => {
  try {
    const [plansData, statusData] = await Promise.all([
      getPlans(),
      getMemberStatus(),
    ])
    plans.value = plansData
    status.value = statusData
    if (plansData.length > 0) {
      const rec = plansData.find(p => p.recommended)
      selectedPlan.value = rec ? rec.planType : plansData[0].planType
    }
    coupons.value = await getAvailableCoupons({ planType: selectedPlan.value })
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function selectCoupon(c: CouponVO) {
  selectedCoupon.value = c
  showCouponPicker.value = false
}

async function handlePay() {
  if (!currentPlan.value) return
  ordering.value = true
  try {
    const data: CreateOrderDTO = {
      planType: selectedPlan.value,
      payType: payType.value,
    }
    if (selectedCoupon.value) {
      data.couponId = selectedCoupon.value.id
    }
    if (selectedPlan.value === 3) {
      data.teamMemberCount = teamMemberCount.value
    }
    await createMemberOrder(data)
    showToast('订单创建成功')
    router.push('/member/orders')
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
        <span class="status-icon">&#x1F451;</span>
        <div class="status-info">
          <div class="status-title">
            {{ status?.memberStatus === 1 ? '会员' : '未开通会员' }}
          </div>
          <div v-if="status?.memberStatus === 1" class="status-expire">
            到期时间：{{ status?.expireTime }} | 剩余{{ status?.daysLeft }}天
          </div>
        </div>
      </div>
    </div>

    <van-cell-group title="当前额度">
      <van-cell title="发票上传" :value="quota.invoiceUsed + '/' + quota.invoiceLimit + ' 张'" />
      <van-cell title="报销单" :value="quota.reimburseUsed + '/' + quota.reimburseLimit + ' 张'" />
    </van-cell-group>

    <div class="plans-section">
      <div class="section-title">选择套餐</div>
      <div class="plan-cards">
        <div
          v-for="plan in plans"
          :key="plan.planType"
          class="plan-card card"
          :class="{ recommended: plan.recommended, active: plan.planType === selectedPlan }"
          @click="selectedPlan = plan.planType"
        >
          <div v-if="plan.recommended" class="recommend-badge">推荐</div>
          <div class="plan-name">{{ plan.name }}</div>
          <div class="plan-price">
            <span class="price-current">{{ formatAmount(plan.price) }}</span>
            <span v-if="plan.originalPrice > plan.price" class="price-original">
              {{ formatAmount(plan.originalPrice) }}
            </span>
          </div>
          <ul class="plan-features">
            <li v-for="(f, i) in plan.features" :key="i">{{ f }}</li>
          </ul>
          <van-button
            :type="plan.planType === selectedPlan ? 'primary' : 'default'"
            block round size="small"
          >
            选择
          </van-button>
        </div>
      </div>
    </div>

    <div v-if="selectedPlan === 3" class="team-count">
      <van-cell-group>
        <van-stepper v-model="teamMemberCount" :min="5" :max="100" />
        <van-cell title="团队人数" :value="teamMemberCount + '人'" />
      </van-cell-group>
    </div>

    <van-cell
      title="优惠券"
      is-link
      @click="showCouponPicker = true"
      :value="selectedCoupon ? '-' + formatAmount(selectedCoupon.discountValue) : (coupons.length > 0 ? coupons.length + '张可用' : '无可用优惠券')"
    />

    <van-cell-group title="支付方式">
      <van-radio-group v-model="payType">
        <van-cell clickable @click="payType = 1">
          <template #title>微信支付</template>
          <template #right-icon><van-radio :name="1" /></template>
        </van-cell>
        <van-cell clickable @click="payType = 2">
          <template #title>支付宝</template>
          <template #right-icon><van-radio :name="2" /></template>
        </van-cell>
      </van-radio-group>
    </van-cell-group>

    <div class="pay-summary">
      <span class="total">合计: {{ formatAmount(finalAmount) }}</span>
      <van-button type="primary" round :loading="ordering" @click="handlePay">
        立即开通
      </van-button>
    </div>

    <van-popup v-model:show="showCouponPicker" position="bottom" round>
      <div class="coupon-picker">
        <div class="coupon-picker-header">选择优惠券</div>
        <div v-if="coupons.length === 0" class="coupon-empty">暂无可用优惠券</div>
        <div
          v-for="c in coupons"
          :key="c.id"
          class="coupon-item"
          @click="selectCoupon(c)"
        >
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-value">-{{ formatAmount(c.discountValue) }}</div>
        </div>
        <van-button block @click="selectedCoupon = null; showCouponPicker = false">不使用优惠券</van-button>
      </div>
    </van-popup>
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
}

.plans-section {
  .section-title {
    padding: 16px 16px 8px;
    font-size: 16px;
    font-weight: 600;
  }

  .plan-cards {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    padding: 0 16px;
  }

  .plan-card {
    position: relative;
    text-align: center;
    padding: 16px 8px;
    margin: 0;
    border: 2px solid transparent;
    border-radius: 8px;
    cursor: pointer;
    transition: border-color 0.2s;

    &.active {
      border-color: var(--color-primary);
    }

    &.recommended {
      border-color: var(--color-primary);
    }

    .recommend-badge {
      position: absolute;
      top: -1px;
      right: 8px;
      background: var(--color-primary);
      color: white;
      font-size: 10px;
      font-weight: 600;
      padding: 2px 6px;
      border-radius: 0 0 4px 4px;
    }

    .plan-name {
      font-size: 14px;
      font-weight: 600;
    }

    .plan-price {
      margin-top: 6px;

      .price-current {
        font-size: 20px;
        font-weight: 700;
        color: var(--color-primary);
      }

      .price-original {
        display: block;
        font-size: 12px;
        color: var(--color-text-tertiary);
        text-decoration: line-through;
      }
    }

    .plan-features {
      list-style: none;
      padding: 0;
      margin: 10px 0;
      text-align: left;

      li {
        font-size: 11px;
        color: var(--color-text-secondary);
        line-height: 1.8;
      }
    }
  }
}

.team-count {
  padding: 8px 16px;
}

.pay-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);

  .total {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-primary);
  }
}

.coupon-picker {
  padding: 16px;

  .coupon-picker-header {
    font-size: 16px;
    font-weight: 600;
    text-align: center;
    margin-bottom: 16px;
  }

  .coupon-empty {
    text-align: center;
    color: var(--color-text-tertiary);
    padding: 24px;
  }

  .coupon-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    border-bottom: 1px solid #eee;
    cursor: pointer;

    .coupon-value {
      color: #ff4d4f;
      font-weight: 600;
    }
  }
}
</style>
