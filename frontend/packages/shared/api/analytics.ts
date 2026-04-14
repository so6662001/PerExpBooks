import { post } from './request'

export interface AnalyticsEvent {
  event: string
  page: string
  properties?: Record<string, any>
  timestamp?: number
}

export const trackEvent = (data: AnalyticsEvent) =>
  post('/analytics/track', {
    ...data,
    timestamp: data.timestamp || Date.now(),
  })

export const trackPageView = (page: string) =>
  post('/analytics/pageview', { page, timestamp: Date.now() })

export const trackBatchEvents = (events: AnalyticsEvent[]) =>
  post('/analytics/batch', { events })
