import { get, post } from './request'
import type { CouponVO } from '../types/member'

export const listMyCoupons = (params?: { status?: number }) =>
  get<CouponVO[]>('/coupon/my', { params })

export const getAvailableCoupons = (params?: { planType?: number }) =>
  get<CouponVO[]>('/coupon/available', { params })

export const receiveCoupon = (templateId: number) =>
  post('/coupon/receive/' + templateId)

// Backward-compatible aliases
export const getMyCoupons = (params?: { status?: number }) =>
  listMyCoupons(params)
export const redeemCoupon = (code: string) =>
  post<CouponVO>('/coupon/redeem', { code })
