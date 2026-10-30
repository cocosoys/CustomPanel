package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelField;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.FieldDefVO;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelFieldService;
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
import java.util.Objects;

/**
 * 字段定义管理实现：CRUD、所属页面校验、类型合法性校验、按页面级联清理。
 *
 * <p>持久化经 SOYS {@link DATA} 门面，本插件不实现存储层。</p>
 */
@CustomLog
public class PanelFieldServiceImpl implements IPanelFieldService {

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId, String fieldKey, String fieldName) {
        List<PanelField> all = DATA.select(PanelField.class);
        List<PanelField> filtered = new ArrayList<>();
        for (PanelField f : all) {
            if (pageId != null && !pageId.equals(f.getPageId())) {
                continue;
            }
            if (!contains(f.getFieldKey(), fieldKey)) {
                continue;
            }
            if (!contains(f.getFieldName(), fieldName)) {
                continue;
            }
            filtered.add(f);
        }
        filtered.sort(Comparator.comparing(PanelField::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PanelField::getFieldId, Comparator.nullsLast(Long::compareTo)));
        List<Object> rows = new ArrayList<>();
        for (PanelField f : filtered) {
            rows.add(toVO(f));
        }
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(Long fieldId) {
        PanelField f = fieldId == null ? null : DATA.get(PanelField.class, fieldId);
        if (f == null) {
            return AjaxResult.error(t("custompanel.field.not-found", "字段不存在"));
        }
        return AjaxResult.success(toVO(f));
    }

    @Override
    public AjaxResult add(PanelField field, CredentialPresentation credential) {
        if (field == null) {
            return AjaxResult.error(t("custompanel.field.key-empty", "字段键不能为空"));
        }
        String key = field.getFieldKey() == null ? "" : field.getFieldKey().trim();
        String name = field.getFieldName() == null ? "" : field.getFieldName().trim();
        if (key.isEmpty()) {
            return AjaxResult.error(t("custompanel.field.key-empty", "字段键不能为空"));
        }
        if (name.isEmpty()) {
            return AjaxResult.error(t("custompanel.field.name-empty", "字段名称不能为空"));
        }
        String type = field.getFieldType() == null || field.getFieldType().trim().isEmpty()
                ? PanelField.FieldType.STRING : field.getFieldType().trim().toUpperCase();
        if (!PanelField.FieldType.valid(type)) {
            return AjaxResult.error(t("custompanel.field.type-invalid", "字段类型非法（STRING/NUMBER/BOOLEAN/DATE/TEXT/SELECT）"));
        }
        if (field.getPageId() == null || DATA.get(PanelPage.class, field.getPageId()) == null) {
            return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
        }
        // 同页字段键唯一（忽略大小写）：重复会导致渲染器列冲突与数据覆盖
        if (findByKey(field.getPageId(), key, null) != null) {
            return AjaxResult.error(t("custompanel.field.key-exists", "字段键已存在"));
        }
        PanelField f = new PanelField();
        f.setPageId(field.getPageId());
        f.setFieldKey(key);
        f.setFieldName(name);
        f.setFieldType(type);
        f.setRequired(field.getRequired() == null || field.getRequired().isEmpty() ? "0" : field.getRequired());
        f.setDefaultValue(field.getDefaultValue() == null ? "" : field.getDefaultValue());
        f.setOptions(field.getOptions() == null ? "" : field.getOptions());
        f.setShowInList(field.getShowInList() == null || field.getShowInList().isEmpty() ? "1" : field.getShowInList());
        f.setSortOrder(field.getSortOrder() == null ? 0 : field.getSortOrder());
        f.setRemark(field.getRemark() == null ? "" : field.getRemark());
        f.setCreateTime(new Date());
        f.setCreateBy(PanelAuth.currentPlayer(credential));
        DATA.insert(f);
        return AjaxResult.success(t("custompanel.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(Long fieldId, PanelField field, CredentialPresentation credential) {
        if (fieldId == null) {
            return AjaxResult.error(t("custompanel.field.missing-fieldId", "缺少 fieldId"));
        }
        PanelField f = DATA.get(PanelField.class, fieldId);
        if (f == null) {
            return AjaxResult.error(t("custompanel.field.not-found", "字段不存在"));
        }
        // 目标页面：更新请求携带新 pageId 则以其为准（须存在），否则沿用当前记录所属页
        Long targetPageId = f.getPageId();
        if (field.getPageId() != null && !field.getPageId().equals(f.getPageId())) {
            if (DATA.get(PanelPage.class, field.getPageId()) == null) {
                return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
            }
            targetPageId = field.getPageId();
        }
        // 目标字段键：请求携带非空 fieldKey 则以其为准，否则沿用原键
        boolean keyChanged = field.getFieldKey() != null && !field.getFieldKey().trim().isEmpty()
                && !field.getFieldKey().trim().equals(f.getFieldKey());
        String targetKey = keyChanged ? field.getFieldKey().trim() : f.getFieldKey();
        // 同页字段键唯一（忽略大小写，排除自身）：键或所属页变化都要重新校验，
        // 否则“只换页不改键”可把重复键搬进目标页
        if (keyChanged || !Objects.equals(targetPageId, f.getPageId())) {
            if (findByKey(targetPageId, targetKey, fieldId) != null) {
                return AjaxResult.error(t("custompanel.field.key-exists", "字段键已存在"));
            }
        }
        if (keyChanged) {
            f.setFieldKey(targetKey);
        }
        if (field.getFieldName() != null && !field.getFieldName().trim().isEmpty()) {
            f.setFieldName(field.getFieldName().trim());
        }
        if (field.getFieldType() != null && !field.getFieldType().trim().isEmpty()) {
            String type = field.getFieldType().trim().toUpperCase();
            if (!PanelField.FieldType.valid(type)) {
                return AjaxResult.error(t("custompanel.field.type-invalid", "字段类型非法（STRING/NUMBER/BOOLEAN/DATE/TEXT/SELECT）"));
            }
            f.setFieldType(type);
        }
        if (field.getRequired() != null && !field.getRequired().isEmpty()) {
            f.setRequired(field.getRequired());
        }
        if (field.getDefaultValue() != null) {
            f.setDefaultValue(field.getDefaultValue());
        }
        if (field.getOptions() != null) {
            f.setOptions(field.getOptions());
        }
        if (field.getShowInList() != null && !field.getShowInList().isEmpty()) {
            f.setShowInList(field.getShowInList());
        }
        if (field.getSortOrder() != null) {
            f.setSortOrder(field.getSortOrder());
        }
        if (field.getRemark() != null) {
            f.setRemark(field.getRemark());
        }
        f.setPageId(targetPageId);
        f.setUpdateTime(new Date());
        f.setUpdateBy(PanelAuth.currentPlayer(credential));
        DATA.updateById(f);
        return AjaxResult.success(t("custompanel.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String fieldIds, CredentialPresentation credential) {
        if (fieldIds == null || fieldIds.trim().isEmpty()) {
            return AjaxResult.error(t("custompanel.field.missing-fieldId", "缺少 fieldId"));
        }
        for (String id : fieldIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            Long fid = parseId(id);
            if (fid != null) {
                DATA.deleteById(PanelField.class, fid);
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
        for (PanelField f : DATA.select(PanelField.class)) {
            if (pageId.equals(f.getPageId()) && f.getFieldId() != null) {
                DATA.deleteById(PanelField.class, f.getFieldId());
                n++;
            }
        }
        return n;
    }

    @Override
    public List<FieldDefVO> listByPageId(Long pageId) {
        if (pageId == null) {
            return new ArrayList<>();
        }
        List<FieldDefVO> rows = new ArrayList<>();
        for (PanelField f : DATA.select(PanelField.class)) {
            if (pageId.equals(f.getPageId())) {
                rows.add(toVO(f));
            }
        }
        rows.sort(Comparator.comparing(FieldDefVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(FieldDefVO::getFieldId, Comparator.nullsLast(Long::compareTo)));
        return rows;
    }

    // ===== 内部 =====

    /** 同页字段键查重（忽略大小写）；excludeFieldId 非空时排除该字段（修改场景排除自身），命中返回记录。 */
    private PanelField findByKey(Long pageId, String key, Long excludeFieldId) {
        for (PanelField f : DATA.select(PanelField.class)) {
            if (pageId != null && pageId.equals(f.getPageId())
                    && f.getFieldKey() != null && f.getFieldKey().equalsIgnoreCase(key)
                    && (excludeFieldId == null || !excludeFieldId.equals(f.getFieldId()))) {
                return f;
            }
        }
        return null;
    }

    /** 实体 → 展示 VO。 */
    private FieldDefVO toVO(PanelField f) {
        FieldDefVO vo = new FieldDefVO();
        vo.setFieldId(f.getFieldId());
        vo.setPageId(f.getPageId());
        vo.setFieldKey(f.getFieldKey());
        vo.setFieldName(f.getFieldName());
        vo.setFieldType(f.getFieldType());
        vo.setRequired(f.getRequired());
        vo.setDefaultValue(f.getDefaultValue());
        vo.setOptions(f.getOptions());
        vo.setShowInList(f.getShowInList());
        vo.setSortOrder(f.getSortOrder());
        vo.setRemark(f.getRemark());
        vo.setCreateTime(f.getCreateTime());
        vo.setUpdateTime(f.getUpdateTime());
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
