/**
 * SOYS 平台契约消费封装（@soys 契约 v5 · 严格消费）。
 *
 * 契约文件 public/__SOYS_CONTEXT__.js 由 SOYS ContractInjector 按 owner 插件注入后下发，
 * 子应用只是接收方：这里【只读取】window.SOYS_CONTEXT 的既有字段，
 * 不新增任何全局命名、不改写契约文件、不提供任何兜底值。
 * 契约缺失或字段残缺一律视为部署错误并直接抛错，避免静默落到错误的 API / 页面命名空间。
 *
 * 契约原语（SoysContextContractVO，SOYS v1.4.1+）：
 *   scheme / host / port / apiPrefix / pluginsPrefix / apiFullPrefix / pageFullPrefix
 *   / webResourcePrefix / pageBase / spaFallback / fpEnabled / fpStrict
 *
 * v5：
 *   - apiBaseURL 直接取 apiFullPrefix（移除 development 三元拆分 '/api' 兜底）；
 *   - pageBase 取契约 pageBase，history / hash 由契约 spaFallback 决定，不再写死 'history'；
 *   - 由 pluginsPrefix 末段（owner 插件名）派生 ERP 模块标识，再按 NameSafe.path 规则派生
 *     moduleKey / moduleMatchPath，供后端菜单树裁剪使用，前端不再散落插件名字符串。
 *
 * 本项目（CustomPanel）与模板的差异：
 *   ERP 侧扩张标识是「插件名 + -ERP」（CustomPanel-ERP，见 PanelExpansion.getIdentifier()），
 *   而 MCERP MenuRouteServiceImpl 用 safeName(identifier) 生成模块路由 path（横线被丢弃，
 *   得 /custompanelerp）；子应用页面路由段则由 PanelExpansion.pageDirRoot() 定为
 *   custompanel-erp（保留横线）。两者永远不同源，故这里分别暴露：
 *     moduleMatchPath → 后端 RouterVO 里的模块 path（/custompanelerp），用于裁剪菜单树；
 *     moduleRoot      → 本地 vue-router 路由根（/custompanel-erp），用于路由注册与页面跳转。
 */
const ctx = typeof window !== 'undefined' ? window.SOYS_CONTEXT : null

if (!ctx) {
  throw new Error('[SOYS] 契约未注入：必须由托管层注入 __SOYS_CONTEXT__.js 后再加载本应用')
}

// 本应用运行所需的契约字段，缺一则视为契约版本不匹配 / 注入异常（不做静默兜底）
const REQUIRED_FIELDS = [
  'scheme', 'host', 'port', 'apiPrefix', 'pluginsPrefix', 'apiFullPrefix',
  'pageFullPrefix', 'pageBase', 'webResourcePrefix', 'spaFallback'
]
const missing = REQUIRED_FIELDS.filter(k => ctx[k] === undefined || ctx[k] === null || ctx[k] === '')
if (missing.length) {
  throw new Error('[SOYS] 契约字段缺失：' + missing.join(', '))
}

// owner 插件标识 = pluginsPrefix 的末段（/plugins/CustomPanel → CustomPanel）
const segments = ctx.pluginsPrefix.split('/').filter(Boolean)
const ownerPlugin = segments[segments.length - 1]
if (!ownerPlugin) {
  throw new Error('[SOYS] 契约 pluginsPrefix 无法派生插件标识：' + ctx.pluginsPrefix)
}

// ERP 侧扩张标识：本项目规范为「插件名 + -ERP」，与后端 PanelExpansion.getIdentifier() 一致。
// 不能直接把 ownerPlugin 当模块标识——它只是插件名，MCERP 模块路由 path 取的是 identifier。
const moduleId = ownerPlugin + '-ERP'

// NameSafe.path / MenuRouteServiceImpl.safeName 规则：仅保留字母数字下划线并转小写
const moduleKey = moduleId.replace(/[^A-Za-z0-9_]/g, '').toLowerCase()
if (!moduleKey) {
  throw new Error('[SOYS] 无法派生模块路由名：' + moduleId)
}

/** 后端 RouterVO 的模块 path（MCERP 按 identifier 派生，横线被丢弃）：/custompanelerp */
const moduleMatchPath = '/' + moduleKey

/**
 * 子应用路由根：必须与 PanelExpansion.pageDirRoot() 的目录段一致（custompanel-erp，保留横线）。
 * 它是页面 URL 段，与 MCERP 的模块路由名永远不同源，故单独声明；
 * menuAdapter 负责把后端模块 path 映射到该根，见 router/menuAdapter.js。
 */
const moduleRoot = '/custompanel-erp'

const apiBaseURL = ctx.apiFullPrefix
const pagePrefix = ctx.pageFullPrefix

const soysContext = {
  version: 5,
  server: ctx,                    // 服务器环境原语（只读）
  injected: true,
  apiPrefix: ctx.apiPrefix,
  pluginsPrefix: ctx.pluginsPrefix,
  pagePrefix,                     // 契约 pageFullPrefix
  pageBase: ctx.pageBase,         // vue-router base（附属插件 = pageFullPrefix + "/"）
  apiBaseURL,                     // axios baseURL / 上传 action / 头像回显统一使用
  moduleId,                       // ERP 模块标识（插件名 + -ERP）
  moduleKey,                      // NameSafe.safe + toLowerCase
  moduleRoot,                     // 本地路由根（= PanelExpansion.pageDirRoot() 目录段）
  moduleMatchPath,                // 后端模块 path（= MCERP safeName(identifier)）
  loginUrl: '/login.html',
  capabilities: {
    spaFallback: ctx.spaFallback  // true → history；false → hash
  },

  /** 识别服务器提供的完整后端地址（基于注入原语运行时组装） */
  resolveServer() {
    const origin = ctx.scheme + '://' + ctx.host + (ctx.port && ctx.port !== 80 ? ':' + ctx.port : '')
    return {
      origin,
      scheme: ctx.scheme,
      host: ctx.host,
      port: ctx.port,
      backendBase: origin + apiBaseURL,
      loginUrl: origin + soysContext.loginUrl
    }
  }
}

// 暴露到全局便于浏览器控制台核对注入结果：window.SOYS_CONTEXT_API（既有调试通道，不新增命名）
if (typeof window !== 'undefined') {
  window.SOYS_CONTEXT_API = soysContext
}

export default soysContext
