import { get, post } from './request'

export const applyWithdrawal = (data: { amount: number; withdrawType: number }) =>
  post('/withdrawal/apply', data)

export const getWithdrawalRecords = () => get('/withdrawal/records')

export const getWithdrawalBalance = () => get('/withdrawal/balance')
