import { get, post } from './request'
import type { CouponVO } from '../types/member'

export const getMyCoupons = () =>
  get<CouponVO[]>('/coupon/my')

export const redeemCoupon = (code: string) =>
  post<CouponVO>('/coupon/redeem', { code })

export const getAvailableCoupons = (planId: string) =>
  get<CouponVO[]>('/coupon/available', { params: { planId } })
