import type { Plugin } from '../types'
import type { Tracker } from '../tracker'

export class ClickPlugin implements Plugin {
  name = 'click'
  private tracker: Tracker | null = null
  private handler: ((e: MouseEvent) => void) | null = null

  install(tracker: Tracker): void {
    this.tracker = tracker
    this.handler = (e: MouseEvent) => this.onClick(e)
    document.addEventListener('click', this.handler, true)
  }

  private onClick(e: MouseEvent): void {
    if (!this.tracker) return
    const target = e.target as HTMLElement | null
    if (!target) return

    const trackName = this.findTrackAttribute(target)
    if (!trackName) return

    this.tracker.trackClick(trackName, {
      elementTag: target.tagName.toLowerCase(),
      elementText: (target.textContent || '').slice(0, 100).trim(),
      elementId: target.id || undefined,
      elementClass: target.className || undefined,
    })
  }

  private findTrackAttribute(el: HTMLElement): string | null {
    let current: HTMLElement | null = el
    let depth = 0
    while (current && depth < 5) {
      const trackValue = current.getAttribute('data-track')
      if (trackValue) return trackValue
      current = current.parentElement
      depth++
    }
    return null
  }

  destroy(): void {
    if (this.handler) {
      document.removeEventListener('click', this.handler, true)
      this.handler = null
    }
    this.tracker = null
  }
}
