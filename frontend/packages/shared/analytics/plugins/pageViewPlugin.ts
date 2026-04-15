import type { Plugin } from '../types'
import type { Tracker } from '../tracker'

export class PageViewPlugin implements Plugin {
  name = 'pageView'

  install(_tracker: Tracker): void {
    // Vue Router integration is handled via createPageViewGuard
  }
}

interface RouteLocation {
  path: string
  meta: Record<string, any>
}

interface Router {
  afterEach(guard: (to: RouteLocation, from: RouteLocation) => void): void
}

export function createPageViewGuard(router: Router, tracker: Tracker): void {
  router.afterEach((to, from) => {
    const pageTitle = (to.meta?.title as string) || ''
    tracker.trackPageView(to.path, pageTitle, from.path)
  })
}
