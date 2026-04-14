import type { Plugin, PerformanceData } from '../types'
import type { Tracker } from '../tracker'

export class PerformancePlugin implements Plugin {
  name = 'performance'
  private tracker: Tracker | null = null
  private observers: PerformanceObserver[] = []
  private metrics: Partial<PerformanceData> = {}
  private reported = false

  install(tracker: Tracker): void {
    this.tracker = tracker
    if (typeof window === 'undefined' || typeof PerformanceObserver === 'undefined') return

    this.observePaint()
    this.observeLCP()
    this.observeFID()
    this.observeCLS()
    this.observeINP()

    if (document.readyState === 'complete') {
      this.onLoad()
    } else {
      window.addEventListener('load', () => this.onLoad(), { once: true })
    }
  }

  private observePaint(): void {
    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.name === 'first-contentful-paint') {
            this.metrics.fcp = Math.round(entry.startTime)
          }
        }
      })
      observer.observe({ type: 'paint', buffered: true })
      this.observers.push(observer)
    } catch {
      // paint observer not supported
    }
  }

  private observeLCP(): void {
    try {
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries()
        if (entries.length > 0) {
          const last = entries[entries.length - 1]
          this.metrics.lcp = Math.round(last.startTime)
        }
      })
      observer.observe({ type: 'largest-contentful-paint', buffered: true })
      this.observers.push(observer)
    } catch {
      // LCP observer not supported
    }
  }

  private observeFID(): void {
    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          const fidEntry = entry as PerformanceEventTiming
          this.metrics.fid = Math.round(fidEntry.processingStart - fidEntry.startTime)
        }
      })
      observer.observe({ type: 'first-input', buffered: true })
      this.observers.push(observer)
    } catch {
      // FID observer not supported
    }
  }

  private observeCLS(): void {
    let clsValue = 0
    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (!(entry as any).hadRecentInput) {
            clsValue += (entry as any).value
          }
        }
        this.metrics.cls = Math.round(clsValue * 1000) / 1000
      })
      observer.observe({ type: 'layout-shift', buffered: true })
      this.observers.push(observer)
    } catch {
      // CLS observer not supported
    }
  }

  private observeINP(): void {
    let maxDuration = 0
    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          const duration = (entry as PerformanceEventTiming).duration
          if (duration > maxDuration) {
            maxDuration = duration
            this.metrics.inp = Math.round(duration)
          }
        }
      })
      observer.observe({ type: 'event', buffered: true })
      this.observers.push(observer)
    } catch {
      // INP observer not supported
    }
  }

  private onLoad(): void {
    setTimeout(() => this.collectAndReport(), 3000)
  }

  private collectAndReport(): void {
    if (!this.tracker || this.reported) return
    this.reported = true

    const navEntries = performance.getEntriesByType('navigation') as PerformanceNavigationTiming[]
    const navEntry = navEntries[0]

    if (navEntry) {
      this.metrics.ttfb = Math.round(navEntry.responseStart - navEntry.requestStart)
      this.metrics.loadTime = Math.round(navEntry.loadEventEnd - navEntry.startTime)
      this.metrics.domReady = Math.round(navEntry.domContentLoadedEventEnd - navEntry.startTime)
    }

    const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[]
    this.metrics.resourceCount = resources.length
    this.metrics.resourceSizeKb = Math.round(
      resources.reduce((sum, r) => sum + (r.transferSize || 0), 0) / 1024,
    )

    let jsHeapSizeMb = 0
    if ((performance as any).memory) {
      jsHeapSizeMb =
        Math.round(((performance as any).memory.usedJSHeapSize / (1024 * 1024)) * 100) / 100
    }
    this.metrics.jsHeapSizeMb = jsHeapSizeMb

    const networkType = this.getNetworkType()
    const config = this.tracker.getConfig()
    const sessionManager = this.tracker.getSessionManager()

    const perfData: PerformanceData = {
      sessionId: sessionManager.getSessionId(),
      pagePath: window.location.pathname,
      platform: config.platform,
      fcp: this.metrics.fcp || 0,
      lcp: this.metrics.lcp || 0,
      fid: this.metrics.fid || 0,
      cls: this.metrics.cls || 0,
      ttfb: this.metrics.ttfb || 0,
      loadTime: this.metrics.loadTime || 0,
      inp: this.metrics.inp || 0,
      domReady: this.metrics.domReady || 0,
      resourceCount: this.metrics.resourceCount || 0,
      resourceSizeKb: this.metrics.resourceSizeKb || 0,
      jsHeapSizeMb: this.metrics.jsHeapSizeMb || 0,
      networkType,
      timestamp: Date.now(),
    }

    this.tracker.getReporter().addPerformance(perfData)
  }

  private getNetworkType(): string {
    if (typeof navigator === 'undefined') return 'unknown'
    const conn =
      (navigator as any).connection ||
      (navigator as any).mozConnection ||
      (navigator as any).webkitConnection
    if (conn && conn.effectiveType) return conn.effectiveType
    return navigator.onLine ? 'unknown' : 'offline'
  }

  destroy(): void {
    this.observers.forEach((o) => o.disconnect())
    this.observers = []
    this.tracker = null
  }
}
