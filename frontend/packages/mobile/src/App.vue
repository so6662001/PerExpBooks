<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@qianku/shared'
import AgreementPopup from './components/AgreementPopup.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const tabItems = [
  { name: 'home', title: '首页', icon: 'home-o', path: '/' },
  { name: 'expense', title: '费用', icon: 'bill-o', path: '/expense' },
  { name: 'reimbursement', title: '报销', icon: 'orders-o', path: '/reimbursement' },
  { name: 'profile', title: '我的', icon: 'user-o', path: '/profile' },
]

const activeTab = computed(() => {
  const path = route.path
  if (path === '/') return 'home'
  const match = tabItems.find((item) => path.startsWith(item.path) && item.path !== '/')
  return match?.name || 'home'
})

const showTabBar = computed(() => {
  const hiddenPages = ['/auth/login', '/expense/upload', '/reimbursement/create', '/trip/create']
  return !hiddenPages.some((p) => route.path.startsWith(p))
})

function onTabChange(name: string | number) {
  const item = tabItems.find((t) => t.name === name)
  if (item) router.push(item.path)
}

onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) {
    appStore.checkAgreementStatus()
  }
})
</script>

<template>
  <div class="app-container">
    <router-view v-slot="{ Component }">
      <keep-alive :include="['Home', 'ExpenseList', 'ReimbursementList', 'Profile']">
        <component :is="Component" />
      </keep-alive>
    </router-view>

    <van-tabbar
      v-if="showTabBar"
      :model-value="activeTab"
      active-color="#007AFF"
      inactive-color="#999"
      @change="onTabChange"
    >
      <van-tabbar-item
        v-for="item in tabItems"
        :key="item.name"
        :name="item.name"
        :icon="item.icon"
      >
        {{ item.title }}
      </van-tabbar-item>
    </van-tabbar>

    <AgreementPopup />
  </div>
</template>

<style lang="scss" scoped>
.app-container {
  min-height: 100vh;
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
