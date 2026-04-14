import type { PageQuery } from './common'
import type { ExpenseVO } from './expense'

export interface ReimbursementCreateDTO {
  title: string
  remark?: string
  expenseIds: number[]
  tripId?: number
}

export interface ReimbursementVO {
  id: number
  userId: number
  reimburseNo: string
  title: string
  totalAmount: number
  invoiceCount: number
  itemCount: number
  remark: string
  pdfUrl: string
  mergedPdfUrl: string
  zipUrl: string
  tripId: number
  reimburseStatus: number
  exportCount: number
  exportedAt: string
  emailSent: number
  emailAddress: string
  emailSentAt: string
  receivedAt: string
  status: number
  createdAt: string
  updatedAt: string
  expenses: ExpenseVO[]
}

export interface ReimbursementQueryDTO extends PageQuery {
  reimburseStatus?: number
}

export type ReimbursementStatus = 'generated' | 'exported' | 'received'

export interface ExportOptions {
  type: 'merged_pdf' | 'zip' | 'report_only' | 'email'
  email?: string
}
