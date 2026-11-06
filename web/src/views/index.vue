<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="16" style="padding-left: 20px">
        <h2>自定义面板（CustomPanel）</h2>
        <p>
          欢迎回来，<b>{{ nickName || name }}</b>。这里是自定义面板的配置台，
          提供页面、字段与动作的统一配置，并按页面定义渲染数据表单。
        </p>
      </el-col>
      <el-col :sm="24" :lg="8">
        <el-card shadow="hover" style="margin-top: 20px">
          <div slot="header"><b>快捷入口</b></div>
          <div style="line-height: 2.4">
            <el-button type="primary" size="mini" plain icon="el-icon-document" @click="go('/custompanel/page')">页面管理</el-button>
            <el-button type="success" size="mini" plain icon="el-icon-s-grid" @click="go('/custompanel/field')">字段管理</el-button>
            <el-button type="warning" size="mini" plain icon="el-icon-s-operation" @click="go('/custompanel/action')">动作管理</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
export default {
  name: 'Index',
  computed: {
    ...mapGetters(['name', 'nickName'])
  },
  methods: {
    go(path) {
      // wujie 模式：通知主应用跳；直接访问：子应用自己 router.push
      if (window.__POWERED_BY_WUJIE__ && window.$wujie && window.$wujie.bus) {
        window.$wujie.bus.$emit('router-push', path)
      } else {
        this.$router.push(path)
      }
    }
  }
}
</script>
