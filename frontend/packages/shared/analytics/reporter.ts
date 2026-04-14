import type { TrackerConfig, EventData, PerformanceData, ErrorData } from './types'

const CACHE_KEY = 'qianku_analytics_cache'
const MAX_CACHE_SIZE = 500

export class Reporter {
  private eventQueue: EventData[] = []
  private performanceQueue: PerformanceData[] = []
  private errorQueue: ErrorData[] = []
  private timer: ReturnType<typeof setInterval> | null = null
  private config: TrackerConfig

  constructor(config: TrackerConfig) {
    this.config = config
    this.startTimer()
    this.replayFromLocal()
    this.bindUnload()
  }

  addEvent(event: EventData): void {
    this.eventQueue.push(event)
    if (this.eventQueue.length >= this.config.batchSize) {
      this.flush()
    }
  }

  addPerformance(perf: PerformanceData): void {
    this.performanceQueue.push(perf)
    if (this.performanceQueue.length >= this.config.batchSize) {
      this.flushPerformance()
    }
  }

  addError(error: ErrorData): void {
    this.errorQueue.push(error)
    this.flushImmediate(error, this.config.errorUrl)
  }

  flushImmediate(data: any, url: string): void {
    this.send(url, Array.isArray(data) ? data : [data])
  }

  private startTimer(): void {
    if (this.timer) return
    this.timer = setInterval(() => {
      this.flush()
      this.flushPerformance()
    }, this.config.flushInterval)
  }

  private flush(): void {
    if (this.eventQueue.length === 0) return
    const batch = this.eventQueue.splice(0, this.config.batchSize)
    this.send(this.config.reportUrl, batch)
  }

  private flushPerformance(): void {
    if (this.performanceQueue.length === 0) return
    const batch = this.performanceQueue.splice(0, this.config.batchSize)
    this.send(this.config.performanceUrl, batch)
  }

  private async send(url: string, data: any[]): Promise<void> {
    try {
      const blob = new Blob([JSON.stringify(data)], { type: 'application/json' })
      if (typeof navigator !== 'undefined' && navigator.sendBeacon) {
        const success = navigator.sendBeacon(url, blob)
        if (!success) {
          await this.fetchFallback(url, blob)
        }
      } else {
        await this.fetchFallback(url, blob)
      }
    } catch {
      this.cacheToLocal(data as EventData[])
    }
  }

  private async fetchFallback(url: string, blob: Blob): Promise<void> {
    await fetch(url, { method: 'POST', body: blob, keepalive: true })
  }

  private cacheToLocal(events: EventData[]): void {
    try {
      const cached = localStorage.getItem(CACHE_KEY)
      let existing: EventData[] = cached ? JSON.parse(cached) : []
      existing = existing.concat(events)
      if (existing.length > MAX_CACHE_SIZE) {
        existing = existing.slice(-MAX_CACHE_SIZE)
      }
      localStorage.setItem(CACHE_KEY, JSON.stringify(existing))
    } catch {
      // localStorage unavailable or full
    }
  }

  private replayFromLocal(): void {
    try {
      const cached = localStorage.getItem(CACHE_KEY)
      if (!cached) return
      const events: EventData[] = JSON.parse(cached)
      if (events.length > 0) {
        localStorage.removeItem(CACHE_KEY)
        this.send(this.config.reportUrl, events)
      }
    } catch {
      // localStorage unavailable
    }
  }

  private bindUnload(): void {
    if (typeof window === 'undefined') return
    window.addEventListener('beforeunload', () => {
      this.flushAll()
    })
    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden') {
          this.flushAll()
        }
      })
    }
  }

  private flushAll(): void {
    if (this.eventQueue.length > 0) {
      const events = this.eventQueue.splice(0)
      this.send(this.config.reportUrl, events)
    }
    if (this.performanceQueue.length > 0) {
      const perfs = this.performanceQueue.splice(0)
      this.send(this.config.performanceUrl, perfs)
    }
  }

  destroy(): void {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
    this.flushAll()
  }
}
