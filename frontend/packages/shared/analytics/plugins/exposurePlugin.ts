import type { Plugin } from '../types'
import type { Tracker } from '../tracker'

interface ExposureEntry {
  eventName: string
  extra?: Record<string, any>
}

export class ExposurePlugin implements Plugin {
  name = 'exposure'
  private tracker: Tracker | null = null
  private observer: IntersectionObserver | null = null
  private elementMap = new WeakMap<Element, ExposureEntry>()
  private exposedSet = new WeakSet<Element>()

  install(tracker: Tracker): void {
    this.tracker = tracker
    if (typeof IntersectionObserver === 'undefined') return

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue
          if (this.exposedSet.has(entry.target)) continue

          const data = this.elementMap.get(entry.target)
          if (!data) continue

          this.exposedSet.add(entry.target)
          this.observer?.unobserve(entry.target)
          this.tracker?.trackExposure(data.eventName, data.extra)
        }
      },
      { threshold: 0.5 },
    )
  }

  observe(element: Element, eventName: string, extra?: Record<string, any>): void {
    if (!this.observer) return
    if (this.exposedSet.has(element)) return
    this.elementMap.set(element, { eventName, extra })
    this.observer.observe(element)
  }

  unobserve(element: Element): void {
    if (!this.observer) return
    this.observer.unobserve(element)
    this.elementMap.delete(element)
  }

  destroy(): void {
    this.observer?.disconnect()
    this.observer = null
    this.tracker = null
  }
}
