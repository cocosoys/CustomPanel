# CustomPanel

完全配置驱动的服务器控制面板，以 MCERP 模块形式运行在 Spigot 1.12.2 上，通过 SOYSHTTPOverMC 的
HTTP-over-MC 通道提供 REST API，菜单、路由与鉴权由 MCERP 中控统一提供。

## 仓库结构

| 路径 | 说明 |
| --- | --- |
| `src/main/java/com/github/cocosoys/mc/custompanel/` | 后端，按 `entity → vo → service → impl → controller` 分层 |
| `src/main/resources/` | `plugin.yml`、语言文件与 `dist/`（前端构建产物，随 jar 打包） |
| `web/` | 前端源码（RuoYi-Vue2 + ElementUI，wujie 子应用） |
| `scripts/` | 本地校验脚本 |

前端源码已由子模块并入主仓 `web/`，与后端共享同一套提交历史。旧子模块工作副本
`MCERP-Vue2-example/` 已删除；根 `.gitignore` 仍保留其忽略规则，避免历史残留被误提交。
需要回溯旧目录时，可从 `1aeba91` 之前的提交（如 `080a1e8`）检出。

## 环境要求

- JDK 8、Maven 3.6+
- 前端：Node ≥ 8.9、npm ≥ 3（建议使用 Node 16）
- 后端构建依赖兄弟工程预先构建的 `SOYSHTTPOverMC`、`MCERP`（`system` scope，见 `pom.xml`）

## 构建

### 后端

```powershell
mvn -o -DskipTests clean package
```

产物：`target/CustomPanel-<version>.jar`。

### 单元测试

```powershell
mvn -o test
```

测试为纯 JVM 单元测试（JUnit 4 + Mockito，`mockito-inline` 静态 mock `DATA` 门面），
不启动 Spigot，也不依赖服务器运行时。当前覆盖：

| 测试类 | 覆盖点 |
| --- | --- |
| `PanelFieldServiceImplTest` | 同页 `fieldKey` 唯一（忽略大小写）、改页后唯一性重校验 |
| `PanelRecordServiceImplTest` | FREE 模式不变量：拒绝向 MANAGED 页面写入 / 迁移记录 |
| `PanelActionPermissionTest` | 动作级权限三分支：放行、拒绝、权限服务不可用时降级 |

打包前建议先跑 `mvn -o test`；`mvn -o clean package` 默认也会执行测试。

### 前端

```powershell
cd web
npm install         # 首次
npm run build:prod  # 生成 web/dist
```

### 同步构建产物并校验

```powershell
robocopy web\dist src\main\resources\dist /MIR /NJH /NJS /NDL /NC /NS
powershell -ExecutionPolicy Bypass -File scripts\verify-web-dist.ps1
```

`/MIR` 会删除目标目录中源目录不存在的文件，确保 `src/main/resources/dist` 与构建产物完全一致。
`verify-web-dist.ps1` 逐文件比对 SHA-256，差异时以非 0 退出码报告，可用 `-Source`/`-Target`
指定其他目录。

**提交前必须**保证 `web/dist` 与 `src/main/resources/dist` 内容一致，避免 jar 内页面与源码版本漂移。

### 提交前强校验（Git hook）

仓库提供 `scripts/hooks/pre-commit`，在每次 `git commit` 前自动执行 `verify-web-dist.ps1`，
产物不一致时直接阻止提交。一次性启用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\install-git-hooks.ps1
```

该脚本等价于 `git config core.hooksPath scripts/hooks`（只影响本仓库，不写入全局配置）。

- `web/dist` 不存在（例如纯后端改动、未构建前端）时 hook 自动跳过，不会阻塞提交；
- 未安装 PowerShell 的环境同样跳过；
- 临时关闭：`git config --unset core.hooksPath`；重新启用再跑一次安装脚本。

如需在 CI 中替代本地 hook：只执行 `cd web && npm ci && npm run build:prod`，再运行
`scripts/verify-web-dist.ps1`（或直接用 `git diff --exit-code src/main/resources/dist` 判断漂移）。

## 部署

1. 将 `CustomPanel-<version>.jar` 放入服务端 `plugins/`；
2. 确保 `SOYSHTTPOverMC`、`MCERP` 已安装并启用（`plugin.yml` 中为硬依赖）；
3. 数据表 `panel_page`、`panel_field`、`panel_action`、`panel_record` 由 SOYS 数据层自动创建；
4. 权限节点共 26 个，前缀 `custompanel.*`，完整列表见 `src/main/resources/plugin.yml`。

## 配置

插件首次启用时自动释放 `config.yml` 到 `plugins/CustomPanel/config.yml`（源码在
`src/main/resources/config.yml`），修改后重启服务器或 `/reload confirm` 生效。

| 配置键 | 默认 | 说明 |
| --- | --- | --- |
| `demo-entity.enabled` | `true` | 是否注册内置演示托管实体 `demo-item`（复用 `PanelRecord` 存储，用于验证 MANAGED 链路）。生产环境建议改为 `false`，避免演示实体被业务页面误绑定写入。 |

关闭后 `/panel/managed` 的 `entities` 列表不再包含 `demo-item`，已绑定该实体的页面调用托管端点
会返回「托管实体未注册」。业务插件仍可通过
`CustomPanel.getInstance().registerManagedEntity(handler)` 注册自有托管实体（幂等，同名覆盖）。

## REST 端点

控制器通过 MCERP 注册，权限由 `@ApiPermission` 声明：

| 前缀 | 端点 |
| --- | --- |
| `/panel/page` | list / schema / detail / add / update / remove |
| `/panel/field` | list / detail / add / update / remove |
| `/panel/action` | list / detail / add / update / remove / execute |
| `/panel/record` | list / detail / add / update / remove |
| `/panel/managed` | entities / list / detail / add / update / remove |

## 前端壳层升级（重新同步 `web/`）

`web/` 是在 MCERP 的 RuoYi-Vue2 壳层基础上加入 `custompanel` 页面得到的。当壳层上游
（RuoYi-Vue2 模板 / MCERP 前端）升级时，按以下流程把新壳层同步进 `web/`，同时保留本插件的页面与接口：

1. **确认自有文件已提交**（升级时不能被壳层覆盖或删除）：
   - `web/src/views/custompanel/`（`page/`、`field/`、`action/`、`render/`）
   - `web/src/api/custompanel.js`
   - 壳层配置改动：`web/vue.config.js`、`web/.env.*`、`web/public/`（wujie 子应用相关）
2. **同步壳层**（`<壳层目录>` 为上游新版本解压目录，排除自有页面与接口）：
   ```powershell
   robocopy <壳层目录> web /MIR /XD web\src\views\custompanel /XF custompanel.js /NJH /NJS /NDL /NC /NS
   ```
   `/XD`、`/XF` 保证自有文件不被删除或覆盖；`node_modules`、`dist` 由 `web/.gitignore` 控制，不受影响。
3. **合并壳层配置**：用 `git diff -- web/vue.config.js web/.env.development web/.env.production web/public`
   复核上游改动，人工合入与 `custompanel` 相关的配置（页面挂载前缀、wujie 子应用参数、构建输出目录）。
4. **重新构建并同步产物**：
   ```powershell
   cd web
   npm install
   npm run build:prod
   cd ..
   robocopy web\dist src\main\resources\dist /MIR /NJH /NJS /NDL /NC /NS
   powershell -ExecutionPolicy Bypass -File scripts\verify-web-dist.ps1
   ```
5. **验证并单次提交**：先 `mvn -o test`，再启动服务端确认 MCERP 侧边栏入口、`custompanel` 各页面与
   `/api/plugins/CustomPanel/panel/*` 端点正常；最后把壳层、自有页面与 `dist` 放在**同一个提交**中。

壳层升级幅度较大时，可先在临时分支完成第 1–4 步并与 `master` 对比，确认没有自有代码丢失后再合并。

## 开发约定

- `web/node_modules/`、`web/dist/`、`web/package-lock.json` 由 `web/.gitignore` 忽略，不提交；
- 分支策略：`master` 为唯一长期分支，前后端改动放在同一个提交 / PR 中。
