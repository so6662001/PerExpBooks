<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useAppStore } from '@qianku/shared'
import { showToast } from 'vant'

const appStore = useAppStore()
const scrolledToBottom = ref(false)
const contentRef = ref<HTMLElement>()
const confirming = ref(false)

const canConfirm = computed(() => scrolledToBottom.value)

function onScroll(e: Event) {
  const el = e.target as HTMLElement
  const threshold = 50
  scrolledToBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < threshold
}

watch(() => appStore.showAgreement, (val) => {
  if (val) {
    scrolledToBottom.value = false
  }
})

async function handleConfirm() {
  if (!canConfirm.value) {
    showToast('请先阅读完协议内容')
    return
  }
  confirming.value = true
  try {
    await appStore.acceptAgreement()
    showToast({ message: '已同意协议', type: 'success' })
  } catch {
    showToast('操作失败，请重试')
  } finally {
    confirming.value = false
  }
}

function handleClose() {
  if (appStore.agreementBlockMode) return
  appStore.dismissAgreement()
}
</script>

<template>
  <van-popup
    :show="appStore.showAgreement"
    position="bottom"
    round
    :close-on-click-overlay="!appStore.agreementBlockMode"
    :closeable="!appStore.agreementBlockMode"
    style="height: 90%"
    @close="handleClose"
  >
    <div class="agreement-popup">
      <div class="agreement-header">
        <h2 class="agreement-title">
          {{ appStore.currentAgreement?.title || '用户协议更新' }}
        </h2>
        <p class="agreement-version">
          版本 {{ appStore.currentAgreement?.version }}
        </p>
      </div>

      <div
        v-if="appStore.currentAgreement?.changeSummary"
        class="change-summary"
      >
        <div class="summary-label">变更摘要</div>
        <div class="summary-content">{{ appStore.currentAgreement.changeSummary }}</div>
      </div>

      <div
        ref="contentRef"
        class="agreement-content"
        @scroll="onScroll"
      >
        <div v-html="appStore.currentAgreement?.content" />
      </div>

      <div class="agreement-footer">
        <van-button
          type="primary"
          block
          round
          :disabled="!canConfirm"
          :loading="confirming"
          @click="handleConfirm"
        >
          {{ canConfirm ? '我已阅读并同意' : '请阅读至底部' }}
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<style lang="scss" scoped>
.agreement-popup {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

.agreement-header {
  text-align: center;
  padding: 20px 16px 12px;
  border-bottom: 0.5px solid var(--color-border);

  .agreement-title {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-text);
  }

  .agreement-version {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 4px;
  }
}

.change-summary {
  margin: 12px 16px;
  padding: 12px;
  background: #FFF8E1;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-warning);

  .summary-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--color-warning);
    margin-bottom: 4px;
  }

  .summary-content {
    font-size: 14px;
    color: var(--color-text);
    line-height: 1.6;
  }
}

.agreement-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text);
  -webkit-overflow-scrolling: touch;
}

.agreement-footer {
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  border-top: 0.5px solid var(--color-border);
  background: var(--color-card);
}
</style>
