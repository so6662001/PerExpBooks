<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="brand">
          <span class="brand-icon">💰</span>
          <h1>钱酷报销</h1>
          <p>智能费用管理平台</p>
        </div>
        <div class="features">
          <div class="feature-item">
            <el-icon :size="24" color="#007AFF"><Camera /></el-icon>
            <div>
              <h4>智能发票识别</h4>
              <p>AI自动识别发票信息，一键录入</p>
            </div>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#34C759"><DataAnalysis /></el-icon>
            <div>
              <h4>数据分析</h4>
              <p>多维度统计分析，费用一目了然</p>
            </div>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#FF9500"><Files /></el-icon>
            <div>
              <h4>一键报销</h4>
              <p>快速生成报销单，支持PDF导出</p>
            </div>
          </div>
        </div>
      </div>

      <div class="login-right">
        <div class="login-form-container">
          <h2>登录</h2>
          <p class="subtitle">使用手机号验证码登录</p>

          <el-form :model="form" class="login-form">
            <el-form-item>
              <el-input
                v-model="form.phone"
                placeholder="请输入手机号"
                maxlength="11"
                size="large"
              >
                <template #prefix>
                  <el-icon><Phone /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <div class="code-row">
                <el-input
                  v-model="form.code"
                  placeholder="验证码"
                  maxlength="6"
                  size="large"
                >
                  <template #prefix>
                    <el-icon><Message /></el-icon>
                  </template>
                </el-input>
                <el-button
                  size="large"
                  :disabled="countdown > 0"
                  :loading="sendingCode"
                  @click="handleSendCode"
                >
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.inviteCode"
                placeholder="邀请码（选填）"
                size="large"
              >
                <template #prefix>
                  <el-icon><Ticket /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                style="width: 100%"
                :loading="logging"
                @click="handleLogin"
              >
                登录 / 注册
              </el-button>
            </el-form-item>
          </el-form>

          <p class="agreement-text">
            登录即表示同意
            <el-button link type="primary" size="small">《用户协议》</el-button>
            和
            <el-button link type="primary" size="small">《隐私政策》</el-button>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import {
  useUserStore,
  useAppStore,
  sendSms,
  smsLogin,
} from '@qianku/shared'
import { Tracker } from '@qianku/shared/analytics'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const form = reactive({
  phone: '',
  code: '',
  inviteCode: '',
})

const sendingCode = ref(false)
const logging = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

async function handleSendCode() {
  if (!form.phone || form.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  sendingCode.value = true
  try {
    await sendSms({ phone: form.phone })
    ElMessage.success('验证码已发送')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch {
    ElMessage.error('发送失败，请重试')
  } finally {
    sendingCode.value = false
  }
}

async function handleLogin() {
  if (!form.phone || form.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!form.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  logging.value = true
  try {
    const res = await smsLogin({
      phone: form.phone,
      code: form.code,
      inviteCode: form.inviteCode || undefined,
    })
    userStore.setToken(res.token)
    await userStore.fetchProfile()

    if (res.agreementCheck) {
      appStore.setAgreementCheckFromLogin(res.agreementCheck)
    }

    try {
      const tracker = Tracker.getInstance()
      tracker.track(res.isNew ? 'user_register' : 'user_login', {
        loginType: 'sms',
        hasInviteCode: !!form.inviteCode,
      })
    } catch {
      // tracker may not be initialized
    }

    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    ElMessage.error('登录失败，请检查验证码')
  } finally {
    logging.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-container {
  display: flex;
  width: 900px;
  min-height: 540px;
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .brand {
    margin-bottom: 48px;

    .brand-icon {
      font-size: 48px;
      display: block;
      margin-bottom: 16px;
    }

    h1 {
      font-size: 32px;
      font-weight: 700;
      margin-bottom: 8px;
    }

    p {
      font-size: 16px;
      color: rgba(255, 255, 255, 0.7);
    }
  }

  .features {
    .feature-item {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      margin-bottom: 24px;

      h4 {
        font-size: 15px;
        font-weight: 600;
        margin-bottom: 4px;
      }

      p {
        font-size: 13px;
        color: rgba(255, 255, 255, 0.6);
      }
    }
  }
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;

  .login-form-container {
    width: 100%;
    max-width: 340px;

    h2 {
      font-size: 28px;
      font-weight: 700;
      margin-bottom: 8px;
      color: #1d1d1f;
    }

    .subtitle {
      font-size: 14px;
      color: #86868b;
      margin-bottom: 32px;
    }

    .code-row {
      display: flex;
      gap: 12px;
      width: 100%;
    }

    .agreement-text {
      text-align: center;
      font-size: 12px;
      color: #86868b;
      margin-top: 16px;
    }
  }
}
</style>
