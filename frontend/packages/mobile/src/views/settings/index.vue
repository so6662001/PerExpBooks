<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { useUserStore } from '@qianku/shared'

defineOptions({ name: 'SettingsIndex' })

const router = useRouter()
const userStore = useUserStore()

const subsidyRate = ref(localStorage.getItem('default_subsidy_rate') || '200')
const financeEmail = ref(localStorage.getItem('finance_email') || '')
const showSubsidyPicker = ref(false)
const appVersion = '1.0.0'

const subsidyOptions = [
  { text: '100 元/天', value: '100' },
  { text: '150 元/天', value: '150' },
  { text: '200 元/天', value: '200' },
  { text: '250 元/天', value: '250' },
  { text: '300 元/天', value: '300' },
  { text: '400 元/天', value: '400' },
  { text: '500 元/天', value: '500' },
]

function onSubsidyConfirm({ selectedOptions }: any) {
  subsidyRate.value = selectedOptions[0]?.value || '200'
  localStorage.setItem('default_subsidy_rate', subsidyRate.value)
  showSubsidyPicker.value = false
  showToast({ message: '已保存', type: 'success' })
}

function saveEmail() {
  localStorage.setItem('finance_email', financeEmail.value)
  showToast({ message: '已保存', type: 'success' })
}

function goAgreement() {
  router.push('/agreement')
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
    <van-nav-bar title="设置" left-arrow @click-left="router.back()" />

    <div class="settings-group card">
      <van-field
        :model-value="`${subsidyRate} 元/天`"
        is-link
        readonly
        label="默认补贴标准"
        @click="showSubsidyPicker = true"
      />
      <van-field
        v-model="financeEmail"
        label="财务邮箱"
        placeholder="请输入常用财务邮箱"
        type="text"
        @blur="saveEmail"
      />
    </div>

    <div class="settings-group card">
      <van-cell title="协议与政策" is-link @click="goAgreement" />
      <van-cell title="关于" :value="`v${appVersion}`" />
    </div>

    <div class="logout-section">
      <van-button block round plain type="danger" @click="handleLogout">
        退出登录
      </van-button>
    </div>

    <van-popup v-model:show="showSubsidyPicker" position="bottom" round>
      <van-picker
        :columns="subsidyOptions"
        @confirm="onSubsidyConfirm"
        @cancel="showSubsidyPicker = false"
      />
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.settings-group {
  margin-top: 12px;

  &:first-of-type {
    margin-top: 12px;
  }
}

.logout-section {
  padding: 40px 24px;
}
</style>
