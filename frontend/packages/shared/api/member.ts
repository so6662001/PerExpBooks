import { get, post } from './request'
import type { PlanVO, OrderVO, CreateOrderDTO, MemberStatusVO } from '../types/member'

export const getPlans = () =>
  get<PlanVO[]>('/member/plans')

export const getMemberStatus = () =>
  get<MemberStatusVO>('/member/status')

export const createMemberOrder = (data: CreateOrderDTO) =>
  post<OrderVO>('/member/create-order', data)

export const listOrders = () =>
  get<OrderVO[]>('/member/orders')

export const getOrderDetail = (orderNo: string) =>
  get<OrderVO>('/member/order/' + orderNo)

export const applyRefund = (orderNo: string) =>
  post('/member/refund/' + orderNo)

// Keep old function names as aliases for backward compatibility
export const getMemberPlans = getPlans
