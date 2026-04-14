import type { PageQuery } from './common'
import type { ExpenseVO } from './expense'

export interface ReimbursementCreateDTO {
  title: string
  remark?: string
  expenseIds: string[]
}

export interface ReimbursementVO {
  id: string
  userId: string
  title: string
  totalAmount: number
  itemCount: number
  status: ReimbursementStatus
  remark: string
  expenses: ExpenseVO[]
  createdAt: string
  updatedAt: string
  exportedAt: string
}

export type ReimbursementStatus = 'generated' | 'exported' | 'received'

export interface ReimbursementQueryDTO extends PageQuery {
  status?: ReimbursementStatus
}

export interface ExportOptions {
  type: 'merged_pdf' | 'zip' | 'report_only' | 'email'
  email?: string
}
