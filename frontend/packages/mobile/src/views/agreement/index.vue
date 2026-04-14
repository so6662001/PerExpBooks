<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getLatestAgreement, formatDate } from '@qianku/shared'
import type { AgreementVO } from '@qianku/shared'

defineOptions({ name: 'AgreementIndex' })

const router = useRouter()
const loading = ref(true)
const agreement = ref<AgreementVO | null>(null)

onMounted(async () => {
  try {
    agreement.value = await getLatestAgreement()
  } catch (e: any) {
    showToast(e.message || '加载失败')
  } finally {
    loading.value = false
  }
})

function goDetail(id: string) {
  router.push(`/agreement/${id}`)
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="协议与政策" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading">
      <div class="agreement-section card">
        <h3 class="section-label">当前协议</h3>
        <div v-if="agreement" class="agreement-item" @click="goDetail(agreement.id)">
          <div class="agreement-info">
            <div class="agreement-title">{{ agreement.title }}</div>
            <div class="agreement-meta">
              版本 {{ agreement.version }} · 生效时间 {{ formatDate(agreement.effectiveAt) }}
            </div>
          </div>
          <van-icon name="arrow" color="#c8c8c8" />
        </div>
        <div v-else class="empty-hint">暂无协议信息</div>
      </div>

      <div class="menu-section">
        <div class="menu-item card" @click="goDetail(agreement?.id || '')">
          <div class="menu-icon">📄</div>
          <span class="menu-title">查看协议全文</span>
          <van-icon name="arrow" class="menu-arrow" />
        </div>
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

.agreement-section {
  .section-label {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-secondary);
    margin-bottom: 12px;
  }

  .agreement-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    cursor: pointer;

    .agreement-info {
      flex: 1;

      .agreement-title {
        font-size: 16px;
        font-weight: 500;
      }

      .agreement-meta {
        font-size: 12px;
        color: var(--color-text-tertiary);
        margin-top: 4px;
      }
    }
  }

  .empty-hint {
    font-size: 14px;
    color: var(--color-text-tertiary);
    text-align: center;
    padding: 20px 0;
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
      font-size: 20px;
    }

    .menu-title {
      flex: 1;
      font-size: 15px;
      font-weight: 500;
    }

    .menu-arrow {
      color: var(--color-text-tertiary);
    }
  }
}
</style>
