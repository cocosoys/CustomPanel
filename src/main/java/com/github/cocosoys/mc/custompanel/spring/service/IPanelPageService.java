package com.github.cocosoys.mc.custompanel.spring.service;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 页面定义管理服务（抽象契约）：页面元数据的 CRUD。
 *
 * <p>实现见 {@link com.github.cocosoys.mc.custompanel.spring.impl.PanelPageServiceImpl}；
 * 校验、去重、审计字段填充等一致性编排由 impl 负责，Controller 不得介入。</p>
 */
public interface IPanelPageService {

    /**
     * 页面列表（分页 + 条件过滤）。
     *
     * @param pageNum   页码（从 1 起，可空）
     * @param pageSize  每页条数（可空）
     * @param pageKey   页面标识模糊匹配（可空）
     * @param pageName  页面名称模糊匹配（可空）
     * @param storeMode 存储模式精确匹配（可空）
     * @return 若依分页体 {code,msg,rows,total}
     */
    TableDataInfo list(Integer pageNum, Integer pageSize, String pageKey, String pageName, String storeMode);

    /**
     * 页面详情。
     *
     * @param pageId 页面 ID
     * @return 成功携带 {@code PageDefVO}；不存在返回错误体
     */
    AjaxResult detail(Long pageId);

    /**
     * 新增页面。
     *
     * @param page       页面定义（请求体）
     * @param credential 当前请求凭证（用于审计字段，可空）
     * @return 操作结果
     */
    AjaxResult add(PanelPage page, CredentialPresentation credential);

    /**
     * 修改页面（字段增量覆盖：非空才覆盖）。
     *
     * @param pageId     页面 ID
     * @param page       页面定义（请求体）
     * @param credential 当前请求凭证（用于审计字段，可空）
     * @return 操作结果
     */
    AjaxResult update(Long pageId, PanelPage page, CredentialPresentation credential);

    /**
     * 批量删除页面（逗号分隔 ID）。
     *
     * @param pageIds    页面 ID 串（如 {@code 1,2,3}）
     * @param credential 当前请求凭证（用于审计字段，可空）
     * @return 操作结果
     */
    AjaxResult remove(String pageIds, CredentialPresentation credential);

    /**
     * 页面定义聚合（Schema）：一次返回页面 + 字段 + 动作，供前端渲染器整页拉取。
     *
     * @param pageId 页面 ID
     * @return 成功携带 {@code PageSchemaVO}；页面不存在返回错误体
     */
    AjaxResult schema(Long pageId);
}
