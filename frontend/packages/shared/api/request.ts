import axios, { type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { Result } from '../types/common'
import { Tracker } from '../analytics/tracker'

const instance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

let onAgreementRequired: (() => void) | null = null

export function setAgreementRequiredHandler(handler: () => void) {
  onAgreementRequired = handler
}

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    ;(config as any).__startTime = Date.now()
    return config
  },
  (error) => Promise.reject(error),
)

function reportApiPerformance(config: InternalAxiosRequestConfig | undefined, statusCode: number, durationMs: number) {
  if (!config) return
  try {
    const tracker = Tracker.getInstance()
    tracker.track('api_request', {
      apiPath: config.url,
      method: config.method?.toUpperCase(),
      statusCode,
      durationMs,
      isTimeout: durationMs > 15000,
    })
  } catch {}
}

instance.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    reportApiPerformance(response.config, response.status, Date.now() - ((response.config as any).__startTime || Date.now()))

    const { code, message, data } = response.data

    if (code === 4002) {
      onAgreementRequired?.()
      return Promise.reject(new Error('AGREEMENT_REQUIRED'))
    }

    if (code === 401) {
      localStorage.removeItem('token')
      window.location.href = '/auth/login'
      return Promise.reject(new Error('Unauthorized'))
    }

    if (code !== 200) {
      return Promise.reject(new Error(message || 'Request failed'))
    }

    return data as any
  },
  (error) => {
    if (error.response) {
      reportApiPerformance(error.config, error.response.status, Date.now() - ((error.config as any)?.__startTime || Date.now()))
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/auth/login'
    }
    return Promise.reject(error)
  },
)

export function get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return instance.get(url, config) as Promise<T>
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return instance.post(url, data, config) as Promise<T>
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return instance.put(url, data, config) as Promise<T>
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return instance.delete(url, config) as Promise<T>
}

export default instance
