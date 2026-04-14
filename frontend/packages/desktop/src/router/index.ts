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
    component: () => import('@/components/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard',
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'expense',
        children: [
          {
            path: 'list',
            name: 'ExpenseList',
            component: () => import('@/views/expense/list.vue'),
            meta: { title: '费用列表' },
          },
          {
            path: 'upload',
            name: 'ExpenseUpload',
            component: () => import('@/views/expense/upload.vue'),
            meta: { title: '上传发票' },
          },
        ],
      },
      {
        path: 'trip',
        children: [
          {
            path: 'list',
            name: 'TripList',
            component: () => import('@/views/trip/list.vue'),
            meta: { title: '出差记录' },
          },
          {
            path: ':id',
            name: 'TripDetail',
            component: () => import('@/views/trip/detail.vue'),
            meta: { title: '出差详情' },
          },
        ],
      },
      {
        path: 'reimbursement',
        children: [
          {
            path: 'list',
            name: 'ReimbursementList',
            component: () => import('@/views/reimbursement/list.vue'),
            meta: { title: '报销单列表' },
          },
          {
            path: 'create',
            name: 'ReimbursementCreate',
            component: () => import('@/views/reimbursement/create.vue'),
            meta: { title: '创建报销单' },
          },
          {
            path: ':id',
            name: 'ReimbursementDetail',
            component: () => import('@/views/reimbursement/detail.vue'),
            meta: { title: '报销单详情' },
          },
        ],
      },
      {
        path: 'stats',
        children: [
          {
            path: 'monthly',
            name: 'StatsMonthly',
            component: () => import('@/views/stats/monthly.vue'),
            meta: { title: '月度趋势' },
          },
          {
            path: 'category',
            name: 'StatsCategory',
            component: () => import('@/views/stats/category.vue'),
            meta: { title: '分类分析' },
          },
          {
            path: 'trip',
            name: 'StatsTrip',
            component: () => import('@/views/stats/trip.vue'),
            meta: { title: '出差统计' },
          },
          {
            path: 'progress',
            name: 'StatsProgress',
            component: () => import('@/views/stats/progress.vue'),
            meta: { title: '报销进度' },
          },
          {
            path: 'yearly',
            name: 'StatsYearly',
            component: () => import('@/views/stats/yearly.vue'),
            meta: { title: '年度对比' },
          },
          {
            path: 'city',
            name: 'StatsCity',
            component: () => import('@/views/stats/city.vue'),
            meta: { title: '城市排行' },
          },
        ],
      },
      {
        path: 'team',
        children: [
          {
            path: 'members',
            name: 'TeamMembers',
            component: () => import('@/views/team/members.vue'),
            meta: { title: '团队成员' },
          },
          {
            path: 'invite',
            name: 'TeamInvite',
            component: () => import('@/views/team/invite.vue'),
            meta: { title: '邀请成员' },
          },
        ],
      },
      {
        path: 'profile',
        children: [
          {
            path: '',
            name: 'Profile',
            component: () => import('@/views/profile/index.vue'),
            meta: { title: '个人信息' },
          },
          {
            path: 'member',
            name: 'ProfileMember',
            component: () => import('@/views/profile/member.vue'),
            meta: { title: '会员管理' },
          },
          {
            path: 'promotion',
            name: 'ProfilePromotion',
            component: () => import('@/views/profile/promotion.vue'),
            meta: { title: '推广中心' },
          },
          {
            path: 'settings',
            name: 'ProfileSettings',
            component: () => import('@/views/profile/settings.vue'),
            meta: { title: '系统设置' },
          },
        ],
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/auth/login')
  } else if (to.path === '/auth/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
