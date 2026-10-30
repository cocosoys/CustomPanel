<template>
  <div class="app-container">
    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="所属页面" prop="pageId">
        <el-select v-model="queryParams.pageId" placeholder="全部页面" clearable filterable size="small"
                   style="width: 220px" @change="handleQuery">
          <el-option v-for="p in pageOptions" :key="p.pageId" :label="p.pageName + '（' + p.pageKey + '）'"
                     :value="p.pageId" />
        </el-select>
      </el-form-item>
      <el-form-item label="动作键" prop="actionKey">
        <el-input v-model="queryParams.actionKey" placeholder="请输入动作键" clearable size="small"
                  style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="动作名称" prop="actionName">
        <el-input v-model="queryParams.actionName" placeholder="请输入动作名称" clearable size="small"
                  style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple"
                   @click="handleDelete">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="actionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="动作ID" align="center" prop="actionId" width="80" />
      <el-table-column label="所属页面" align="center" prop="pageId" width="80" />
      <el-table-column label="动作键" align="center" prop="actionKey" width="120" />
      <el-table-column label="动作名称" align="center" prop="actionName" width="150" />
      <el-table-column label="类型" align="center" prop="actionType" width="100">
        <template slot-scope="scope">
          <el-tag :type="typeTag(scope.row.actionType)">{{ scope.row.actionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="动作目标" align="center" prop="actionTarget" show-overflow-tooltip />
      <el-table-column label="二次确认" align="center" prop="confirm" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.confirm === '1'" type="warning">是</el-tag>
          <el-tag v-else type="info">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="权限" align="center" prop="permission" width="180" show-overflow-tooltip />
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增 / 修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="所属页面" prop="pageId">
          <el-select v-model="form.pageId" placeholder="请选择所属页面" filterable style="width: 100%">
            <el-option v-for="p in pageOptions" :key="p.pageId" :label="p.pageName + '（' + p.pageKey + '）'"
                       :value="p.pageId" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作键" prop="actionKey">
          <el-input v-model="form.actionKey" placeholder="英文标识，如 reset" />
        </el-form-item>
        <el-form-item label="动作名称" prop="actionName">
          <el-input v-model="form.actionName" placeholder="按钮文案，如 重置库存" />
        </el-form-item>
        <el-form-item label="动作类型" prop="actionType">
          <el-select v-model="form.actionType" style="width: 100%">
            <el-option label="后台命令 COMMAND" value="COMMAND" />
            <el-option label="HTTP 回调 HTTP" value="HTTP" />
            <el-option label="内部处理器 INTERNAL" value="INTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作目标" prop="actionTarget">
          <el-input v-model="form.actionTarget"
                    placeholder="命令串 / URL / 内部处理器名（执行引擎后续批次接入）" />
        </el-form-item>
        <el-form-item label="二次确认" prop="confirm">
          <el-radio-group v-model="form.confirm">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="权限标识" prop="permission">
          <el-input v-model="form.permission" placeholder="触发所需权限（空 = 沿用页面权限）" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPage } from '@/api/custompanel'
import { listAction, getAction, addAction, editAction, removeAction } from '@/api/custompanel'

export default {
  name: 'CustomPanelAction',
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 动作表格数据
      actionList: [],
      // 页面下拉数据
      pageOptions: [],
      // 弹出层标题
      title: '',
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        pageId: undefined,
        actionKey: undefined,
        actionName: undefined
      },
      // 表单参数
      form: {
        actionId: undefined,
        pageId: undefined,
        actionKey: undefined,
        actionName: undefined,
        actionType: 'COMMAND',
        actionTarget: undefined,
        confirm: '0',
        permission: undefined,
        sortOrder: 0,
        remark: undefined
      },
      // 表单校验
      rules: {
        pageId: [{ required: true, message: '所属页面不能为空', trigger: 'change' }],
        actionKey: [{ required: true, message: '动作键不能为空', trigger: 'blur' }],
        actionName: [{ required: true, message: '动作名称不能为空', trigger: 'blur' }],
        actionType: [{ required: true, message: '动作类型不能为空', trigger: 'change' }],
        actionTarget: [{ required: true, message: '动作目标不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadPageOptions()
    this.getList()
  },
  methods: {
    /** 动作类型标签配色 */
    typeTag(type) {
      if (type === 'COMMAND') return 'success'
      if (type === 'HTTP') return 'warning'
      if (type === 'INTERNAL') return 'info'
      return ''
    },
    /** 加载页面下拉（全量） */
    loadPageOptions() {
      listPage({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.pageOptions = response.rows
      })
    },
    /** 查询动作列表 */
    getList() {
      this.loading = true
      listAction(this.queryParams).then(response => {
        this.actionList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        actionId: undefined,
        pageId: this.queryParams.pageId,
        actionKey: undefined,
        actionName: undefined,
        actionType: 'COMMAND',
        actionTarget: undefined,
        confirm: '0',
        permission: undefined,
        sortOrder: 0,
        remark: undefined
      }
      this.resetForm('form')
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.actionId)
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增动作'
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      getAction(row.actionId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改动作'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.actionId !== undefined && this.form.actionId !== null) {
            editAction(this.form.actionId, this.form).then(response => {
              this.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addAction(this.form).then(response => {
              this.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const actionIds = row.actionId || this.ids
      this.$confirm('确认删除选中的动作数据？', '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return removeAction(actionIds)
      }).then(() => {
        this.getList()
        this.msgSuccess('删除成功')
      })
    }
  }
}
</script>
