import { post } from './request'
import type { SmsLoginDTO, LoginVO } from '../types/user'

export const sendSms = (data: { phone: string }) =>
  post('/auth/send-sms', data)

export const smsLogin = (data: SmsLoginDTO) =>
  post<LoginVO>('/auth/sms-login', data)

export const logout = () =>
  post('/auth/logout')

export const refreshToken = () =>
  post<{ token: string }>('/auth/refresh-token')
