<template>
  <div class="page-container upload-page">
    <div class="page-header">
      <h2>上传发票</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>选择发票文件</span>
          </template>
          <el-upload
            class="upload-area"
            drag
            action=""
            :auto-upload="false"
            :show-file-list="true"
            :file-list="fileList"
            accept=".jpg,.jpeg,.png,.pdf"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            multiple
          >
            <el-icon class="el-icon--upload" :size="48"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 JPG/PNG/PDF，单文件不超过 10MB</div>
            </template>
          </el-upload>
          <el-button
            type="primary"
            style="width: 100%; margin-top: 16px"
            :disabled="fileList.length === 0"
            :loading="uploading"
            @click="startUpload"
          >
            开始识别上传 ({{ fileList.length }} 个文件)
          </el-button>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>识别结果</span>
          </template>
          <el-empty v-if="results.length === 0" description="上传发票后将显示识别结果" />
          <div v-else class="result-list">
            <div v-for="(item, index) in results" :key="index" class="result-item">
              <div class="result-header">
                <el-tag :type="item.success ? 'success' : 'danger'" size="small">
                  {{ item.success ? '识别成功' : '识别失败' }}
                </el-tag>
                <span class="filename">{{ item.filename }}</span>
              </div>
              <template v-if="item.success && item.invoice">
                <el-descriptions :column="1" border size="small" class="result-detail">
                  <el-descriptions-item label="发票号">{{ item.invoice.invoiceNo }}</el-descriptions-item>
                  <el-descriptions-item label="日期">{{ item.invoice.invoiceDate }}</el-descriptions-item>
                  <el-descriptions-item label="金额">{{ formatAmount(item.invoice.amount) }}</el-descriptions-item>
                  <el-descriptions-item label="销方">{{ item.invoice.seller }}</el-descriptions-item>
                  <el-descriptions-item label="置信度">
                    <el-progress
                      :percentage="Math.round(item.invoice.ocrConfidence * 100)"
                      :color="item.invoice.ocrConfidence > 0.8 ? '#34C759' : '#FF9500'"
                    />
                  </el-descriptions-item>
                </el-descriptions>
                <div class="result-actions">
                  <el-select v-model="item.category" placeholder="选择分类" size="small">
                    <el-option label="交通" value="transport" />
                    <el-option label="住宿" value="accommodation" />
                    <el-option label="餐饮" value="meal" />
                    <el-option label="办公" value="office" />
                    <el-option label="通讯" value="communication" />
                    <el-option label="其他" value="other" />
                  </el-select>
                  <el-button
                    type="primary"
                    size="small"
                    :loading="item.creating"
                    @click="createFromInvoice(item)"
                  >
                    创建费用
                  </el-button>
                </div>
              </template>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  uploadInvoice,
  createExpense,
  formatAmount,
  type InvoiceUploadVO,
} from '@qianku/shared'
import { ElMessage, type UploadFile } from 'element-plus'
import { Tracker } from '@qianku/shared/analytics'

interface UploadResult {
  filename: string
  success: boolean
  invoice?: InvoiceUploadVO
  category: string
  creating: boolean
  created: boolean
}

const fileList = ref<UploadFile[]>([])
const uploading = ref(false)
const results = ref<UploadResult[]>([])

function handleFileChange(file: UploadFile, files: UploadFile[]) {
  fileList.value = files
}

function handleFileRemove(_file: UploadFile, files: UploadFile[]) {
  fileList.value = files
}

async function startUpload() {
  uploading.value = true
  results.value = []

  for (const file of fileList.value) {
    if (!file.raw) continue
    try {
      const invoice = await uploadInvoice(file.raw)
      try { Tracker.getInstance().track('invoice_upload_success', { parseSuccess: true }) } catch {}
      results.value.push({
        filename: file.name,
        success: true,
        invoice,
        category: 'other',
        creating: false,
        created: false,
      })
    } catch {
      try { Tracker.getInstance().track('invoice_upload_fail', { errorType: 'upload_error' }) } catch {}
      results.value.push({
        filename: file.name,
        success: false,
        category: 'other',
        creating: false,
        created: false,
      })
    }
  }
  uploading.value = false
  fileList.value = []
}

async function createFromInvoice(item: UploadResult) {
  if (!item.invoice) return
  item.creating = true
  try {
    await createExpense({
      invoiceId: item.invoice.invoiceId,
      category: item.category,
      amount: item.invoice.amount,
      description: `${item.invoice.seller} - ${item.invoice.items?.[0]?.name || '费用'}`,
      expenseDate: item.invoice.invoiceDate,
    })
    item.created = true
    ElMessage.success('费用创建成功')
  } catch {
    ElMessage.error('创建失败')
  } finally {
    item.creating = false
  }
}
</script>

<style lang="scss" scoped>
.upload-page {
  .upload-area {
    width: 100%;

    :deep(.el-upload) {
      width: 100%;
    }

    :deep(.el-upload-dragger) {
      width: 100%;
      padding: 40px 20px;
    }
  }

  .result-list {
    .result-item {
      padding: 16px;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .result-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;

        .filename {
          font-size: 13px;
          color: #86868b;
        }
      }

      .result-detail {
        margin-bottom: 12px;
      }

      .result-actions {
        display: flex;
        gap: 8px;
        align-items: center;
      }
    }
  }
}
</style>
