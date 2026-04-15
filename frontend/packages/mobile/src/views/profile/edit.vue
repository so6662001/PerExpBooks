<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore, updateUserProfile } from '@qianku/shared'

defineOptions({ name: 'ProfileEdit' })

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const submitting = ref(false)

const form = ref({
  nickname: '',
  company: '',
  department: '',
})

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchProfile()
  }
  if (userStore.userInfo) {
    form.value.nickname = userStore.userInfo.nickname || ''
    form.value.company = userStore.userInfo.company || ''
    form.value.department = userStore.userInfo.department || ''
  }
  loading.value = false
})

async function handleSubmit() {
  if (!form.value.nickname.trim()) {
    showToast('请输入昵称')
    return
  }
  submitting.value = true
  try {
    await updateUserProfile({
      nickname: form.value.nickname,
      company: form.value.company,
      department: form.value.department,
    })
    await userStore.fetchProfile()
    showToast({ message: '保存成功', type: 'success' })
    router.back()
  } catch (e: any) {
    showToast(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="编辑个人信息" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="avatar-section">
        <van-image
          round
          width="80"
          height="80"
          :src="userStore.userInfo?.avatarUrl || ''"
          fit="cover"
        >
          <template #error>
            <div class="avatar-placeholder">
              {{ (form.nickname || '用')[0] }}
            </div>
          </template>
        </van-image>
      </div>

      <div class="form-section card">
        <van-field
          v-model="form.nickname"
          label="昵称"
          placeholder="请输入昵称"
          required
        />
        <van-field
          v-model="form.company"
          label="公司名称"
          placeholder="请输入公司名称"
        />
        <van-field
          v-model="form.department"
          label="部门"
          placeholder="请输入部门"
        />
      </div>

      <div class="submit-bar">
        <van-button
          type="primary"
          block
          round
          size="large"
          :loading="submitting"
          @click="handleSubmit"
        >
          保存
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

.avatar-section {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px;

  .avatar-placeholder {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: linear-gradient(135deg, #007AFF, #5856D6);
    color: white;
    font-size: 32px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.form-section {
  margin-top: 12px;
}

.submit-bar {
  padding: 24px 16px;
}
</style>
