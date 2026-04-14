import type { PageQuery } from './common'

export interface PromotionInfoVO {
  level: string
  totalEarnings: number
  monthlyEarnings: number
  inviteCode: string
  inviteLink: string
  inviteCount: number
  pointsBalance: number
  commissionRate: number
}

export interface InviteRecordVO {
  id: string
  inviteePhone: string
  inviteeNickname: string
  inviteeAvatar: string
  status: 'registered' | 'subscribed'
  commission: number
  createdAt: string
}

export interface InviteRecordQueryDTO extends PageQuery {
  status?: string
}

export interface WithdrawalCreateDTO {
  amount: number
  method: 'wechat' | 'alipay' | 'bank'
  account: string
  accountName: string
}

export interface WithdrawalVO {
  id: string
  amount: number
  method: string
  status: 'pending' | 'processing' | 'completed' | 'rejected'
  createdAt: string
  completedAt: string
}
