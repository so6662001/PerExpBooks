<template>
  <div class="page-container team-members">
    <div class="page-header">
      <h2>团队管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="$router.push('/team/invite')">
          <el-icon><Plus /></el-icon>邀请成员
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" v-if="team" class="team-info">
      <el-col :span="24">
        <el-card>
          <div class="team-header">
            <div class="team-detail">
              <h3>{{ team.name }}</h3>
              <el-tag size="small">{{ team.planName || '基础版' }}</el-tag>
            </div>
            <div class="team-meta">
              <span>{{ team.memberCount }} 名成员</span>
              <el-button link type="primary" @click="showEditDialog = true">编辑团队</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="!team && !loading" style="margin-bottom: 16px">
      <el-empty description="您还没有团队">
        <el-button type="primary" @click="showCreateDialog = true">创建团队</el-button>
      </el-empty>
    </el-card>

    <el-card v-if="team">
      <template #header>
        <span>成员列表</span>
      </template>
      <el-table :data="members" stripe v-loading="loadingMembers">
        <el-table-column label="成员" min-width="200">
          <template #default="{ row }">
            <div class="member-cell">
              <el-avatar :size="36" :src="row.avatar">
                {{ row.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="name">{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'owner' ? 'danger' : row.role === 'admin' ? 'warning' : 'info'" size="small">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="joinedAt" label="加入时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.joinedAt, 'YYYY-MM-DD') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 'owner'"
              link
              type="danger"
              size="small"
              @click="handleRemove(row)"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showCreateDialog" title="创建团队" width="400px">
      <el-form>
        <el-form-item label="团队名称" required>
          <el-input v-model="teamName" placeholder="请输入团队名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateTeam">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑团队" width="400px">
      <el-form>
        <el-form-item label="团队名称" required>
          <el-input v-model="editName" placeholder="请输入团队名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="updating" @click="handleUpdateTeam">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getMyTeam,
  createTeam,
  updateTeam,
  getTeamMembers,
  removeTeamMember,
  formatDate,
  type TeamVO,
  type TeamMemberVO,
} from '@qianku/shared'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const loadingMembers = ref(false)
const team = ref<TeamVO | null>(null)
const members = ref<TeamMemberVO[]>([])
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const teamName = ref('')
const editName = ref('')
const creating = ref(false)
const updating = ref(false)

function roleLabel(role: string) {
  const map: Record<string, string> = { owner: '所有者', admin: '管理员', member: '成员' }
  return map[role] || role
}

async function loadTeam() {
  loading.value = true
  try {
    team.value = await getMyTeam()
    if (team.value) {
      editName.value = team.value.name
      loadMembers()
    }
  } catch {
    team.value = null
  } finally {
    loading.value = false
  }
}

async function loadMembers() {
  loadingMembers.value = true
  try {
    members.value = await getTeamMembers()
  } catch {
    members.value = []
  } finally {
    loadingMembers.value = false
  }
}

async function handleCreateTeam() {
  if (!teamName.value) {
    ElMessage.warning('请输入团队名称')
    return
  }
  creating.value = true
  try {
    await createTeam({ name: teamName.value })
    ElMessage.success('团队创建成功')
    showCreateDialog.value = false
    loadTeam()
  } catch {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

async function handleUpdateTeam() {
  if (!editName.value) return
  updating.value = true
  try {
    await updateTeam({ name: editName.value })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadTeam()
  } catch {
    ElMessage.error('更新失败')
  } finally {
    updating.value = false
  }
}

async function handleRemove(member: TeamMemberVO) {
  try {
    await ElMessageBox.confirm(`确定要移除成员 ${member.nickname} 吗？`, '提示', { type: 'warning' })
    await removeTeamMember(member.userId)
    ElMessage.success('已移除')
    loadMembers()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadTeam()
})
</script>

<style lang="scss" scoped>
.team-members {
  .team-info {
    margin-bottom: 16px;
  }

  .team-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .team-detail {
      display: flex;
      align-items: center;
      gap: 12px;

      h3 {
        font-size: 18px;
        font-weight: 600;
      }
    }

    .team-meta {
      display: flex;
      align-items: center;
      gap: 16px;
      color: #86868b;
    }
  }

  .member-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .name {
      font-weight: 500;
    }
  }
}
</style>
