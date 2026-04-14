<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyCoupons, formatAmount, formatDate } from '@qianku/shared'
import type { CouponVO } from '@qianku/shared'

defineOptions({ name: 'CouponList' })

const router = useRouter()
const coupons = ref<CouponVO[]>([])
const loading = ref(true)
const activeTab = ref('available')

onMounted(async () => {
  try {
    coupons.value = await getMyCoupons()
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

const availableCoupons = computed(() =>
  coupons.value.filter(c => !c.used && new Date(c.expireAt) > new Date()),
)

const usedCoupons = computed(() =>
  coupons.value.filter(c => c.used),
)

const expiredCoupons = computed(() =>
  coupons.value.filter(c => !c.used && new Date(c.expireAt) <= new Date()),
)

const currentList = computed(() => {
  if (activeTab.value === 'available') return availableCoupons.value
  if (activeTab.value === 'used') return usedCoupons.value
  return expiredCoupons.value
})

function getCouponValue(coupon: CouponVO) {
  if (coupon.type === 'discount') return `${coupon.value}折`
  return formatAmount(coupon.value)
}

function getCouponCondition(coupon: CouponVO) {
  if (coupon.minAmount > 0) return `满${formatAmount(coupon.minAmount)}可用`
  return '无门槛'
}

function goUse() {
  router.push('/member')
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="我的优惠券" left-arrow @click-left="router.back()" />

    <van-tabs v-model:active="activeTab" sticky shrink>
      <van-tab name="available" :title="`可用 (${availableCoupons.length})`" />
      <van-tab name="used" :title="`已使用 (${usedCoupons.length})`" />
      <van-tab name="expired" :title="`已过期 (${expiredCoupons.length})`" />
    </van-tabs>

    <van-loading v-if="loading" class="page-loading" />

    <div v-if="!loading" class="coupon-list">
      <div
        v-for="coupon in currentList"
        :key="coupon.id"
        class="coupon-card"
        :class="{ disabled: activeTab !== 'available' }"
      >
        <div class="coupon-left">
          <div class="coupon-value">{{ getCouponValue(coupon) }}</div>
          <div class="coupon-condition">{{ getCouponCondition(coupon) }}</div>
        </div>
        <div class="coupon-divider" />
        <div class="coupon-right">
          <div class="coupon-code">{{ coupon.code }}</div>
          <div class="coupon-expire">有效期至 {{ formatDate(coupon.expireAt) }}</div>
          <van-button
            v-if="activeTab === 'available'"
            type="primary"
            size="mini"
            round
            @click="goUse"
          >
            去使用
          </van-button>
          <span v-else-if="activeTab === 'used'" class="used-label">已使用</span>
          <span v-else class="expired-label">已过期</span>
        </div>
      </div>

      <div v-if="currentList.length === 0" class="empty-state">
        <div class="empty-icon">🎫</div>
        <div class="empty-text">
          {{ activeTab === 'available' ? '暂无可用优惠券' : activeTab === 'used' ? '暂无已使用优惠券' : '暂无已过期优惠券' }}
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.coupon-list {
  padding: 12px 16px;
}

.coupon-card {
  display: flex;
  align-items: stretch;
  background: var(--color-card);
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);

  &.disabled {
    opacity: 0.55;
  }

  .coupon-left {
    width: 100px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #007AFF, #5856D6);
    padding: 16px 8px;

    .coupon-value {
      font-size: 22px;
      font-weight: 700;
      color: #fff;
    }

    .coupon-condition {
      font-size: 11px;
      color: rgba(255, 255, 255, 0.8);
      margin-top: 4px;
    }
  }

  .coupon-divider {
    width: 1px;
    background: repeating-linear-gradient(
      to bottom,
      transparent,
      transparent 4px,
      #e0e0e0 4px,
      #e0e0e0 8px
    );
  }

  .coupon-right {
    flex: 1;
    padding: 14px 16px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .coupon-code {
      font-size: 15px;
      font-weight: 600;
      font-family: monospace;
    }

    .coupon-expire {
      font-size: 12px;
      color: var(--color-text-tertiary);
    }

    .van-button {
      align-self: flex-start;
      margin-top: 4px;
    }

    .used-label, .expired-label {
      font-size: 12px;
      color: var(--color-text-tertiary);
      margin-top: 4px;
    }
  }
}
</style>
