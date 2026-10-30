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

前端源码已由子模块并入主仓 `web/`，与后端共享同一套提交历史；旧子模块工作副本
`MCERP-Vue2-example/` 已被根 `.gitignore` 忽略，可安全删除。

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

## 部署

1. 将 `CustomPanel-<version>.jar` 放入服务端 `plugins/`；
2. 确保 `SOYSHTTPOverMC`、`MCERP` 已安装并启用（`plugin.yml` 中为硬依赖）；
3. 数据表 `panel_page`、`panel_field`、`panel_action`、`panel_record` 由 SOYS 数据层自动创建；
4. 权限节点共 26 个，前缀 `custompanel.*`，完整列表见 `src/main/resources/plugin.yml`。

## REST 端点

控制器通过 MCERP 注册，权限由 `@ApiPermission` 声明：

| 前缀 | 端点 |
| --- | --- |
| `/panel/page` | list / schema / detail / add / update / remove |
| `/panel/field` | list / detail / add / update / remove |
| `/panel/action` | list / detail / add / update / remove / execute |
| `/panel/record` | list / detail / add / update / remove |
| `/panel/managed` | entities / list / detail / add / update / remove |

## 开发约定

- `web/node_modules/`、`web/dist/`、`web/package-lock.json` 由 `web/.gitignore` 忽略，不提交；
- 分支策略：`master` 为唯一长期分支，前后端改动放在同一个提交 / PR 中。
