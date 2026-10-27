package com.github.cocosoys.mc.custompanel.spring.service;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelField;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.FieldDefVO;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

import java.util.List;

/**
 * 字段定义管理服务（抽象契约）：字段元数据的 CRUD 与按页面级联清理。
 *
 * <p>实现见 {@link com.github.cocosoys.mc.custompanel.spring.impl.PanelFieldServiceImpl}。</p>
 */
public interface IPanelFieldService {

    /**
     * 字段列表（分页 + 条件过滤）。
     *
     * @param pageNum   页码（从 1 起，可空）
     * @param pageSize  每页条数（可空）
     * @param pageId    所属页面 ID 精确过滤（可空）
     * @param fieldKey  字段键模糊匹配（可空）
     * @param fieldName 字段名称模糊匹配（可空）
     * @return 若依分页体 {code,msg,rows,total}
     */
    TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId, String fieldKey, String fieldName);

    /**
     * 字段详情。
     */
    AjaxResult detail(Long fieldId);

    /**
     * 新增字段（所属页面必须存在）。
     */
    AjaxResult add(PanelField field, CredentialPresentation credential);

    /**
     * 修改字段（非空才覆盖）。
     */
    AjaxResult update(Long fieldId, PanelField field, CredentialPresentation credential);

    /**
     * 批量删除字段（逗号分隔 ID）。
     */
    AjaxResult remove(String fieldIds, CredentialPresentation credential);

    /**
     * 按页面全量取字段展示对象（供页面定义聚合 Schema 使用，按排序规则有序）。
     *
     * @param pageId 页面 ID（非空）
     * @return 该页面下全部字段的展示对象列表
     */
    List<FieldDefVO> listByPageId(Long pageId);

    /**
     * 按页面级联清理：删除某页面下的全部字段定义（供页面删除时调用）。
     *
     * @param pageId 页面 ID
     * @return 实际删除条数
     */
    int removeByPageId(Long pageId);
}
