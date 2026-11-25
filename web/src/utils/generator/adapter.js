/**
 * RuoYi generator conf ↔ 本插件表单模型适配层（P2）。
 *
 * 后端 panel_layout.fieldsJson 是控件数组（每项含 tag / vModel / label / defaultValue…），
 * render.js 按 props:['conf'] 直接渲染 Element 控件。本模块负责：
 *   1) 解析 confJson / fieldsJson 并剔除设计器专用元数据（避免落到 Element props 产生告警）；
 *   2) conf → formData 默认值（按 tag 归类型）；
 *   3) formData → 提交数据（与后端 {fieldKey: string} 契约对齐）；
 *   4) conf → el-form-item rules（required / regList.pattern）。
 *
 * 不修改 render.js 的 conf 契约；值回显由 render.js 的 $attrs.value 绑定完成。
 */

/** 设计器内部字段：不参与 Element 控件渲染 */
const DESIGNER_META = [
  'tagIcon', 'changeTag', 'document', 'showInList', 'sortOrder',
  '__vModel__', '__config__', 'formId', 'layout', 'key', 'noLabel',
  'labelWidth', 'regList', 'required'
]

/** 需要数组初值的控件 */
const ARRAY_TAGS = ['el-checkbox-group', 'el-cascader', 'el-upload']

/** 需要数值初值的控件 */
const NUMBER_TAGS = ['el-input-number', 'el-rate', 'el-slider']

/**
 * 解析布局 JSON（容错：脏 JSON 返回 null，由调用方降级到旧字段渲染分支）。
 */
export function parseLayout(layout) {
  if (!layout) return null
  let formConf = {}
  let fields = []
  try {
    formConf = layout.formConfJson ? JSON.parse(layout.formConfJson) : {}
  } catch (e) {
    formConf = {}
  }
  try {
    const raw = layout.fieldsJson ? JSON.parse(layout.fieldsJson) : []
    fields = Array.isArray(raw) ? raw : []
  } catch (e) {
    fields = []
  }
  return { formConf, fields }
}

/**
 * 过滤出可渲染控件：必须有 tag，且建议有 vModel（布局控件可无 vModel）。
 * 同时剔除设计器元数据。
 */
export function renderableFields(fields) {
  if (!Array.isArray(fields)) return []
  return fields
    .filter(f => f && f.tag)
    .map(f => {
      const conf = {}
      Object.keys(f).forEach(key => {
        if (DESIGNER_META.indexOf(key) < 0) conf[key] = f[key]
      })
      // render.js 的 componentChild 直接对 options 调 forEach：缺字段必须补空数组，否则运行时抛错
      if (['el-select', 'el-radio-group', 'el-checkbox-group', 'el-cascader'].indexOf(conf.tag) >= 0
          && !Array.isArray(conf.options)) {
        conf.options = []
      }
      return conf
    })
}

/** 控件默认表单值（按 tag 归类型） */
export function defaultValueOf(field) {
  if (!field) return ''
  if (ARRAY_TAGS.indexOf(field.tag) >= 0) return []
  if (NUMBER_TAGS.indexOf(field.tag) >= 0) return undefined
  if (field.tag === 'el-switch' || field.tag === 'el-radio' || field.tag === 'el-checkbox') return false
  if (field.defaultValue !== undefined && field.defaultValue !== null) return field.defaultValue
  return ''
}

/** conf 数组 → formData 初值 */
export function buildFormModel(fields) {
  const model = {}
  renderableFields(fields).forEach(f => {
    if (!f.vModel) return
    model[f.vModel] = defaultValueOf(f)
  })
  return model
}

/** 按控件类型把表单值归一为后端契约（字符串 / 数值 / 布尔 / 数组） */
export function normalizeValue(field, value) {
  if (!field) return value
  if (ARRAY_TAGS.indexOf(field.tag) >= 0) return Array.isArray(value) ? value : []
  if (NUMBER_TAGS.indexOf(field.tag) >= 0) {
    return (value === undefined || value === null || value === '') ? undefined : Number(value)
  }
  if (field.tag === 'el-switch' || field.tag === 'el-radio' || field.tag === 'el-checkbox') {
    return value === true || value === 'true' || value === '1'
  }
  return (value === undefined || value === null) ? '' : String(value)
}

/** formData → 提交数据（键 = vModel，值按控件类型归一） */
export function toSubmitData(fields, formData) {
  const data = {}
  renderableFields(fields).forEach(f => {
    if (!f.vModel) return
    const value = normalizeValue(f, (formData || {})[f.vModel])
    if (value !== undefined && value !== null && value !== '') data[f.vModel] = value
  })
  return data
}

/** conf → el-form rules（required + regList.pattern；不 eval 正则字符串以外内容） */
export function buildRules(fields) {
  const rules = {}
  renderableFields(fields).forEach(f => {
    if (!f.vModel) return
    const list = []
    const label = f.label || f.vModel
    const meta = findMeta(fields, f.vModel)
    if (meta && meta.required === true) {
      list.push({ required: true, message: label + '不能为空', trigger: 'blur' })
    }
    const patterns = (meta && Array.isArray(meta.regList)) ? meta.regList : []
    patterns.forEach(p => {
      if (!p) return
      const pattern = typeof p === 'string' ? p : p.pattern
      if (!pattern) return
      list.push({
        pattern: new RegExp(pattern),
        message: (typeof p === 'object' && p.message) ? p.message : label + '格式不正确',
        trigger: 'blur'
      })
    })
    if (list.length) rules[f.vModel] = list
  })
  return rules
}

/** 从原始 fields 找同名控件元数据（required / regList 在渲染 conf 中被剔除） */
function findMeta(fields, vModel) {
  if (!Array.isArray(fields)) return null
  for (let i = 0; i < fields.length; i++) {
    if (fields[i] && fields[i].vModel === vModel) return fields[i]
  }
  return null
}

/** 插值：target 中 {key} ← ctx[key]；未命中保留原样（后端 executeInline 亦做同一插值） */
export function interpolate(target, ctx) {
  if (!target) return ''
  let out = String(target)
  const source = ctx || {}
  Object.keys(source).forEach(key => {
    const placeholder = '{' + key + '}'
    if (out.indexOf(placeholder) >= 0) {
      const val = source[key]
      out = out.split(placeholder).join(val === undefined || val === null ? '' : String(val))
    }
  })
  return out
}

/** 上下文 → 后端 params（仅保留标量，对象/数组转 JSON 字符串） */
export function toParams(ctx) {
  const params = {}
  const source = ctx || {}
  Object.keys(source).forEach(key => {
    const val = source[key]
    if (val === undefined || val === null) return
    if (typeof val === 'object') {
      try { params[key] = JSON.stringify(val) } catch (e) { /* 忽略循环引用 */ }
    } else {
      params[key] = String(val)
    }
  })
  return params
}
