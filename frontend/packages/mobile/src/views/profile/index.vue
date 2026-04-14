<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog } from 'vant'
import { useUserStore, formatPhone } from '@qianku/shared'

defineOptions({ name: 'Profile' })

const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { icon: '👑', title: '会员中心', path: '/member', color: '#FF9500' },
  { icon: '📢', title: '推广中心', path: '/promotion', color: '#007AFF' },
  { icon: '✈️', title: '出差管理', path: '/trip', color: '#5856D6' },
  { icon: '📊', title: '统计报表', path: '/stats', color: '#34C759' },
]

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchProfile()
  }
  if (!userStore.memberStatus) {
    userStore.fetchMemberStatus()
  }
})

function goTo(path: string) {
  router.push(path)
}

async function handleLogout() {
  try {
    await showDialog({
      title: '提示',
      message: '确定要退出登录吗？',
      showCancelButton: true,
      confirmButtonColor: '#FF3B30',
    })
    userStore.logout()
    router.replace('/auth/login')
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <span class="page-title">我的</span>
    </div>

    <div class="profile-card card">
      <div class="profile-info">
        <van-image
          round
          width="60"
          height="60"
          :src="userStore.userInfo?.avatar || ''"
          fit="cover"
        >
          <template #error>
            <div class="avatar-placeholder">
              {{ (userStore.userInfo?.nickname || '用')[0] }}
            </div>
          </template>
        </van-image>
        <div class="profile-detail">
          <div class="profile-name">{{ userStore.userInfo?.nickname || '未设置昵称' }}</div>
          <div class="profile-phone">{{ formatPhone(userStore.userInfo?.phone || '') }}</div>
          <div v-if="userStore.memberStatus?.isMember" class="member-badge">
            👑 {{ userStore.memberStatus.level }} 会员
          </div>
        </div>
      </div>
    </div>

    <div class="menu-section">
      <div
        v-for="item in menuItems"
        :key="item.path"
        class="menu-item card"
        @click="goTo(item.path)"
      >
        <div class="menu-icon" :style="{ background: item.color + '15' }">
          {{ item.icon }}
        </div>
        <span class="menu-title">{{ item.title }}</span>
        <van-icon name="arrow" class="menu-arrow" />
      </div>
    </div>

    <div class="logout-section">
      <van-button
        block
        round
        plain
        type="danger"
        @click="handleLogout"
      >
        退出登录
      </van-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.profile-card {
  margin-top: 12px;

  .profile-info {
    display: flex;
    align-items: center;
    gap: 16px;

    .avatar-placeholder {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      background: linear-gradient(135deg, #007AFF, #5856D6);
      color: white;
      font-size: 24px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .profile-detail {
      .profile-name {
        font-size: 20px;
        font-weight: 600;
      }

      .profile-phone {
        font-size: 14px;
        color: var(--color-text-secondary);
        margin-top: 2px;
      }

      .member-badge {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        margin-top: 6px;
        padding: 2px 10px;
        background: linear-gradient(135deg, #FFF3E0, #FFE0B2);
        border-radius: 12px;
        font-size: 12px;
        font-weight: 500;
        color: #E65100;
      }
    }
  }
}

.menu-section {
  margin-top: 8px;

  .menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 0 16px 8px;
    padding: 14px 16px;
    cursor: pointer;
    transition: transform 0.15s;

    &:active { transform: scale(0.98); }

    .menu-icon {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .menu-title {
      flex: 1;
      font-size: 16px;
      font-weight: 500;
    }

    .menu-arrow {
      color: var(--color-text-tertiary);
    }
  }
}

.logout-section {
  padding: 32px 24px;
}
</style>
