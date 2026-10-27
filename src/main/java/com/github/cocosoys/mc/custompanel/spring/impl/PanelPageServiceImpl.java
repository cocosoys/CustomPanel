package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.PageDefVO;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.PageSchemaVO;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelActionService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelFieldService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelPageService;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelRecordService;
import com.github.cocosoys.mc.custompanel.util.PanelAuth;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 页面定义管理实现：CRUD、唯一性校验、审计字段填充。
 *
 * <p>全部业务逻辑集中在本层——Controller 只做参数绑定与权限声明。
 * 持久化经 SOYS {@link DATA} 门面（SQL / YAML 自动路由），本插件不实现存储层。</p>
 */
@CustomLog
public class PanelPageServiceImpl implements IPanelPageService {

    private final IPanelFieldService fieldService;
    private final IPanelActionService actionService;
    private final IPanelRecordService recordService;

    /**
     * @param fieldService  字段服务（删除页面时级联清理字段）
     * @param actionService 动作服务（删除页面时级联清理动作）
     * @param recordService 记录服务（删除页面时级联清理自由模式记录）
     */
    public PanelPageServiceImpl(IPanelFieldService fieldService,
                                IPanelActionService actionService,
                                IPanelRecordService recordService) {
        this.fieldService = fieldService;
        this.actionService = actionService;
        this.recordService = recordService;
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
        if (PanelPage.StoreMode.MANAGED.equals(p.getStoreMode())
                && (p.getEntityName() == null || p.getEntityName().trim().isEmpty())) {
            return AjaxResult.error(t("custompanel.page.managed-need-entity", "托管模式必须指定绑定实体"));
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
        for (String id : pageIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            Long pid = parseId(id);
            if (pid == null) {
                continue;
            }
            // 级联清理：页面下的字段、动作与自由模式记录先于页面本身移除
            fields += fieldService == null ? 0 : fieldService.removeByPageId(pid);
            actions += actionService == null ? 0 : actionService.removeByPageId(pid);
            records += recordService == null ? 0 : recordService.removeByPageId(pid);
            DATA.deleteById(PanelPage.class, pid);
        }
        log.infoT("custompanel.log.page-removed", "页面已删除 -> 字段 {0} 条，动作 {1} 条，记录 {2} 条",
                fields, actions, records);
        return AjaxResult.success(t("custompanel.common.delete-success", "删除成功"));
    }

    @Override
    public AjaxResult schema(Long pageId) {
        if (pageId == null || DATA.get(PanelPage.class, pageId) == null) {
            return AjaxResult.error(t("custompanel.schema.not-found", "页面定义不存在"));
        }
        PageSchemaVO vo = new PageSchemaVO();
        vo.setPage(toVO(DATA.get(PanelPage.class, pageId)));
        vo.setFields(fieldService == null ? new ArrayList<>() : fieldService.listByPageId(pageId));
        vo.setActions(actionService == null ? new ArrayList<>() : actionService.listByPageId(pageId));
        return AjaxResult.success(vo);
    }

    // ===== 内部 =====

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
