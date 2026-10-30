import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout：wujie 嵌入时主应用 MCERP 提供侧边栏（空 Layout）；直接访问时用完整 Layout（自带侧边栏） */
import RealLayout from '@/layout'
import EmptyLayout from '@/layout/Empty'

// wujie 子应用标记（主应用加载时注入 window.__POWERED_BY_WUJIE__）
export const isWujie = typeof window !== 'undefined' && !!window.__POWERED_BY_WUJIE__
const Layout = isWujie ? EmptyLayout : RealLayout

// 运行时 base：生产 = 契约 pageFullPrefix（/web/plugins/CustomPanel）；dev = '/'
const ctx = (typeof window !== 'undefined' && window.SOYS_CONTEXT) || {}
const routeBase = process.env.NODE_ENV === 'production'
  ? (ctx.pageFullPrefix || '/web/plugins/CustomPanel/')
  : '/'

/**
 * 公共路由：所有模式都加载（基础页面 + 首页）。
 * 业务菜单路由不在此列——wujie 模式下用 wujieRoutes 兜底，独立模式下由后端 getRouters 动态下发。
 */
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: 'index',
    children: [
      {
        path: 'index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/lock',
    component: () => import('@/views/lock'),
    hidden: true,
    meta: { title: '锁定屏幕' }
  }
]

/**
 * CustomPanel 业务路由。
 * <p>CustomPanel 是纯 MCERP 模块（wujie 嵌入，主应用 MCERP 统一鉴权/菜单），
 * 后端 PanelExpansion.menus() 硬编码菜单 component 为 /page、/field、/action，
 * 主应用菜单点击经 bus 下发 router-push(path) → 子应用内部导航。
 * 因此本路由不做 isWujie 条件化：wujie 与独立/开发模式均直接注册（CustomPanel 无后端动态菜单）。
 * <p>菜单结构严格对齐后端 PanelExpansion.menus()；/view/:pageId 为渲染器（页面管理"打开"进入）。
 */
export const wujieRoutes = [
  {
    path: '/page',
    component: () => import('@/views/custompanel/page/index'),
    name: 'CustomPanelPage',
    meta: { title: '页面管理' }
  },
  {
    path: '/field',
    component: () => import('@/views/custompanel/field/index'),
    name: 'CustomPanelField',
    meta: { title: '字段管理' }
  },
  {
    path: '/action',
    component: () => import('@/views/custompanel/action/index'),
    name: 'CustomPanelAction',
    meta: { title: '动作管理' }
  },
  {
    path: '/view/:pageId',
    component: () => import('@/views/custompanel/render/index'),
    name: 'CustomPanelRender',
    meta: { title: '页面渲染' }
  }
]

// 动态路由（预留）：菜单由后端 getRouters 动态驱动（ERP 模块菜单），此处保持空
export const dynamicRoutes = []

// 防止连续点击多次路由报错
let routerPush = Router.prototype.push
let routerReplace = Router.prototype.replace
// push
Router.prototype.push = function push(location) {
  return routerPush.call(this, location).catch(err => err)
}
// replace
Router.prototype.replace = function push(location) {
  return routerReplace.call(this, location).catch(err => err)
}

export default new Router({
  mode: 'history', // SOYS 1.4.0 已实现 spaFallback，深层 URL 刷新不会 404
  base: routeBase,
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes.concat(wujieRoutes)
})
