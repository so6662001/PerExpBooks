<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { sendSms, smsLogin, useUserStore, useAppStore } from '@qianku/shared'
import { Tracker } from '@qianku/shared/analytics'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const phone = ref('')
const code = ref('')
const agreed = ref(false)
const sending = ref(false)
const logging = ref(false)
const countdown = ref(0)
const inviteCode = ref('')
let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  const invite = (route.query.inviteCode || route.query.invite || '') as string
  if (invite) {
    inviteCode.value = invite
  }
})

const canSend = computed(() => phone.value.length === 11 && countdown.value === 0 && !sending.value)
const canLogin = computed(() => phone.value.length === 11 && code.value.length >= 4 && agreed.value && !logging.value)

async function handleSendSms() {
  if (!canSend.value) return
  if (!agreed.value) {
    showToast('请先同意用户协议')
    return
  }
  sending.value = true
  try {
    await sendSms({ phone: phone.value })
    showToast({ message: '验证码已发送', type: 'success' })
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer!)
        timer = null
      }
    }, 1000)
  } catch (e: any) {
    showToast(e.message || '发送失败')
  } finally {
    sending.value = false
  }
}

async function handleLogin() {
  if (!canLogin.value) return
  logging.value = true
  try {
    const result = await smsLogin({
      phone: phone.value,
      code: code.value,
      inviteCode: inviteCode.value || undefined,
    })
    userStore.setToken(result.token)
    await userStore.fetchProfile()

    if (result.agreementCheck) {
      appStore.setAgreementCheckFromLogin(result.agreementCheck)
    }

    try {
      const tracker = Tracker.getInstance()
      tracker.track(result.isNew ? 'user_register' : 'user_login', {
        loginType: 'sms',
        hasInviteCode: !!inviteCode.value,
      })
    } catch {
      // tracker may not be initialized
    }

    showToast({ message: '登录成功', type: 'success' })
    router.replace('/')
  } catch (e: any) {
    showToast(e.message || '登录失败')
  } finally {
    logging.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-header">
      <div class="logo">💰</div>
      <h1 class="app-name">钱库</h1>
      <p class="app-desc">智能费用管理助手</p>
    </div>

    <div class="login-form">
      <div class="form-item">
        <van-field
          v-model="phone"
          type="tel"
          maxlength="11"
          placeholder="请输入手机号"
          :border="false"
          class="input-field"
        >
          <template #left-icon>
            <span class="field-icon">📱</span>
          </template>
        </van-field>
      </div>

      <div class="form-item">
        <van-field
          v-model="code"
          type="number"
          maxlength="6"
          placeholder="请输入验证码"
          :border="false"
          class="input-field"
        >
          <template #left-icon>
            <span class="field-icon">🔐</span>
          </template>
          <template #button>
            <van-button
              size="small"
              type="primary"
              :disabled="!canSend"
              :loading="sending"
              round
              @click="handleSendSms"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </van-button>
          </template>
        </van-field>
      </div>

      <van-button
        type="primary"
        block
        round
        size="large"
        :disabled="!canLogin"
        :loading="logging"
        class="login-btn"
        @click="handleLogin"
      >
        登录
      </van-button>

      <div class="agreement-check">
        <van-checkbox v-model="agreed" icon-size="16px" checked-color="#007AFF">
          <span class="agreement-text">
            我已阅读并同意
            <a href="javascript:;">《用户协议》</a>
            和
            <a href="javascript:;">《隐私政策》</a>
          </span>
        </van-checkbox>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #F8F9FF 0%, var(--color-bg) 100%);
  display: flex;
  flex-direction: column;
  padding: 0 24px;
}

.login-header {
  text-align: center;
  padding-top: 100px;
  margin-bottom: 48px;

  .logo {
    font-size: 64px;
    margin-bottom: 16px;
  }

  .app-name {
    font-size: 32px;
    font-weight: 700;
    color: var(--color-text);
    letter-spacing: -0.5px;
  }

  .app-desc {
    font-size: 15px;
    color: var(--color-text-secondary);
    margin-top: 8px;
  }
}

.login-form {
  .form-item {
    margin-bottom: 16px;

    .input-field {
      background: var(--color-card);
      border-radius: var(--radius-md);
      padding: 4px 12px;
      box-shadow: var(--shadow-sm);

      .field-icon {
        font-size: 20px;
        margin-right: 4px;
      }
    }
  }

  .login-btn {
    margin-top: 24px;
    height: 50px;
    font-size: 17px;
    font-weight: 600;
  }

  .agreement-check {
    margin-top: 20px;
    display: flex;
    justify-content: center;

    .agreement-text {
      font-size: 12px;
      color: var(--color-text-secondary);

      a {
        color: var(--color-primary);
      }
    }
  }
}
</style>
