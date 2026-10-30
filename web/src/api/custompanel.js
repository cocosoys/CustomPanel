import request from '@/utils/request'

/**
 * CustomPanel 插件 API 封装
 * baseURL 由 SOYS 契约注入（生产 = /api/plugins/CustomPanel，开发 = VUE_APP_BASE_API 代理）
 * 路径与后端 Controller 类级前缀一一对应：/panel/page、/panel/field、/panel/action、/panel/record
 */

/* ================= 页面定义 ================= */
export function listPage(params) {
  return request({ url: '/panel/page/list', method: 'get', params })
}
/** 页面定义聚合（Schema）：page + fields + actions 一次拉取，驱动渲染器 */
export function schemaPage(pageId) {
  return request({ url: '/panel/page/schema/' + pageId, method: 'get' })
}
export function getPage(pageId) {
  return request({ url: '/panel/page/' + pageId, method: 'get' })
}
export function addPage(data) {
  return request({ url: '/panel/page', method: 'post', data })
}
export function editPage(pageId, data) {
  return request({ url: '/panel/page/' + pageId, method: 'put', data })
}
export function removePage(pageIds) {
  return request({ url: '/panel/page/' + pageIds, method: 'delete' })
}

/* ================= 字段定义 ================= */
export function listField(params) {
  return request({ url: '/panel/field/list', method: 'get', params })
}
export function getField(fieldId) {
  return request({ url: '/panel/field/' + fieldId, method: 'get' })
}
export function addField(data) {
  return request({ url: '/panel/field', method: 'post', data })
}
export function editField(fieldId, data) {
  return request({ url: '/panel/field/' + fieldId, method: 'put', data })
}
export function removeField(fieldIds) {
  return request({ url: '/panel/field/' + fieldIds, method: 'delete' })
}

/* ================= 动作定义 ================= */
export function listAction(params) {
  return request({ url: '/panel/action/list', method: 'get', params })
}
export function getAction(actionId) {
  return request({ url: '/panel/action/' + actionId, method: 'get' })
}
export function addAction(data) {
  return request({ url: '/panel/action', method: 'post', data })
}
export function editAction(actionId, data) {
  return request({ url: '/panel/action/' + actionId, method: 'put', data })
}
export function removeAction(actionIds) {
  return request({ url: '/panel/action/' + actionIds, method: 'delete' })
}
/** 执行动作（COMMAND/HTTP/INTERNAL 由后端按类型分派；data 可带 recordId/params 参与占位符插值） */
export function executeAction(actionId, data) {
  return request({ url: '/panel/action/execute/' + actionId, method: 'post', data })
}

/* ================= 自由模式记录 ================= */
export function listRecord(params) {
  return request({ url: '/panel/record/list', method: 'get', params })
}
export function getRecord(recordId) {
  return request({ url: '/panel/record/' + recordId, method: 'get' })
}
export function addRecord(data) {
  return request({ url: '/panel/record', method: 'post', data })
}
export function editRecord(recordId, data) {
  return request({ url: '/panel/record/' + recordId, method: 'put', data })
}
export function removeRecord(recordIds) {
  return request({ url: '/panel/record/' + recordIds, method: 'delete' })
}

/* ================= 托管实体（MANAGED） ================= */
/** 已注册托管实体名列表（驱动页面绑定下拉与渲染器数据通道） */
export function listManagedEntities() {
  return request({ url: '/panel/managed/entities', method: 'get' })
}
export function managedList(entityName, params) {
  return request({ url: '/panel/managed/' + entityName + '/list', method: 'get', params })
}
export function managedGet(entityName, id, params) {
  return request({ url: '/panel/managed/' + entityName + '/' + id, method: 'get', params })
}
export function managedAdd(entityName, data) {
  return request({ url: '/panel/managed/' + entityName, method: 'post', data })
}
export function managedUpdate(entityName, id, data) {
  return request({ url: '/panel/managed/' + entityName + '/' + id, method: 'put', data })
}
export function managedRemove(entityName, ids, params) {
  return request({ url: '/panel/managed/' + entityName + '/' + ids, method: 'delete', params })
}
