import { get, post, put, del } from './request'

export interface TeamVO {
  id: string
  name: string
  ownerId: string
  memberCount: number
  planName: string
  createdAt: string
}

export interface TeamMemberVO {
  userId: string
  nickname: string
  avatar: string
  role: 'owner' | 'admin' | 'member'
  joinedAt: string
}

export const getMyTeam = () =>
  get<TeamVO>('/team')

export const createTeam = (data: { name: string }) =>
  post<TeamVO>('/team', data)

export const updateTeam = (data: { name: string }) =>
  put<TeamVO>('/team', data)

export const getTeamMembers = () =>
  get<TeamMemberVO[]>('/team/members')

export const inviteTeamMember = (data: { phone: string; role: string }) =>
  post('/team/invite', data)

export const removeTeamMember = (userId: string) =>
  del(`/team/members/${userId}`)
