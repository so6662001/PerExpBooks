import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/auth/login',
    name: 'Login',
    component: () => import('@/views/auth/login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/landing',
    name: 'Landing',
    component: () => import('@/views/landing/index.vue'),
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
    path: '/expense/add',
    name: 'ExpenseAdd',
    component: () => import('@/views/expense/add.vue'),
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
    path: '/trip/edit/:id',
    name: 'TripEdit',
    component: () => import('@/views/trip/edit.vue'),
  },
  {
    path: '/trip/:id',
    name: 'TripDetail',
    component: () => import('@/views/trip/detail.vue'),
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
    path: '/reimbursement/preview/:id',
    name: 'ReimbursementPreview',
    component: () => import('@/views/reimbursement/preview.vue'),
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
    path: '/stats/monthly',
    name: 'StatsMonthly',
    component: () => import('@/views/stats/monthly.vue'),
  },
  {
    path: '/stats/category',
    name: 'StatsCategory',
    component: () => import('@/views/stats/category.vue'),
  },
  {
    path: '/stats/trip',
    name: 'StatsTrip',
    component: () => import('@/views/stats/trip.vue'),
  },
  {
    path: '/stats/progress',
    name: 'StatsProgress',
    component: () => import('@/views/stats/progress.vue'),
  },
  {
    path: '/stats/yearly',
    name: 'StatsYearly',
    component: () => import('@/views/stats/yearly.vue'),
  },
  {
    path: '/stats/city',
    name: 'StatsCity',
    component: () => import('@/views/stats/city.vue'),
  },
  {
    path: '/stats/calendar',
    name: 'StatsCalendar',
    component: () => import('@/views/stats/calendar.vue'),
  },
  {
    path: '/member',
    name: 'MemberCenter',
    component: () => import('@/views/member/center.vue'),
  },
  {
    path: '/member/orders',
    name: 'MemberOrders',
    component: () => import('@/views/member/orders.vue'),
  },
  {
    path: '/promotion',
    name: 'PromotionCenter',
    component: () => import('@/views/promotion/center.vue'),
  },
  {
    path: '/promotion/withdraw',
    name: 'PromotionWithdraw',
    component: () => import('@/views/promotion/withdraw.vue'),
  },
  {
    path: '/promotion/points',
    name: 'PromotionPoints',
    component: () => import('@/views/promotion/points.vue'),
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/index.vue'),
  },
  {
    path: '/profile/edit',
    name: 'ProfileEdit',
    component: () => import('@/views/profile/edit.vue'),
  },
  {
    path: '/coupon/list',
    name: 'CouponList',
    component: () => import('@/views/coupon/list.vue'),
  },
  {
    path: '/team',
    name: 'TeamIndex',
    component: () => import('@/views/team/index.vue'),
  },
  {
    path: '/team/join',
    name: 'TeamJoin',
    component: () => import('@/views/team/join.vue'),
  },
  {
    path: '/agreement',
    name: 'AgreementIndex',
    component: () => import('@/views/agreement/index.vue'),
  },
  {
    path: '/agreement/:id',
    name: 'AgreementDetail',
    component: () => import('@/views/agreement/detail.vue'),
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/settings/index.vue'),
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
