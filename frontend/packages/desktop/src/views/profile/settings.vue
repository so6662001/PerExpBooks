<template>
  <div class="page-container settings-page">
    <div class="page-header">
      <h2>系统设置</h2>
    </div>

    <el-card>
      <template #header>
        <span>通知设置</span>
      </template>
      <div class="setting-item">
        <div class="setting-info">
          <h4>报销提醒</h4>
          <p>有待报销费用时发送提醒</p>
        </div>
        <el-switch v-model="settings.reimbursementReminder" />
      </div>
      <div class="setting-item">
        <div class="setting-info">
          <h4>出差提醒</h4>
          <p>出差行程开始前发送提醒</p>
        </div>
        <el-switch v-model="settings.tripReminder" />
      </div>
      <div class="setting-item">
        <div class="setting-info">
          <h4>月度报表</h4>
          <p>每月自动生成费用报表</p>
        </div>
        <el-switch v-model="settings.monthlyReport" />
      </div>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>数据管理</span>
      </template>
      <div class="setting-item">
        <div class="setting-info">
          <h4>导出数据</h4>
          <p>导出您的所有费用和报销数据</p>
        </div>
        <el-button size="small" @click="handleExportData">导出</el-button>
      </div>
      <div class="setting-item">
        <div class="setting-info">
          <h4>默认币种</h4>
          <p>设置费用默认显示币种</p>
        </div>
        <el-select v-model="settings.currency" style="width: 120px" size="small">
          <el-option label="人民币 (¥)" value="CNY" />
          <el-option label="美元 ($)" value="USD" />
          <el-option label="欧元 (€)" value="EUR" />
        </el-select>
      </div>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>关于</span>
      </template>
      <div class="about-section">
        <div class="about-item">
          <span>版本号</span>
          <span>v1.0.0</span>
        </div>
        <div class="about-item">
          <span>用户协议</span>
          <el-button link type="primary" @click="showAgreement">查看</el-button>
        </div>
        <div class="about-item">
          <span>隐私政策</span>
          <el-button link type="primary">查看</el-button>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 16px">
      <div class="danger-zone">
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  useUserStore,
  useAppStore,
  logout as logoutApi,
} from '@qianku/shared'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const settings = reactive({
  reimbursementReminder: true,
  tripReminder: true,
  monthlyReport: false,
  currency: 'CNY',
})

function handleExportData() {
  ElMessage.info('数据导出功能即将上线')
}

function showAgreement() {
  appStore.checkAgreementStatus()
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    try {
      await logoutApi()
    } finally {
      userStore.logout()
      router.push('/auth/login')
    }
  } catch {
    // cancelled
  }
}
</script>

<style lang="scss" scoped>
.settings-page {
  .setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .setting-info {
      h4 {
        font-size: 15px;
        font-weight: 500;
        margin-bottom: 4px;
      }

      p {
        font-size: 13px;
        color: #86868b;
      }
    }
  }

  .about-section {
    .about-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f5f5f5;
      font-size: 14px;

      &:last-child {
        border-bottom: none;
      }
    }
  }

  .danger-zone {
    text-align: center;
    padding: 8px 0;
  }
}
</style>
