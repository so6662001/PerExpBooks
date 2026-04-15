import { get, put, post, del } from './request'
import type { UserVO, UserUpdateDTO } from '../types/user'

export const getUserProfile = () =>
  get<UserVO>('/user/profile')

export const updateUserProfile = (data: UserUpdateDTO) =>
  put<UserVO>('/user/profile', data)

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return post<{ url: string }>('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const deleteAccount = () =>
  del('/user/account')
