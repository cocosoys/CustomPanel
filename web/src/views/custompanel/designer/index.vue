<template>
  <div class="designer-page">
    <!-- 顶部工具条 -->
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-label">编辑页面</span>
        <el-select v-model="pageId" filterable placeholder="选择要设计的页面" style="width: 240px"
                   @change="onPageChange">
          <el-option v-for="p in pages" :key="p.pageId" :label="p.pageName + '（' + p.pageKey + '）'"
                     :value="p.pageId" />
        </el-select>
        <el-tag v-if="currentPage" size="mini" :type="currentPage.storeMode === 'MANAGED' ? 'warning' : 'success'">
          {{ currentPage.storeMode === 'MANAGED' ? 'MANAGED' : 'FREE' }}
        </el-tag>
        <span v-if="layoutMeta.version" class="toolbar-meta">v{{ layoutMeta.version }} · {{ layoutMeta.status || 'DRAFT' }}</span>
      </div>
      <div class="toolbar-right">
        <span class="limit-hint">{{ fieldCount }}/{{ maxFields }} 控件 · {{ confBytes }}/{{ maxConfBytes }} 字节</span>
        <el-button size="mini" icon="el-icon-view" :disabled="!fieldList.length" @click="openPreview">预览</el-button>
        <el-button size="mini" icon="el-icon-download" :disabled="!fieldList.length" @click="openExport">导出 SFC</el-button>
        <el-button size="mini" type="primary" icon="el-icon-upload2" :disabled="!pageId"
                   :loading="saving" @click="saveDraft">保存草稿</el-button>
        <el-button size="mini" type="success" icon="el-icon-check" :disabled="!pageId"
                   :loading="publishing" @click="publishLayout">发布</el-button>
      </div>
    </div>

    <div v-loading="loading" class="designer-body">
      <!-- 左：组件面板 -->
      <aside class="panel panel-left">
        <div class="panel-head"><i class="el-icon-menu" /> 组件库</div>
        <el-scrollbar class="panel-scroll" wrap-class="panel-scroll-wrap">
          <div v-for="group in componentGroups" :key="group.name" class="comp-group">
            <div class="comp-group-title">{{ group.name }}</div>
            <draggable :group="{ name: 'designer-fields', pull: 'clone', put: false }"
                       :sort="false" :clone="handleClone" :animation="150" class="comp-grid">
              <div v-for="(c, ci) in group.items" :key="group.name + '-' + (c.tag || c.layout) + '-' + ci"
                   class="comp-item" :title="'拖入画布：' + c.label">
                <i :class="'el-icon-' + (c.tagIcon || 'document')" />
                <span>{{ c.label }}</span>
              </div>
            </draggable>
          </div>
          <el-alert type="info" :closable="false" class="comp-tip"
                    title="拖拽组件到中间画布，点击画布中的组件编辑右侧属性" />
        </el-scrollbar>
      </aside>

      <!-- 中：画布 -->
      <main class="panel panel-center">
        <div class="panel-head">
          <i class="el-icon-edit-outline" /> 画布
          <span class="canvas-hint">拖动排序 · 悬浮操作复制/删除</span>
        </div>
        <el-scrollbar class="panel-scroll" wrap-class="panel-scroll-wrap">
          <div class="canvas-paper">
            <div class="canvas-title">{{ currentPage ? currentPage.pageName : '未选择页面' }}</div>
            <el-form :label-width="formConf.labelWidth + 'px'" :size="formConf.size || 'medium'"
                     class="canvas-form" @submit.native.prevent>
              <draggable v-model="fieldList" group="designer-fields" handle=".drag-handle"
                         :animation="180" ghost-class="ghost-field" class="canvas-list">
                <div v-for="(f, idx) in fieldList" :key="f.__uid"
                     class="canvas-field" :class="{ 'is-active': activeUid === f.__uid }"
                     @click="selectField(f)">
                  <div class="field-toolbar">
                    <i class="el-icon-rank drag-handle" title="拖动排序" />
                    <span class="field-tag">{{ f.label || f.tag }}</span>
                    <i class="el-icon-document-copy" title="复制" @click.stop="copyField(idx)" />
                    <i class="el-icon-delete" title="删除" @click.stop="removeField(idx)" />
                  </div>
                  <div class="field-preview" @click.stop="selectField(f)">
                    <div v-if="!f.tag" class="layout-placeholder">
                      <i class="el-icon-layout" /> {{ f.layout || '布局容器' }}
                      <span v-if="f.children && f.children.length">（{{ f.children.length }} 个子组件）</span>
                    </div>
                    <FormRender v-else :conf="renderConf(f)" />
                  </div>
                </div>
              </draggable>
              <div v-if="!fieldList.length" class="canvas-empty">
                <i class="el-icon-plus" />
                <span>从左侧拖入组件开始设计</span>
              </div>
            </el-form>
          </div>
        </el-scrollbar>
      </main>

      <!-- 右：属性 / 事件面板 -->
      <aside class="panel panel-right">
        <el-tabs v-model="rightTab" class="right-tabs">
          <el-tab-pane label="属性" name="prop">
            <el-scrollbar class="panel-scroll" wrap-class="panel-scroll-wrap">
              <div v-if="!activeField" class="prop-empty">请先在画布中选择一个组件</div>
              <div v-else class="prop-form">
                <el-form label-width="76px" size="mini">
                  <el-form-item label="标签"><el-input v-model="activeField.label" @input="touchField(activeField)" /></el-form-item>
                  <el-form-item label="字段标识">
                    <el-input v-model="activeField.vModel" :disabled="!activeField.vModel"
                              placeholder="按钮可留空"><template slot="prepend">vModel</template></el-input>
                  </el-form-item>
                  <el-form-item label="占位宽度">
                    <el-select v-model="activeField.span" style="width:100%">
                      <el-option v-for="s in [24,12,8,6,16]" :key="s" :label="s + ' / 24'" :value="s" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="默认值">
                    <el-input v-model="activeField.defaultValue" placeholder="留空则无默认值" />
                  </el-form-item>
                  <el-form-item label="提示文字">
                    <el-input v-model="activeField.placeholder" />
                  </el-form-item>
                  <el-form-item label="必填">
                    <el-switch v-model="activeField.required" />
                  </el-form-item>
                  <el-form-item label="禁用">
                    <el-switch v-model="activeField.disabled" />
                  </el-form-item>
                  <el-form-item label="列表展示">
                    <el-switch v-model="activeField.showInList" />
                  </el-form-item>

                  <!-- 选项配置 -->
                  <div v-if="hasOptions(activeField)" class="prop-section">
                    <div class="prop-section-title">
                      选项
                      <el-button size="mini" type="text" icon="el-icon-plus" @click="addOption">添加</el-button>
                    </div>
                    <div v-for="(o, oi) in activeField.options" :key="oi" class="option-row">
                      <el-input v-model="o.label" size="mini" placeholder="显示" />
                      <el-input v-model="o.value" size="mini" placeholder="值" />
                      <i class="el-icon-delete" @click="activeField.options.splice(oi, 1)" />
                    </div>
                  </div>

                  <!-- 校验正则 -->
                  <div class="prop-section">
                    <div class="prop-section-title">校验正则</div>
                    <el-input v-model="patternText" size="mini" placeholder="如 ^[a-z]+$（留空不校验）"
                              @change="applyPattern" />
                  </div>

                  <!-- 事件快捷 -->
                  <div class="prop-section">
                    <div class="prop-section-title">该组件事件</div>
                    <el-button size="mini" plain icon="el-icon-link" @click="addEventFor(activeField, 'onChange')">值变化</el-button>
                    <el-button size="mini" plain icon="el-icon-thumb" @click="addEventFor(activeField, 'onClick')">点击</el-button>
                  </div>
                </el-form>
              </div>
            </el-scrollbar>
          </el-tab-pane>

          <el-tab-pane label="事件" name="event">
            <div class="event-pane-head">
              <el-button size="mini" type="primary" plain icon="el-icon-plus" @click="addEventFor(null, 'onLoad')">新增事件</el-button>
            </div>
            <el-scrollbar class="panel-scroll" wrap-class="panel-scroll-wrap">
              <div v-if="!eventList.length" class="prop-empty">尚无事件，点击「新增事件」绑定动作或内联命令</div>
              <div v-for="e in eventList" :key="e.eventId || e.__uid" class="event-card">
                <div class="event-card-head">
                  <span class="event-name">{{ e.name || '未命名事件' }}</span>
                  <el-switch v-model="e.enabledBool" size="mini" @change="toggleEventEnabled(e)" />
                  <i class="el-icon-edit" title="编辑" @click="editEventRow(e)" />
                  <i class="el-icon-delete" title="删除" @click="deleteEventRow(e)" />
                </div>
                <div class="event-card-meta">
                  <el-tag size="mini">{{ e.trigger }}</el-tag>
                  <span v-if="e.targetKey">目标：{{ e.targetKey }}</span>
                  <el-tag v-if="e.inlineType" size="mini" type="warning">内联 {{ e.inlineType }}</el-tag>
                  <el-tag v-else-if="e.actionId" size="mini" type="success">动作 #{{ e.actionId }}</el-tag>
                </div>
              </div>
            </el-scrollbar>
          </el-tab-pane>

          <el-tab-pane label="表单" name="form">
            <el-scrollbar class="panel-scroll" wrap-class="panel-scroll-wrap">
              <div class="prop-form">
                <el-form label-width="86px" size="mini">
                  <el-form-item label="标签宽度"><el-input-number v-model="formConf.labelWidth" :min="40" :max="200" size="mini" /></el-form-item>
                  <el-form-item label="尺寸">
                    <el-select v-model="formConf.size" size="mini" style="width:100%">
                      <el-option label="medium" value="medium" /><el-option label="small" value="small" /><el-option label="mini" value="mini" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="标签位置">
                    <el-select v-model="formConf.labelPosition" size="mini" style="width:100%">
                      <el-option label="右" value="right" /><el-option label="上" value="top" /><el-option label="左" value="left" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="栏间距"><el-input-number v-model="formConf.gutter" :min="0" :max="40" size="mini" /></el-form-item>
                  <el-form-item label="显示按钮"><el-switch v-model="formConf.formBtns" /></el-form-item>
                </el-form>
              </div>
            </el-scrollbar>
          </el-tab-pane>
        </el-tabs>
      </aside>
    </div>

    <!-- 预览弹窗 -->
    <el-dialog title="页面预览" :visible.sync="previewVisible" width="640px" append-to-body>
      <el-form :label-width="formConf.labelWidth + 'px'" :model="previewModel" size="mini">
        <el-form-item v-for="f in renderableList" :key="'pv-' + f.__uid" :label="f.label">
          <FormRender :conf="renderConf(f)" v-model="previewModel[f.vModel]" />
        </el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="previewVisible = false">关闭</el-button></div>
    </el-dialog>

    <!-- 事件编辑弹窗 -->
    <el-dialog :title="eventEditing.eventId ? '编辑事件' : '新增事件'" :visible.sync="eventDialogVisible"
               width="560px" append-to-body>
      <el-form :model="eventEditing" label-width="92px" size="mini">
        <el-form-item label="事件名称"><el-input v-model="eventEditing.name" placeholder="便于识别的名称" /></el-form-item>
        <el-form-item label="触发时机">
          <el-select v-model="eventEditing.trigger" style="width:100%">
            <el-option v-for="t in triggerOptions" :key="t.value" :label="t.label + ' · ' + t.value" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标组件">
          <el-select v-model="eventEditing.targetKey" clearable filterable placeholder="留空=页面级" style="width:100%">
            <el-option v-for="f in fieldList" :key="f.__uid" :label="(f.label || f.tag) + '（' + f.vModel + '）'"
                       :value="f.vModel || ''" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行方式">
          <el-radio-group v-model="eventEditing.mode" @change="onEventModeChange">
            <el-radio label="action">绑定动作</el-radio>
            <el-radio label="inline">内联命令</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="eventEditing.mode === 'action'" label="动作">
          <el-select v-model="eventEditing.actionId" filterable placeholder="选择已配置动作" style="width:100%">
            <el-option v-for="a in actionList" :key="a.actionId" :label="a.actionName + '（' + a.actionType + '）'"
                       :value="a.actionId" />
          </el-select>
        </el-form-item>
        <template v-else>
          <el-form-item label="内联类型">
            <el-select v-model="eventEditing.inlineType" style="width:100%">
              <el-option label="HTTP" value="HTTP" /><el-option label="INTERNAL" value="INTERNAL" />
            </el-select>
            <div class="form-tip">仅支持 HTTP / INTERNAL；COMMAND 必须到「动作管理」绑定</div>
          </el-form-item>
          <el-form-item label="命令模板">
            <el-input v-model="eventEditing.inlineTarget" type="textarea" :rows="3"
                      placeholder="如 INTERNAL: GET /api/xxx/{fieldKey}" />
          </el-form-item>
        </template>
        <el-form-item label="条件 JSON">
          <el-input v-model="eventEditing.conditionJson" type="textarea" :rows="2"
                    placeholder='如 {"op":"and","rules":[{"field":"status","eq":"1"}]}' />
        </el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="eventEditing.priority" :min="0" size="mini" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="eventDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="eventSaving" @click="submitEvent">保存事件</el-button>
      </div>
    </el-dialog>

    <!-- 导出 SFC 弹窗 -->
    <el-dialog title="导出 Vue 组件（SFC）" :visible.sync="exportVisible" width="620px" append-to-body>
      <el-alert type="info" :closable="false" class="export-tip"
                title="导出为二次开发参考源码（弹窗 modal 风格），不回灌运行时；运行时以 confJson 为准。" />
      <el-input v-model="exportCode" type="textarea" :rows="16" readonly class="export-code" />
      <div slot="footer">
        <el-button @click="exportVisible = false">关闭</el-button>
        <el-button type="primary" icon="el-icon-download" @click="downloadSfc">下载 .vue</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import FormRender from '@/utils/generator/render'
import { formConf as defaultFormConf, inputComponents, selectComponents, layoutComponents } from '@/utils/generator/config'
import { makeUpHtml, vueTemplate, vueScript, cssStyle } from '@/utils/generator/html'
import { makeUpJs } from '@/utils/generator/js'
import { makeUpCss } from '@/utils/generator/css'
import { buildFormModel } from '@/utils/generator/adapter'
// 设计器内部字段：不参与 Element 控件渲染（与 adapter.DESIGNER_META 对齐）
const DESIGNER_META = ['__uid', '__config__', 'layout', 'showInList', 'required', 'regList', 'changeTag', 'document', 'tagIcon']
import {
  listPage, layoutDetail, saveLayout, publishLayout,
  listEvent, addEvent, editEvent, removeEvent, listAction
} from '@/api/custompanel'

// 与后端 config.yml designer.* 默认值对齐（前端预校验，后端再校验一次）
const MAX_FIELDS = 200
const MAX_CONF_BYTES = 262144

const TRIGGERS = [
  { value: 'onLoad', label: '页面加载' },
  { value: 'onChange', label: '值变化' },
  { value: 'onClick', label: '点击' },
  { value: 'beforeSubmit', label: '提交前' },
  { value: 'afterSubmit', label: '提交后' }
]

let uidSeed = 1
function nextUid() { return 'f_' + Date.now().toString(36) + '_' + (uidSeed++) }

export default {
  name: 'CustomPanelDesigner',
  components: { draggable, FormRender },
  data() {
    return {
      loading: false,
      saving: false,
      publishing: false,
      pageId: undefined,
      pages: [],
      formConf: Object.assign({}, defaultFormConf),
      fieldList: [],
      activeUid: undefined,
      rightTab: 'prop',
      layoutMeta: { version: 0, status: '' },
      maxFields: MAX_FIELDS,
      maxConfBytes: MAX_CONF_BYTES,
      // 事件
      eventList: [],
      triggerOptions: TRIGGERS,
      eventDialogVisible: false,
      eventSaving: false,
      eventEditing: this.emptyEvent(),
      // 动作库
      actionList: [],
      // 预览 / 导出
      previewVisible: false,
      previewModel: {},
      exportVisible: false,
      exportCode: ''
    }
  },
  computed: {
    componentGroups() {
      return [
        { name: '基础输入', items: inputComponents },
        { name: '选择 / 日期', items: selectComponents },
        { name: '布局 / 按钮', items: layoutComponents }
      ]
    },
    currentPage() {
      return this.pages.find(p => p.pageId === this.pageId) || null
    },
    activeField() {
      return this.fieldList.find(f => f.__uid === this.activeUid) || null
    },
    renderableList() {
      return this.fieldList.filter(f => f.tag || f.layout)
    },
    fieldCount() {
      return this.fieldList.length
    },
    confBytes() {
      try {
        return JSON.stringify(this.serializeFields()).length
      } catch (e) {
        return 0
      }
    },
    patternText: {
      get() {
        const f = this.activeField
        if (!f || !Array.isArray(f.regList) || !f.regList.length) return ''
        const p = f.regList[0]
        return typeof p === 'string' ? p : (p && p.pattern) || ''
      },
      set() { /* 由 applyPattern 处理 */ }
    }
  },
  created() {
    this.pageId = this.$route.query.pageId ? Number(this.$route.query.pageId) : undefined
    this.loadPages()
    this.loadActions()
    if (this.pageId) this.loadLayout()
  },
  methods: {
    emptyEvent() {
      return {
        eventId: null, __uid: null, name: '', trigger: 'onLoad', targetKey: '',
        mode: 'action', actionId: null, inlineType: 'HTTP', inlineTarget: '',
        conditionJson: '', priority: 0, enabled: '1', enabledBool: true
      }
    },
    loadPages() {
      listPage({ pageNum: 1, pageSize: 500 }).then(res => {
        this.pages = res.rows || []
        if (!this.pageId && this.pages.length) {
          this.pageId = this.pages[0].pageId
          this.loadLayout()
        }
      })
    },
    loadActions() {
      listAction({ pageNum: 1, pageSize: 500 }).then(res => { this.actionList = res.rows || [] })
    },
    onPageChange() {
      this.activeUid = undefined
      this.loadLayout()
    },
    /** 读取布局；后端 detail 会触发一次性迁移（旧 panel_field → v1 PUBLISHED） */
    loadLayout() {
      if (!this.pageId) return
      this.loading = true
      layoutDetail(this.pageId).then(res => {
        const layout = res.data || null
        this.formConf = Object.assign({}, defaultFormConf, layout ? this.safeParse(layout.formConfJson, {}) : {})
        const raw = layout ? this.safeParse(layout.fieldsJson, []) : []
        this.fieldList = (Array.isArray(raw) ? raw : []).map(f => this.decorateField(f))
        this.layoutMeta = layout
          ? { version: layout.version, status: layout.status }
          : { version: 0, status: '' }
        this.activeUid = undefined
        this.loadEvents()
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    safeParse(json, fallback) {
      if (!json) return fallback
      try { return JSON.parse(json) } catch (e) { return fallback }
    },
    /** 载入字段补前端内部 uid（不写入 confJson） */
    decorateField(f) {
      const copy = Object.assign({}, f)
      copy.__uid = f.__uid || nextUid()
      if (Array.isArray(f.options)) copy.options = f.options.map(o => Object.assign({}, o))
      if (Array.isArray(f.regList)) copy.regList = f.regList.slice()
      return copy
    },
    // ===== 画布操作 =====
    /** 从左侧组件库克隆：clone 钩子返回的对象被插入画布数组 */
    cloneFromPalette(comp) {
      const base = Object.assign({}, comp)
      base.__uid = nextUid()
      base.layout = comp.layout || 'colFormItem'
      if (!base.vModel && base.tag && base.tag !== 'el-button') base.vModel = 'field_' + base.__uid
      if (!base.label) base.label = comp.label || comp.tag
      if (base.tag === 'el-button' && !base.default) base.default = base.label
      if (Array.isArray(comp.options)) base.options = comp.options.map(o => Object.assign({}, o))
      if (Array.isArray(comp.regList)) base.regList = comp.regList.slice()
      return base
    },
    // vuedraggable 的 clone：左侧 pull:'clone' 拖入画布时生成新字段
    handleClone(comp) { return this.cloneFromPalette(comp) },
    selectField(f) { this.activeUid = f.__uid },
    touchField() { /* 属性面板双向绑定即改，预留钩子 */ },
    copyField(idx) {
      const src = this.fieldList[idx]
      if (!src) return
      const copy = this.cloneFromPalette(src)
      copy.label = (src.label || '') + ' 副本'
      if (copy.vModel) copy.vModel = 'field_' + copy.__uid
      this.fieldList.splice(idx + 1, 0, copy)
      this.activeUid = copy.__uid
    },
    removeField(idx) {
      const removed = this.fieldList.splice(idx, 1)[0]
      if (removed && removed.__uid === this.activeUid) this.activeUid = undefined
    },
    hasOptions(f) {
      return f && ['el-select', 'el-radio-group', 'el-checkbox-group', 'el-cascader'].indexOf(f.tag) >= 0
    },
    addOption() {
      if (!this.activeField) return
      if (!Array.isArray(this.activeField.options)) this.$set(this.activeField, 'options', [])
      this.activeField.options.push({ label: '新选项', value: 'value' + (this.activeField.options.length + 1) })
    },
    applyPattern(value) {
      if (!this.activeField) return
      const text = String(value || '').trim()
      if (!text) {
        this.$set(this.activeField, 'regList', [])
        return
      }
      this.$set(this.activeField, 'regList', [{ pattern: text, message: '格式不正确' }])
    },
    renderConf(f) {
      // 剔除设计器内部字段，避免落到 Element props 产生告警
      const conf = {}
      Object.keys(f).forEach(k => { if (DESIGNER_META.indexOf(k) < 0) conf[k] = f[k] })
      if (conf.vModel === '' || conf.vModel === undefined || conf.vModel === null) delete conf.vModel
      return conf
    },
    // ===== 序列化 / 保存 =====
    serializeFields() {
      return this.fieldList.map(f => {
        const out = {}
        Object.keys(f).forEach(k => { if (k !== '__uid') out[k] = f[k] })
        return out
      })
    },
    checkLimits() {
      if (this.fieldCount > this.maxFields) {
        this.$message.error('控件数超限：' + this.fieldCount + ' > ' + this.maxFields)
        return false
      }
      const bytes = new Blob([JSON.stringify(this.serializeFields())]).size
      if (bytes > this.maxConfBytes) {
        this.$message.error('布局数据超限：' + bytes + ' > ' + this.maxConfBytes + ' 字节')
        return false
      }
      return true
    },
    saveDraft() {
      if (!this.pageId || !this.checkLimits()) return
      this.saving = true
      saveLayout({
        pageId: this.pageId,
        formConfJson: JSON.stringify(this.formConf),
        fieldsJson: JSON.stringify(this.serializeFields()),
        remark: '设计器保存'
      }).then(() => {
        this.saving = false
        this.$message.success('草稿已保存')
        this.loadLayout()
      }).catch(() => { this.saving = false })
    },
    publishLayout() {
      if (!this.pageId || !this.checkLimits()) return
      this.$confirm('发布后当前布局将立即对渲染页生效，确认发布？', '发布确认', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(() => {
        this.publishing = true
        // 先保存再发布，确保发布的是当前画布内容
        return saveLayout({
          pageId: this.pageId,
          formConfJson: JSON.stringify(this.formConf),
          fieldsJson: JSON.stringify(this.serializeFields()),
          remark: '发布前保存'
        }).then(() => publishLayout(this.pageId))
      }).then(() => {
        this.publishing = false
        this.$message.success('已发布')
        this.loadLayout()
      }).catch(() => { this.publishing = false })
    },
    // ===== 事件 =====
    loadEvents() {
      if (!this.pageId) return
      listEvent({ pageNum: 1, pageSize: 500, pageId: this.pageId }).then(res => {
        this.eventList = (res.rows || []).map(e => Object.assign({}, e, { enabledBool: e.enabled !== '0' }))
      })
    },
    addEventFor(field, trigger) {
      this.eventEditing = this.emptyEvent()
      if (field) this.eventEditing.targetKey = field.vModel || ''
      this.eventEditing.trigger = trigger || 'onLoad'
      this.eventDialogVisible = true
    },
    editEventRow(e) {
      this.eventEditing = Object.assign({}, e, {
        mode: e.inlineType ? 'inline' : 'action',
        enabledBool: e.enabled !== '0'
      })
      this.eventDialogVisible = true
    },
    onEventModeChange() { /* 模板切换由 v-if 驱动 */ },
    submitEvent() {
      const e = this.eventEditing
      if (!e.name || !e.name.trim()) { this.$message.error('请填写事件名称'); return }
      if (!this.pageId) { this.$message.error('请先选择页面'); return }
      const payload = {
        pageId: this.pageId,
        name: e.name.trim(),
        trigger: e.trigger,
        targetKey: e.targetKey || null,
        conditionJson: e.conditionJson || null,
        priority: e.priority || 0,
        enabled: e.enabledBool ? '1' : '0',
        actionId: null,
        inlineType: null,
        inlineTarget: null
      }
      if (e.mode === 'inline') {
        payload.inlineType = e.inlineType
        payload.inlineTarget = e.inlineTarget
      } else {
        if (!e.actionId) { this.$message.error('请选择动作'); return }
        payload.actionId = e.actionId
      }
      this.eventSaving = true
      const req = e.eventId ? editEvent(e.eventId, payload) : addEvent(payload)
      req.then(() => {
        this.eventSaving = false
        this.eventDialogVisible = false
        this.$message.success('事件已保存')
        this.loadEvents()
      }).catch(() => { this.eventSaving = false })
    },
    toggleEventEnabled(e) {
      const payload = {
        pageId: e.pageId, name: e.name, trigger: e.trigger, targetKey: e.targetKey,
        conditionJson: e.conditionJson, priority: e.priority,
        enabled: e.enabledBool ? '1' : '0', actionId: e.actionId,
        inlineType: e.inlineType, inlineTarget: e.inlineTarget
      }
      editEvent(e.eventId, payload).catch(() => { e.enabledBool = !e.enabledBool })
    },
    deleteEventRow(e) {
      this.$confirm('确认删除事件「' + (e.name || e.eventId) + '」？', '删除确认', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(() => removeEvent(e.eventId)).then(() => {
        this.$message.success('已删除')
        this.loadEvents()
      })
    },
    // ===== 预览 / 导出 =====
    openPreview() {
      this.previewModel = buildFormModel(this.serializeFields())
      this.previewVisible = true
    },
    openExport() {
      const conf = Object.assign({}, this.formConf, { fields: this.serializeFields() })
      try {
        // RuoYi generator 的三件套返回裸片段：makeUpHtml=template 内容、
        // makeUpJs=export default 对象、makeUpCss=CSS 文本。必须分别用
        // vueTemplate/vueScript/cssStyle 包成合法 SFC，否则导出的 .vue 无法编译。
        const template = vueTemplate(makeUpHtml(conf, 'dialog'))
        const script = vueScript(makeUpJs(conf, 'dialog'))
        const style = cssStyle(makeUpCss(conf))
        const header = [
          '<!-- 由 CustomPanel 页面设计器导出 · dialog(modal) 风格 -->',
          '<!-- 二次开发参考源码；运行时以 confJson 为准，本文件不回灌插件 -->',
          '<!-- 页面：' + (this.currentPage ? this.currentPage.pageName + '（' + this.currentPage.pageKey + '）' : '未命名') + ' -->',
          ''
        ].join('\n')
        this.exportCode = [header, template, script, style].join('\n\n')
        this.exportVisible = true
      } catch (e) {
        this.$message.error('导出失败：' + e.message)
      }
    },
    downloadSfc() {
      const blob = new Blob([this.exportCode], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = (this.currentPage ? this.currentPage.pageKey : 'panel') + '.vue'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    }
  }
}
</script>

<style scoped>
.designer-page { display: flex; flex-direction: column; height: calc(100vh - 84px); }
.designer-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 8px 14px;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 4px; margin-bottom: 8px; }
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 8px; }
.toolbar-label { font-size: 13px; color: #606266; font-weight: 600; }
.toolbar-meta, .limit-hint { font-size: 12px; color: #909399; }
.designer-body { display: flex; flex: 1; gap: 8px; min-height: 0; }
.panel { background: #fff; border: 1px solid #e4e7ed; border-radius: 4px; display: flex; flex-direction: column; min-height: 0; }
.panel-left { width: 240px; flex-shrink: 0; }
.panel-center { flex: 1; }
.panel-right { width: 340px; flex-shrink: 0; }
.panel-head { padding: 8px 12px; border-bottom: 1px solid #ebeef5; font-size: 13px; font-weight: 600; color: #303133; }
.canvas-hint { float: right; font-size: 11px; font-weight: 400; color: #c0c4cc; }
.panel-scroll { flex: 1; min-height: 0; }
.panel-scroll-wrap { height: 100%; padding: 10px; }
.comp-group { margin-bottom: 14px; }
.comp-group-title { font-size: 12px; color: #909399; margin-bottom: 8px; }
.comp-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.comp-item { display: flex; align-items: center; gap: 5px; padding: 7px 6px; border: 1px solid #dcdfe6;
  border-radius: 3px; font-size: 12px; cursor: grab; color: #606266; background: #fafafa; }
.comp-item:hover { border-color: #409eff; color: #409eff; }
.comp-tip { margin-top: 10px; }
.canvas-paper { min-height: 100%; background: #fafafa; border: 1px dashed #dcdfe6; border-radius: 4px; padding: 16px; }
.canvas-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 14px; }
.canvas-list { min-height: 120px; }
.canvas-field { position: relative; border: 1px solid transparent; border-radius: 4px; padding: 6px; margin-bottom: 6px; background: #fff; }
.canvas-field:hover { border-color: #c0c4cc; }
.canvas-field.is-active { border-color: #409eff; box-shadow: 0 0 0 1px #409eff inset; }
.field-toolbar { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #909399; margin-bottom: 4px; }
.field-toolbar i { cursor: pointer; }
.field-toolbar i:hover { color: #409eff; }
.drag-handle { cursor: move; }
.field-tag { flex: 1; color: #606266; }
.ghost-field { opacity: 0.5; background: #ecf5ff; }
.layout-placeholder { padding: 16px; border: 1px dashed #dcdfe6; border-radius: 4px; text-align: center;
  color: #909399; font-size: 12px; background: #fafafa; }
.canvas-empty { display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 48px 0; color: #c0c4cc; gap: 8px; font-size: 13px; }
.canvas-empty i { font-size: 32px; }
.right-tabs { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.right-tabs /deep/ .el-tabs__content { flex: 1; min-height: 0; }
.right-tabs /deep/ .el-tab-pane { height: 100%; }
.prop-empty { color: #909399; font-size: 12px; text-align: center; padding: 40px 10px; }
.prop-form { padding: 4px 12px 12px 4px; }
.prop-section { margin-top: 10px; padding-top: 8px; border-top: 1px dashed #ebeef5; }
.prop-section-title { font-size: 12px; color: #606266; margin-bottom: 6px; display: flex; justify-content: space-between; }
.option-row { display: flex; gap: 4px; align-items: center; margin-bottom: 4px; }
.option-row i { cursor: pointer; color: #909399; }
.event-pane-head { padding: 8px 12px 0; }
.event-card { margin: 8px 12px; padding: 8px; border: 1px solid #ebeef5; border-radius: 4px; }
.event-card-head { display: flex; align-items: center; gap: 8px; }
.event-name { flex: 1; font-size: 13px; color: #303133; }
.event-card-head i { cursor: pointer; color: #909399; }
.event-card-head i:hover { color: #409eff; }
.event-card-meta { display: flex; gap: 6px; align-items: center; margin-top: 6px; font-size: 11px; color: #909399; }
.form-tip { font-size: 11px; color: #909399; margin-top: 2px; }
.export-tip, .export-code { margin-bottom: 10px; }
</style>
