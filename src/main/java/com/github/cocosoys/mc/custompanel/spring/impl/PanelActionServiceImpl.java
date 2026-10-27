package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelAction;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.ActionDefVO;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelActionService;
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
 * 动作定义管理实现：CRUD、所属页面校验、类型合法性校验、按页面级联清理。
 *
 * <p>本批次只落地动作定义的元数据管理，执行引擎（命令派发 / HTTP 回调）后续接入。</p>
 */
@CustomLog
public class PanelActionServiceImpl implements IPanelActionService {

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId, String actionKey, String actionName) {
        List<PanelAction> all = DATA.select(PanelAction.class);
        List<PanelAction> filtered = new ArrayList<>();
        for (PanelAction a : all) {
            if (pageId != null && !pageId.equals(a.getPageId())) {
                continue;
            }
            if (!contains(a.getActionKey(), actionKey)) {
                continue;
            }
            if (!contains(a.getActionName(), actionName)) {
                continue;
            }
            filtered.add(a);
        }
        filtered.sort(Comparator.comparing(PanelAction::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PanelAction::getActionId, Comparator.nullsLast(Long::compareTo)));
        List<Object> rows = new ArrayList<>();
        for (PanelAction a : filtered) {
            rows.add(toVO(a));
        }
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(Long actionId) {
        PanelAction a = actionId == null ? null : DATA.get(PanelAction.class, actionId);
        if (a == null) {
            return AjaxResult.error(t("custompanel.action.not-found", "动作不存在"));
        }
        return AjaxResult.success(toVO(a));
    }

    @Override
    public AjaxResult add(PanelAction action, CredentialPresentation credential) {
        if (action == null) {
            return AjaxResult.error(t("custompanel.action.key-empty", "动作键不能为空"));
        }
        String key = action.getActionKey() == null ? "" : action.getActionKey().trim();
        String name = action.getActionName() == null ? "" : action.getActionName().trim();
        if (key.isEmpty()) {
            return AjaxResult.error(t("custompanel.action.key-empty", "动作键不能为空"));
        }
        if (name.isEmpty()) {
            return AjaxResult.error(t("custompanel.action.name-empty", "动作名称不能为空"));
        }
        String type = action.getActionType() == null || action.getActionType().trim().isEmpty()
                ? PanelAction.ActionType.COMMAND : action.getActionType().trim().toUpperCase();
        if (!PanelAction.ActionType.valid(type)) {
            return AjaxResult.error(t("custompanel.action.type-invalid", "动作类型非法（COMMAND/HTTP/INTERNAL）"));
        }
        if (action.getPageId() == null || DATA.get(PanelPage.class, action.getPageId()) == null) {
            return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
        }
        PanelAction a = new PanelAction();
        a.setPageId(action.getPageId());
        a.setActionKey(key);
        a.setActionName(name);
        a.setActionType(type);
        a.setActionTarget(action.getActionTarget() == null ? "" : action.getActionTarget());
        a.setConfirm(action.getConfirm() == null || action.getConfirm().isEmpty() ? "0" : action.getConfirm());
        a.setPermission(action.getPermission() == null ? "" : action.getPermission());
        a.setSortOrder(action.getSortOrder() == null ? 0 : action.getSortOrder());
        a.setRemark(action.getRemark() == null ? "" : action.getRemark());
        a.setCreateTime(new Date());
        a.setCreateBy(PanelAuth.currentPlayer(credential));
        DATA.insert(a);
        return AjaxResult.success(t("custompanel.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(Long actionId, PanelAction action, CredentialPresentation credential) {
        if (actionId == null) {
            return AjaxResult.error(t("custompanel.action.missing-actionId", "缺少 actionId"));
        }
        PanelAction a = DATA.get(PanelAction.class, actionId);
        if (a == null) {
            return AjaxResult.error(t("custompanel.action.not-found", "动作不存在"));
        }
        if (action.getActionKey() != null && !action.getActionKey().trim().isEmpty()) {
            a.setActionKey(action.getActionKey().trim());
        }
        if (action.getActionName() != null && !action.getActionName().trim().isEmpty()) {
            a.setActionName(action.getActionName().trim());
        }
        if (action.getActionType() != null && !action.getActionType().trim().isEmpty()) {
            String type = action.getActionType().trim().toUpperCase();
            if (!PanelAction.ActionType.valid(type)) {
                return AjaxResult.error(t("custompanel.action.type-invalid", "动作类型非法（COMMAND/HTTP/INTERNAL）"));
            }
            a.setActionType(type);
        }
        if (action.getPageId() != null) {
            if (DATA.get(PanelPage.class, action.getPageId()) == null) {
                return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
            }
            a.setPageId(action.getPageId());
        }
        if (action.getActionTarget() != null) {
            a.setActionTarget(action.getActionTarget());
        }
        if (action.getConfirm() != null && !action.getConfirm().isEmpty()) {
            a.setConfirm(action.getConfirm());
        }
        if (action.getPermission() != null) {
            a.setPermission(action.getPermission());
        }
        if (action.getSortOrder() != null) {
            a.setSortOrder(action.getSortOrder());
        }
        if (action.getRemark() != null) {
            a.setRemark(action.getRemark());
        }
        a.setUpdateTime(new Date());
        a.setUpdateBy(PanelAuth.currentPlayer(credential));
        DATA.updateById(a);
        return AjaxResult.success(t("custompanel.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String actionIds, CredentialPresentation credential) {
        if (actionIds == null || actionIds.trim().isEmpty()) {
            return AjaxResult.error(t("custompanel.action.missing-actionId", "缺少 actionId"));
        }
        for (String id : actionIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            Long aid = parseId(id);
            if (aid != null) {
                DATA.deleteById(PanelAction.class, aid);
            }
        }
        return AjaxResult.success(t("custompanel.common.delete-success", "删除成功"));
    }

    @Override
    public int removeByPageId(Long pageId) {
        if (pageId == null) {
            return 0;
        }
        int n = 0;
        for (PanelAction a : DATA.select(PanelAction.class)) {
            if (pageId.equals(a.getPageId()) && a.getActionId() != null) {
                DATA.deleteById(PanelAction.class, a.getActionId());
                n++;
            }
        }
        return n;
    }

    @Override
    public List<ActionDefVO> listByPageId(Long pageId) {
        if (pageId == null) {
            return new ArrayList<>();
        }
        List<ActionDefVO> rows = new ArrayList<>();
        for (PanelAction a : DATA.select(PanelAction.class)) {
            if (pageId.equals(a.getPageId())) {
                rows.add(toVO(a));
            }
        }
        rows.sort(Comparator.comparing(ActionDefVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ActionDefVO::getActionId, Comparator.nullsLast(Long::compareTo)));
        return rows;
    }

    // ===== 内部 =====

    /** 实体 → 展示 VO。 */
    private ActionDefVO toVO(PanelAction a) {
        ActionDefVO vo = new ActionDefVO();
        vo.setActionId(a.getActionId());
        vo.setPageId(a.getPageId());
        vo.setActionKey(a.getActionKey());
        vo.setActionName(a.getActionName());
        vo.setActionType(a.getActionType());
        vo.setActionTarget(a.getActionTarget());
        vo.setConfirm(a.getConfirm());
        vo.setPermission(a.getPermission());
        vo.setSortOrder(a.getSortOrder());
        vo.setRemark(a.getRemark());
        vo.setCreateTime(a.getCreateTime());
        vo.setUpdateTime(a.getUpdateTime());
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
