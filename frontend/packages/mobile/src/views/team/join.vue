<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyTeam, post } from '@qianku/shared'
import type { TeamVO } from '@qianku/shared'

defineOptions({ name: 'TeamJoin' })

const route = useRoute()
const router = useRouter()
const team = ref<TeamVO | null>(null)
const loading = ref(true)
const joining = ref(false)
const joined = ref(false)
const code = ref('')

onMounted(async () => {
  code.value = (route.query.code as string) || ''
  if (!code.value) {
    showToast('邀请码无效')
    loading.value = false
    return
  }
  try {
    team.value = await getMyTeam()
  } catch {
    // not in a team yet, which is expected
  } finally {
    loading.value = false
  }
})

async function handleJoin() {
  if (!code.value) return
  joining.value = true
  try {
    await post('/team/join', { code: code.value })
    showToast({ message: '加入成功', type: 'success' })
    joined.value = true
    setTimeout(() => router.replace('/team'), 1500)
  } catch (e: any) {
    showToast(e.message || '加入失败')
  } finally {
    joining.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="加入团队" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="join-card card">
        <div class="join-icon">👥</div>
        <div class="join-title">邀请你加入团队</div>
        <div class="join-code">邀请码：{{ code }}</div>

        <div v-if="joined" class="join-success">
          <van-icon name="checked" size="48" color="#34C759" />
          <div class="success-text">已成功加入团队</div>
        </div>

        <van-button
          v-if="!joined"
          type="primary"
          block
          round
          size="large"
          :loading="joining"
          @click="handleJoin"
          style="margin-top: 24px"
        >
          确认加入
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

.join-card {
  margin: 40px 16px 0;
  text-align: center;
  padding: 40px 24px;

  .join-icon {
    font-size: 56px;
    margin-bottom: 16px;
  }

  .join-title {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .join-code {
    font-size: 14px;
    color: var(--color-text-secondary);
    font-family: monospace;
  }

  .join-success {
    margin-top: 24px;

    .success-text {
      font-size: 16px;
      font-weight: 500;
      color: #34C759;
      margin-top: 12px;
    }
  }
}
</style>
