import type { PageQuery } from './common'

export interface InvoiceUploadVO {
  invoiceId: string
  invoiceType: string
  invoiceNo: string
  invoiceDate: string
  amount: number
  seller: string
  buyer: string
  items: InvoiceItemVO[]
  ocrConfidence: number
  fileUrl: string
}

export interface InvoiceItemVO {
  name: string
  quantity: number
  unitPrice: number
  amount: number
  taxRate: number
  taxAmount: number
}

export interface ExpenseCreateDTO {
  invoiceId?: string
  category: string
  amount: number
  description: string
  expenseDate: string
  tripId?: string
  attachments?: string[]
}

export interface ExpenseVO {
  id: string
  userId: string
  invoiceId: string
  category: string
  amount: number
  description: string
  expenseDate: string
  status: ExpenseStatus
  tripId: string
  reimbursementId: string
  attachments: string[]
  invoiceInfo: InvoiceUploadVO
  createdAt: string
  updatedAt: string
}

export type ExpenseStatus = 'pending' | 'reimbursing' | 'reimbursed'

export interface ExpenseQueryDTO extends PageQuery {
  status?: ExpenseStatus
  category?: string
  startDate?: string
  endDate?: string
  tripId?: string
}

export interface SubsidyCreateDTO {
  category: string
  amount: number
  description: string
  expenseDate: string
  tripId?: string
}
