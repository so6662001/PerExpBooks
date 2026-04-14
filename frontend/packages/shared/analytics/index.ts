export { Tracker } from './tracker'
export { Reporter } from './reporter'
export { SessionManager } from './sessionManager'
export { PageViewPlugin, createPageViewGuard } from './plugins/pageViewPlugin'
export { ClickPlugin } from './plugins/clickPlugin'
export { PerformancePlugin } from './plugins/performancePlugin'
export { ExposurePlugin } from './plugins/exposurePlugin'
export { ErrorPlugin } from './plugins/errorPlugin'
export * from './types'

import type { TrackerConfig } from './types'
import { Tracker } from './tracker'
import { ClickPlugin } from './plugins/clickPlugin'
import { PerformancePlugin } from './plugins/performancePlugin'
import { ErrorPlugin } from './plugins/errorPlugin'

export function initTracker(config: Partial<TrackerConfig>): Tracker {
  const tracker = Tracker.getInstance()
  tracker.init(config)

  if (config.enableClick !== false) {
    tracker.use(new ClickPlugin())
  }
  if (config.enablePerformance !== false) {
    tracker.use(new PerformancePlugin())
  }
  if (config.enableError !== false) {
    tracker.use(new ErrorPlugin())
  }

  return tracker
}
