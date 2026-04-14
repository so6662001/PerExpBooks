<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo" @click="router.push('/dashboard')">
        <span class="logo-icon">💰</span>
        <span v-show="!isCollapsed" class="logo-text">钱酷报销</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        class="sidebar-menu"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>

        <el-sub-menu index="expense">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>费用管理</span>
          </template>
          <el-menu-item index="/expense/list">费用列表</el-menu-item>
          <el-menu-item index="/expense/upload">上传发票</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/trip/list">
          <el-icon><Location /></el-icon>
          <template #title>出差管理</template>
        </el-menu-item>

        <el-sub-menu index="reimbursement">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>报销中心</span>
          </template>
          <el-menu-item index="/reimbursement/list">报销单列表</el-menu-item>
          <el-menu-item index="/reimbursement/create">创建报销单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="stats">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>统计分析</span>
          </template>
          <el-menu-item index="/stats/monthly">月度趋势</el-menu-item>
          <el-menu-item index="/stats/category">分类分析</el-menu-item>
          <el-menu-item index="/stats/trip">出差统计</el-menu-item>
          <el-menu-item index="/stats/progress">报销进度</el-menu-item>
          <el-menu-item index="/stats/yearly">年度对比</el-menu-item>
          <el-menu-item index="/stats/city">城市排行</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/team/members">
          <el-icon><UserFilled /></el-icon>
          <template #title>团队管理</template>
        </el-menu-item>

        <el-sub-menu index="profile">
          <template #title>
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/profile">个人信息</el-menu-item>
          <el-menu-item index="/profile/member">会员管理</el-menu-item>
          <el-menu-item index="/profile/promotion">推广中心</el-menu-item>
          <el-menu-item index="/profile/settings">系统设置</el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="sidebar-footer" @click="isCollapsed = !isCollapsed">
        <el-icon :size="18">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
        <span v-show="!isCollapsed">收起菜单</span>
      </div>
    </el-aside>

    <el-container class="main-area">
      <el-header class="top-header">
        <div class="header-left">
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content-area">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore, logout as logoutApi } from '@qianku/shared'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || '钱酷报销')

onMounted(() => {
  userStore.fetchProfile()
})

function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/profile/settings')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(async () => {
        try {
          await logoutApi()
        } finally {
          userStore.logout()
          router.push('/auth/login')
        }
      }).catch(() => {})
      break
  }
}
</script>

<style lang="scss" scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    cursor: pointer;
    border-bottom: 1px solid #f5f5f5;
    flex-shrink: 0;

    .logo-icon {
      font-size: 24px;
    }

    .logo-text {
      font-size: 18px;
      font-weight: 700;
      background: linear-gradient(135deg, #007AFF, #5856D6);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      white-space: nowrap;
    }
  }

  .sidebar-menu {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 8px 0;
  }

  .sidebar-footer {
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-top: 1px solid #f5f5f5;
    cursor: pointer;
    color: #86868b;
    font-size: 13px;
    flex-shrink: 0;
    transition: background 0.2s;

    &:hover {
      background: #f5f5f7;
    }
  }
}

.main-area {
  background: #F2F2F7;
  overflow: hidden;
}

.top-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;

  .header-left {
    .page-title {
      font-size: 16px;
      font-weight: 600;
      color: #1d1d1f;
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 8px;
      transition: background 0.2s;

      &:hover {
        background: #f5f5f7;
      }

      .username {
        font-size: 14px;
        color: #1d1d1f;
      }
    }
  }
}

.content-area {
  padding: 0;
  overflow-y: auto;
}
</style>
