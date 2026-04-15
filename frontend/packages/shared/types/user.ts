import type { AgreementCheckVO } from '../api/agreement'

export interface SmsLoginDTO {
  phone: string
  code: string
  inviteCode?: string
}

export interface LoginVO {
  token: string
  isNew: boolean
  user: UserVO
  memberStatus: UserMemberStatusVO | null
  agreementCheck: AgreementCheckVO | null
}

export interface UserVO {
  id: string
  phone: string
  nickname: string
  avatarUrl: string
  company: string
  department: string
  inviteCode: string
  memberType: number
  memberStatus: number
  memberExpireTime: string
  trialEndTime: string
  teamId: string
  monthlyInvoiceUsed: number
  monthlyReimburseUsed: number
  defaultSubsidy: number
  createdAt: string
}

export interface UserUpdateDTO {
  nickname?: string
  avatarUrl?: string
  company?: string
  department?: string
  defaultSubsidy?: number
}

export interface UserMemberStatusVO {
  memberType: number
  memberStatus: number
  memberExpireTime: string
  trialEndTime: string
  isTrial: boolean
  isExpired: boolean
}
