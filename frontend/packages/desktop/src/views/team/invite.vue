<template>
  <div class="page-container team-invite">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <el-button @click="$router.back()" circle>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>邀请成员</h2>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>手机号邀请</span>
          </template>
          <el-form :model="inviteForm" label-width="80px">
            <el-form-item label="手机号" required>
              <el-input v-model="inviteForm.phone" placeholder="请输入被邀请人手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="inviteForm.role" style="width: 100%">
                <el-option label="普通成员" value="member" />
                <el-option label="管理员" value="admin" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="inviting" @click="handleInvite">
                发送邀请
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>邀请链接</span>
          </template>
          <div class="invite-link-section">
            <p class="description">分享以下链接或二维码给团队成员，他们注册后将自动加入团队。</p>
            <el-input
              v-model="inviteLink"
              readonly
              class="link-input"
            >
              <template #append>
                <el-button @click="copyLink">
                  <el-icon><CopyDocument /></el-icon>复制
                </el-button>
              </template>
            </el-input>
          </div>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header>
            <span>邀请记录</span>
          </template>
          <el-table :data="inviteRecords" stripe size="small">
            <el-table-column prop="inviteeNickname" label="成员" min-width="120">
              <template #default="{ row }">
                <div class="member-cell">
                  <el-avatar :size="28" :src="row.inviteeAvatar">
                    {{ row.inviteeNickname?.charAt(0) }}
                  </el-avatar>
                  <span>{{ row.inviteeNickname || formatPhone(row.inviteePhone) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'subscribed' ? 'success' : 'info'" size="small">
                  {{ row.status === 'subscribed' ? '已订阅' : '已注册' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="110">
              <template #default="{ row }">
                {{ formatDate(row.createdAt, 'MM-DD HH:mm') }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  inviteTeamMember,
  getPromotionInfo,
  getInviteRecords,
  formatDate,
  formatPhone,
  copyToClipboard,
  type InviteRecordVO,
} from '@qianku/shared'
import { ElMessage } from 'element-plus'

const inviting = ref(false)
const inviteLink = ref('')
const inviteRecords = ref<InviteRecordVO[]>([])

const inviteForm = reactive({
  phone: '',
  role: 'member',
})

async function handleInvite() {
  if (!inviteForm.phone || inviteForm.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  inviting.value = true
  try {
    await inviteTeamMember({ phone: inviteForm.phone, role: inviteForm.role })
    ElMessage.success('邀请已发送')
    inviteForm.phone = ''
    loadRecords()
  } catch {
    ElMessage.error('邀请失败')
  } finally {
    inviting.value = false
  }
}

async function copyLink() {
  try {
    await copyToClipboard(inviteLink.value)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

async function loadRecords() {
  try {
    const res = await getInviteRecords({ pageNum: 1, pageSize: 20 })
    inviteRecords.value = res.list
  } catch {
    inviteRecords.value = []
  }
}

onMounted(async () => {
  try {
    const info = await getPromotionInfo()
    inviteLink.value = info.inviteLink
  } catch {
    // silent
  }
  loadRecords()
})
</script>

<style lang="scss" scoped>
.team-invite {
  .invite-link-section {
    .description {
      font-size: 14px;
      color: #86868b;
      margin-bottom: 16px;
      line-height: 1.6;
    }

    .link-input {
      margin-bottom: 16px;
    }
  }

  .member-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>
