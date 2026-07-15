import { createRouter, createWebHashHistory } from 'vue-router'
import { auth } from './store/auth'

// 路由守卫工厂：未登录重定向
const requireAuth = () => (_to: any, _from: any, next: any) => {
  if (auth.isAuthed.value) next()
  else next('/login')
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/workbench' },

    // —— 公开 ——
    { path: '/login', component: () => import('./views/LoginView.vue') },

    // —— 临床工作 ——
    {
      path: '/workbench', component: () => import('./views/WorkbenchView.vue'),
      beforeEnter: requireAuth(),
    },
    {
      path: '/patients', component: () => import('./views/DataIngestionView.vue'),
      beforeEnter: requireAuth(),
    },
    {
      path: '/imaging', component: () => import('./views/ImagingView.vue'),
      beforeEnter: requireAuth(),
    },
    {
      path: '/consultations', component: () => import('./views/ConsultationView.vue'),
      beforeEnter: requireAuth(),
    },
    {
      path: '/room/:consultationId', component: () => import('./views/ConsultationRoomView.vue'),
      beforeEnter: requireAuth(),
    },

    // —— 系统管理（仅管理员） ——
    {
      path: '/admin', component: () => import('./views/AdminView.vue'),
      beforeEnter: (_to: any, _from: any, next: any) => {
        if (auth.isAuthed.value && auth.state.role === 'ADMIN') next()
        else next('/workbench')
      },
    },

    // —— 兼容旧路由 ——
    { path: '/data-ingestion', redirect: '/patients' },
  ]
})

export default router
