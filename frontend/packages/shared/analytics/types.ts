import type { Tracker } from './tracker'

export interface TrackerConfig {
  appVersion: string
  platform: 'miniprogram' | 'h5_mobile' | 'h5_desktop'
  reportUrl: string
  performanceUrl: string
  errorUrl: string
  batchSize: number
  flushInterval: number
  enablePerformance: boolean
  enableError: boolean
  enableClick: boolean
}

export interface EventData {
  eventId: string
  eventType: 'page_view' | 'click' | 'exposure' | 'custom'
  eventName: string
  userId: number
  sessionId: string
  pagePath: string
  pageTitle: string
  referrerPath: string
  platform: string
  deviceModel: string
  os: string
  osVersion: string
  screenWidth: number
  screenHeight: number
  networkType: string
  appVersion: string
  memberType: string
  extra: Record<string, any>
  timestamp: number
}

export interface PerformanceData {
  sessionId: string
  pagePath: string
  platform: string
  fcp: number
  lcp: number
  fid: number
  cls: number
  ttfb: number
  loadTime: number
  inp: number
  domReady: number
  resourceCount: number
  resourceSizeKb: number
  jsHeapSizeMb: number
  networkType: string
  timestamp: number
}

export interface ErrorData {
  sessionId: string
  errorType: 'js_error' | 'promise_error' | 'resource_error' | 'api_error'
  errorMessage: string
  errorStack: string
  pagePath: string
  userActionBefore: string[]
  platform: string
  deviceModel: string
  os: string
  appVersion: string
  timestamp: number
}

export interface Plugin {
  name: string
  install(tracker: Tracker): void
  destroy?(): void
}

export const DEFAULT_CONFIG: TrackerConfig = {
  appVersion: '1.0.0',
  platform: 'h5_desktop',
  reportUrl: '/api/v1/analytics/report',
  performanceUrl: '/api/v1/analytics/performance',
  errorUrl: '/api/v1/analytics/error',
  batchSize: 10,
  flushInterval: 5000,
  enablePerformance: true,
  enableError: true,
  enableClick: true,
}
