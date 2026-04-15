<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '@qianku/shared'

defineOptions({ name: 'LandingIndex' })

const route = useRoute()
const inviteCode = ref('')
const inviterName = ref('')

const features = [
  { icon: '📸', title: '拍照即识别', desc: '发票拍照自动识别，告别手动录入' },
  { icon: '📊', title: '一键报销', desc: '费用智能归类，生成报销单秒级完成' },
  { icon: '✈️', title: '出差管理', desc: '出差记录关联费用，补贴自动计算' },
]

onMounted(async () => {
  inviteCode.value = (route.query.code as string) || ''
  if (inviteCode.value) {
    try {
      const info = await get<{ nickname: string }>('/share/inviter', { params: { code: inviteCode.value } })
      inviterName.value = info.nickname || ''
    } catch {
      // ignore
    }
  }
})

function handleRegister() {
  const url = inviteCode.value ? `/auth/login?invite=${inviteCode.value}` : '/auth/login'
  window.location.href = url
}
</script>

<template>
  <div class="landing-page">
    <div class="hero-section">
      <h1 class="hero-title">发票拖进去<br />报销单出来</h1>
      <p class="hero-subtitle">智能费用管理，让报销不再烦恼</p>
    </div>

    <div class="features-section">
      <div v-for="feat in features" :key="feat.title" class="feature-card">
        <div class="feature-icon">{{ feat.icon }}</div>
        <div class="feature-content">
          <div class="feature-title">{{ feat.title }}</div>
          <div class="feature-desc">{{ feat.desc }}</div>
        </div>
      </div>
    </div>

    <div v-if="inviterName" class="invite-banner">
      <span class="invite-text">你的好友 <strong>{{ inviterName }}</strong> 邀请你使用</span>
    </div>

    <div class="cta-section">
      <van-button type="primary" block round size="large" @click="handleRegister">
        立即注册体验
      </van-button>
    </div>

    <div class="promo-banner">
      <span class="promo-icon">🎁</span>
      <span class="promo-text">专享：试用期延长至45天</span>
    </div>

    <div class="footer">
      <span class="footer-text">钱酷 · 智能费用管理</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.landing-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #007AFF 0%, #5856D6 50%, #F2F2F7 50%);
}

.hero-section {
  text-align: center;
  padding: 60px 24px 40px;
  color: white;

  .hero-title {
    font-size: 34px;
    font-weight: 800;
    line-height: 1.25;
    letter-spacing: -0.5px;
  }

  .hero-subtitle {
    font-size: 16px;
    opacity: 0.85;
    margin-top: 12px;
  }
}

.features-section {
  padding: 0 20px;

  .feature-card {
    display: flex;
    align-items: center;
    gap: 16px;
    background: #fff;
    border-radius: 16px;
    padding: 20px;
    margin-bottom: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

    .feature-icon {
      width: 52px;
      height: 52px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      background: #F2F2F7;
      border-radius: 14px;
      flex-shrink: 0;
    }

    .feature-content {
      .feature-title {
        font-size: 17px;
        font-weight: 600;
      }

      .feature-desc {
        font-size: 13px;
        color: #8e8e93;
        margin-top: 4px;
      }
    }
  }
}

.invite-banner {
  text-align: center;
  padding: 20px;

  .invite-text {
    font-size: 15px;
    color: #3c3c43;

    strong {
      color: #007AFF;
    }
  }
}

.cta-section {
  padding: 16px 24px;
}

.promo-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 16px;
  margin: 8px 24px;
  background: linear-gradient(135deg, #FFF3E0, #FFE0B2);
  border-radius: 12px;

  .promo-icon {
    font-size: 18px;
  }

  .promo-text {
    font-size: 14px;
    font-weight: 500;
    color: #E65100;
  }
}

.footer {
  text-align: center;
  padding: 32px 0 48px;

  .footer-text {
    font-size: 13px;
    color: #8e8e93;
  }
}
</style>
