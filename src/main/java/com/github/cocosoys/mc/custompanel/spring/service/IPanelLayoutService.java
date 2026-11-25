package com.github.cocosoys.mc.custompanel.spring.service;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelLayout;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.PanelLayoutSaveRequest;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 页面设计器布局服务契约：confJson 单源的 CRUD、发布与旧数据迁移。
 */
public interface IPanelLayoutService {

    /** 按页面取当前生效布局（含一次性迁移），返回 LayoutVO。 */
    AjaxResult detail(Long pageId);

    /** 保存草稿（每次保存追加一个新 DRAFT 版本，版本递增）；字段数 / 大小按 config.yml 上限校验。 */
    AjaxResult save(PanelLayoutSaveRequest request, CredentialPresentation credential);

    /** 发布指定页面当前布局：旧版本置 DRAFT，最新版置 PUBLISHED。 */
    AjaxResult publish(Long pageId, CredentialPresentation credential);

    /** 按 layoutId 删除布局。 */
    AjaxResult remove(Long layoutId);

    /** 取页面当前 PUBLISHED 布局（不触发迁移），供 schema 聚合使用；无则 null。 */
    PanelLayout latestPublished(Long pageId);

    /** 旧数据幂等迁移：panel_field 有数据且本页无布局时生成 v1 PUBLISHED 布局。 */
    void ensureMigrated(Long pageId);

    /** 级联删除：清掉页面下全部布局版本，返回删除条数。 */
    int removeByPageId(Long pageId);
}
