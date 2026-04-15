import { get } from './request'

export const getRealtimeOverview = () => get('/admin/analytics/realtime')

export const getPageStats = (params: { startDate: string; endDate: string }) =>
  get('/admin/analytics/page-stats', { params })

export const getFeatureUsage = (params: { startDate: string; endDate: string }) =>
  get('/admin/analytics/feature-usage', { params })

export const getSlowApis = (params: { startDate: string; endDate: string }) =>
  get('/admin/analytics/slow-apis', { params })

export const getErrors = (params: { startDate: string; endDate: string }) =>
  get('/admin/analytics/errors', { params })

export const getUserRetention = (params: { startDate: string; endDate: string }) =>
  get('/admin/analytics/user-retention', { params })

export const getConversionFunnel = (params: { startDate: string; endDate: string }) =>
  get('/admin/analytics/conversion-funnel', { params })
