<template>
  <el-dialog
    v-model="appStore.showAgreement"
    title="用户协议更新"
    width="600px"
    :close-on-click-modal="!appStore.agreementBlockMode"
    :close-on-press-escape="!appStore.agreementBlockMode"
    :show-close="!appStore.agreementBlockMode"
    center
  >
    <div class="agreement-content">
      <div v-if="summaryText" class="change-summary">
        <el-alert
          :title="summaryText"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
      <div class="agreement-body">
        <p>请阅读并同意以下更新的协议内容。</p>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button
          v-if="!appStore.agreementBlockMode"
          @click="appStore.dismissAgreement()"
        >
          稍后再说
        </el-button>
        <el-button type="primary" :loading="confirming" @click="handleConfirm">
          我已阅读并同意
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAppStore } from '@qianku/shared'
import { ElMessage } from 'element-plus'

const appStore = useAppStore()
const confirming = ref(false)

const summaryText = computed(() => {
  return appStore.pendingAgreements
    .filter(a => a.changeSummary)
    .map(a => a.changeSummary)
    .join('；')
})

async function handleConfirm() {
  confirming.value = true
  try {
    await appStore.acceptAgreement()
    ElMessage.success('协议已确认')
  } catch {
    ElMessage.error('确认失败，请重试')
  } finally {
    confirming.value = false
  }
}
</script>

<style lang="scss" scoped>
.agreement-content {
  max-height: 400px;
  overflow-y: auto;

  .change-summary {
    margin-bottom: 16px;
  }

  .agreement-body {
    font-size: 14px;
    line-height: 1.8;
    color: #333;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
