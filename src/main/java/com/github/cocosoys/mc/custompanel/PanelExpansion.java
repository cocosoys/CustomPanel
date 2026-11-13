package com.github.cocosoys.mc.custompanel;

import com.github.cocosoys.mc.custompanel.managed.ManagedEntityHandler;
import com.github.cocosoys.mc.custompanel.managed.ManagedEntityRegistry;
import com.github.cocosoys.mc.custompanel.managed.impl.DemoItemHandler;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelActionController;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelFieldController;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelManagedController;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelPageController;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelRecordController;
import com.github.cocosoys.mc.custompanel.spring.impl.ManagedEntityServiceImpl;
import com.github.cocosoys.mc.custompanel.spring.impl.PanelActionServiceImpl;
import com.github.cocosoys.mc.custompanel.spring.impl.PanelFieldServiceImpl;
import com.github.cocosoys.mc.custompanel.spring.impl.PanelPageServiceImpl;
import com.github.cocosoys.mc.custompanel.spring.impl.PanelRecordServiceImpl;
import com.github.cocosoys.mc.custompanel.spring.service.IManagedEntityService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelActionService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelFieldService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelPageService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelRecordService;
import com.github.cocosoys.mc.mcerp.ErpMenus;
import com.github.cocosoys.mc.mcerp.McerpExpansion;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysHttpOverMcApi;
import lombok.CustomLog;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

/**
 * CustomPanel 的 MCERP 模块扩展：本插件接入 ERP 控制台的唯一入口。
 *
 * <p>继承 {@link McerpExpansion} 后，一次 {@code register()} 自动完成：</p>
 * <ul>
 *   <li>端点登记：{@code /api/plugins/CustomPanel/panel/page/*}（由 {@link #buildControllers()} 提供实例）；</li>
 *   <li>页面托管：{@code resourceRoot()="dist"} → {@code /web/plugins/CustomPanel/*}
 *       （磁盘 {@code plugins/CustomPanel/dist} 优先，回退 jar 内同名目录）；</li>
 *   <li>菜单与路由合成：{@link #menus()} 声明的树登记进 MCERP {@code ErpRegistry}，即安即生效；</li>
 *   <li>失败回滚：任一环节失败由骨架统一回滚，卸载时按 {@code expansion:<identifier>} tag 精确摘除。</li>
 * </ul>
 *
 * <p><b>三处前缀的分工</b>（改名 {@code CustomPanel-ERP} 后必须同时对齐，缺一处即 404）：</p>
 * <ul>
 *   <li><b>ERP 模块标识</b>：{@code CustomPanel-ERP}（{@link #getIdentifier()}）——
 *       插件名 + {@code -ERP} 后缀，与 SOYSGiftLoft / ihomepages / SOYSOceanBox 等兄弟模块
 *       的 ERP 侧扩张同规则；</li>
 *   <li><b>页面 URL 前缀</b>：{@code /web/plugins/CustomPanel}——页面托管以 owner 插件名为键，
 *       故 {@link #pageRoot()} 覆写为插件名（与 SOYSMonthlyCard 同款处理）；
 *       若交给默认实现会得到 {@code /web/plugins/CustomPanel-ERP}，该命名空间无任何插件伺服；</li>
 *   <li><b>子应用目录段</b>：{@code custompanel-erp}——骨架默认段由
 *       {@code NameSafe.path(identifier)} 求得，横线会被丢弃得到 {@code custompanelerp}，
 *       与前端路由 {@code /custompanel-erp/*} 不符，故 {@link #pageDirRoot()} 显式覆写。</li>
 * </ul>
 *
 * <p>菜单 component 由此拼接为 {@code /web/plugins/CustomPanel/custompanel-erp/<menuId>}，
 * 主应用 {@code WujieLink} 取插件前缀后的子路径 {@code /custompanel-erp/<menuId>}
 * 作为 {@code props.path} 下发给子应用。注意主应用侧边栏模块 route 的 path 由 MCERP
 * {@code safeName()} 生成（去横线 {@code /custompanelerp}），与本处子应用前缀不同源，
 * 二者互不影响。</p>
 */
@CustomLog
public class PanelExpansion extends McerpExpansion {

    private final CustomPanel plugin;
    private final IPanelPageService pageService;
    private final IPanelFieldService fieldService;
    private final IPanelActionService actionService;
    private final IPanelRecordService recordService;
    private final ManagedEntityRegistry managedRegistry;
    private final IManagedEntityService managedService;

    public PanelExpansion(CustomPanel plugin) {
        this.plugin = plugin;
        this.fieldService = new PanelFieldServiceImpl();
        this.actionService = new PanelActionServiceImpl(plugin);
        this.recordService = new PanelRecordServiceImpl();
        // 托管实体注册表：演示实体（demo-item）由 config.yml 控制开关，默认开启；
        // 业务插件经 CustomPanel.getInstance().registerManagedEntity(handler) 注册
        this.managedRegistry = new ManagedEntityRegistry();
        if (plugin.getConfig().getBoolean("demo-entity.enabled", true)) {
            this.managedRegistry.register(new DemoItemHandler());
        }
        this.managedService = new ManagedEntityServiceImpl(managedRegistry);
        // 删除页面需级联清理其字段、动作与记录 → 页面服务反向依赖另三个服务
        this.pageService = new PanelPageServiceImpl(fieldService, actionService, recordService, managedRegistry);
    }

    /** 业务插件注册托管实体（幂等，同名覆盖）；返回是否生效。 */
    public boolean registerManagedEntity(ManagedEntityHandler handler) {
        if (handler == null) {
            return false;
        }
        managedRegistry.register(handler);
        return true;
    }

    /** 托管实体注册表（供读取已注册实体）。 */
    public ManagedEntityRegistry getManagedEntityRegistry() {
        return managedRegistry;
    }

    /**
     * ERP 模块标识：{@code <插件名>-ERP}，与插件名 {@code CustomPanel} 显式区分。
     *
     * <p>与兄弟模块 ERP 侧扩张同规则：{@code SOYSGiftLoft-ERP} / {@code ihomepages-erp} /
     * {@code soysoceanbox-erp} / {@code soysmonthlycard-erp}。</p>
     */
    @Override
    public String getIdentifier() {
        return "CustomPanel-ERP";
    }

    /** 子应用目录段：保留横线——{@code NameSafe.path()} 会丢弃横线得到 {@code custompanelerp}。 */
    private static final String ERP_DIR_SEGMENT = "custompanel-erp";

    /**
     * 子应用路由前缀：{@code pageRoot() + "/custompanel-erp"}。
     *
     * <p>骨架默认实现用 {@code NameSafe.path(identifier)} 生成段名，横线被丢弃后为
     * {@code custompanelerp}，菜单 component 会变成
     * {@code /web/plugins/CustomPanel/custompanelerp/page}，与前端 {@code wujieRoutes}
     * 的 {@code /custompanel-erp/*} 不一致，wujie 必然 404，故此处显式覆写。</p>
     */
    @Override
    public String pageDirRoot() {
        return pageRoot() + "/" + ERP_DIR_SEGMENT;
    }

    @Override
    public String getAuthor() {
        return "cocosoys";
    }

    @Override
    public String getVersion() {
        return plugin == null ? "" : plugin.getDescription().getVersion();
    }

    /** ERP 侧边栏显示名。 */
    @Override
    protected String displayName() {
        return "自定义面板";
    }

    /** 侧边栏图标。 */
    @Override
    protected String icon() {
        return "dashboard";
    }

    /** 排序号（排在 MCERP 内置模块之后）。 */
    @Override
    protected int sortOrder() {
        return 100;
    }

    /** wujie 微前端子应用（需前端 history 路由 + 绝对 publicPath + 空 Layout）。 */
    @Override
    protected boolean wujie() {
        return true;
    }

    /** 前端页面资源根（骨架自动托管，磁盘优先）。 */
    @Override
    protected String resourceRoot() {
        return "dist";
    }

    /** 页面 URL 前缀：覆盖为 owner 插件名，与页面托管路径一致。 */
    @Override
    public String pageRoot() {
        Plugin owner = getOwner();
        String name = owner == null ? (plugin == null ? getIdentifier() : plugin.getName()) : owner.getName();
        SoysHttpOverMcApi api = getApi();
        String prefix = api == null ? null : api.getToolkit().pageFullPrefix(name);
        return prefix == null || prefix.isEmpty() ? "/web/plugins/" + name : prefix;
    }

    /** 无子菜单时的默认页。 */
    @Override
    protected String homeUrl() {
        return pageRoot() + "/index.html";
    }

    /**
     * 菜单树：模块本身即侧边栏顶级菜单（{@link #displayName()}="自定义面板"），
     * 此处只声明其下的叶子菜单，不再额外套一层同名目录（避免"自定义面板 → 自定义面板"重复层级）。
     * <p>component 不手写——{@code ErpMenus} 自动拼接
     * {@code pageDirRoot()+"/"+menuId}（{@code /web/plugins/CustomPanel/custompanel-erp/page|field|action}），
     * 主应用取插件前缀后的子路径下发 {@code props.path}/{@code router-push}
     * （{@code /custompanel-erp/page|field|action}），与前端路由严格对齐。
     * menu 绑定各自 Controller，按钮由 {@code ControllerPermScanner}
     * 从 {@code @ApiName}/{@code @ApiPermission} 自动投影，无需手写。
     */
    @Override
    protected ErpMenus menus() {
        return ErpMenus.create()
                .menu("page", "页面管理", "build", PanelPageController.class)
                // 记录 CRUD 与托管实体数据都是「页面」这一层的业务能力：
                // 记录按 pageId 归属页面、托管实体由页面渲染时读取，
                // 故其按钮权限统一挂在「页面管理」菜单下（permsFrom 作用于最近一次 menu）。
                .permsFrom(PanelRecordController.class, PanelManagedController.class)
                .menu("field", "字段管理", "list", PanelFieldController.class)
                .menu("action", "动作管理", "flash-on", PanelActionController.class);
    }

    /** 待登记端点（本类不写端点）。 */
    @Override
    protected List<Object> buildControllers() {
        return Arrays.asList(
                new PanelPageController(pageService),
                new PanelFieldController(fieldService),
                new PanelActionController(actionService),
                new PanelRecordController(recordService),
                new PanelManagedController(managedService));
    }
}
