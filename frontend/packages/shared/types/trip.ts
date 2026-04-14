import type { PageQuery } from './common'

export interface TripCreateDTO {
  destination: string
  startDate: string
  endDate: string
  purpose: string
  budget?: number
  remark?: string
}

export interface TripVO {
  id: string
  userId: string
  destination: string
  startDate: string
  endDate: string
  days: number
  purpose: string
  budget: number
  totalExpense: number
  expenseCount: number
  status: TripStatus
  remark: string
  createdAt: string
}

export type TripStatus = 'planned' | 'ongoing' | 'completed' | 'cancelled'

export interface TripQueryDTO extends PageQuery {
  status?: TripStatus
  startDate?: string
  endDate?: string
}
