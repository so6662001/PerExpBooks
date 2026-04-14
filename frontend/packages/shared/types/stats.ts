export interface StatsOverviewVO {
  totalExpense: number
  reimbursedAmount: number
  pendingAmount: number
  tripDays: number
  tripCount: number
  invoiceCount: number
}

export interface MonthlyTrendVO {
  month: string
  expense: number
  reimbursed: number
}

export interface CategoryStatsVO {
  category: string
  amount: number
  count: number
  percentage: number
}

export interface StatsQueryDTO {
  year?: number
  month?: number
  startDate?: string
  endDate?: string
}
