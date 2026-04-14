import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserVO, MemberStatusVO } from '../types/user'
import { getUserProfile } from '../api/user'
import { getMemberStatus } from '../api/member'

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
