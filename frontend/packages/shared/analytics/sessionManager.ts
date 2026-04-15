const SESSION_KEY = 'qianku_analytics_session'
const SESSION_LAST_ACTIVE_KEY = 'qianku_analytics_last_active'
const SESSION_TIMEOUT_MS = 30 * 60 * 1000

function generateUUID(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export class SessionManager {
  private sessionId: string = ''
  private lastActiveTime: number = 0

  constructor() {
    this.restore()
  }

  getSessionId(): string {
    if (this.isExpired()) {
      this.refreshSession()
    }
    this.touch()
    return this.sessionId
  }

  refreshSession(): void {
    this.sessionId = generateUUID()
    this.lastActiveTime = Date.now()
    this.persist()
  }

  private isExpired(): boolean {
    if (!this.sessionId) return true
    return Date.now() - this.lastActiveTime > SESSION_TIMEOUT_MS
  }

  private touch(): void {
    this.lastActiveTime = Date.now()
    try {
      sessionStorage.setItem(SESSION_LAST_ACTIVE_KEY, String(this.lastActiveTime))
    } catch {
      // sessionStorage unavailable
    }
  }

  private persist(): void {
    try {
      sessionStorage.setItem(SESSION_KEY, this.sessionId)
      sessionStorage.setItem(SESSION_LAST_ACTIVE_KEY, String(this.lastActiveTime))
    } catch {
      // sessionStorage unavailable
    }
  }

  private restore(): void {
    try {
      const storedId = sessionStorage.getItem(SESSION_KEY)
      const storedTime = sessionStorage.getItem(SESSION_LAST_ACTIVE_KEY)
      if (storedId && storedTime) {
        this.sessionId = storedId
        this.lastActiveTime = Number(storedTime)
      }
    } catch {
      // sessionStorage unavailable
    }
    if (!this.sessionId) {
      this.refreshSession()
    }
  }
}
