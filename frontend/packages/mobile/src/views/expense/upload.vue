<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { uploadInvoice, createExpense, listCategories, get } from '@qianku/shared'
import type { InvoiceUploadVO } from '@qianku/shared'
import { Tracker } from '@qianku/shared/analytics'

const router = useRouter()
const fileList = ref<any[]>([])
const invoiceResult = ref<InvoiceUploadVO | null>(null)
const uploading = ref(false)
const creating = ref(false)
const step = ref<'upload' | 'confirm'>('upload')

const categoryId = ref<number | undefined>(undefined)
const description = ref('')
const manualAmount = ref<number | undefined>(undefined)
const manualDate = ref('')

const categoryOptions = ref<{ text: string; value: number }[]>([])
const showShareGuide = ref(false)
const shareGuideMessage = ref('')

onMounted(async () => {
  try {
    const result = await listCategories()
    categoryOptions.value = (result as any[]).map((c: any) => ({
      text: c.name,
      value: c.id,
    }))
    if (categoryOptions.value.length > 0) {
      categoryId.value = categoryOptions.value[0].value
    }
  } catch {
    // ignore
  }
})

const canSubmit = computed(() =>
  invoiceResult.value && categoryId.value && !creating.value,
)

async function handleUpload(file: any) {
  uploading.value = true
  showLoadingToast({ message: '上传中...', forbidClick: true, duration: 0 })
  try {
    const result = await uploadInvoice(file.file)
    invoiceResult.value = result
    step.value = 'confirm'
    try { Tracker.getInstance().track('invoice_upload_success', { parseSuccess: result.parseSuccess }) } catch {}
    if (result.parseSuccess) {
      showToast({ message: '识别成功', type: 'success' })
    } else {
      showToast({ message: result.parseMessage || '请手动填写信息' })
    }
  } catch (e: any) {
    try { Tracker.getInstance().track('invoice_upload_fail', { errorType: 'upload_error' }) } catch {}
    showToast(e.message || '上传失败')
  } finally {
    uploading.value = false
    closeToast()
  }
  return false
}

async function handleCreate() {
  if (!invoiceResult.value || !canSubmit.value) return
  const amount = invoiceResult.value.amount || manualAmount.value
  const expenseDate = invoiceResult.value.invoiceDate || manualDate.value
  if (!amount || !expenseDate) {
    showToast('请填写金额和日期')
    return
  }
  creating.value = true
  try {
    await createExpense({
      categoryId: categoryId.value!,
      type: 1,
      amount: amount,
      invoiceNo: invoiceResult.value.invoiceNo || undefined,
      invoiceCode: invoiceResult.value.invoiceCode || undefined,
      invoiceDate: invoiceResult.value.invoiceDate || undefined,
      sellerName: invoiceResult.value.sellerName || undefined,
      buyerName: invoiceResult.value.buyerName || undefined,
      taxAmount: invoiceResult.value.taxAmount || undefined,
      fileUrl: invoiceResult.value.fileUrl || undefined,
      fileName: invoiceResult.value.fileName || undefined,
      description: description.value || undefined,
      expenseDate: expenseDate,
    })
    showToast({ message: '创建成功', type: 'success' })
    try {
      const triggerResult = await get('/trigger/check', { params: { scene: 'INVOICE_PARSED' } })
      if (triggerResult?.show) {
        showShareGuide.value = true
        shareGuideMessage.value = triggerResult.message
        return
      }
    } catch { /* ignore trigger errors */ }
    router.back()
  } catch (e: any) {
    showToast(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function resetUpload() {
  step.value = 'upload'
  invoiceResult.value = null
  fileList.value = []
  manualAmount.value = undefined
  manualDate.value = ''
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="上传发票" left-arrow @click-left="router.back()" />

    <template v-if="step === 'upload'">
      <div class="upload-area card">
        <van-uploader
          v-model="fileList"
          :max-count="1"
          accept=".pdf,.jpg,.jpeg,.png"
          :after-read="handleUpload"
          :before-read="() => true"
        >
          <div class="upload-trigger">
            <van-icon name="photograph" size="48" color="#007AFF" />
            <p class="upload-text">点击上传发票</p>
            <p class="upload-hint">支持 PDF、JPG、PNG 格式</p>
          </div>
        </van-uploader>
      </div>

      <div class="tips card">
        <h4>📋 上传须知</h4>
        <ul>
          <li>请确保发票图片清晰完整</li>
          <li>系统将自动识别发票信息</li>
          <li>识别后可手动修改信息</li>
        </ul>
      </div>
    </template>

    <template v-if="step === 'confirm' && invoiceResult">
      <div v-if="!invoiceResult.parseSuccess" class="parse-hint card">
        <p style="color: #FF9500; font-weight: 500;">{{ invoiceResult.parseMessage }}</p>
      </div>

      <div class="result-card card">
        <h3 class="result-title">📄 发票信息</h3>
        <div class="result-items">
          <div v-if="invoiceResult.invoiceNo" class="result-item">
            <span class="label">发票号码</span>
            <span class="value">{{ invoiceResult.invoiceNo }}</span>
          </div>
          <div v-if="invoiceResult.invoiceDate" class="result-item">
            <span class="label">开票日期</span>
            <span class="value">{{ invoiceResult.invoiceDate }}</span>
          </div>
          <div v-if="invoiceResult.amount" class="result-item">
            <span class="label">金额</span>
            <span class="value amount" style="color: var(--color-primary)">
              ¥{{ Number(invoiceResult.amount).toFixed(2) }}
            </span>
          </div>
          <div v-if="invoiceResult.sellerName" class="result-item">
            <span class="label">销方</span>
            <span class="value">{{ invoiceResult.sellerName }}</span>
          </div>
        </div>
      </div>

      <div v-if="!invoiceResult.parseSuccess" class="form-card card">
        <van-field
          v-model.number="manualAmount"
          label="金额(元)"
          type="number"
          placeholder="请输入金额"
          required
        />
        <van-field
          v-model="manualDate"
          label="费用日期"
          placeholder="请输入日期 YYYY-MM-DD"
          required
        />
      </div>

      <div class="form-card card">
        <van-radio-group v-model="categoryId" direction="horizontal" class="category-group">
          <van-radio
            v-for="opt in categoryOptions"
            :key="opt.value"
            :name="opt.value"
          >
            {{ opt.text }}
          </van-radio>
        </van-radio-group>
        <van-field
          v-model="description"
          label="备注"
          placeholder="可选填写备注信息"
          type="textarea"
          rows="2"
          autosize
        />
      </div>

      <div class="action-bar">
        <van-button plain round @click="resetUpload">重新上传</van-button>
        <van-button
          type="primary"
          round
          :disabled="!canSubmit"
          :loading="creating"
          @click="handleCreate"
        >
          确认创建
        </van-button>
      </div>
    </template>

    <van-popup v-model:show="showShareGuide" position="center" round style="padding: 24px; width: 80%">
      <div style="text-align: center">
        <div style="font-size: 16px; font-weight: 600; margin-bottom: 12px">分享给好友</div>
        <div style="font-size: 14px; color: #666">{{ shareGuideMessage }}</div>
        <van-button type="primary" round block style="margin-top: 16px" @click="showShareGuide = false; router.back()">
          知道了
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.upload-area {
  text-align: center;
  padding: 40px 20px;

  .upload-trigger {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 40px 20px;
    border: 2px dashed var(--color-border);
    border-radius: var(--radius-lg);
    cursor: pointer;

    .upload-text {
      font-size: 16px;
      font-weight: 500;
      color: var(--color-text);
    }

    .upload-hint {
      font-size: 13px;
      color: var(--color-text-secondary);
    }
  }
}

.tips {
  h4 {
    font-size: 15px;
    margin-bottom: 8px;
  }

  ul {
    padding-left: 20px;

    li {
      font-size: 14px;
      color: var(--color-text-secondary);
      line-height: 2;
    }
  }
}

.result-card {
  .result-title {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .result-items {
    .result-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 0.5px solid var(--color-divider);

      &:last-child { border-bottom: none; }

      .label {
        font-size: 14px;
        color: var(--color-text-secondary);
      }

      .value {
        font-size: 14px;
        font-weight: 500;
      }
    }
  }
}

.form-card {
  .category-group {
    padding: 8px 16px 16px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.action-bar {
  display: flex;
  gap: 12px;
  padding: 16px;

  .van-button {
    flex: 1;
  }
}
</style>
