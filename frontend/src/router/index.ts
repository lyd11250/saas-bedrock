import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTabsStore } from '@/stores/tabs'
import type { MenuNode } from '@/api/menu'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/error/403',
    name: 'error-403',
    component: () => import('@/views/error/Error403.vue'),
    meta: { public: true },
  },
  {
    path: '/error/404',
    name: 'error-404',
    component: () => import('@/views/error/Error404.vue'),
    meta: { public: true },
  },
  {
    path: '/error/500',
    name: 'error-500',
    component: () => import('@/views/error/Error500.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    name: 'layout',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: 'party/accounts/:partyId',
        name: 'party-accounts',
        component: () => import('@/views/party/AccountList.vue'),
        meta: { title: '账户管理' },
      },
      {
        path: 'party/qualifications/:partyId',
        name: 'party-qualifications',
        component: () => import('@/views/party/QualificationList.vue'),
        meta: { title: '资质管理' },
      },
    ],
  },
  // 兜底：未匹配的路径。注意不能用 redirect 直跳 /error/404——
  // redirect 会在全局守卫前解析为公开页，导致刷新动态路由页时守卫被短路、
  // 动态路由来不及挂载就落到 404。这里改为渲染组件，由守卫统一处理。
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/Error404.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 后端菜单 component 字符串（如 system/UserList）→ 视图组件
const viewModules = import.meta.glob('../views/**/*.vue')
function resolveComponent(component?: string) {
  if (!component) return undefined
  return viewModules[`../views/${component}.vue`]
}

// 已注册的动态路由名，便于登出时清理
const dynamicNames: string[] = []
let dynamicAdded = false

/** 由导航菜单树挂载动态路由到 layout 下（仅 C 型页面）。 */
function setupDynamicRoutes(menus: MenuNode[]) {
  const walk = (nodes: MenuNode[]) => {
    for (const node of nodes) {
      if (node.type === 'C' && node.path && node.component) {
        const name = `menu-${node.id}`
        // 解析不到对应组件时回退到「页面缺失」占位，避免点击菜单无响应
        const rawLoader =
          resolveComponent(node.component) ?? (() => import('@/views/error/Error404.vue'))
        // 给组件注入 name（取路由名），使 <keep-alive :include> 可按名命中缓存
        const comp = async () => {
          const mod = (await rawLoader()) as { default: { name?: string } }
          if (mod.default && !mod.default.name) mod.default.name = name
          return mod
        }
        if (!router.hasRoute(name)) {
          router.addRoute('layout', {
            path: node.path,
            name,
            component: comp,
            meta: { title: node.name, permission: node.perm, keepAlive: node.keepAlive === 1 },
          })
          dynamicNames.push(name)
        }
      }
      if (node.children?.length) {
        walk(node.children)
      }
    }
  }
  walk(menus)
  dynamicAdded = true
}

/** 登出时清理动态路由，便于换账号后按新菜单重建。 */
export function resetDynamicRoutes() {
  for (const name of dynamicNames.splice(0)) {
    if (router.hasRoute(name)) router.removeRoute(name)
  }
  dynamicAdded = false
}

// 全局前置守卫：登录校验 + 动态路由挂载 + 角色/权限校验
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return true
  }
  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 有令牌但用户信息未加载（如刷新页面）→ 先拉取用户与菜单
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  // 首次进入：按菜单挂载动态路由，再重新解析当前导航
  // （刷新动态路由页时，初次匹配会落到兜底 not-found，挂载后重新导航即可命中）
  if (!dynamicAdded) {
    setupDynamicRoutes(auth.menus)
    return to.fullPath
  }
  // 动态路由已就绪仍落到兜底 → 路径确实不存在，跳 404 页
  if (to.name === 'not-found') {
    return { name: 'error-404' }
  }
  if (to.meta.role && !auth.hasRole(to.meta.role as string)) {
    return { name: 'error-403' }
  }
  if (to.meta.permission && !auth.hasPermission(to.meta.permission as string)) {
    return { name: 'error-403' }
  }
  return true
})

// 全局后置钩子：导航完成后把目标页记入页签栏（公开页/错误页除外）
router.afterEach((to) => {
  if (to.meta.public || !to.name) return
  const tabs = useTabsStore()
  tabs.addView(to)
})

export default router
