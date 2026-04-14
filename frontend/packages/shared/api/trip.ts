import { get, post, put, del } from './request'
import type { TripCreateDTO, TripVO, TripQueryDTO } from '../types/trip'
import type { PageResult } from '../types/common'

export const createTrip = (data: TripCreateDTO) =>
  post<TripVO>('/trip', data)

export const listTrips = (params: TripQueryDTO) =>
  get<PageResult<TripVO>>('/trip/list', { params })

export const getTripDetail = (id: string) =>
  get<TripVO>(`/trip/${id}`)

export const updateTrip = (id: string, data: Partial<TripCreateDTO>) =>
  put<TripVO>(`/trip/${id}`, data)

export const deleteTrip = (id: string) =>
  del(`/trip/${id}`)

export const completeTrip = (id: string) =>
  post(`/trip/${id}/complete`)
