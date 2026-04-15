import { get, post, del } from './request'

export interface TeamVO {
  id: number
  name: string
  ownerId: number
  ownerNickname: string
  inviteCode: string
  memberCount: number
  maxMember: number
  myRole: number
  createdAt: string
}

export interface TeamMemberVO {
  userId: number
  nickname: string
  avatarUrl: string
  role: number
  joinedAt: string
}

export const createTeam = (data: { name: string }) =>
  post<TeamVO>('/team/create', data)

export const getTeamInfo = () =>
  get<TeamVO>('/team/info')

export const generateInviteLink = () =>
  post<{ inviteCode: string; inviteLink: string }>('/team/invite')

export const joinTeam = (data: { inviteCode: string }) =>
  post('/team/join', data)

export const removeMember = (userId: number) =>
  del('/team/member/' + userId)

export const listMembers = () =>
  get<TeamMemberVO[]>('/team/members')

export const getTeamStats = () =>
  get<Record<string, any>>('/team/stats')

// Backward-compatible aliases
export const getMyTeam = getTeamInfo
export const getTeamMembers = listMembers
export const removeTeamMember = removeMember
export const inviteTeamMember = (data: { phone: string; role: string }) =>
  post('/team/invite', data)
export const updateTeam = (data: { name: string }) =>
  post('/team/create', data)
