package com.github.cocosoys.mc.custompanel.spring.service;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelEvent;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.List;

/**
 * 页面事件绑定服务契约：CRUD + 按页面取启用事件（供 schema 注入）。
 */
public interface IPanelEventService {

    /** 分页列表（按 pageId / trigger 过滤）。 */
    TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId, String trigger);

    /** 事件详情。 */
    AjaxResult detail(Long eventId);

    /** 新增事件（校验 trigger / inlineType 合法性、内联上限）。 */
    AjaxResult add(PanelEvent event, CredentialPresentation credential);

    /** 修改事件。 */
    AjaxResult update(Long eventId, PanelEvent event, CredentialPresentation credential);

    /** 批量删除（逗号分隔 eventId）。 */
    AjaxResult remove(String eventIds);

    /** 按页面取启用事件（enabled=1），供 schema 下发；无则空列表。 */
    List<PanelEvent> listByPage(Long pageId);

    /** 级联删除：清掉页面下全部事件，返回删除条数。 */
    int removeByPageId(Long pageId);
}
