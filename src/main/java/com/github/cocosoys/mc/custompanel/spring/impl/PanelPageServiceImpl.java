package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.managed.ManagedEntityRegistry;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelEvent;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelField;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelLayout;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.EventVO;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.FieldDefVO;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.LayoutVO;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.PageDefVO;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.PageSchemaVO;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelActionService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelEventService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelLayoutService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelPageService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelRecordService;
import com.github.cocosoys.mc.custompanel.util.PanelAuth;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 页面定义管理实现：CRUD、唯一性校验、审计字段填充、托管实体绑定校验。
 *
 * <p>全部业务逻辑集中在本层——Controller 只做参数绑定与权限声明。
 * 持久化经 SOYS {@link DATA} 门面（SQL / YAML 自动路由），本插件不实现存储层。</p>
 */
@CustomLog
public class PanelPageServiceImpl implements IPanelPageService {

    private final IPanelActionService actionService;
    private final IPanelRecordService recordService;
    private final IPanelLayoutService layoutService;
    private final IPanelEventService eventService;
    private final ManagedEntityRegistry managedRegistry;

    /**
     * @param actionService   动作服务（删除页面时级联清理动作）
     * @param recordService   记录服务（删除页面时级联清理自由模式记录）
     * @param layoutService   布局服务（schema 单源装配 + 删除页面时级联清理）
     * @param eventService    事件服务（schema 装配 + 删除页面时级联清理）
     * @param managedRegistry 托管实体注册表（MANAGED 页面 entityName 必须已注册才放行）
     */
    public PanelPageServiceImpl(IPanelActionService actionService,
                                IPanelRecordService recordService,
                                IPanelLayoutService layoutService,
                                IPanelEventService eventService,
                                ManagedEntityRegistry managedRegistry) {
        this.actionService = actionService;
        this.recordService = recordService;
        this.layoutService = layoutService;
        this.eventService = eventService;
        this.managedRegistry = managedRegistry;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String pageKey, String pageName, String storeMode) {
        List<PanelPage> all = DATA.select(PanelPage.class);
        List<PanelPage> filtered = new ArrayList<>();
        for (PanelPage p : all) {
            if (!contains(p.getPageKey(), pageKey)) {
                continue;
            }
            if (!contains(p.getPageName(), pageName)) {
                continue;
            }
            if (storeMode != null && !storeMode.isEmpty() && !storeMode.equals(p.getStoreMode())) {
                continue;
            }
            filtered.add(p);
        }
        filtered.sort(Comparator.comparing(PanelPage::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PanelPage::getPageId, Comparator.nullsLast(Long::compareTo)));
        List<Object> rows = new ArrayList<>();
        for (PanelPage p : filtered) {
            rows.add(toVO(p));
        }
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(Long pageId) {
        PanelPage p = pageId == null ? null : DATA.get(PanelPage.class, pageId);
        if (p == null) {
            return AjaxResult.error(t("custompanel.page.not-found", "页面不存在"));
        }
        return AjaxResult.success(toVO(p));
    }

    @Override
    public AjaxResult add(PanelPage page, CredentialPresentation credential) {
        if (page == null) {
            return AjaxResult.error(t("custompanel.page.key-empty", "页面标识不能为空"));
        }
        String key = page.getPageKey() == null ? "" : page.getPageKey().trim();
        String name = page.getPageName() == null ? "" : page.getPageName().trim();
        if (key.isEmpty()) {
            return AjaxResult.error(t("custompanel.page.key-empty", "页面标识不能为空"));
        }
        if (name.isEmpty()) {
            return AjaxResult.error(t("custompanel.page.name-empty", "页面名称不能为空"));
        }
        String mode = page.getStoreMode() == null || page.getStoreMode().trim().isEmpty()
                ? PanelPage.StoreMode.FREE : page.getStoreMode().trim().toUpperCase();
        if (!PanelPage.StoreMode.valid(mode)) {
            return AjaxResult.error(t("custompanel.page.store-mode-invalid", "存储模式非法（仅支持 MANAGED / FREE）"));
        }
        if (PanelPage.StoreMode.MANAGED.equals(mode)
                && (page.getEntityName() == null || page.getEntityName().trim().isEmpty())) {
            return AjaxResult.error(t("custompanel.page.managed-need-entity", "托管模式必须指定绑定实体"));
        }
        if (PanelPage.StoreMode.MANAGED.equals(mode)
                && !isManagedEntityRegistered(page.getEntityName().trim())) {
            return AjaxResult.error(t("custompanel.page.managed-entity-unregistered",
                    "托管实体未注册：{0}", page.getEntityName().trim()));
        }
        if (findByKey(key) != null) {
            return AjaxResult.error(t("custompanel.page.key-exists", "页面标识已存在"));
        }
        PanelPage p = new PanelPage();
        p.setPageKey(key);
        p.setPageName(name);
        p.setIcon(page.getIcon() == null ? "" : page.getIcon());
        p.setStoreMode(mode);
        p.setEntityName(PanelPage.StoreMode.MANAGED.equals(mode) ? page.getEntityName().trim() : "");
        p.setOrderNum(page.getOrderNum() == null ? 0 : page.getOrderNum());
        p.setVisible(page.getVisible() == null || page.getVisible().isEmpty() ? "0" : page.getVisible());
        p.setRemark(page.getRemark() == null ? "" : page.getRemark());
        p.setCreateTime(new Date());
        p.setCreateBy(PanelAuth.currentPlayer(credential));
        DATA.insert(p);
        log.infoT("custompanel.log.page-added", "页面已新增 -> {0}", key);
        return AjaxResult.success(t("custompanel.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(Long pageId, PanelPage page, CredentialPresentation credential) {
        if (pageId == null) {
            return AjaxResult.error(t("custompanel.page.missing-pageId", "缺少 pageId"));
        }
        PanelPage p = DATA.get(PanelPage.class, pageId);
        if (p == null) {
            return AjaxResult.error(t("custompanel.page.not-found", "页面不存在"));
        }
        if (page.getPageKey() != null && !page.getPageKey().trim().isEmpty()
                && !page.getPageKey().trim().equals(p.getPageKey())) {
            PanelPage dup = findByKey(page.getPageKey().trim());
            if (dup != null && !pageId.equals(dup.getPageId())) {
                return AjaxResult.error(t("custompanel.page.key-exists", "页面标识已存在"));
            }
            p.setPageKey(page.getPageKey().trim());
        }
        if (page.getPageName() != null && !page.getPageName().trim().isEmpty()) {
            p.setPageName(page.getPageName().trim());
        }
        if (page.getIcon() != null) {
            p.setIcon(page.getIcon());
        }
        if (page.getStoreMode() != null && !page.getStoreMode().trim().isEmpty()) {
            String mode = page.getStoreMode().trim().toUpperCase();
            if (!PanelPage.StoreMode.valid(mode)) {
                return AjaxResult.error(t("custompanel.page.store-mode-invalid", "存储模式非法（仅支持 MANAGED / FREE）"));
            }
            p.setStoreMode(mode);
        }
        if (page.getEntityName() != null) {
            p.setEntityName(page.getEntityName());
        }
        String finalMode = p.getStoreMode();
        String finalEntity = p.getEntityName() == null ? "" : p.getEntityName().trim();
        if (PanelPage.StoreMode.MANAGED.equals(finalMode)) {
            if (finalEntity.isEmpty()) {
                return AjaxResult.error(t("custompanel.page.managed-need-entity", "托管模式必须指定绑定实体"));
            }
            if (!isManagedEntityRegistered(finalEntity)) {
                return AjaxResult.error(t("custompanel.page.managed-entity-unregistered",
                        "托管实体未注册：{0}", finalEntity));
            }
        } else {
            // FREE 模式：托管实体名无意义，一律清空（防止残留脏绑定）
            p.setEntityName("");
        }
        if (page.getOrderNum() != null) {
            p.setOrderNum(page.getOrderNum());
        }
        if (page.getVisible() != null && !page.getVisible().isEmpty()) {
            p.setVisible(page.getVisible());
        }
        if (page.getRemark() != null) {
            p.setRemark(page.getRemark());
        }
        p.setUpdateTime(new Date());
        p.setUpdateBy(PanelAuth.currentPlayer(credential));
        DATA.updateById(p);
        return AjaxResult.success(t("custompanel.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String pageIds, CredentialPresentation credential) {
        if (pageIds == null || pageIds.trim().isEmpty()) {
            return AjaxResult.error(t("custompanel.page.missing-pageId", "缺少 pageId"));
        }
        int fields = 0;
        int actions = 0;
        int records = 0;
        int layouts = 0;
        int events = 0;
        for (String id : pageIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            Long pid = parseId(id);
            if (pid == null) {
                continue;
            }
            // 级联清理：页面下的布局、事件、动作与自由模式记录先于页面本身移除。
            // 旧 panel_field 表已停写（P1 起单源），此处直接内联兜底清理迁移前遗留记录，
            // 不再依赖已删除的 fieldService。
            layouts += layoutService == null ? 0 : layoutService.removeByPageId(pid);
            events += eventService == null ? 0 : eventService.removeByPageId(pid);
            actions += actionService == null ? 0 : actionService.removeByPageId(pid);
            records += recordService == null ? 0 : recordService.removeByPageId(pid);
            fields += removeLegacyFields(pid);
            DATA.deleteById(PanelPage.class, pid);
        }
        log.infoT("custompanel.log.page-removed",
                "页面已删除 -> 布局 {0} 条，事件 {1} 条，动作 {2} 条，记录 {3} 条，旧字段 {4} 条",
                layouts, events, actions, records, fields);
        return AjaxResult.success(t("custompanel.common.delete-success", "删除成功"));
    }

    @Override
    public AjaxResult schema(Long pageId) {
        // 原实现两次 DATA.get(PanelPage.class, pageId)：一次判空、一次装配，这里收敛为一次
        PanelPage page = pageId == null ? null : DATA.get(PanelPage.class, pageId);
        if (page == null) {
            return AjaxResult.error(t("custompanel.schema.not-found", "页面定义不存在"));
        }
        // 单源：先触发幂等迁移（旧 panel_field → v1 PUBLISHED 布局），再读生效布局
        if (layoutService != null) {
            layoutService.ensureMigrated(pageId);
        }
        PanelLayout layout = layoutService == null ? null : layoutService.latestPublished(pageId);
        PageSchemaVO vo = new PageSchemaVO();
        vo.setPage(toVO(page));
        vo.setLayout(layout == null ? null : toLayoutVO(layout));
        vo.setFields(layout == null ? new ArrayList<>() : fieldsFromConf(pageId, layout.getFieldsJson()));
        vo.setEvents(eventService == null ? new ArrayList<>() : toEventVOs(eventService.listByPage(pageId)));
        vo.setActions(actionService == null ? new ArrayList<>() : actionService.listByPageId(pageId));
        return AjaxResult.success(vo);
    }

    // ===== 内部 =====

    /** 兜底清理迁移前遗留的旧 panel_field 记录（P4 起 fieldService 已删除，直接内联）。 */
    private static int removeLegacyFields(Long pageId) {
        int n = 0;
        for (PanelField f : DATA.select(PanelField.class)) {
            if (pageId.equals(f.getPageId()) && f.getFieldId() != null) {
                DATA.deleteById(PanelField.class, f.getFieldId());
                n++;
            }
        }
        return n;
    }

    /** RuoYi 控件 conf → FieldDefVO：schema 的 fields 由 layout.fieldsJson 派生（单源）。 */
    private static List<FieldDefVO> fieldsFromConf(Long pageId, String fieldsJson) {
        List<FieldDefVO> out = new ArrayList<>();
        if (fieldsJson == null || fieldsJson.trim().isEmpty()) {
            return out;
        }
        try {
            List<Object> arr = JsonReader.parseArray(fieldsJson);
            if (arr == null) {
                return out;
            }
            for (Object item : arr) {
                Map<String, Object> conf = asMap(item);
                if (conf == null) {
                    continue;
                }
                Object vModel = conf.get("vModel");
                if (vModel == null) {
                    continue;
                }
                FieldDefVO f = new FieldDefVO();
                f.setPageId(pageId);
                f.setFieldKey(String.valueOf(vModel));
                Object label = conf.get("label");
                f.setFieldName(label == null ? String.valueOf(vModel) : String.valueOf(label));
                f.setFieldType(fieldTypeOf(conf));
                f.setRequired(flagOf(conf.get("required")));
                Object def = conf.get("defaultValue");
                f.setDefaultValue(def == null ? "" : String.valueOf(def));
                f.setOptions(optionsOf(conf.get("options")));
                f.setShowInList(flagOf(conf.get("showInList")));
                f.setSortOrder(intOf(conf.get("sortOrder"), out.size()));
                out.add(f);
            }
        } catch (Exception ignored) {
            // 脏 JSON 不阻断整个 schema：按空字段降级，设计器重新保存即可修复
        }
        out.sort(Comparator.comparing(FieldDefVO::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
        return out;
    }

    /** 控件 tag → 旧字段类型（保持 FieldDefVO 既有契约，供旧渲染器与分析工具读取）。 */
    private static String fieldTypeOf(Map<String, Object> conf) {
        String tag = conf.get("tag") == null ? "" : String.valueOf(conf.get("tag"));
        if ("el-input-number".equals(tag)) {
            return PanelField.FieldType.NUMBER;
        }
        if ("el-switch".equals(tag)) {
            return PanelField.FieldType.BOOLEAN;
        }
        if ("el-date-picker".equals(tag)) {
            return PanelField.FieldType.DATE;
        }
        if ("el-select".equals(tag)) {
            return PanelField.FieldType.SELECT;
        }
        if ("textarea".equals(String.valueOf(conf.get("type")))) {
            return PanelField.FieldType.TEXT;
        }
        return PanelField.FieldType.STRING;
    }

    /** 选项数组 [{label,value}] → 旧 options 字符串（逗号分隔 value）。 */
    private static String optionsOf(Object options) {
        if (!(options instanceof List)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object o : (List<?>) options) {
            Map<String, Object> m = asMap(o);
            if (m == null) {
                continue;
            }
            Object value = m.get("value");
            if (value == null) {
                value = m.get("label");
            }
            if (value == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private static String flagOf(Object v) {
        if (v instanceof Boolean) {
            return ((Boolean) v) ? "1" : "0";
        }
        if (v instanceof Number) {
            return ((Number) v).intValue() != 0 ? "1" : "0";
        }
        return "1".equals(String.valueOf(v)) ? "1" : "0";
    }

    private static int intOf(Object v, int fallback) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ex) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    private static LayoutVO toLayoutVO(PanelLayout l) {
        LayoutVO vo = new LayoutVO();
        vo.setLayoutId(l.getLayoutId());
        vo.setPageId(l.getPageId());
        vo.setFormConfJson(l.getFormConfJson());
        vo.setFieldsJson(l.getFieldsJson());
        vo.setVersion(l.getVersion());
        vo.setStatus(l.getStatus());
        return vo;
    }

    private static List<EventVO> toEventVOs(List<PanelEvent> events) {
        List<EventVO> out = new ArrayList<>();
        if (events == null) {
            return out;
        }
        for (PanelEvent e : events) {
            EventVO vo = new EventVO();
            vo.setEventId(e.getEventId());
            vo.setPageId(e.getPageId());
            vo.setName(e.getName());
            vo.setTrigger(e.getTrigger());
            vo.setTargetKey(e.getTargetKey());
            vo.setConditionJson(e.getConditionJson());
            vo.setActionId(e.getActionId());
            vo.setInlineType(e.getInlineType());
            vo.setInlineTarget(e.getInlineTarget());
            vo.setPriority(e.getPriority());
            vo.setEnabled(e.getEnabled());
            out.add(vo);
        }
        return out;
    }

    /** 托管实体是否已注册（注册表查重，封堵 MANAGED 任意字符串缺口）。 */
    private boolean isManagedEntityRegistered(String entityName) {
        return managedRegistry != null && managedRegistry.contains(entityName);
    }

    /** 按页面标识精确查找（忽略大小写）；不存在返回 null。 */
    private PanelPage findByKey(String key) {
        for (PanelPage p : DATA.select(PanelPage.class)) {
            if (p.getPageKey() != null && p.getPageKey().equalsIgnoreCase(key)) {
                return p;
            }
        }
        return null;
    }

    /** 实体 → 展示 VO（Controller 禁止 Map，统一走 VO 投影）。 */
    private PageDefVO toVO(PanelPage p) {
        PageDefVO vo = new PageDefVO();
        vo.setPageId(p.getPageId());
        vo.setPageKey(p.getPageKey());
        vo.setPageName(p.getPageName());
        vo.setIcon(p.getIcon());
        vo.setStoreMode(p.getStoreMode());
        vo.setEntityName(p.getEntityName());
        vo.setOrderNum(p.getOrderNum());
        vo.setVisible(p.getVisible());
        vo.setRemark(p.getRemark());
        vo.setCreateTime(p.getCreateTime());
        vo.setUpdateTime(p.getUpdateTime());
        return vo;
    }

    private static boolean contains(String source, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    private static Long parseId(String id) {
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
