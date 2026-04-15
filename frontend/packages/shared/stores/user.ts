import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserVO } from '../types/user'
import type { MemberStatusVO } from '../types/member'

import { getUserProfile } from '../api/user'
import { getMemberStatus } from '../api/member'
import { Tracker } from '../analytics/tracker'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<UserVO | null>(null)
  const memberStatus = ref<MemberStatusVO | null>(null)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    memberStatus.value = null
    localStorage.removeItem('token')
  }

  async function fetchProfile() {
    try {
      userInfo.value = await getUserProfile()
      try {
        const tracker = Tracker.getInstance()
        tracker.setUser(Number(userInfo.value!.id), String(userInfo.value!.memberType || 'free'))
      } catch {}
    } catch (e) {
      console.error('Failed to fetch profile:', e)
    }
  }

  async function fetchMemberStatus() {
    try {
      memberStatus.value = await getMemberStatus()
    } catch (e) {
      console.error('Failed to fetch member status:', e)
    }
  }

  return {
    token,
    userInfo,
    memberStatus,
    setToken,
    logout,
    fetchProfile,
    fetchMemberStatus,
  }
})
