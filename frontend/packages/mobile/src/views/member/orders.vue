<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { formatAmount, formatDate, get } from '@qianku/shared'

defineOptions({ name: 'MemberOrders' })

const router = useRouter()
const loading = ref(true)

interface OrderItem {
  orderId: string
  planName: string
  amount: number
  status: 'pending' | 'paid' | 'cancelled' | 'refunded'
  createdAt: string
}

const orders = ref<OrderItem[]>([])

onMounted(async () => {
  try {
    const result = await get<{ list: OrderItem[] }>('/member/orders', { params: { pageNum: 1, pageSize: 50 } })
    orders.value = result.list || []
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '待支付',
    paid: '已支付',
    cancelled: '已取消',
    refunded: '已退款',
  }
  return map[status] || status
}

function getStatusColor(status: string) {
  const map: Record<string, string> = {
    pending: '#FF9500',
    paid: '#34C759',
    cancelled: '#999',
    refunded: '#FF3B30',
  }
  return map[status] || '#999'
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="我的订单" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div v-for="order in orders" :key="order.orderId" class="order-card card">
        <div class="order-header">
          <span class="order-no">订单号: {{ order.orderId.slice(0, 16) }}</span>
          <span class="order-status" :style="{ color: getStatusColor(order.status) }">
            {{ getStatusLabel(order.status) }}
          </span>
        </div>
        <div class="order-body">
          <div class="order-plan">{{ order.planName }}</div>
          <div class="order-amount amount">{{ formatAmount(order.amount) }}</div>
        </div>
        <div class="order-footer">
          <span class="order-date">{{ formatDate(order.createdAt, 'YYYY-MM-DD HH:mm') }}</span>
        </div>
      </div>

      <div v-if="orders.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无订单记录</div>
        <van-button type="primary" size="small" round @click="router.push('/member')" style="margin-top: 16px">
          去开通会员
        </van-button>
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

.order-card {
  margin: 0 16px 10px;
  padding: 16px;

  &:first-of-type {
    margin-top: 12px;
  }

  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .order-no {
      font-size: 12px;
      color: var(--color-text-tertiary);
      font-family: monospace;
    }

    .order-status {
      font-size: 13px;
      font-weight: 500;
    }
  }

  .order-body {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .order-plan {
      font-size: 16px;
      font-weight: 600;
    }

    .order-amount {
      font-size: 18px;
      font-weight: 700;
      color: var(--color-primary);
    }
  }

  .order-footer {
    margin-top: 10px;
    padding-top: 10px;
    border-top: 0.5px solid var(--color-divider);

    .order-date {
      font-size: 12px;
      color: var(--color-text-secondary);
    }
  }
}
</style>
