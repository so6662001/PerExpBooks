import { get, post, put, del } from './request'
import type { ReimbursementCreateDTO, ReimbursementVO, ExportOptions } from '../types/reimbursement'

export const generateReimbursement = (data: ReimbursementCreateDTO) =>
  post<ReimbursementVO>('/reimbursement/generate', data)

export const createReimbursement = generateReimbursement

export const listReimbursements = (params?: any) =>
  get<ReimbursementVO[]>('/reimbursement/list', params ? { params } : undefined)

export const getReimbursementDetail = (id: number | string) =>
  get<ReimbursementVO>(`/reimbursement/${id}`)

export const updateReimbursement = (id: number | string, data: any) =>
  put<ReimbursementVO>(`/reimbursement/${id}`, data)

export const confirmReceived = (id: number | string) =>
  put(`/reimbursement/${id}/received`)

export const markReceived = confirmReceived

export const getPdfUrl = (id: number | string) =>
  get<string>(`/reimbursement/${id}/pdf`)

export const getMergedPdfUrl = (id: number | string) =>
  get<string>(`/reimbursement/${id}/merged-pdf`)

export const getZipUrl = (id: number | string) =>
  get<string>(`/reimbursement/${id}/zip`)

export const sendEmail = (id: number | string, data: { email: string; attachType?: number; message?: string }) =>
  post(`/reimbursement/${id}/send-email`, data)

export const regenerateReimbursement = (id: number | string) =>
  post<ReimbursementVO>(`/reimbursement/${id}/regenerate`)

export const cancelReimbursement = (id: number | string) =>
  del(`/reimbursement/${id}`)

export const exportReimbursement = async (id: number | string, options: ExportOptions) => {
  if (options.type === 'merged_pdf') {
    const url = await getMergedPdfUrl(id)
    return { url }
  } else if (options.type === 'zip') {
    const url = await getZipUrl(id)
    return { url }
  } else if (options.type === 'report_only') {
    const url = await getPdfUrl(id)
    return { url }
  } else if (options.type === 'email' && options.email) {
    await sendEmail(id, { email: options.email, attachType: options.attachType })
    return {}
  }
  return {}
}

