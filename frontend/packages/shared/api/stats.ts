import { get } from './request'
import type {
  StatsOverviewVO,
  MonthlyTrendVO,
  CategoryRatioVO,
  TripSummaryVO,
  ReimburseProgressVO,
  YearlyCompareVO,
  CityStats,
  CalendarDayVO,
} from '../types/stats'

export const getStatsOverview = (params?: { year?: number }) =>
  get<StatsOverviewVO>('/stats/overview', { params })

export const getMonthlyTrend = (params: { year: number }) =>
  get<MonthlyTrendVO[]>('/stats/monthly-trend', { params })

export const getCategoryRatio = (params?: { startDate?: string; endDate?: string }) =>
  get<CategoryRatioVO[]>('/stats/category-ratio', { params })

export const getTripSummary = (params?: { year?: number }) =>
  get<TripSummaryVO>('/stats/trip-summary', { params })

export const getReimburseProgress = (params?: { year?: number }) =>
  get<ReimburseProgressVO[]>('/stats/reimburse-progress', { params })

export const getYearlyCompare = (params?: { year?: number }) =>
  get<YearlyCompareVO[]>('/stats/yearly-compare', { params })

export const getCityRanking = (params?: { year?: number }) =>
  get<CityStats[]>('/stats/city-ranking', { params })

export const getExpenseCalendar = (params: { year: number; month: number }) =>
  get<CalendarDayVO[]>('/stats/calendar', { params })

export const exportStats = (params?: { startDate?: string; endDate?: string; categoryId?: number }) =>
  get('/stats/export', { params, responseType: 'blob' })
