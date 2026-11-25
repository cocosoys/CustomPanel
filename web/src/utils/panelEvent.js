/**
 * 页面事件分发器（P2）：把 schema.events 按 trigger / targetKey 挂载到渲染页。
 *
 * 安全约束（与后端 P1 对齐）：
 *   - 条件 DSL 只做受限字段比较（eq/ne/gt/gte/lt/lte/contains/in + and/or），不 eval、不执行函数；
 *   - 字段白名单由调用方传入（当前页 fieldKey），白名单外的字段一律判 false；
 *   - 内联命令 type 仅 HTTP / INTERNAL，COMMAND 后端强拒绝；
 *   - 单事件失败记日志后继续；仅 beforeSubmit 显式返回 blocked=true 才阻断提交。
 */
import { executeAction, executeInline } from '@/api/custompanel'
import { toParams } from '@/utils/generator/adapter'

/** 条件 DSL 支持的运算符 */
const OPS = ['eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'contains', 'in']

/** 数值比较：双方均可转数值则按数值，否则按字符串 */
function compareValues(a, b) {
  const na = Number(a)
  const nb = Number(b)
  if (!isNaN(na) && !isNaN(nb) && a !== '' && b !== '') {
    return na === nb ? 0 : (na < nb ? -1 : 1)
  }
  const sa = String(a)
  const sb = String(b)
  return sa === sb ? 0 : (sa < sb ? -1 : 1)
}

/** 单条规则求值 */
function evalRule(rule, ctx, allowedFields, depth) {
  if (!rule || typeof rule !== 'object' || depth > 5) return true
  const field = rule.field
  if (!field) return true
  // 字段白名单：未传白名单时不做限制（兼容纯前端预览），传入后严格拒绝越权字段
  if (allowedFields && allowedFields.indexOf(field) < 0) return false
  const actual = ctx ? ctx[field] : undefined
  for (let i = 0; i < OPS.length; i++) {
    const op = OPS[i]
    if (!(op in rule)) continue
    const expected = rule[op]
    switch (op) {
      case 'eq': return String(actual) === String(expected)
      case 'ne': return String(actual) !== String(expected)
      case 'gt': return compareValues(actual, expected) > 0
      case 'gte': return compareValues(actual, expected) >= 0
      case 'lt': return compareValues(actual, expected) < 0
      case 'lte': return compareValues(actual, expected) <= 0
      case 'contains': return String(actual === undefined ? '' : actual).indexOf(String(expected)) >= 0
      case 'in': return Array.isArray(expected) && expected.some(v => String(v) === String(actual))
      default: return true
    }
  }
  return true
}

/** 条件表达式求值（and/or 可嵌套，深度上限 5，规则数上限 50） */
export function evalCondition(conditionJson, ctx, allowedFields) {
  if (!conditionJson || (typeof conditionJson === 'string' && !conditionJson.trim())) return true
  let cond
  try {
    cond = typeof conditionJson === 'string' ? JSON.parse(conditionJson) : conditionJson
  } catch (e) {
    return false // 脏 JSON 安全失败：条件不成立，不触发事件
  }
  return evalNode(cond, ctx, allowedFields, 0)
}

function evalNode(node, ctx, allowedFields, depth) {
  if (!node || typeof node !== 'object' || depth > 5) return false
  // 兼容单条规则（顶层直接是 {field, eq:...}）
  if (!Array.isArray(node.rules)) return evalRule(node, ctx, allowedFields, depth)
  const rules = node.rules.slice(0, 50)
  if (!rules.length) return false
  const op = String(node.op || 'and').toLowerCase()
  const results = rules.map(r => {
    if (r && (r.op || Array.isArray(r.rules))) return evalNode(r, ctx, allowedFields, depth + 1)
    return evalRule(r, ctx, allowedFields, depth + 1)
  })
  return op === 'or' ? results.some(Boolean) : results.every(Boolean)
}

/** 按 trigger + targetKey 取启用事件，按 priority 升序 */
export function pickEvents(events, trigger, targetKey) {
  if (!Array.isArray(events)) return []
  return events
    .filter(e => e && e.enabled !== '0' && e.trigger === trigger &&
      (e.targetKey === targetKey || !e.targetKey))
    .slice()
    .sort((a, b) => (a.priority || 0) - (b.priority || 0))
}

/**
 * 触发事件（异步）：内联优先，否则走绑定 action。
 * @returns Promise<{ blocked: boolean, results: Array }>
 */
export async function fireEvent(trigger, targetKey, ctx, options) {
  const opts = options || {}
  const chosen = pickEvents(opts.events, trigger, targetKey)
  const results = []
  for (let i = 0; i < chosen.length; i++) {
    const e = chosen[i]
    if (!evalCondition(e.conditionJson, ctx, opts.allowedFields)) continue
    const params = toParams(ctx)
    const payloadBase = {
      params,
      pageId: ctx && ctx.pageId,
      recordId: ctx && ctx.recordId
    }
    try {
      let response
      if (e.inlineType) {
        // 仅 HTTP / INTERNAL；COMMAND 由后端强拒绝（设计器也不提供该选项）
        response = await executeInline(Object.assign({}, payloadBase, {
          type: e.inlineType,
          target: e.inlineTarget
        }))
      } else if (e.actionId) {
        response = await executeAction(e.actionId, payloadBase)
      } else {
        continue
      }
      const data = response && response.data ? response.data : {}
      results.push({ event: e, response, blocked: data.blocked === true })
    } catch (err) {
      // 单事件失败不中断其余事件（方案 §6.4）
      console.warn('[CustomPanel] 事件执行失败', e.eventId, err)
      results.push({ event: e, error: err, blocked: false })
    }
  }
  const blocked = trigger === 'beforeSubmit' && results.some(r => r.blocked === true)
  return { blocked, results }
}
