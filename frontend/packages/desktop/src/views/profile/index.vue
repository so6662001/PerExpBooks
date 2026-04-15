<template>
  <div class="page-container profile-page">
    <div class="page-header">
      <h2>个人信息</h2>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="avatar-card">
          <div class="avatar-section">
            <el-upload
              class="avatar-uploader"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              @change="handleAvatarChange"
            >
              <el-avatar :size="100" :src="userStore.userInfo?.avatarUrl">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="upload-tip">点击更换头像</div>
            </el-upload>
            <h3>{{ userStore.userInfo?.nickname || '用户' }}</h3>
            <p class="phone">{{ userStore.userInfo?.phone ? formatPhone(userStore.userInfo.phone) : '' }}</p>
            <el-tag v-if="userStore.memberStatus?.isMember" type="warning" size="small">
              {{ userStore.memberStatus.level }} 会员
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <template #header>
            <span>基本信息</span>
          </template>
          <el-form :model="form" label-width="100px">
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="公司">
              <el-input v-model="form.company" placeholder="请输入公司名称" />
            </el-form-item>
            <el-form-item label="部门">
              <el-input v-model="form.department" placeholder="请输入部门" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import {
  useUserStore,
  updateUserProfile,
  uploadAvatar,
  formatPhone,
} from '@qianku/shared'
import { ElMessage, type UploadFile } from 'element-plus'

const userStore = useUserStore()
const saving = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  company: '',
  department: '',
})

watch(() => userStore.userInfo, (info) => {
  if (info) {
    form.nickname = info.nickname || ''
    form.email = info.email || ''
    form.company = info.company || ''
    form.department = info.department || ''
  }
}, { immediate: true })

async function handleAvatarChange(file: UploadFile) {
  if (!file.raw) return
  try {
    const res = await uploadAvatar(file.raw)
    await updateUserProfile({ avatarUrl: res.url })
    userStore.fetchProfile()
    ElMessage.success('头像已更新')
  } catch {
    ElMessage.error('上传失败')
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateUserProfile({
      nickname: form.nickname,
      email: form.email,
      company: form.company,
      department: form.department,
    })
    userStore.fetchProfile()
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  userStore.fetchProfile()
  userStore.fetchMemberStatus()
})
</script>

<style lang="scss" scoped>
.profile-page {
  .avatar-card {
    .avatar-section {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 0;

      .avatar-uploader {
        cursor: pointer;
        margin-bottom: 12px;
        text-align: center;

        .upload-tip {
          font-size: 12px;
          color: #86868b;
          margin-top: 8px;
        }
      }

      h3 {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 4px;
      }

      .phone {
        font-size: 14px;
        color: #86868b;
        margin-bottom: 8px;
      }
    }
  }
}
</style>
