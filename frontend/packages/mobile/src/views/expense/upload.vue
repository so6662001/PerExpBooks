<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showLoadingToast, closeToast } from 'vant'
import { uploadInvoice, createExpense } from '@qianku/shared'
import type { InvoiceUploadVO } from '@qianku/shared'

const router = useRouter()
const fileList = ref<any[]>([])
const invoiceResult = ref<InvoiceUploadVO | null>(null)
const uploading = ref(false)
const creating = ref(false)
const step = ref<'upload' | 'confirm'>('upload')

const category = ref('other')
const description = ref('')

const categoryOptions = [
  { text: '交通', value: 'transport' },
  { text: '住宿', value: 'accommodation' },
  { text: '餐饮', value: 'meal' },
  { text: '办公', value: 'office' },
  { text: '通讯', value: 'communication' },
  { text: '其他', value: 'other' },
]

const canSubmit = computed(() =>
  invoiceResult.value && category.value && !creating.value,
)

async function handleUpload(file: any) {
  uploading.value = true
  showLoadingToast({ message: '识别中...', forbidClick: true, duration: 0 })
  try {
    const result = await uploadInvoice(file.file)
    invoiceResult.value = result
    step.value = 'confirm'
    showToast({ message: '识别成功', type: 'success' })
  } catch (e: any) {
    showToast(e.message || '上传失败')
  } finally {
    uploading.value = false
    closeToast()
  }
  return false
}

async function handleCreate() {
  if (!invoiceResult.value || !canSubmit.value) return
  creating.value = true
  try {
    await createExpense({
      invoiceId: invoiceResult.value.invoiceId,
      category: category.value,
      amount: invoiceResult.value.amount,
      description: description.value || `${invoiceResult.value.seller} - 发票`,
      expenseDate: invoiceResult.value.invoiceDate,
    })
    showToast({ message: '创建成功', type: 'success' })
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
      <div class="result-card card">
        <h3 class="result-title">📄 发票信息</h3>
        <div class="result-items">
          <div class="result-item">
            <span class="label">发票号码</span>
            <span class="value">{{ invoiceResult.invoiceNo }}</span>
          </div>
          <div class="result-item">
            <span class="label">开票日期</span>
            <span class="value">{{ invoiceResult.invoiceDate }}</span>
          </div>
          <div class="result-item">
            <span class="label">金额</span>
            <span class="value amount" style="color: var(--color-primary)">
              ¥{{ invoiceResult.amount.toFixed(2) }}
            </span>
          </div>
          <div class="result-item">
            <span class="label">销方</span>
            <span class="value">{{ invoiceResult.seller }}</span>
          </div>
          <div class="result-item">
            <span class="label">识别置信度</span>
            <span class="value">{{ (invoiceResult.ocrConfidence * 100).toFixed(0) }}%</span>
          </div>
        </div>
      </div>

      <div class="form-card card">
        <van-field
          v-model="category"
          is-link
          readonly
          label="费用类别"
          placeholder="选择类别"
          @click="() => {}"
        />
        <van-radio-group v-model="category" direction="horizontal" class="category-group">
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
