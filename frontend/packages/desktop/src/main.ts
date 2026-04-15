import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/global.scss'
import { initTracker, createPageViewGuard } from '@qianku/shared/analytics'
import { useUserStore } from '@qianku/shared/stores/user'

const app = createApp(App)
const pinia = createPinia()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: undefined })

try {
  const tracker = initTracker({
    appVersion: '1.0.0',
    platform: 'h5_desktop',
    reportUrl: '/api/v1/analytics/report',
    performanceUrl: '/api/v1/analytics/performance',
    errorUrl: '/api/v1/analytics/error',
  })

  createPageViewGuard(router, tracker)

  const userStore = useUserStore()
  if (userStore.token && userStore.userInfo) {
    tracker.setUser(Number(userStore.userInfo.id), String(userStore.userInfo.memberType || 'free'))
  }
} catch (e) {
  console.warn('[Analytics] Init failed:', e)
}

app.mount('#app')
