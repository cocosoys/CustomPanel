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
      <el-form-item label="字段键" prop="fieldKey">
        <el-input v-model="queryParams.fieldKey" placeholder="请输入字段键" clearable size="small"
                  style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="字段名称" prop="fieldName">
        <el-input v-model="queryParams.fieldName" placeholder="请输入字段名称" clearable size="small"
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

    <el-table v-loading="loading" :data="fieldList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="字段ID" align="center" prop="fieldId" width="80" />
      <el-table-column label="所属页面" align="center" prop="pageId" width="80" />
      <el-table-column label="字段键" align="center" prop="fieldKey" width="120" />
      <el-table-column label="字段名称" align="center" prop="fieldName" width="150" />
      <el-table-column label="类型" align="center" prop="fieldType" width="90">
        <template slot-scope="scope">
          <el-tag>{{ scope.row.fieldType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="必填" align="center" prop="required" width="70">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.required === '1'" type="danger">是</el-tag>
          <el-tag v-else type="info">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="列表展示" align="center" prop="showInList" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.showInList === '1'" type="success">是</el-tag>
          <el-tag v-else type="info">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
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
        <el-form-item label="字段键" prop="fieldKey">
          <el-input v-model="form.fieldKey" placeholder="英文字段名，如 price" />
        </el-form-item>
        <el-form-item label="字段名称" prop="fieldName">
          <el-input v-model="form.fieldName" placeholder="表头 / 表单标签，如 价格" />
        </el-form-item>
        <el-form-item label="字段类型" prop="fieldType">
          <el-select v-model="form.fieldType" style="width: 100%">
            <el-option label="单行文本 STRING" value="STRING" />
            <el-option label="数字 NUMBER" value="NUMBER" />
            <el-option label="布尔 BOOLEAN" value="BOOLEAN" />
            <el-option label="日期 DATE" value="DATE" />
            <el-option label="多行文本 TEXT" value="TEXT" />
            <el-option label="下拉选择 SELECT" value="SELECT" />
          </el-select>
        </el-form-item>
        <el-form-item label="必填" prop="required">
          <el-radio-group v-model="form.required">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="默认值" prop="defaultValue">
          <el-input v-model="form.defaultValue" placeholder="新增记录时的默认值（可空）" />
        </el-form-item>
        <el-form-item v-if="form.fieldType === 'SELECT'" label="下拉选项" prop="options">
          <el-input v-model="form.options" placeholder="逗号分隔，如 男,女,保密" />
        </el-form-item>
        <el-form-item label="列表展示" prop="showInList">
          <el-radio-group v-model="form.showInList">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否</el-radio>
          </el-radio-group>
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
import { listField, getField, addField, editField, removeField } from '@/api/custompanel'

export default {
  name: 'CustomPanelField',
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
      // 字段表格数据
      fieldList: [],
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
        fieldKey: undefined,
        fieldName: undefined
      },
      // 表单参数
      form: {
        fieldId: undefined,
        pageId: undefined,
        fieldKey: undefined,
        fieldName: undefined,
        fieldType: 'STRING',
        required: '0',
        defaultValue: undefined,
        options: undefined,
        showInList: '1',
        sortOrder: 0,
        remark: undefined
      },
      // 表单校验
      rules: {
        pageId: [{ required: true, message: '所属页面不能为空', trigger: 'change' }],
        fieldKey: [{ required: true, message: '字段键不能为空', trigger: 'blur' }],
        fieldName: [{ required: true, message: '字段名称不能为空', trigger: 'blur' }],
        fieldType: [{ required: true, message: '字段类型不能为空', trigger: 'change' }]
      }
    }
  },
  created() {
    this.loadPageOptions()
    this.getList()
  },
  methods: {
    /** 加载页面下拉（全量） */
    loadPageOptions() {
      listPage({ pageNum: 1, pageSize: 1000 }).then(response => {
        this.pageOptions = response.rows
      })
    },
    /** 查询字段列表 */
    getList() {
      this.loading = true
      listField(this.queryParams).then(response => {
        this.fieldList = response.rows
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
        fieldId: undefined,
        pageId: this.queryParams.pageId,
        fieldKey: undefined,
        fieldName: undefined,
        fieldType: 'STRING',
        required: '0',
        defaultValue: undefined,
        options: undefined,
        showInList: '1',
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
      this.ids = selection.map(item => item.fieldId)
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增字段'
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      getField(row.fieldId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改字段'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.fieldId !== undefined && this.form.fieldId !== null) {
            editField(this.form.fieldId, this.form).then(response => {
              this.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addField(this.form).then(response => {
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
      const fieldIds = row.fieldId || this.ids
      this.$confirm('确认删除选中的字段数据？', '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return removeField(fieldIds)
      }).then(() => {
        this.getList()
        this.msgSuccess('删除成功')
      })
    }
  }
}
</script>
