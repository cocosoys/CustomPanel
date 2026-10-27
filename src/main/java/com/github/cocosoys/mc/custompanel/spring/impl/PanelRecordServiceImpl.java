package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelRecord;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.PanelRecordVO;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelRecordService;
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

/**
 * 自由模式记录管理实现：CRUD、所属页面校验、存储模式校验、按页面级联清理。
 *
 * <p>业务规则：仅 FREE 自由模式页面可写入本表（MANAGED 托管模式走绑定实体真实列，
 * 两种模式数据不允许混存）。持久化经 SOYS {@link DATA} 门面，本插件不实现存储层。</p>
 */
@CustomLog
public class PanelRecordServiceImpl implements IPanelRecordService {

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId) {
        List<PanelRecord> all = DATA.select(PanelRecord.class);
        List<PanelRecord> filtered = new ArrayList<>();
        for (PanelRecord r : all) {
            if (pageId != null && !pageId.equals(r.getPageId())) {
                continue;
            }
            filtered.add(r);
        }
        filtered.sort(Comparator.comparing(PanelRecord::getRecordId, Comparator.nullsLast(Long::compareTo)));
        List<Object> rows = new ArrayList<>();
        for (PanelRecord r : filtered) {
            rows.add(toVO(r));
        }
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(Long recordId) {
        PanelRecord r = recordId == null ? null : DATA.get(PanelRecord.class, recordId);
        if (r == null) {
            return AjaxResult.error(t("custompanel.record.not-found", "记录不存在"));
        }
        return AjaxResult.success(toVO(r));
    }

    @Override
    public AjaxResult add(PanelRecord record, CredentialPresentation credential) {
        if (record == null) {
            return AjaxResult.error(t("custompanel.record.data-empty", "数据内容不能为空"));
        }
        Long pageId = record.getPageId();
        if (pageId == null || DATA.get(PanelPage.class, pageId) == null) {
            return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
        }
        PanelPage page = DATA.get(PanelPage.class, pageId);
        if (!PanelPage.StoreMode.FREE.equals(page.getStoreMode())) {
            return AjaxResult.error(t("custompanel.record.page-not-free", "仅自由模式页面可写入记录"));
        }
        String data = record.getDataJson() == null ? "" : record.getDataJson().trim();
        if (data.isEmpty()) {
            return AjaxResult.error(t("custompanel.record.data-empty", "数据内容不能为空"));
        }
        PanelRecord r = new PanelRecord();
        r.setPageId(pageId);
        r.setDataJson(data);
        r.setCreateTime(new Date());
        r.setCreateBy(PanelAuth.currentPlayer(credential));
        DATA.insert(r);
        log.infoT("custompanel.log.record-added", "记录已新增 -> pageId {0}", pageId);
        return AjaxResult.success(t("custompanel.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(Long recordId, PanelRecord record, CredentialPresentation credential) {
        if (recordId == null) {
            return AjaxResult.error(t("custompanel.record.missing-recordId", "缺少 recordId"));
        }
        PanelRecord r = DATA.get(PanelRecord.class, recordId);
        if (r == null) {
            return AjaxResult.error(t("custompanel.record.not-found", "记录不存在"));
        }
        if (record.getPageId() != null) {
            if (DATA.get(PanelPage.class, record.getPageId()) == null) {
                return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
            }
            r.setPageId(record.getPageId());
        }
        if (record.getDataJson() != null) {
            r.setDataJson(record.getDataJson());
        }
        r.setUpdateTime(new Date());
        r.setUpdateBy(PanelAuth.currentPlayer(credential));
        DATA.updateById(r);
        return AjaxResult.success(t("custompanel.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String recordIds, CredentialPresentation credential) {
        if (recordIds == null || recordIds.trim().isEmpty()) {
            return AjaxResult.error(t("custompanel.record.missing-recordId", "缺少 recordId"));
        }
        for (String id : recordIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            Long rid = parseId(id);
            if (rid != null) {
                DATA.deleteById(PanelRecord.class, rid);
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
        for (PanelRecord r : DATA.select(PanelRecord.class)) {
            if (pageId.equals(r.getPageId()) && r.getRecordId() != null) {
                DATA.deleteById(PanelRecord.class, r.getRecordId());
                n++;
            }
        }
        return n;
    }

    // ===== 内部 =====

    /** 实体 → 展示 VO。 */
    private PanelRecordVO toVO(PanelRecord r) {
        PanelRecordVO vo = new PanelRecordVO();
        vo.setRecordId(r.getRecordId());
        vo.setPageId(r.getPageId());
        vo.setDataJson(r.getDataJson());
        vo.setCreateTime(r.getCreateTime());
        vo.setUpdateTime(r.getUpdateTime());
        return vo;
    }

    private static Long parseId(String id) {
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
