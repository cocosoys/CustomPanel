<template>
  <div class="app-container">
    <div v-loading="loading">
      <!-- 页面头 -->
      <div v-if="schema.page" class="render-header">
        <el-button icon="el-icon-back" size="mini" @click="goBack">返回</el-button>
        <h2 class="render-title">
          {{ schema.page.pageName }}
          <el-tag v-if="schema.page.storeMode === 'FREE'" type="success" size="mini">FREE 自由模式</el-tag>
          <el-tag v-else-if="schema.page.storeMode === 'MANAGED'" type="warning" size="mini">MANAGED 托管模式</el-tag>
        </h2>
        <div class="render-meta">
          <span v-if="schema.page.pageKey">标识：{{ schema.page.pageKey }}</span>
          <span v-if="schema.page.storeMode === 'MANAGED' && schema.page.entityName">｜ 实体：{{ schema.page.entityName }}</span>
          <span v-if="schema.page.remark">｜ {{ schema.page.remark }}</span>
        </div>
        <!-- 动作按钮区：confirm=1 的动作用警示色，其余主色 -->
        <div v-if="schema.actions && schema.actions.length" class="render-actions">
          <el-button v-for="a in schema.actions" :key="a.actionId" size="small"
                     :type="a.confirm === '1' ? 'danger' : 'primary'" plain
                     icon="el-icon-caret-right" :loading="actionLoading === a.actionId"
                     @click="handleAction(a)">{{ a.actionName }}</el-button>
        </div>
      </div>

      <!-- 托管模式：经实体处理器真实 CRUD（行键 = 字段 fieldKey） -->
      <template v-if="schema.page && schema.page.storeMode === 'MANAGED'">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="managedMultiple"
                       @click="handleDelete">删除</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="managedLoading" :data="managedRows" @selection-change="handleManagedSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="ID" align="center" prop="id" width="80" />
          <el-table-column v-for="col in columns" :key="'mcol-' + col.key" :label="col.label" align="center"
                           :min-width="col.type === 'TEXT' ? 200 : 120" show-overflow-tooltip>
            <template slot-scope="scope">
              <span>{{ formatCell(scope.row, col) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="managedTotal > 0" :total="managedTotal" :page.sync="managedQuery.pageNum"
                    :limit.sync="managedQuery.pageSize" @pagination="loadManagedList" />
        <el-empty v-if="!managedLoading && !managedRows.length" description="该托管实体暂无数据，点击「新增」创建" />
      </template>

      <!-- 自由模式：动态列表 + 记录 CRUD -->
      <template v-if="schema.page && schema.page.storeMode === 'FREE'">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple"
                       @click="handleDelete">删除</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="记录ID" align="center" prop="recordId" width="80" />
          <el-table-column v-for="col in columns" :key="'col-' + col.key" :label="col.label" align="center"
                           :min-width="col.type === 'TEXT' ? 200 : 120" show-overflow-tooltip>
            <template slot-scope="scope">
              <span>{{ formatCell(scope.row, col) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
          <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
                    :limit.sync="queryParams.pageSize" @pagination="getList" />
      </template>

      <!-- 共用对话框：字段定义驱动动态表单（FREE 无字段定义时直接编辑 dataJson） -->
      <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
        <el-alert v-if="!formFields.length && isFree" type="info" show-icon :closable="false"
                  title="该页面尚未配置字段，将直接编辑 dataJson 原始 JSON。" style="margin-bottom: 12px" />
        <el-alert v-if="!formFields.length && !isFree" type="warning" show-icon :closable="false"
                  title="托管页面必须配置字段定义才能写入数据。" style="margin-bottom: 12px" />
        <el-form ref="dataForm" :model="formData" :rules="activeRules" :label-width="formLabelWidth">
          <!-- confJson 单源渲染：有 layout 时由 render.js 解释 RuoYi 控件 conf -->
          <template v-if="hasLayout">
            <el-form-item v-for="f in confFields" :key="'conf-' + f.vModel" :label="f.label"
                          :prop="f.vModel" :rules="activeRules[f.vModel]">
              <FormRender :conf="f" v-model="formData[f.vModel]"
                          @change="onFieldChange(f, $event)" @click="onFieldClick(f)" />
            </el-form-item>
          </template>
          <!-- 旧字段兼容分支：无布局的遗留页面（后端首次拉取 schema 时一次性迁移为 layout，迁移完成前走此分支） -->
          <template v-else>
            <el-form-item v-for="f in formFields" :key="f.fieldKey" :label="f.fieldName"
                          :prop="f.fieldKey" :rules="fieldRule(f)">
              <el-input v-if="f.fieldType === 'STRING'" v-model="formData[f.fieldKey]"
                        :placeholder="f.defaultValue ? '默认：' + f.defaultValue : ''" />
              <el-input-number v-else-if="f.fieldType === 'NUMBER'" v-model="formData[f.fieldKey]"
                               :controls-position="'right'" style="width: 100%" />
              <el-switch v-else-if="f.fieldType === 'BOOLEAN'" v-model="formData[f.fieldKey]"
                         active-text="是" inactive-text="否" />
              <el-date-picker v-else-if="f.fieldType === 'DATE'" v-model="formData[f.fieldKey]"
                              type="datetime" value-format="yyyy-MM-dd HH:mm:ss"
                              placeholder="选择日期时间" style="width: 100%" />
              <el-input v-else-if="f.fieldType === 'TEXT'" v-model="formData[f.fieldKey]"
                        type="textarea" :rows="3" :placeholder="f.defaultValue ? '默认：' + f.defaultValue : ''" />
              <el-select v-else-if="f.fieldType === 'SELECT'" v-model="formData[f.fieldKey]"
                         :placeholder="f.defaultValue ? '默认：' + f.defaultValue : ''" style="width: 100%">
                <el-option v-for="o in optionList(f.options)" :key="o" :label="o" :value="o" />
              </el-select>
              <el-input v-else v-model="formData[f.fieldKey]" placeholder="请输入" />
            </el-form-item>
          </template>
          <el-form-item v-if="!formFields.length && !hasLayout && isFree" label="dataJson">
            <el-input v-model="rawJson" type="textarea" :rows="8"
                      placeholder="JSON 对象，如 {\"name\":\"张三\",\"age\":20}" />
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </el-dialog>

      <el-empty v-if="!loading && !schema.page" description="页面定义不存在或已被删除" />
    </div>
  </div>
</template>

<script>
import { schemaPage } from '@/api/custompanel'
import { listRecord, addRecord, editRecord, removeRecord, executeAction } from '@/api/custompanel'
import { managedList, managedAdd, managedUpdate, managedRemove } from '@/api/custompanel'
import FormRender from '@/utils/generator/render'
import {
  parseLayout, renderableFields, buildRules, buildFormModel, normalizeValue, toSubmitData, toParams
} from '@/utils/generator/adapter'
import { fireEvent } from '@/utils/panelEvent'

export default {
  name: 'CustomPanelRender',
  components: { FormRender },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 页面 ID（路由参数）
      pageId: undefined,
      // Schema：{ page, layout, fields, events, actions }
      schema: { page: null, layout: null, fields: [], events: [], actions: [] },
      // confJson 单源渲染状态（有 layout 时走 render.js，否则旧字段分支）
      hasLayout: false,
      rawFields: [],
      confFields: [],
      confRules: {},
      formConf: {},
      // onLoad 只触发一次，避免列表/托管双通道重复触发
      loadEventFired: false,
      // 动态列（字段定义 → 表格列）
      columns: [],
      // 自由模式记录表格数据
      recordList: [],
      // 选中数组（自由）
      ids: [],
      // 非单个禁用（自由）
      multiple: true,
      // 总条数（自由）
      total: 0,
      // 查询参数（自由）
      queryParams: { pageNum: 1, pageSize: 10 },
      // 托管模式数据
      managedRows: [],
      managedIds: [],
      managedMultiple: true,
      managedTotal: 0,
      managedQuery: { pageNum: 1, pageSize: 10 },
      managedLoading: false,
      // 弹出层
      open: false,
      title: '',
      // 动态表单模型（键 = fieldKey，两模式共用）
      formData: {},
      // 无字段定义时直接编辑原始 JSON（仅 FREE）
      rawJson: '',
      // 当前编辑的记录 ID（FREE）
      currentRecordId: undefined,
      // 当前编辑的托管数据 ID（MANAGED）
      currentManagedId: undefined,
      // 正在执行的动作 ID（按钮级 loading）
      actionLoading: undefined
    }
  },
  computed: {
    // 表单字段（全量，含不在列表展示的字段）
    formFields() {
      return this.schema.fields || []
    },
    formRules() {
      return {}
    },
    // 生效校验规则：confJson 分支由 required/regList 生成，旧分支沿用字段定义
    activeRules() {
      return this.hasLayout ? this.confRules : this.formRules
    },
    // 表单标签宽度取 formConf.labelWidth（RuoYi 默认值 100）
    formLabelWidth() {
      const w = this.formConf && this.formConf.labelWidth
      return w ? String(w) + 'px' : '120px'
    },
    // 事件条件 DSL 的字段白名单（当前页 fieldKey）
    allowedFields() {
      return (this.schema.fields || []).map(f => f.fieldKey)
    },
    // 是否自由模式（对话框 dataJson 分支判定）
    isFree() {
      return !this.schema.page || this.schema.page.storeMode === 'FREE'
    }
  },
  created() {
    this.pageId = Number(this.$route.params.pageId)
    this.loadSchema()
  },
  methods: {
    /** 加载页面 Schema（page + layout + fields + events + actions），按模式拉取数据 */
    loadSchema() {
      this.loading = true
      this.loadEventFired = false
      schemaPage(this.pageId).then(response => {
        this.schema = response.data || { page: null, layout: null, fields: [], events: [], actions: [] }
        this.prepareConf()
        this.buildColumns()
        if (this.schema.page && this.schema.page.storeMode === 'FREE') {
          this.getList()
        } else if (this.schema.page && this.schema.page.storeMode === 'MANAGED') {
          this.loadManagedList()
        } else {
          this.loading = false
          this.fireLoadEvent()
        }
      }).catch(() => {
        this.schema = { page: null, layout: null, fields: [], events: [], actions: [] }
        this.prepareConf()
        this.loading = false
      })
    },
    /** 解析 confJson 单源布局，准备 render.js 控件与校验规则 */
    prepareConf() {
      const parsed = parseLayout(this.schema.layout)
      this.hasLayout = !!this.schema.layout
      this.formConf = parsed ? parsed.formConf : {}
      this.rawFields = parsed ? parsed.fields : []
      this.confFields = renderableFields(this.rawFields)
      this.confRules = parsed ? buildRules(this.rawFields) : {}
    },
    /** 页面加载完成事件（onLoad）：列表数据就绪后触发一次 */
    fireLoadEvent() {
      if (this.loadEventFired || !this.schema.page) return
      this.loadEventFired = true
      fireEvent('onLoad', null, this.buildContext(), {
        events: this.schema.events,
        allowedFields: this.allowedFields
      })
    },
    /** 构建动态列表列：showInList='1' 字段（升序）；无展示字段时用全部字段 */
    buildColumns() {
      let shown = (this.schema.fields || []).filter(f => f.showInList === '1')
        .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      if (!shown.length) {
        shown = (this.schema.fields || []).slice().sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      }
      this.columns = shown.map(f => ({ key: f.fieldKey, label: f.fieldName, type: f.fieldType }))
    },
    /** 查询记录列表（FREE） */
    getList() {
      this.loading = true
      listRecord(Object.assign({}, this.queryParams, { pageId: this.pageId })).then(response => {
        this.recordList = (response.rows || []).map(r => {
          let data = {}
          try { data = r.dataJson ? JSON.parse(r.dataJson) : {} } catch (e) { data = {} }
          return Object.assign({}, r, { _data: data })
        })
        this.total = response.total
        if (!this.columns.length) this.buildColumns()
        this.loading = false
        this.fireLoadEvent()
      })
    },
    /** 托管数据列表（MANAGED）：经实体处理器，行键 = 字段 fieldKey */
    loadManagedList() {
      this.managedLoading = true
      managedList(this.schema.page.entityName, Object.assign({}, this.managedQuery, { pageId: this.pageId }))
        .then(response => {
          this.managedRows = response.rows || []
          this.managedTotal = response.total
          this.managedLoading = false
          this.fireLoadEvent()
        }).catch(() => {
          this.managedRows = []
          this.managedTotal = 0
          this.managedLoading = false
        })
    },
    /** 单元格格式化：布尔转是/否（FREE 行 _data / MANAGED 行平铺键） */
    formatCell(row, col) {
      const src = row._data || row
      const val = src[col.key]
      if (col.type === 'BOOLEAN') {
        return (val === true || val === 'true' || val === '1') ? '是' : '否'
      }
      if (val === undefined || val === null || val === '') return '-'
      return String(val)
    },
    /** SELECT 选项拆分 */
    optionList(options) {
      if (!options) return []
      return String(options).split(',').map(o => o.trim()).filter(Boolean)
    },
    /** 字段级校验规则 */
    fieldRule(f) {
      if (f.required === '1') {
        return [{ required: true, message: f.fieldName + '不能为空', trigger: 'blur' }]
      }
      return []
    },
    /** 返回页面管理 */
    goBack() {
      if (this.$route.query.from) {
        this.$router.push(this.$route.query.from)
      } else {
        this.$router.push('/custompanel-erp/page')
      }
    },
    // 多选框选中数据（FREE）
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.recordId)
      this.multiple = !selection.length
    },
    // 多选框选中数据（MANAGED）
    handleManagedSelectionChange(selection) {
      this.managedIds = selection.map(item => item.id)
      this.managedMultiple = !selection.length
    },
    /** 事件上下文：{ pageId, recordId, operator, ...formData, _row } */
    buildContext(row) {
      const ctx = {
        pageId: this.pageId,
        recordId: this.currentRecordId || this.currentManagedId ||
          (row && (row.recordId || row.id)) || undefined,
        operator: (this.$store && this.$store.getters && this.$store.getters.name) || ''
      }
      Object.keys(this.formData || {}).forEach(k => {
        if (!(k in ctx)) ctx[k] = this.formData[k]
      })
      ctx._row = row || null
      return ctx
    },
    /** conf 控件值变化 → onChange 事件（targetKey = vModel） */
    onFieldChange(field, value) {
      if (!field || !field.vModel) return
      fireEvent('onChange', field.vModel, this.buildContext(), {
        events: this.schema.events,
        allowedFields: this.allowedFields
      })
    },
    /** conf 控件点击 → onClick 事件（targetKey = vModel） */
    onFieldClick(field) {
      if (!field || !field.vModel) return
      fireEvent('onClick', field.vModel, this.buildContext(), {
        events: this.schema.events,
        allowedFields: this.allowedFields
      })
    },
    /** 动作按钮：onClick 事件 + 真实上下文注入（原先传 {}，后端 {fieldKey} 插值断链） */
    handleAction(a) {
      const run = () => {
        const ctx = this.buildContext()
        fireEvent('onClick', a.actionKey, ctx, {
          events: this.schema.events,
          allowedFields: this.allowedFields
        })
        this.actionLoading = a.actionId
        // 后端 ActionExecuteRequest 只认 params/recordId：把上下文收敛为插值参数
        executeAction(a.actionId, {
          params: toParams(ctx),
          pageId: ctx.pageId,
          recordId: ctx.recordId
        }).then(response => {
          this.actionLoading = undefined
          this.msgSuccess(response.msg || '动作已派发执行')
        }).catch(() => {
          // 失败消息由 request.js 统一弹出（后端 msg / HTTP 错误）
          this.actionLoading = undefined
        })
      }
      if (a.confirm === '1') {
        this.$confirm('确认执行动作「' + a.actionName + '」？', '动作确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(run).catch(() => {})
      } else {
        run()
      }
    },
    /** 新增按钮（按模式） */
    handleAdd() {
      this.resetFormData()
      this.open = true
      this.title = this.isFree ? '新增记录' : '新增数据'
    },
    /** 修改按钮（按模式） */
    handleUpdate(row) {
      fireEvent('onClick', 'edit', this.buildContext(row), {
        events: this.schema.events,
        allowedFields: this.allowedFields
      })
      this.resetFormData()
      if (!this.isFree) {
        this.currentManagedId = row.id
        this.fillForm(row)
        this.open = true
        this.title = '修改数据'
        return
      }
      this.currentRecordId = row.recordId
      this.fillForm(row._data || {})
      this.open = true
      this.title = '修改记录'
    },
    /** 行数据 → 表单模型回填（按字段类型转换） */
    fillForm(obj) {
      if (this.hasLayout) {
        this.rawFields.forEach(f => {
          if (f && f.vModel) this.formData[f.vModel] = normalizeValue(f, obj[f.vModel])
        })
        return
      }
      const fields = this.formFields
      if (fields.length) {
        for (const f of fields) {
          const v = obj[f.fieldKey]
          if (f.fieldType === 'NUMBER') {
            this.formData[f.fieldKey] = (v === undefined || v === null || v === '') ? undefined : Number(v)
          } else if (f.fieldType === 'BOOLEAN') {
            this.formData[f.fieldKey] = (v === true || v === 'true' || v === '1')
          } else {
            this.formData[f.fieldKey] = (v === undefined || v === null) ? '' : String(v)
          }
        }
      } else if (this.isFree) {
        this.rawJson = JSON.stringify(obj, null, 2)
      }
    },
    /** 表单数据重置 */
    resetFormData() {
      this.currentRecordId = undefined
      this.currentManagedId = undefined
      this.rawJson = ''
      if (this.hasLayout) {
        this.formData = buildFormModel(this.rawFields)
        return
      }
      const data = {}
      for (const f of this.formFields) {
        if (f.fieldType === 'NUMBER') {
          data[f.fieldKey] = undefined
        } else if (f.fieldType === 'BOOLEAN') {
          data[f.fieldKey] = false
        } else if (f.defaultValue !== undefined && f.defaultValue !== null && f.defaultValue !== '') {
          data[f.fieldKey] = f.fieldType === 'DATE' ? f.defaultValue : String(f.defaultValue)
        } else {
          data[f.fieldKey] = ''
        }
      }
      this.formData = data
    },
    // 取消按钮
    cancel() {
      this.open = false
    },
    /** 提交：校验通过 → beforeSubmit 事件 → 保存 → afterSubmit 事件 */
    submitForm() {
      if (this.formFields.length || this.hasLayout) {
        this.$refs.dataForm.validate(valid => {
          if (valid) this.runBeforeSubmit(() => this.doSubmit())
        })
        return
      }
      if (this.isFree) {
        // 无字段定义：校验原始 JSON
        try {
          const obj = JSON.parse(this.rawJson)
          if (typeof obj !== 'object' || obj === null || Array.isArray(obj)) {
            this.$message.error('dataJson 必须是 JSON 对象')
            return
          }
          this.runBeforeSubmit(() => this.doSubmit(obj))
        } catch (e) {
          this.$message.error('dataJson 不是合法 JSON：' + e.message)
        }
      }
    },
    /** beforeSubmit：仅显式 blocked=true 阻断，其余失败记日志继续 */
    runBeforeSubmit(next) {
      fireEvent('beforeSubmit', null, this.buildContext(), {
        events: this.schema.events,
        allowedFields: this.allowedFields
      }).then(result => {
        if (result.blocked) {
          this.$message.warning('提交被事件阻断')
          return
        }
        next()
      }).catch(() => next())
    },
    /** 实际保存：confJson 分支按控件类型归一，旧分支沿用字段定义 */
    doSubmit(rawObj) {
      if (this.hasLayout) {
        const data = toSubmitData(this.rawFields, this.formData)
        if (this.isFree) this.saveRecord(JSON.stringify(data))
        else this.saveManaged(data)
        return
      }
      if (rawObj) {
        this.saveRecord(JSON.stringify(rawObj))
        return
      }
      if (this.isFree) this.saveRecord(JSON.stringify(this.formData))
      else this.saveManaged(this.formData)
    },
    /** afterSubmit：保存成功后触发（非阻断） */
    fireAfterSubmit() {
      fireEvent('afterSubmit', null, this.buildContext(), {
        events: this.schema.events,
        allowedFields: this.allowedFields
      })
    },
    /** 保存记录（FREE 新增 / 修改） */
    saveRecord(dataJson) {
      const payload = { pageId: this.pageId, dataJson }
      if (this.currentRecordId !== undefined) {
        editRecord(this.currentRecordId, payload).then(() => {
          this.msgSuccess('修改成功')
          this.open = false
          this.fireAfterSubmit()
          this.getList()
        })
      } else {
        addRecord(payload).then(() => {
          this.msgSuccess('新增成功')
          this.open = false
          this.fireAfterSubmit()
          this.getList()
        })
      }
    },
    /** 保存托管数据（MANAGED 新增 / 修改）：表单值转字符串，键 = fieldKey */
    saveManaged(obj) {
      const data = {}
      for (const k in obj) {
        if (obj[k] !== undefined && obj[k] !== null && obj[k] !== '') {
          data[k] = String(obj[k])
        }
      }
      const payload = { pageId: this.pageId, data }
      if (this.currentManagedId !== undefined) {
        managedUpdate(this.schema.page.entityName, this.currentManagedId, payload).then(() => {
          this.msgSuccess('修改成功')
          this.open = false
          this.fireAfterSubmit()
          this.loadManagedList()
        })
      } else {
        managedAdd(this.schema.page.entityName, payload).then(() => {
          this.msgSuccess('新增成功')
          this.open = false
          this.fireAfterSubmit()
          this.loadManagedList()
        })
      }
    },
    /** 删除按钮（按模式） */
    handleDelete(row) {
      fireEvent('onClick', 'delete', this.buildContext(row), {
        events: this.schema.events,
        allowedFields: this.allowedFields
      })
      if (!this.isFree) {
        const ids = row.id || this.managedIds
        this.$confirm('确认删除选中的托管数据？', '删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          return managedRemove(this.schema.page.entityName, ids, { pageId: this.pageId })
        }).then(() => {
          this.loadManagedList()
          this.msgSuccess('删除成功')
        })
        return
      }
      const recordIds = row.recordId || this.ids
      this.$confirm('确认删除选中的记录数据？', '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return removeRecord(recordIds)
      }).then(() => {
        this.getList()
        this.msgSuccess('删除成功')
      })
    }
  }
}
</script>

<style scoped>
.render-header {
  margin-bottom: 16px;
}
.render-title {
  display: inline-block;
  margin: 0 0 0 12px;
  vertical-align: middle;
  font-size: 20px;
}
.render-meta {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}
.render-actions {
  margin-top: 12px;
}
</style>
