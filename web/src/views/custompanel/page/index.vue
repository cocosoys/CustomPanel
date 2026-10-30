<template>
  <div class="app-container">
    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="页面标识" prop="pageKey">
        <el-input v-model="queryParams.pageKey" placeholder="请输入页面标识" clearable size="small"
                  style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="页面名称" prop="pageName">
        <el-input v-model="queryParams.pageName" placeholder="请输入页面名称" clearable size="small"
                  style="width: 180px" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="存储模式" prop="storeMode">
        <el-select v-model="queryParams.storeMode" placeholder="全部" clearable size="small" style="width: 130px">
          <el-option label="自由模式 FREE" value="FREE" />
          <el-option label="托管模式 MANAGED" value="MANAGED" />
        </el-select>
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

    <el-table v-loading="loading" :data="pageList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="页面ID" align="center" prop="pageId" width="80" />
      <el-table-column label="页面标识" align="center" prop="pageKey" width="120" />
      <el-table-column label="页面名称" align="center" prop="pageName" width="150" />
      <el-table-column label="图标" align="center" prop="icon" width="90">
        <template slot-scope="scope">
          <svg-icon v-if="scope.row.icon" :icon-class="scope.row.icon" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="存储模式" align="center" prop="storeMode" width="140">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.storeMode === 'FREE'" type="success">FREE 自由</el-tag>
          <el-tag v-else-if="scope.row.storeMode === 'MANAGED'" type="warning">MANAGED 托管</el-tag>
          <span v-else>{{ scope.row.storeMode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="绑定实体" align="center" prop="entityName" width="160" show-overflow-tooltip />
      <el-table-column label="排序" align="center" prop="orderNum" width="70" />
      <el-table-column label="显示" align="center" prop="visible" width="70">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.visible === '0'" type="success">显示</el-tag>
          <el-tag v-else type="info">隐藏</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleOpen(scope.row)">打开</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增 / 修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="页面标识" prop="pageKey">
          <el-input v-model="form.pageKey" placeholder="英文字母键，如 shop" />
        </el-form-item>
        <el-form-item label="页面名称" prop="pageName">
          <el-input v-model="form.pageName" placeholder="侧边栏显示名，如 商店管理" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="Element 图标名，如 goods（可空）" />
        </el-form-item>
        <el-form-item label="存储模式" prop="storeMode">
          <el-radio-group v-model="form.storeMode">
            <el-radio label="FREE">自由模式（通用行存储）</el-radio>
            <el-radio label="MANAGED">托管模式（绑定实体）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.storeMode === 'MANAGED'" label="绑定实体" prop="entityName">
          <el-select v-model="form.entityName" placeholder="选择已注册托管实体（MANAGED 必填）"
                     style="width: 100%" :loading="entitiesLoading">
            <el-option v-for="e in managedEntities" :key="e" :label="e" :value="e" />
          </el-select>
          <div v-if="!managedEntities.length" class="el-form-item__error"
               style="padding-top: 4px; line-height: 1.4">
            暂无已注册托管实体：业务插件需实现 ManagedEntityHandler 并调用 CustomPanel.getInstance().registerManagedEntity(handler)
          </div>
        </el-form-item>
        <el-form-item label="排序号" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="显示状态" prop="visible">
          <el-radio-group v-model="form.visible">
            <el-radio label="0">显示</el-radio>
            <el-radio label="1">隐藏</el-radio>
          </el-radio-group>
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
import { listPage, getPage, addPage, editPage, removePage, listManagedEntities } from '@/api/custompanel'

export default {
  name: 'CustomPanelPage',
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
      // 页面表格数据
      pageList: [],
      // 弹出层标题
      title: '',
      // 是否显示弹出层
      open: false,
      // 已注册托管实体（MANAGED 绑定下拉）
      managedEntities: [],
      // 实体下拉加载中
      entitiesLoading: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        pageKey: undefined,
        pageName: undefined,
        storeMode: undefined
      },
      // 表单参数
      form: {
        pageId: undefined,
        pageKey: undefined,
        pageName: undefined,
        icon: undefined,
        storeMode: 'FREE',
        entityName: undefined,
        orderNum: 0,
        visible: '0',
        remark: undefined
      },
      // 表单校验
      rules: {
        pageKey: [{ required: true, message: '页面标识不能为空', trigger: 'blur' }],
        pageName: [{ required: true, message: '页面名称不能为空', trigger: 'blur' }],
        storeMode: [{ required: true, message: '存储模式不能为空', trigger: 'change' }],
        entityName: [{
          validator: (rule, value, callback) => {
            if (this.form.storeMode === 'MANAGED' && (!value || !value.trim())) {
              callback(new Error('托管模式必须选择绑定实体'))
            } else {
              callback()
            }
          },
          trigger: 'change'
        }]
      }
    }
  },
  created() {
    this.getList()
    this.loadManagedEntities()
  },
  methods: {
    /** 加载已注册托管实体（MANAGED 绑定下拉） */
    loadManagedEntities() {
      this.entitiesLoading = true
      listManagedEntities().then(response => {
        this.managedEntities = response.data || []
        this.entitiesLoading = false
      }).catch(() => {
        this.managedEntities = []
        this.entitiesLoading = false
      })
    },
    /** 查询页面列表 */
    getList() {
      this.loading = true
      listPage(this.queryParams).then(response => {
        this.pageList = response.rows
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
        pageId: undefined,
        pageKey: undefined,
        pageName: undefined,
        icon: undefined,
        storeMode: 'FREE',
        entityName: undefined,
        orderNum: 0,
        visible: '0',
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
      this.ids = selection.map(item => item.pageId)
      this.multiple = !selection.length
    },
    /** 打开渲染器：跳转 /view/:pageId */
    handleOpen(row) {
      this.$router.push({ path: '/view/' + row.pageId })
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增页面'
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      getPage(row.pageId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改页面'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.pageId !== undefined && this.form.pageId !== null) {
            editPage(this.form.pageId, this.form).then(response => {
              this.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addPage(this.form).then(response => {
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
      const pageIds = row.pageId || this.ids
      this.$confirm('确认删除选中的页面数据？删除后其下字段、动作与自由模式记录将一并清理。', '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return removePage(pageIds)
      }).then(() => {
        this.getList()
        this.msgSuccess('删除成功')
      })
    }
  }
}
</script>
