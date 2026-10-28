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
 * <p><b>页面前缀覆写</b>：页面登记以 owner 插件名为键（{@code /web/plugins/CustomPanel}），
 * 而 {@code pageRoot()} 默认取 identifier（小写 {@code custompanel}）——二者大小写不同会导致
 * 菜单 component 指向的地址 404，故此处覆写为 owner 插件名（与 SOYSMonthlyCard 同款处理）；
 * dist 直接托管在插件页面根，无 identifier 子目录，{@code pageDirRoot()} 同步返回根路径。</p>
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
        // 托管实体注册表：内置演示实体（demo-item，复用 PanelRecord 存储验证链路）；
        // 业务插件经 CustomPanel.getInstance().registerManagedEntity(handler) 注册
        this.managedRegistry = new ManagedEntityRegistry();
        this.managedRegistry.register(new DemoItemHandler());
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

    @Override
    public String getIdentifier() {
        return "custompanel";
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

    /** dist 直接托管在插件页面根，无 identifier 子目录。 */
    @Override
    public String pageDirRoot() {
        return pageRoot();
    }

    /** 无子菜单时的默认页。 */
    @Override
    protected String homeUrl() {
        return pageRoot() + "/index.html";
    }

    /**
     * 菜单树：自定义面板（目录）→ 页面管理 / 字段管理 / 动作管理。
     * menu 绑定各自 Controller，按钮由 {@code ControllerPermScanner}
     * 从 {@code @ApiName}/{@code @ApiPermission} 自动投影，无需手写。
     */
    @Override
    protected ErpMenus menus() {
        return ErpMenus.create()
                .dir("custompanel", "自定义面板", "dashboard", d -> d
                        .menu("page", "页面管理", "build", PanelPageController.class).component("/page")
                        .menu("field", "字段管理", "list", PanelFieldController.class).component("/field")
                        .menu("action", "动作管理", "flash-on", PanelActionController.class).component("/action"));
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
