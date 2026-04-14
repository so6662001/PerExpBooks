import { get } from './request'
import type { StatsOverviewVO, MonthlyTrendVO, CategoryStatsVO, StatsQueryDTO } from '../types/stats'

export const getStatsOverview = (params?: StatsQueryDTO) =>
  get<StatsOverviewVO>('/stats/overview', { params })

export const getMonthlyTrend = (params?: StatsQueryDTO) =>
  get<MonthlyTrendVO[]>('/stats/monthly-trend', { params })

export const getCategoryStats = (params?: StatsQueryDTO) =>
  get<CategoryStatsVO[]>('/stats/category', { params })
