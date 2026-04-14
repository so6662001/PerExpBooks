import { get, post, put, del } from './request'
import type { InvoiceUploadVO, ExpenseCreateDTO, ExpenseVO, ExpenseQueryDTO, SubsidyCreateDTO } from '../types/expense'
import type { PageResult } from '../types/common'

export const uploadInvoice = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return post<InvoiceUploadVO>('/expense/upload-invoice', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const createExpense = (data: ExpenseCreateDTO) =>
  post<ExpenseVO>('/expense', data)

export const listExpenses = (params: ExpenseQueryDTO) =>
  get<PageResult<ExpenseVO>>('/expense/list', { params })

export const getExpenseDetail = (id: string) =>
  get<ExpenseVO>(`/expense/${id}`)

export const updateExpense = (id: string, data: Partial<ExpenseCreateDTO>) =>
  put<ExpenseVO>(`/expense/${id}`, data)

export const deleteExpense = (id: string) =>
  del(`/expense/${id}`)

export const createSubsidy = (data: SubsidyCreateDTO) =>
  post<ExpenseVO>('/expense/subsidy', data)

export const getRecentExpenses = (limit: number = 5) =>
  get<ExpenseVO[]>('/expense/recent', { params: { limit } })
