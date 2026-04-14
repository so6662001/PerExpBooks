import { get, post } from './request'
import type { MemberStatusVO } from '../types/user'
import type { MemberPlanVO, MemberOrderDTO, MemberOrderVO } from '../types/member'

export const getMemberStatus = () =>
  get<MemberStatusVO>('/member/status')

export const getMemberPlans = () =>
  get<MemberPlanVO[]>('/member/plans')

export const createMemberOrder = (data: MemberOrderDTO) =>
  post<MemberOrderVO>('/member/create-order', data)

export const checkPaymentStatus = (orderId: string) =>
  get<{ paid: boolean }>(`/member/order/${orderId}/status`)
