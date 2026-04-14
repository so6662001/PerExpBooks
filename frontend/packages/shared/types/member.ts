export interface MemberPlanVO {
  id: string
  name: string
  level: string
  price: number
  originalPrice: number
  duration: number
  durationUnit: 'month' | 'year'
  features: string[]
  monthlyQuota: number
  recommended: boolean
}

export interface MemberOrderDTO {
  planId: string
  couponId?: string
  paymentMethod: string
}

export interface MemberOrderVO {
  orderId: string
  planName: string
  amount: number
  paymentUrl: string
  expireAt: string
}

export interface CouponVO {
  id: string
  code: string
  type: 'discount' | 'amount'
  value: number
  minAmount: number
  expireAt: string
  used: boolean
}
