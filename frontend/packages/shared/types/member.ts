export interface PlanVO {
  planType: number
  name: string
  price: number
  originalPrice: number
  features: string[]
  recommended: boolean
}

export interface MemberStatusVO {
  memberType: number
  memberStatus: number
  expireTime: string
  daysLeft: number
  quotaInfo: QuotaInfo
}

export interface QuotaInfo {
  monthlyInvoiceUsed: number
  monthlyInvoiceLimit: number
  monthlyReimburseUsed: number
  monthlyReimburseLimit: number
}

export interface CreateOrderDTO {
  planType: number
  payType: number
  couponId?: number
  teamMemberCount?: number
}

export interface OrderVO {
  id: number
  orderNo: string
  planType: number
  planName: string
  originalAmount: number
  discountAmount: number
  payAmount: number
  payType: number
  payStatus: number
  payTime: string
  tradeNo: string
  memberStart: string
  memberEnd: string
  isRenewal: number
  refundStatus: number
  createdAt: string
}

export interface CouponVO {
  id: number
  name: string
  type: number
  typeName: string
  discountValue: number
  minAmount: number
  applicablePlanType: number
  useStatus: number
  expireAt: string
  createdAt: string
}

// Keep old types as aliases for backward compatibility
export type MemberPlanVO = PlanVO
export type MemberOrderDTO = CreateOrderDTO
export type MemberOrderVO = OrderVO
