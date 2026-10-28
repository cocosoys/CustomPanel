package com.github.cocosoys.mc.custompanel.managed.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.managed.ManagedEntityHandler;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelRecord;
import com.github.cocosoys.mc.custompanel.util.PanelAuth;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonWriter;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置演示托管实体（{@code demo-item}）：验证 MANAGED 数据通道全链路用。
 *
 * <p>存储复用自由模式通用行表 {@link PanelRecord}（以 pageId 区分页），仅作 SPI 演示：
 * 真实业务实体应由插件实现 {@link ManagedEntityHandler} 并注册，数据落 ORM 真实列。
 * 行键 = 页面字段 fieldKey（dataJson 解析），额外附 {@code id}（= recordId）供主键操作。</p>
 */
@CustomLog
public class DemoItemHandler implements ManagedEntityHandler {

    /** 演示实体注册名：页面 entityName 填此值即可绑定。 */
    public static final String NAME = "demo-item";

    @Override
    public String entityName() {
        return NAME;
    }

    @Override
    public TableDataInfo list(CredentialPresentation credential, Long pageId, Integer pageNum, Integer pageSize) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PanelRecord r : DATA.select(PanelRecord.class)) {
            if (pageId != null && pageId.equals(r.getPageId())) {
                rows.add(toRow(r));
            }
        }
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(CredentialPresentation credential, Long pageId, Long id) {
        PanelRecord r = id == null ? null : DATA.get(PanelRecord.class, id);
        if (r == null || !pageId.equals(r.getPageId())) {
            return AjaxResult.error(t("custompanel.record.not-found", "记录不存在"));
        }
        return AjaxResult.success(toRow(r));
    }

    @Override
    public AjaxResult add(CredentialPresentation credential, Long pageId, Map<String, String> data) {
        PanelRecord r = new PanelRecord();
        r.setPageId(pageId);
        r.setDataJson(JsonWriter.write(data == null ? new LinkedHashMap<>() : data));
        r.setCreateTime(new Date());
        r.setCreateBy(PanelAuth.currentPlayer(credential));
        DATA.insert(r);
        log.infoT("custompanel.log.managed-added", "托管数据已新增 -> entity {0}, pageId {1}, recordId {2}",
                NAME, pageId, r.getRecordId());
        return AjaxResult.success(t("custompanel.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(CredentialPresentation credential, Long pageId, Long id, Map<String, String> data) {
        PanelRecord r = id == null ? null : DATA.get(PanelRecord.class, id);
        if (r == null || !pageId.equals(r.getPageId())) {
            return AjaxResult.error(t("custompanel.record.not-found", "记录不存在"));
        }
        r.setDataJson(JsonWriter.write(data == null ? new LinkedHashMap<>() : data));
        r.setUpdateTime(new Date());
        r.setUpdateBy(PanelAuth.currentPlayer(credential));
        DATA.updateById(r);
        return AjaxResult.success(t("custompanel.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(CredentialPresentation credential, Long pageId, String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return AjaxResult.error(t("custompanel.record.missing-recordId", "缺少 recordId"));
        }
        for (String id : ids.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            try {
                Long rid = Long.valueOf(id.trim());
                PanelRecord r = DATA.get(PanelRecord.class, rid);
                if (r != null && pageId.equals(r.getPageId())) {
                    DATA.deleteById(PanelRecord.class, rid);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return AjaxResult.success(t("custompanel.common.delete-success", "删除成功"));
    }

    // ===== 内部 =====

    /** 记录 → 行：{id, recordId, ...dataJson 键}；dataJson 解析失败给空对象。 */
    private Map<String, Object> toRow(PanelRecord r) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getRecordId());
        row.put("recordId", r.getRecordId());
        if (r.getDataJson() != null && !r.getDataJson().trim().isEmpty()) {
            try {
                row.putAll(JsonReader.parseObject(r.getDataJson()));
            } catch (Exception ignored) {
            }
        }
        return row;
    }
}
