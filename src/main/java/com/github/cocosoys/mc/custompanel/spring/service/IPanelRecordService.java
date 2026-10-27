package com.github.cocosoys.mc.custompanel.spring.service;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelRecord;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 自由模式记录管理服务（抽象契约）：通用行存储数据的 CRUD 与按页面级联清理。
 *
 * <p>实现见 {@link com.github.cocosoys.mc.custompanel.spring.impl.PanelRecordServiceImpl}。</p>
 */
public interface IPanelRecordService {

    /**
     * 记录列表（分页 + 按页面精确过滤）。
     *
     * @param pageNum  页码（从 1 起，可空）
     * @param pageSize 每页条数（可空）
     * @param pageId   所属页面 ID 精确过滤（可空）
     * @return 若依分页体 {code,msg,rows,total}
     */
    TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId);

    /**
     * 记录详情。
     */
    AjaxResult detail(Long recordId);

    /**
     * 新增记录（所属页面必须存在且为 FREE 自由模式）。
     */
    AjaxResult add(PanelRecord record, CredentialPresentation credential);

    /**
     * 修改记录（非空才覆盖）。
     */
    AjaxResult update(Long recordId, PanelRecord record, CredentialPresentation credential);

    /**
     * 批量删除记录（逗号分隔 ID）。
     */
    AjaxResult remove(String recordIds, CredentialPresentation credential);

    /**
     * 按页面级联清理：删除某页面下的全部记录（供页面删除时调用）。
     *
     * @param pageId 页面 ID
     * @return 实际删除条数
     */
    int removeByPageId(Long pageId);
}
