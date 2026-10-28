package com.github.cocosoys.mc.custompanel.managed;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.Map;

/**
 * MANAGED（托管模式）实体处理器 SPI：供业务插件把"编译期实体"绑定到自定义面板页面。
 *
 * <p>页面定义 {@code storeMode=MANAGED} 且 {@code entityName} 与 {@link #entityName()}
 * 精确相等时，CustomPanel 把该页面的数据操作路由到本接口实现。实现方决定存储
 * （ORM 真实列 / 自有表均可），CustomPanel 只负责路由、鉴权与基于字段定义的表单渲染。</p>
 *
 * <p><b>数据契约</b>（渲染器与实现方之间的约定）：
 * <ul>
 *   <li>{@link #list} 返回行的键必须与该页面字段定义（PanelField）的 {@code fieldKey}
 *       一一对应，渲染器据此展示列；建议额外携带 {@code id}（主键）供 detail/update/remove
 *       使用；</li>
 *   <li>{@link #detail} 返回对象与 list 行同构；</li>
 *   <li>{@link #add} / {@link #update} 的 data 键 = 字段 fieldKey，值为字符串
 *       （渲染器动态表单产出，布尔/数字以字符串表达）；</li>
 *   <li>{@link #remove} 的 ids 为逗号分隔主键串（与其他 remove 端点一致）。</li>
 * </ul></p>
 *
 * <p>注册：实现类在插件启用时调用
 * {@code CustomPanel.getInstance().registerManagedEntity(handler)}（幂等，同名覆盖）。</p>
 */
public interface ManagedEntityHandler {

    /**
     * 实体注册名（与页面定义 entityName 精确匹配，大小写敏感）。
     *
     * @return 非空注册名
     */
    String entityName();

    /**
     * 分页列表。
     *
     * @param credential 调用方凭证（实现方可按需二次鉴权）
     * @param pageId     当前页面 ID（恒为调用页，供实现方过滤）
     * @param pageNum    页码（从 1 起，可空）
     * @param pageSize   每页条数（可空）
     * @return 若依分页体 {code,msg,rows,total}；rows 行键 = 字段 fieldKey，含 id
     */
    TableDataInfo list(CredentialPresentation credential, Long pageId, Integer pageNum, Integer pageSize);

    /**
     * 详情。
     *
     * @return AjaxResult 成功时 data 为行对象（键 = 字段 fieldKey，含 id）
     */
    AjaxResult detail(CredentialPresentation credential, Long pageId, Long id);

    /**
     * 新增。
     *
     * @param data 键 = 字段 fieldKey（字符串）
     * @return AjaxResult
     */
    AjaxResult add(CredentialPresentation credential, Long pageId, Map<String, String> data);

    /**
     * 修改。
     *
     * @param data 键 = 字段 fieldKey（字符串），仅覆盖非空字段语义由实现方决定
     * @return AjaxResult
     */
    AjaxResult update(CredentialPresentation credential, Long pageId, Long id, Map<String, String> data);

    /**
     * 删除。
     *
     * @param ids 逗号分隔主键串
     * @return AjaxResult
     */
    AjaxResult remove(CredentialPresentation credential, Long pageId, String ids);
}
