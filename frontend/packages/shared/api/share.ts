import { get, post } from './request'

export interface ShareConfig {
  title: string
  description: string
  imageUrl: string
  link: string
}

export const getShareConfig = (type: string, id?: string) =>
  get<ShareConfig>('/share/config', { params: { type, id } })

export const createShareLink = (data: { type: string; id?: string }) =>
  post<{ shortLink: string }>('/share/link', data)

export const recordShare = (data: { type: string; platform: string; id?: string }) =>
  post('/share/record', data)
