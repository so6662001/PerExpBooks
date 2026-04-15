export interface StatsOverviewVO {
  totalExpense: number
  totalReimbursed: number
  totalPending: number
  totalTripDays: number
  tripCount: number
  invoiceCount: number
}

export interface MonthlyTrendVO {
  month: string
  amount: number
  reimbursed: number
}

export interface CategoryRatioVO {
  categoryId: number
  categoryName: string
  amount: number
  ratio: number
}

export interface TripSummaryVO {
  tripCount: number
  totalDays: number
  totalSubsidy: number
  cityDistribution: CityStats[]
}

export interface CityStats {
  city: string
  count: number
  amount: number
}

export interface ReimburseProgressVO {
  month: string
  submitted: number
  received: number
  pending: number
}

export interface YearlyCompareVO {
  month: string
  currentYear: number
  lastYear: number
}

export interface CalendarDayVO {
  date: string
  amount: number
  count: number
}
