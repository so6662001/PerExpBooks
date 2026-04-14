import type { PageQuery } from './common'

export interface InvoiceUploadVO {
  fileUrl: string
  fileName: string
  invoiceNo: string
  invoiceCode: string
  invoiceDate: string
  amount: number
  taxAmount: number
  sellerName: string
  buyerName: string
  invoiceType: string
  parsedSuccess: boolean
  parseSuccess: boolean
  parseMessage: string
}

export interface ExpenseCreateDTO {
  categoryId: number
  tripId?: number
  type: number
  amount: number
  taxAmount?: number
  invoiceNo?: string
  invoiceCode?: string
  invoiceDate?: string
  invoiceType?: string
  sellerName?: string
  buyerName?: string
  fileUrl?: string
  fileName?: string
  description?: string
  expenseDate: string
}

export interface ExpenseVO {
  id: number
  userId: number
  categoryId: number
  categoryName: string
  tripId: number
  type: number
  amount: number
  taxAmount: number
  invoiceNo: string
  invoiceCode: string
  invoiceDate: string
  invoiceType: string
  sellerName: string
  buyerName: string
  fileUrl: string
  fileName: string
  description: string
  expenseDate: string
  reimburseStatus: number
  reimbursementId: number
  dataSign: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface ExpenseQueryDTO extends PageQuery {
  categoryId?: number
  type?: number
  reimburseStatus?: number
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}
