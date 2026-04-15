import { get, post, put, del } from './request'
import type { TripCreateDTO, TripVO } from '../types/trip'

export const createTrip = (data: TripCreateDTO) =>
  post<TripVO>('/trip', data)

export const listTrips = (params?: any) =>
  get<TripVO[]>('/trip/list', params ? { params } : undefined)

export const getTripDetail = (id: number | string) =>
  get<TripVO>(`/trip/${id}`)

export const updateTrip = (id: number | string, data: Partial<TripCreateDTO>) =>
  put<TripVO>(`/trip/${id}`, data)

export const deleteTrip = (id: number | string) =>
  del(`/trip/${id}`)

export const completeTrip = (id: number | string) =>
  put(`/trip/${id}`, { status: 'completed' })
