import { get, post } from './request'
import type { WithdrawalCreateDTO, WithdrawalVO } from '../types/promotion'
import type { PageResult, PageQuery } from '../types/common'

export const createWithdrawal = (data: WithdrawalCreateDTO) =>
  post<WithdrawalVO>('/withdrawal', data)

export const getWithdrawalRecords = (params?: PageQuery) =>
  get<PageResult<WithdrawalVO>>('/withdrawal/list', { params })

export const getWithdrawalDetail = (id: string) =>
  get<WithdrawalVO>(`/withdrawal/${id}`)
