package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.CustomPanel;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelAction;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelEvent;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.EventVO;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelEventService;
import com.github.cocosoys.mc.custompanel.util.PanelAuth;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 页面事件绑定服务实现：CRUD + 按页面取启用事件（供 schema 注入）。
 *
 * <p><b>安全边界</b>：{@code trigger} 五类白名单；{@code inlineType} 仅 HTTP / INTERNAL，
 * COMMAND 不在此列——命令必须绑定已有 action（{@code actionId} 必须指向真实存在的记录）。
 * 内联命令模板长度由 config.yml {@code designer.max-inline-target-bytes} 约束，代码仅兜底缺省值。</p>
 *
 * <p>内联优先：{@code inlineType} 非空走内联，否则走 {@code actionId}；二者皆空拒绝落库，
 * 避免产生永远不会触发的死事件。</p>
 *
 * <p>条件 DSL 在服务端只做 JSON 合法性校验；字段白名单 / 求值在前端事件分发器执行
 * （不引入 eval，见方案 §6.2）。</p>
 */
@CustomLog
public class PanelEventServiceImpl implements IPanelEventService {

    /** config.yml 未配置时的兜底值（4 KiB） */
    private static final int DEFAULT_MAX_INLINE_TARGET_BYTES = 4096;

    private final CustomPanel plugin;

    public PanelEventServiceImpl(CustomPanel plugin) {
        this.plugin = plugin;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId, String trigger) {
        List<PanelEvent> filtered = new ArrayList<>();
        for (PanelEvent e : DATA.select(PanelEvent.class)) {
            if (pageId != null && !pageId.equals(e.getPageId())) {
                continue;
            }
            if (trigger != null && !trigger.isEmpty() && !trigger.equals(e.getTrigger())) {
                continue;
            }
            filtered.add(e);
        }
        filtered.sort(Comparator.comparing(PanelEvent::getPriority, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PanelEvent::getEventId, Comparator.nullsLast(Long::compareTo)));
        List<Object> rows = new ArrayList<>();
        for (PanelEvent e : filtered) {
            rows.add(toVO(e));
        }
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(Long eventId) {
        PanelEvent e = eventId == null ? null : DATA.get(PanelEvent.class, eventId);
        if (e == null) {
            return AjaxResult.error(t("custompanel.event.not-found", "事件不存在"));
        }
        return AjaxResult.success(toVO(e));
    }

    @Override
    public AjaxResult add(PanelEvent event, CredentialPresentation credential) {
        AjaxResult invalid = validate(event);
        if (invalid != null) {
            return invalid;
        }
        PanelEvent e = new PanelEvent();
        copyEditable(event, e);
        e.setEnabled(normalizeEnabled(event.getEnabled()));
        e.setCreateTime(new Date());
        e.setCreateBy(PanelAuth.currentPlayer(credential));
        DATA.insert(e);
        log.infoT("custompanel.log.event-saved", "事件已新增 -> pageId {0}, trigger {1}, targetKey {2}",
                e.getPageId(), e.getTrigger(), e.getTargetKey());
        return AjaxResult.success(t("custompanel.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(Long eventId, PanelEvent event, CredentialPresentation credential) {
        if (eventId == null) {
            return AjaxResult.error(t("custompanel.event.missing-eventId", "缺少 eventId"));
        }
        PanelEvent current = DATA.get(PanelEvent.class, eventId);
        if (current == null) {
            return AjaxResult.error(t("custompanel.event.not-found", "事件不存在"));
        }
        // 非空覆盖合并：请求未携带的字段沿用原记录，避免前端局部更新误清空
        PanelEvent merged = merge(current, event);
        AjaxResult invalid = validate(merged);
        if (invalid != null) {
            return invalid;
        }
        copyEditable(merged, current);
        current.setEnabled(normalizeEnabled(merged.getEnabled()));
        current.setUpdateTime(new Date());
        current.setUpdateBy(PanelAuth.currentPlayer(credential));
        DATA.updateById(current);
        log.infoT("custompanel.log.event-saved", "事件已修改 -> eventId {0}, trigger {1}, targetKey {2}",
                eventId, current.getTrigger(), current.getTargetKey());
        return AjaxResult.success(t("custompanel.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String eventIds) {
        if (eventIds == null || eventIds.trim().isEmpty()) {
            return AjaxResult.error(t("custompanel.event.missing-eventId", "缺少 eventId"));
        }
        for (String id : eventIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            Long eid = parseId(id);
            if (eid != null) {
                DATA.deleteById(PanelEvent.class, eid);
            }
        }
        return AjaxResult.success(t("custompanel.common.delete-success", "删除成功"));
    }

    @Override
    public List<PanelEvent> listByPage(Long pageId) {
        List<PanelEvent> rows = new ArrayList<>();
        if (pageId == null) {
            return rows;
        }
        for (PanelEvent e : DATA.select(PanelEvent.class)) {
            if (pageId.equals(e.getPageId()) && isEnabled(e)) {
                rows.add(e);
            }
        }
        rows.sort(Comparator.comparing(PanelEvent::getPriority, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PanelEvent::getEventId, Comparator.nullsLast(Long::compareTo)));
        return rows;
    }

    @Override
    public int removeByPageId(Long pageId) {
        if (pageId == null) {
            return 0;
        }
        int n = 0;
        for (PanelEvent e : DATA.select(PanelEvent.class)) {
            if (pageId.equals(e.getPageId()) && e.getEventId() != null) {
                DATA.deleteById(PanelEvent.class, e.getEventId());
                n++;
            }
        }
        return n;
    }

    // ===== 内部 =====

    /** 落库前强校验；返回 null 表示通过。 */
    private AjaxResult validate(PanelEvent e) {
        if (e == null) {
            return AjaxResult.error(t("custompanel.event.empty", "事件内容不能为空"));
        }
        if (e.getPageId() == null || DATA.get(PanelPage.class, e.getPageId()) == null) {
            return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
        }
        if (e.getTrigger() == null || !PanelEvent.Trigger.valid(e.getTrigger())) {
            return AjaxResult.error(t("custompanel.event.trigger-invalid",
                    "触发时机非法（onLoad/onChange/onClick/beforeSubmit/afterSubmit）"));
        }
        String inlineType = trimToNull(e.getInlineType());
        String inlineTarget = e.getInlineTarget();
        boolean hasInlineTarget = inlineTarget != null && !inlineTarget.trim().isEmpty();
        if (inlineType != null && !PanelEvent.InlineType.valid(inlineType)) {
            return AjaxResult.error(t("custompanel.event.inline-type-invalid", "内联类型非法（仅 HTTP / INTERNAL）"));
        }
        if (hasInlineTarget && inlineType == null) {
            return AjaxResult.error(t("custompanel.event.inline-type-missing", "填写内联命令时必须选择内联类型"));
        }
        if (inlineType != null && !hasInlineTarget) {
            return AjaxResult.error(t("custompanel.event.inline-target-empty", "内联类型已选，内联命令不能为空"));
        }
        if (hasInlineTarget) {
            int bytes = inlineTarget.getBytes(StandardCharsets.UTF_8).length;
            int max = maxInlineTargetBytes();
            if (bytes > max) {
                return AjaxResult.error(t("custompanel.event.inline-target-too-large",
                        "内联命令超限（上限 {0} 字节，当前 {1}）", max, bytes));
            }
        }
        if (e.getActionId() != null && DATA.get(PanelAction.class, e.getActionId()) == null) {
            return AjaxResult.error(t("custompanel.event.action-not-found", "绑定的动作不存在"));
        }
        if (e.getActionId() == null && inlineType == null) {
            return AjaxResult.error(t("custompanel.event.no-action", "事件必须绑定动作或填写内联命令"));
        }
        String condition = trimToNull(e.getConditionJson());
        if (condition != null) {
            try {
                JsonReader.parseObject(condition);
            } catch (Exception ex) {
                return AjaxResult.error(t("custompanel.event.condition-invalid", "条件 JSON 格式非法"));
            }
        }
        return null;
    }

    /** 合并：请求字段非空才覆盖（trigger / enabled 同此规则）。 */
    private static PanelEvent merge(PanelEvent current, PanelEvent patch) {
        PanelEvent out = new PanelEvent();
        copyEditable(current, out);
        if (patch != null) {
            if (patch.getPageId() != null) {
                out.setPageId(patch.getPageId());
            }
            if (trimToNull(patch.getName()) != null) {
                out.setName(patch.getName());
            }
            if (trimToNull(patch.getTrigger()) != null) {
                out.setTrigger(patch.getTrigger());
            }
            if (trimToNull(patch.getTargetKey()) != null) {
                out.setTargetKey(patch.getTargetKey());
            }
            if (trimToNull(patch.getConditionJson()) != null) {
                out.setConditionJson(patch.getConditionJson());
            }
            if (patch.getActionId() != null) {
                out.setActionId(patch.getActionId());
            }
            if (trimToNull(patch.getInlineType()) != null) {
                out.setInlineType(patch.getInlineType());
            }
            if (patch.getInlineTarget() != null) {
                out.setInlineTarget(patch.getInlineTarget());
            }
            if (patch.getPriority() != null) {
                out.setPriority(patch.getPriority());
            }
            if (trimToNull(patch.getEnabled()) != null) {
                out.setEnabled(patch.getEnabled());
            }
        }
        return out;
    }

    /** 仅复制允许落库的可编辑字段（审计 / 主键由调用方处理）。 */
    private static void copyEditable(PanelEvent src, PanelEvent dst) {
        dst.setPageId(src.getPageId());
        dst.setName(src.getName() == null ? "" : src.getName().trim());
        dst.setTrigger(src.getTrigger());
        dst.setTargetKey(trimToNull(src.getTargetKey()));
        dst.setConditionJson(trimToNull(src.getConditionJson()));
        dst.setActionId(src.getActionId());
        dst.setInlineType(trimToNull(src.getInlineType()));
        dst.setInlineTarget(src.getInlineTarget());
        dst.setPriority(src.getPriority() == null ? 0 : src.getPriority());
        dst.setEnabled(normalizeEnabled(src.getEnabled()));
    }

    /** enabled 归一：仅显式 "0" 为禁用，空值默认启用。 */
    private static String normalizeEnabled(String enabled) {
        return "0".equals(enabled) ? "0" : "1";
    }

    private static boolean isEnabled(PanelEvent e) {
        return !"0".equals(e.getEnabled());
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private int maxInlineTargetBytes() {
        return plugin == null ? DEFAULT_MAX_INLINE_TARGET_BYTES
                : plugin.getConfig().getInt("designer.max-inline-target-bytes", DEFAULT_MAX_INLINE_TARGET_BYTES);
    }

    private static EventVO toVO(PanelEvent e) {
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
        return vo;
    }

    private static Long parseId(String id) {
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
