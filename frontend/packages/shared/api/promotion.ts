import { get } from './request'
import type { PromotionInfoVO, InviteRecordVO, InviteRecordQueryDTO } from '../types/promotion'
import type { PageResult } from '../types/common'

export const getPromotionInfo = () =>
  get<PromotionInfoVO>('/promotion/info')

export const getInviteRecords = (params?: InviteRecordQueryDTO) =>
  get<PageResult<InviteRecordVO>>('/promotion/invite-records', { params })

export const getPromotionStats = () =>
  get<{ totalEarnings: number; monthlyEarnings: number; inviteCount: number }>('/promotion/stats')
