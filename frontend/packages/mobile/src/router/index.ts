import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/auth/login',
    name: 'Login',
    component: () => import('@/views/auth/login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
  },
  {
    path: '/expense',
    name: 'ExpenseList',
    component: () => import('@/views/expense/list.vue'),
  },
  {
    path: '/expense/upload',
    name: 'ExpenseUpload',
    component: () => import('@/views/expense/upload.vue'),
  },
  {
    path: '/expense/:id',
    name: 'ExpenseDetail',
    component: () => import('@/views/expense/detail.vue'),
  },
  {
    path: '/trip',
    name: 'TripList',
    component: () => import('@/views/trip/list.vue'),
  },
  {
    path: '/trip/create',
    name: 'TripCreate',
    component: () => import('@/views/trip/create.vue'),
  },
  {
    path: '/reimbursement',
    name: 'ReimbursementList',
    component: () => import('@/views/reimbursement/list.vue'),
  },
  {
    path: '/reimbursement/create',
    name: 'ReimbursementCreate',
    component: () => import('@/views/reimbursement/create.vue'),
  },
  {
    path: '/reimbursement/:id',
    name: 'ReimbursementDetail',
    component: () => import('@/views/reimbursement/detail.vue'),
  },
  {
    path: '/stats',
    name: 'StatsOverview',
    component: () => import('@/views/stats/overview.vue'),
  },
  {
    path: '/member',
    name: 'MemberCenter',
    component: () => import('@/views/member/center.vue'),
  },
  {
    path: '/promotion',
    name: 'PromotionCenter',
    component: () => import('@/views/promotion/center.vue'),
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/index.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth === false) {
    next()
    return
  }
  if (!token && to.path !== '/auth/login') {
    next('/auth/login')
    return
  }
  next()
})

export default router
