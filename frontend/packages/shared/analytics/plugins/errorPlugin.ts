import type { Plugin, ErrorData } from '../types'
import type { Tracker } from '../tracker'

export class ErrorPlugin implements Plugin {
  name = 'error'
  private tracker: Tracker | null = null
  private errorHandler: ((e: ErrorEvent) => void) | null = null
  private rejectionHandler: ((e: PromiseRejectionEvent) => void) | null = null

  install(tracker: Tracker): void {
    this.tracker = tracker

    this.errorHandler = (e: ErrorEvent) => {
      if (e.target && (e.target as HTMLElement).tagName) {
        this.reportResourceError(e)
      } else {
        this.reportJsError(e)
      }
    }

    this.rejectionHandler = (e: PromiseRejectionEvent) => {
      this.reportPromiseError(e)
    }

    window.addEventListener('error', this.errorHandler, true)
    window.addEventListener('unhandledrejection', this.rejectionHandler)
  }

  private reportJsError(e: ErrorEvent): void {
    if (!this.tracker) return
    const errorData = this.buildErrorData('js_error', e.message || 'Unknown error', e.error?.stack)
    this.tracker.getReporter().addError(errorData)
  }

  private reportResourceError(e: ErrorEvent): void {
    if (!this.tracker) return
    const target = e.target as HTMLElement
    const src = (target as HTMLImageElement).src || (target as HTMLScriptElement).src || ''
    const message = `Resource load failed: ${target.tagName.toLowerCase()} ${src}`
    const errorData = this.buildErrorData('resource_error', message, '')
    this.tracker.getReporter().addError(errorData)
  }

  private reportPromiseError(e: PromiseRejectionEvent): void {
    if (!this.tracker) return
    const reason = e.reason
    const message = reason instanceof Error ? reason.message : String(reason)
    const stack = reason instanceof Error ? reason.stack : ''
    const errorData = this.buildErrorData('promise_error', message, stack)
    this.tracker.getReporter().addError(errorData)
  }

  private buildErrorData(
    errorType: ErrorData['errorType'],
    message: string,
    stack?: string,
  ): ErrorData {
    const config = this.tracker!.getConfig()
    const sessionManager = this.tracker!.getSessionManager()

    return {
      sessionId: sessionManager.getSessionId(),
      errorType,
      errorMessage: message.slice(0, 500),
      errorStack: (stack || '').slice(0, 500),
      pagePath: typeof window !== 'undefined' ? window.location.pathname : '',
      userActionBefore: this.tracker!.getActionHistory(),
      platform: config.platform,
      deviceModel: this.getDeviceModel(),
      os: this.getOS(),
      appVersion: config.appVersion,
      timestamp: Date.now(),
    }
  }

  private getDeviceModel(): string {
    if (typeof navigator === 'undefined') return 'unknown'
    const ua = navigator.userAgent
    if (/iPhone/.test(ua)) return 'iPhone'
    if (/iPad/.test(ua)) return 'iPad'
    if (/Android/.test(ua)) {
      const match = ua.match(/;\s*([^;)]+)\s*Build/)
      return match?.[1]?.trim() || 'Android Device'
    }
    if (/Mac/.test(ua)) return 'Mac'
    return 'PC'
  }

  private getOS(): string {
    if (typeof navigator === 'undefined') return 'unknown'
    const ua = navigator.userAgent
    if (/Windows/.test(ua)) return 'Windows'
    if (/Mac OS X/.test(ua)) return 'macOS'
    if (/Android/.test(ua)) return 'Android'
    if (/iPhone|iPad/.test(ua)) return 'iOS'
    if (/Linux/.test(ua)) return 'Linux'
    return 'unknown'
  }

  destroy(): void {
    if (this.errorHandler) {
      window.removeEventListener('error', this.errorHandler, true)
      this.errorHandler = null
    }
    if (this.rejectionHandler) {
      window.removeEventListener('unhandledrejection', this.rejectionHandler)
      this.rejectionHandler = null
    }
    this.tracker = null
  }
}
