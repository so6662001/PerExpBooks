import { get, post } from './request'

export const getPromotionDashboard = () => get('/promotion/dashboard')

export const getInviteCode = () => get('/promotion/invite-code')

export const getInviteRecords = () => get('/promotion/invite-records')

export const getCommissionRecords = () => get('/promotion/commission')

export const getPointsLog = () => get('/promotion/points-log')

export const redeemPoints = (data: { redeemType: string }) => post('/promotion/points/redeem', data)

export const getLevelInfo = () => get('/promotion/level-info')

export const getPoster = (params?: { type?: string }) => get('/promotion/poster', { params })

export const generateCustomCard = (data: { cardType: string }) => post('/promotion/poster/custom', data)

export const recordDailyShare = () => post('/promotion/daily-share')
