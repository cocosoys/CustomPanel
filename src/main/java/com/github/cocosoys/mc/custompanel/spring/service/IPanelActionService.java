package com.github.cocosoys.mc.custompanel.spring.service;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelAction;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.ActionDefVO;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.ActionExecuteRequest;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.List;

/**
 * 动作定义管理服务（抽象契约）：动作元数据的 CRUD、按页面级联清理与执行分派。
 *
 * <p>实现见 {@link com.github.cocosoys.mc.custompanel.spring.impl.PanelActionServiceImpl}。</p>
 */
public interface IPanelActionService {

    /**
     * 动作列表（分页 + 条件过滤）。
     *
     * @param pageNum    页码（从 1 起，可空）
     * @param pageSize   每页条数（可空）
     * @param pageId     所属页面 ID 精确过滤（可空）
     * @param actionKey  动作键模糊匹配（可空）
     * @param actionName 动作名称模糊匹配（可空）
     * @return 若依分页体 {code,msg,rows,total}
     */
    TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId, String actionKey, String actionName);

    /**
     * 动作详情。
     */
    AjaxResult detail(Long actionId);

    /**
     * 新增动作（所属页面必须存在）。
     */
    AjaxResult add(PanelAction action, CredentialPresentation credential);

    /**
     * 修改动作（非空才覆盖）。
     */
    AjaxResult update(Long actionId, PanelAction action, CredentialPresentation credential);

    /**
     * 批量删除动作（逗号分隔 ID）。
     */
    AjaxResult remove(String actionIds, CredentialPresentation credential);

    /**
     * 按页面级联清理：删除某页面下的全部动作定义（供页面删除时调用）。
     *
     * @param pageId 页面 ID
     * @return 实际删除条数
     */
    int removeByPageId(Long pageId);

    /**
     * 按页面全量取动作展示对象（供页面定义聚合 Schema 使用，按排序规则有序）。
     *
     * @param pageId 页面 ID（非空）
     * @return 该页面下全部动作的展示对象列表
     */
    List<ActionDefVO> listByPageId(Long pageId);

    /**
     * 执行动作（按 {@code actionType} 分派：COMMAND 主线程命令 / HTTP 出站请求 / INTERNAL 本地分发）。
     *
     * <p>COMMAND 经 Bukkit 调度到主线程派发（可能操作世界数据）；HTTP 与 INTERNAL 经
     * SOYS {@code HttpClientApi} 在异步线程执行，避免阻塞网关线程。
     * 动作目标支持 {@code {fieldKey}} 占位符，由记录上下文（recordId 对应 dataJson）与
     * 请求参数（params，同名优先）插值。</p>
     *
     * <p>动作级权限：{@code panel_action.permission} 非空时，执行前先校验调用方凭证是否持有该节点
     * （{@code ':' ≡ '.'} 归一），无权限返回 403；为空则沿用页面权限。</p>
     *
     * @param actionId   动作 ID
     * @param request    执行上下文（可空；recordId + params）
     * @param credential 调用方凭证
     * @return AjaxResult（派发成功 / 参数或安全校验失败）
     */
    AjaxResult execute(Long actionId, ActionExecuteRequest request, CredentialPresentation credential);
}
