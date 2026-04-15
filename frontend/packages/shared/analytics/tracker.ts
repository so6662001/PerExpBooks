import type { TrackerConfig, EventData, Plugin } from './types'
import { DEFAULT_CONFIG } from './types'
import { Reporter } from './reporter'
import { SessionManager } from './sessionManager'

function generateEventId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export class Tracker {
  private static instance: Tracker | null = null
  private config!: TrackerConfig
  private reporter!: Reporter
  private sessionManager!: SessionManager
  private plugins: Plugin[] = []
  private userId: number = 0
  private memberType: string = 'free'
  private actionHistory: string[] = []
  private initialized = false

  private constructor() {}

  static getInstance(): Tracker {
    if (!Tracker.instance) {
      Tracker.instance = new Tracker()
    }
    return Tracker.instance
  }

  init(config: Partial<TrackerConfig>): void {
    if (this.initialized) return
    this.config = { ...DEFAULT_CONFIG, ...config }
    this.sessionManager = new SessionManager()
    this.reporter = new Reporter(this.config)
    this.initialized = true
  }

  setUser(userId: number, memberType: string): void {
    this.userId = userId
    this.memberType = memberType
  }

  track(eventName: string, extra?: Record<string, any>): void {
    this.ensureInitialized()
    const event = this.buildEventData('custom', eventName, extra)
    this.pushAction(eventName)
    this.reporter.addEvent(event)
  }

  trackPageView(pagePath: string, pageTitle: string, referrerPath?: string): void {
    this.ensureInitialized()
    const event = this.buildEventData('page_view', 'page_view', {
      pagePath,
      pageTitle,
      referrerPath: referrerPath || '',
    })
    event.pagePath = pagePath
    event.pageTitle = pageTitle
    event.referrerPath = referrerPath || ''
    this.pushAction(`page_view:${pagePath}`)
    this.reporter.addEvent(event)
  }

  trackClick(eventName: string, extra?: Record<string, any>): void {
    this.ensureInitialized()
    const event = this.buildEventData('click', eventName, extra)
    this.pushAction(`click:${eventName}`)
    this.reporter.addEvent(event)
  }

  trackExposure(eventName: string, extra?: Record<string, any>): void {
    this.ensureInitialized()
    const event = this.buildEventData('exposure', eventName, extra)
    this.reporter.addEvent(event)
  }

  use(plugin: Plugin): void {
    this.ensureInitialized()
    this.plugins.push(plugin)
    plugin.install(this)
  }

  getReporter(): Reporter {
    return this.reporter
  }

  getSessionManager(): SessionManager {
    return this.sessionManager
  }

  getConfig(): TrackerConfig {
    return this.config
  }

  getActionHistory(): string[] {
    return [...this.actionHistory]
  }

  private buildEventData(
    eventType: EventData['eventType'],
    eventName: string,
    extra?: Record<string, any>,
  ): EventData {
    const device = this.getDeviceInfo()
    return {
      eventId: generateEventId(),
      eventType,
      eventName,
      userId: this.userId,
      sessionId: this.sessionManager.getSessionId(),
      pagePath: typeof window !== 'undefined' ? window.location.pathname : '',
      pageTitle: typeof document !== 'undefined' ? document.title : '',
      referrerPath: typeof document !== 'undefined' ? document.referrer : '',
      platform: this.config.platform,
      deviceModel: device.deviceModel,
      os: device.os,
      osVersion: device.osVersion,
      screenWidth: device.screenWidth,
      screenHeight: device.screenHeight,
      networkType: device.networkType,
      appVersion: this.config.appVersion,
      memberType: this.memberType,
      extra: extra || {},
      timestamp: Date.now(),
    }
  }

  private getDeviceInfo(): {
    deviceModel: string
    os: string
    osVersion: string
    screenWidth: number
    screenHeight: number
    networkType: string
  } {
    if (typeof navigator === 'undefined') {
      return {
        deviceModel: 'unknown',
        os: 'unknown',
        osVersion: 'unknown',
        screenWidth: 0,
        screenHeight: 0,
        networkType: 'unknown',
      }
    }

    const ua = navigator.userAgent
    let os = 'unknown'
    let osVersion = 'unknown'
    let deviceModel = 'unknown'

    if (/Windows/.test(ua)) {
      os = 'Windows'
      const match = ua.match(/Windows NT (\d+\.\d+)/)
      osVersion = match?.[1] || 'unknown'
      deviceModel = 'PC'
    } else if (/Mac OS X/.test(ua)) {
      os = 'macOS'
      const match = ua.match(/Mac OS X (\d+[._]\d+[._]?\d*)/)
      osVersion = match?.[1]?.replace(/_/g, '.') || 'unknown'
      deviceModel = 'Mac'
    } else if (/Android/.test(ua)) {
      os = 'Android'
      const match = ua.match(/Android (\d+\.?\d*)/)
      osVersion = match?.[1] || 'unknown'
      const modelMatch = ua.match(/;\s*([^;)]+)\s*Build/)
      deviceModel = modelMatch?.[1]?.trim() || 'Android Device'
    } else if (/iPhone|iPad/.test(ua)) {
      os = 'iOS'
      const match = ua.match(/OS (\d+[_]\d+)/)
      osVersion = match?.[1]?.replace(/_/g, '.') || 'unknown'
      deviceModel = /iPad/.test(ua) ? 'iPad' : 'iPhone'
    } else if (/Linux/.test(ua)) {
      os = 'Linux'
      deviceModel = 'PC'
    }

    const networkType = this.getNetworkType()

    return {
      deviceModel,
      os,
      osVersion,
      screenWidth: typeof screen !== 'undefined' ? screen.width : 0,
      screenHeight: typeof screen !== 'undefined' ? screen.height : 0,
      networkType,
    }
  }

  private getNetworkType(): string {
    if (typeof navigator === 'undefined') return 'unknown'
    const conn = (navigator as any).connection || (navigator as any).mozConnection || (navigator as any).webkitConnection
    if (conn && conn.effectiveType) {
      return conn.effectiveType
    }
    return navigator.onLine ? 'unknown' : 'offline'
  }

  private pushAction(action: string): void {
    this.actionHistory.push(action)
    if (this.actionHistory.length > 3) {
      this.actionHistory.shift()
    }
  }

  private ensureInitialized(): void {
    if (!this.initialized) {
      throw new Error('[Analytics] Tracker not initialized. Call init() first.')
    }
  }

  destroy(): void {
    this.plugins.forEach((p) => p.destroy?.())
    this.plugins = []
    this.reporter.destroy()
    this.initialized = false
    Tracker.instance = null
  }
}
