export interface SmsLoginDTO {
  phone: string
  code: string
  inviteCode?: string
}

export interface LoginVO {
  token: string
  userId: string
  phone: string
  nickname: string
  avatar: string
  isNewUser: boolean
}

export interface UserVO {
  id: string
  phone: string
  nickname: string
  avatar: string
  email: string
  company: string
  department: string
  createdAt: string
}

export interface UserUpdateDTO {
  nickname?: string
  avatar?: string
  email?: string
  company?: string
  department?: string
}

export interface MemberStatusVO {
  isMember: boolean
  level: string
  expireAt: string
  remainingDays: number
  monthlyQuota: number
  usedQuota: number
}
