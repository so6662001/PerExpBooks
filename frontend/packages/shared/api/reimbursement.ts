import { get, post } from './request'
import type { ReimbursementCreateDTO, ReimbursementVO, ReimbursementQueryDTO, ExportOptions } from '../types/reimbursement'
import type { PageResult } from '../types/common'

export const createReimbursement = (data: ReimbursementCreateDTO) =>
  post<ReimbursementVO>('/reimbursement', data)

export const listReimbursements = (params: ReimbursementQueryDTO) =>
  get<PageResult<ReimbursementVO>>('/reimbursement/list', { params })

export const getReimbursementDetail = (id: string) =>
  get<ReimbursementVO>(`/reimbursement/${id}`)

export const exportReimbursement = (id: string, options: ExportOptions) =>
  post<{ url?: string }>(`/reimbursement/${id}/export`, options)

export const markReceived = (id: string) =>
  post(`/reimbursement/${id}/received`)

export const getPendingExpenses = () =>
  get('/reimbursement/pending-expenses')
