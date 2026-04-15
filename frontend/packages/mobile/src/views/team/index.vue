<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { getMyTeam, createTeam, getTeamMembers, removeTeamMember, copyToClipboard } from '@qianku/shared'
import type { TeamVO, TeamMemberVO } from '@qianku/shared'

defineOptions({ name: 'TeamIndex' })

const router = useRouter()
const team = ref<TeamVO | null>(null)
const members = ref<TeamMemberVO[]>([])
const loading = ref(true)
const hasTeam = ref(false)
const showCreateDialog = ref(false)
const teamName = ref('')
const creating = ref(false)

onMounted(async () => {
  await loadTeam()
})

async function loadTeam() {
  loading.value = true
  try {
    team.value = await getMyTeam()
    hasTeam.value = true
    members.value = await getTeamMembers()
  } catch {
    hasTeam.value = false
  } finally {
    loading.value = false
  }
}

async function handleCreateTeam() {
  if (!teamName.value.trim()) {
    showToast('请输入团队名称')
    return
  }
  creating.value = true
  try {
    await createTeam({ name: teamName.value })
    showToast({ message: '创建成功', type: 'success' })
    showCreateDialog.value = false
    teamName.value = ''
    await loadTeam()
  } catch (e: any) {
    showToast(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function getRoleLabel(role: number) {
  const map: Record<number, string> = {
    1: '管理员',
    2: '成员',
  }
  return map[role] || '成员'
}

function getRoleColor(role: number) {
  const map: Record<number, string> = {
    1: '#FF9500',
    2: '#34C759',
  }
  return map[role] || '#999'
}

async function copyInviteLink() {
  if (!team.value) return
  const link = `${window.location.origin}/team/join?code=${team.value.id}`
  try {
    await copyToClipboard(link)
    showToast({ message: '邀请链接已复制', type: 'success' })
  } catch {
    showToast('复制失败')
  }
}

async function handleRemoveMember(member: TeamMemberVO) {
  try {
    await showDialog({
      title: '确认移除',
      message: `确定要移除成员 ${member.nickname} 吗？`,
      showCancelButton: true,
      confirmButtonColor: '#FF3B30',
    })
    await removeTeamMember(member.userId)
    showToast({ message: '已移除', type: 'success' })
    members.value = members.value.filter(m => m.userId !== member.userId)
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="page">
    <van-nav-bar title="团队管理" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="page-loading" />

    <template v-if="!loading && !hasTeam">
      <div class="empty-state" style="padding-top: 80px">
        <div class="empty-icon">👥</div>
        <div class="empty-text">您还没有团队</div>
        <van-button type="primary" round style="margin-top: 16px" @click="showCreateDialog = true">
          创建团队
        </van-button>
      </div>
    </template>

    <template v-if="!loading && hasTeam && team">
      <div class="team-info card">
        <div class="team-header">
          <div class="team-icon">👥</div>
          <div class="team-detail">
            <div class="team-name">{{ team.name }}</div>
            <div class="team-meta">{{ team.memberCount }} 位成员</div>
          </div>
        </div>
      </div>

      <div class="action-section">
        <van-button type="primary" block round @click="copyInviteLink">
          生成邀请链接
        </van-button>
      </div>

      <div class="section-title">成员列表</div>

      <div class="member-list">
        <div v-for="member in members" :key="member.userId" class="member-item card">
          <van-image
            round
            width="40"
            height="40"
            :src="member.avatarUrl"
            fit="cover"
          >
            <template #error>
              <div class="avatar-mini">{{ (member.nickname || '?')[0] }}</div>
            </template>
          </van-image>
          <div class="member-info">
            <div class="member-name">{{ member.nickname }}</div>
            <span class="role-tag" :style="{ background: getRoleColor(member.role) + '18', color: getRoleColor(member.role) }">
              {{ getRoleLabel(member.role) }}
            </span>
          </div>
          <van-icon
            v-if="member.role === 2"
            name="delete-o"
            size="20"
            color="#FF3B30"
            @click="handleRemoveMember(member)"
          />
        </div>
      </div>

      <div v-if="members.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <div class="empty-text">暂无成员</div>
      </div>
    </template>

    <van-dialog
      v-model:show="showCreateDialog"
      title="创建团队"
      show-cancel-button
      :before-close="() => true"
      @confirm="handleCreateTeam"
    >
      <div style="padding: 16px">
        <van-field v-model="teamName" label="团队名称" placeholder="请输入团队名称" />
      </div>
    </van-dialog>
  </div>
</template>

<style lang="scss" scoped>
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.team-info {
  .team-header {
    display: flex;
    align-items: center;
    gap: 14px;

    .team-icon {
      width: 52px;
      height: 52px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      background: #007AFF15;
      border-radius: 14px;
    }

    .team-detail {
      .team-name {
        font-size: 18px;
        font-weight: 600;
      }

      .team-meta {
        font-size: 13px;
        color: var(--color-text-secondary);
        margin-top: 2px;
      }
    }
  }
}

.action-section {
  padding: 16px;
}

.member-list {
  .member-item {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 0 16px 8px;
    padding: 12px 16px;

    .avatar-mini {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: var(--color-primary);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      font-weight: 600;
    }

    .member-info {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 8px;

      .member-name {
        font-size: 15px;
        font-weight: 500;
      }

      .role-tag {
        font-size: 11px;
        font-weight: 500;
        padding: 2px 8px;
        border-radius: 10px;
      }
    }
  }
}
</style>
