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
 * wujie 模式业务路由：主应用未通过 bus 下发路由时的兜底。
 * <p><b>仅在 isWujie=true 时合并进 router</b>；独立访问时完全不加载，避免与后端 getRouters 动态路由重复。
 * <p>path 必须与后端 PanelExpansion.menus() 自动拼接的 component 对齐：
 * component = /web/plugins/CustomPanel/custompanel/&lt;menuId&gt;，主应用（WujieLink）取插件前缀后的
 * 子路径作为 props.path 与 router-push 载荷，即 /custompanel/&lt;menuId&gt;。
 * <p>/view/:pageId 为页面渲染器（页面管理"打开"进入），不属菜单，但沿用同一前缀。
 */
export const wujieRoutes = isWujie ? [
  {
    path: '/custompanel',
    component: Layout,
    redirect: '/custompanel/page',
    children: [
      {
        path: 'page',
        component: () => import('@/views/custompanel/page/index'),
        name: 'CustomPanelPage',
        meta: { title: '页面管理' }
      },
      {
        path: 'field',
        component: () => import('@/views/custompanel/field/index'),
        name: 'CustomPanelField',
        meta: { title: '字段管理' }
      },
      {
        path: 'action',
        component: () => import('@/views/custompanel/action/index'),
        name: 'CustomPanelAction',
        meta: { title: '动作管理' }
      },
      {
        path: 'view/:pageId',
        component: () => import('@/views/custompanel/render/index'),
        name: 'CustomPanelRender',
        meta: { title: '页面渲染' }
      }
    ]
  }
] : []

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
