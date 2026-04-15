import { get, post } from './request'

export const logShare = (data: { shareType: string; contentType: string; shareScene: string }) =>
  post('/share/log', data)

export const getShareTemplates = () => get('/share/templates')
