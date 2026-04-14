<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getLatestAgreement, formatDate } from '@qianku/shared'
import type { AgreementVO } from '@qianku/shared'

defineOptions({ name: 'AgreementDetail' })

const route = useRoute()
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
</script>

<template>
  <div class="page">
    <van-nav-bar title="协议详情" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="agreement">
      <div v-if="agreement.changeSummary" class="change-banner">
        <van-icon name="info-o" />
        <span>变更摘要：{{ agreement.changeSummary }}</span>
      </div>

      <div class="agreement-header card">
        <h2 class="agreement-title">{{ agreement.title }}</h2>
        <div class="agreement-meta">
          <span>版本 {{ agreement.version }}</span>
          <span>生效日期 {{ formatDate(agreement.effectiveAt) }}</span>
        </div>
      </div>

      <div class="agreement-content card" v-html="agreement.content" />
    </template>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.change-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #FFF3E0;
  color: #E65100;
  font-size: 13px;

  .van-icon {
    flex-shrink: 0;
  }
}

.agreement-header {
  .agreement-title {
    font-size: 20px;
    font-weight: 700;
    margin-bottom: 8px;
  }

  .agreement-meta {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}

.agreement-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text);

  :deep(h1), :deep(h2), :deep(h3) {
    margin: 16px 0 8px;
    font-weight: 600;
  }

  :deep(h1) { font-size: 18px; }
  :deep(h2) { font-size: 16px; }
  :deep(h3) { font-size: 15px; }

  :deep(p) {
    margin: 8px 0;
  }

  :deep(ul), :deep(ol) {
    padding-left: 20px;
    margin: 8px 0;
  }

  :deep(li) {
    margin: 4px 0;
  }
}
</style>
