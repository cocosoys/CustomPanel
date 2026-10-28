package com.github.cocosoys.mc.custompanel.spring.service;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.Map;

/**
 * 托管实体（MANAGED）数据通道服务（抽象契约）：实体注册表查询 + 路由到
 * {@link com.github.cocosoys.mc.custompanel.managed.ManagedEntityHandler}。
 *
 * <p>实现见 {@link com.github.cocosoys.mc.custompanel.spring.impl.ManagedEntityServiceImpl}。
 * 路由前强制校验：实体必须已注册，且目标页面存在、为 MANAGED 模式、
 * 页面绑定的 entityName 与请求实体名一致（封堵"任意字符串"缺口）。</p>
 */
public interface IManagedEntityService {

    /** 已注册托管实体名列表（驱动前端实体下拉）。 */
    AjaxResult entities(CredentialPresentation credential);

    /** 路由分页列表到实体处理器。 */
    TableDataInfo list(String entityName, Long pageId, Integer pageNum, Integer pageSize,
                       CredentialPresentation credential);

    /** 路由详情到实体处理器。 */
    AjaxResult detail(String entityName, Long pageId, Long id, CredentialPresentation credential);

    /** 路由新增到实体处理器。 */
    AjaxResult add(String entityName, Long pageId, Map<String, String> data, CredentialPresentation credential);

    /** 路由修改到实体处理器。 */
    AjaxResult update(String entityName, Long pageId, Long id, Map<String, String> data,
                      CredentialPresentation credential);

    /** 路由删除到实体处理器。 */
    AjaxResult remove(String entityName, Long pageId, String ids, CredentialPresentation credential);
}
