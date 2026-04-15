export interface LevelInfoVO {
  level: number
  levelName: string
  level1Rate: number
  level2Rate: number
  inviteCount: number
  paidInviteCount: number
  totalCommission: number
  points: number
  totalPoints: number
  nextLevelInviteRequired: number | null
  nextLevelName: string | null
}

export interface DashboardVO {
  levelInfo: LevelInfoVO
  totalCommission: number
  availableBalance: number
  frozenBalance: number
  points: number
  totalInvite: number
  paidInvite: number
}

export interface InviteCodeVO {
  inviteCode: string
  inviteLink: string
}

export interface InviteRecordVO {
  inviteeId: number
  inviteeNickname: string
  inviteeAvatarUrl: string
  inviteePhone?: string
  level: number
  inviteeStatus: number
  hasPaid?: boolean
  createdAt: string
}

export interface CommissionVO {
  id: number
  inviteeId: number
  inviteeNickname: string
  level: number
  commissionType: number
  orderAmount: number
  commissionRate: number
  commissionAmount: number
  status: number
  statusName: string
  settleTime: string
  createdAt: string
}

export interface BalanceVO {
  availableBalance: number
  frozenBalance: number
  totalCommission: number
  withdrawnAmount: number
}

export interface WithdrawalVO {
  id: number
  amount: number
  withdrawType: number
  withdrawTypeName: string
  status: number
  statusName: string
  rejectReason: string
  processedAt: string
  createdAt: string
}

export interface PointsLogVO {
  id: number
  points: number
  balanceAfter: number
  action: string
  remark: string
  createdAt: string
}
