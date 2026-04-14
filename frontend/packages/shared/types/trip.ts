import type { PageQuery } from './common'
import type { ExpenseVO } from './expense'

export interface TripCreateDTO {
  title: string
  destination: string
  startDate: string
  endDate: string
  subsidyPerDay: number
  remark?: string
}

export interface TripVO {
  id: number
  userId: number
  title: string
  destination: string
  startDate: string
  endDate: string
  days: number
  subsidyPerDay: number
  subsidyTotal: number
  remark: string
  status: number
  createdAt: string
  updatedAt: string
  expenseCount: number
  expenses: ExpenseVO[]
  [key: string]: any
}

export interface TripQueryDTO extends PageQuery {
  status?: string
  startDate?: string
  endDate?: string
}
