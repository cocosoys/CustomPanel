package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.CustomPanel;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelField;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelLayout;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.LayoutVO;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.PanelLayoutSaveRequest;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelLayoutService;
import com.github.cocosoys.mc.custompanel.util.PanelAuth;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonWriter;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面设计器布局服务实现：confJson 单源的 CRUD / 发布 / 旧数据迁移。
 *
 * <p><b>单源</b>：{@code panel_layout.fieldsJson} 是字段定义唯一真源；旧 {@code panel_field}
 * 表自 P1 起停写，仅通过 {@link #ensureMigrated(Long)} 一次性迁移。迁移幂等——已有布局页面跳过。</p>
 *
 * <p><b>版本</b>：同页面可存多版本，{@code status=PUBLISHED} 唯一生效；发布时旧版本降 DRAFT。</p>
 *
 * <p><b>上限</b>：字段数与 fieldsJson 字节数由 config.yml（designer.max-*）配置，缺省兜底，
 * 代码不硬编码业务阈值。</p>
 */
@CustomLog
public class PanelLayoutServiceImpl implements IPanelLayoutService {

    /** 配置缺省值（config.yml 未配置时兜底） */
    private static final int DEFAULT_MAX_FIELDS = 200;
    private static final int DEFAULT_MAX_CONF_BYTES = 262144; // 256 KiB

    private final CustomPanel plugin;

    public PanelLayoutServiceImpl(CustomPanel plugin) {
        this.plugin = plugin;
    }

    @Override
    public AjaxResult detail(Long pageId) {
        if (pageId == null || DATA.get(PanelPage.class, pageId) == null) {
            return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
        }
        ensureMigrated(pageId);
        PanelLayout layout = latestPublished(pageId);
        if (layout == null) {
            return AjaxResult.success(null);
        }
        return AjaxResult.success(toVO(layout));
    }

    @Override
    public AjaxResult save(PanelLayoutSaveRequest request, CredentialPresentation credential) {
        if (request == null || request.getPageId() == null) {
            return AjaxResult.error(t("custompanel.layout.missing-pageId", "缺少 pageId"));
        }
        Long pageId = request.getPageId();
        if (DATA.get(PanelPage.class, pageId) == null) {
            return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
        }
        String formConf = request.getFormConfJson() == null ? "{}" : request.getFormConfJson();
        String fields = request.getFieldsJson() == null ? "[]" : request.getFieldsJson();
        // 上限校验（配置驱动）
        int maxFields = maxFields();
        int maxBytes = maxConfBytes();
        int count = fieldCount(fields);
        if (count > maxFields) {
            return AjaxResult.error(t("custompanel.layout.too-many-fields", "控件数超限（上限 {0}，当前 {1}）",
                    maxFields, count));
        }
        int bytes = fields.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > maxBytes) {
            return AjaxResult.error(t("custompanel.layout.conf-too-large", "布局数据超限（上限 {0} 字节，当前 {1}）",
                    maxBytes, bytes));
        }
        // 取当前最大版本（含 DRAFT），新版本 = max+1；保存一律落 DRAFT
        int nextVersion = nextVersion(pageId);
        PanelLayout layout = new PanelLayout();
        layout.setPageId(pageId);
        layout.setFormConfJson(formConf);
        layout.setFieldsJson(fields);
        layout.setVersion(nextVersion);
        layout.setStatus(PanelLayout.Status.DRAFT);
        layout.setRemark(request.getRemark() == null ? "" : request.getRemark());
        layout.setCreateTime(new Date());
        layout.setCreateBy(PanelAuth.currentPlayer(credential));
        DATA.insert(layout);
        log.infoT("custompanel.log.layout-saved", "布局草稿已保存 -> pageId {0}, version {1}", pageId, nextVersion);
        return AjaxResult.success(t("custompanel.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult publish(Long pageId, CredentialPresentation credential) {
        if (pageId == null) {
            return AjaxResult.error(t("custompanel.layout.missing-pageId", "缺少 pageId"));
        }
        List<PanelLayout> all = layoutsOf(pageId);
        if (all.isEmpty()) {
            return AjaxResult.error(t("custompanel.layout.not-found", "布局不存在"));
        }
        all.sort(Comparator.comparing(PanelLayout::getVersion, Comparator.nullsLast(Integer::compareTo)));
        PanelLayout newest = all.get(all.size() - 1);
        // 旧版本全部降 DRAFT，最新版置 PUBLISHED
        for (PanelLayout l : all) {
            String target = l.getLayoutId().equals(newest.getLayoutId())
                    ? PanelLayout.Status.PUBLISHED : PanelLayout.Status.DRAFT;
            if (!target.equals(l.getStatus())) {
                l.setStatus(target);
                l.setUpdateTime(new Date());
                l.setUpdateBy(PanelAuth.currentPlayer(credential));
                DATA.updateById(l);
            }
        }
        log.infoT("custompanel.log.layout-published", "布局已发布 -> pageId {0}, version {1}",
                pageId, newest.getVersion());
        return AjaxResult.success(t("custompanel.layout.publish-success", "发布成功"));
    }

    @Override
    public AjaxResult remove(Long layoutId) {
        if (layoutId == null) {
            return AjaxResult.error(t("custompanel.layout.missing-layoutId", "缺少 layoutId"));
        }
        DATA.deleteById(PanelLayout.class, layoutId);
        return AjaxResult.success(t("custompanel.common.delete-success", "删除成功"));
    }

    @Override
    public PanelLayout latestPublished(Long pageId) {
        if (pageId == null) {
            return null;
        }
        PanelLayout best = null;
        for (PanelLayout l : DATA.select(PanelLayout.class)) {
            if (!pageId.equals(l.getPageId()) || !PanelLayout.Status.PUBLISHED.equals(l.getStatus())) {
                continue;
            }
            if (best == null || versionOf(l) > versionOf(best)) {
                best = l;
            }
        }
        return best;
    }

    @Override
    public void ensureMigrated(Long pageId) {
        if (pageId == null || !layoutsOf(pageId).isEmpty()) {
            return; // 幂等：已有布局（含 DRAFT）不再迁移
        }
        List<PanelField> fields = new ArrayList<>();
        for (PanelField f : DATA.select(PanelField.class)) {
            if (pageId.equals(f.getPageId())) {
                fields.add(f);
            }
        }
        if (fields.isEmpty()) {
            return; // 无旧字段数据，无需迁移
        }
        fields.sort(Comparator.comparing(PanelField::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
        Map<String, Object> formConf = defaultFormConf();
        List<Object> fieldConfs = new ArrayList<>();
        for (PanelField f : fields) {
            fieldConfs.add(migrateField(f));
        }
        PanelLayout layout = new PanelLayout();
        layout.setPageId(pageId);
        layout.setFormConfJson(JsonWriter.write(formConf));
        layout.setFieldsJson(JsonWriter.write(fieldConfs));
        layout.setVersion(1);
        layout.setStatus(PanelLayout.Status.PUBLISHED);
        layout.setRemark("由 panel_field 自动迁移");
        layout.setCreateTime(new Date());
        DATA.insert(layout);
        log.infoT("custompanel.log.layout-migrated", "旧字段已迁移为布局 -> pageId {0}, 字段 {1} 条",
                pageId, fields.size());
    }

    @Override
    public int removeByPageId(Long pageId) {
        if (pageId == null) {
            return 0;
        }
        int n = 0;
        for (PanelLayout l : DATA.select(PanelLayout.class)) {
            if (pageId.equals(l.getPageId()) && l.getLayoutId() != null) {
                DATA.deleteById(PanelLayout.class, l.getLayoutId());
                n++;
            }
        }
        return n;
    }

    // ===== 内部 =====

    /** 默认 RuoYi formConf（对齐 config.js formConf）。 */
    private static Map<String, Object> defaultFormConf() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("formRef", "elForm");
        m.put("formModel", "formData");
        m.put("size", "medium");
        m.put("labelPosition", "right");
        m.put("labelWidth", 100);
        m.put("formRules", "rules");
        m.put("gutter", 15);
        m.put("disabled", false);
        m.put("span", 24);
        m.put("formBtns", true);
        return m;
    }

    /** 旧 PanelField → RuoYi 控件 conf（字段类型映射到 Element tag）。 */
    private static Map<String, Object> migrateField(PanelField f) {
        String type = f.getFieldType() == null ? PanelField.FieldType.STRING : f.getFieldType();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", f.getFieldName() == null ? f.getFieldKey() : f.getFieldName());
        m.put("vModel", f.getFieldKey());
        m.put("tag", tagOf(type));
        m.put("tagIcon", tagIconOf(type));
        m.put("span", 24);
        m.put("required", "1".equals(f.getRequired()));
        m.put("defaultValue", f.getDefaultValue() == null ? "" : f.getDefaultValue());
        m.put("showInList", "1".equals(f.getShowInList()));
        m.put("sortOrder", f.getSortOrder() == null ? 0 : f.getSortOrder());
        m.put("placeholder", "");
        m.put("disabled", false);
        m.put("readonly", false);
        m.put("regList", new ArrayList<>());
        if (PanelField.FieldType.TEXT.equals(type)) {
            m.put("type", "textarea");
        }
        if (PanelField.FieldType.SELECT.equals(type)) {
            List<Object> options = new ArrayList<>();
            String raw = f.getOptions();
            if (raw != null && !raw.trim().isEmpty()) {
                for (String part : raw.split(",")) {
                    String v = part.trim();
                    if (!v.isEmpty()) {
                        Map<String, Object> opt = new LinkedHashMap<>();
                        opt.put("label", v);
                        opt.put("value", v);
                        options.add(opt);
                    }
                }
            }
            m.put("options", options);
        }
        return m;
    }

    private static String tagOf(String type) {
        if (PanelField.FieldType.NUMBER.equals(type)) {
            return "el-input-number";
        }
        if (PanelField.FieldType.BOOLEAN.equals(type)) {
            return "el-switch";
        }
        if (PanelField.FieldType.DATE.equals(type)) {
            return "el-date-picker";
        }
        if (PanelField.FieldType.SELECT.equals(type)) {
            return "el-select";
        }
        return "el-input";
    }

    private static String tagIconOf(String type) {
        if (PanelField.FieldType.NUMBER.equals(type)) {
            return "number";
        }
        if (PanelField.FieldType.BOOLEAN.equals(type)) {
            return "switch";
        }
        if (PanelField.FieldType.DATE.equals(type)) {
            return "date";
        }
        if (PanelField.FieldType.SELECT.equals(type)) {
            return "select";
        }
        return "input";
    }

    private List<PanelLayout> layoutsOf(Long pageId) {
        List<PanelLayout> out = new ArrayList<>();
        for (PanelLayout l : DATA.select(PanelLayout.class)) {
            if (pageId.equals(l.getPageId())) {
                out.add(l);
            }
        }
        return out;
    }

    private int nextVersion(Long pageId) {
        int max = 0;
        for (PanelLayout l : layoutsOf(pageId)) {
            if (versionOf(l) > max) {
                max = versionOf(l);
            }
        }
        return max + 1;
    }

    private static int versionOf(PanelLayout l) {
        return l.getVersion() == null ? 0 : l.getVersion();
    }

    /** fieldsJson 数组长度；解析失败按 0（校验交给上限，不因脏 JSON 拒绝整个保存）。 */
    private static int fieldCount(String fieldsJson) {
        try {
            List<Object> arr = JsonReader.parseArray(fieldsJson);
            return arr == null ? 0 : arr.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int maxFields() {
        return plugin == null ? DEFAULT_MAX_FIELDS : plugin.getConfig().getInt("designer.max-fields", DEFAULT_MAX_FIELDS);
    }

    private int maxConfBytes() {
        return plugin == null ? DEFAULT_MAX_CONF_BYTES
                : plugin.getConfig().getInt("designer.max-conf-bytes", DEFAULT_MAX_CONF_BYTES);
    }

    private static LayoutVO toVO(PanelLayout l) {
        LayoutVO vo = new LayoutVO();
        vo.setLayoutId(l.getLayoutId());
        vo.setPageId(l.getPageId());
        vo.setFormConfJson(l.getFormConfJson());
        vo.setFieldsJson(l.getFieldsJson());
        vo.setVersion(l.getVersion());
        vo.setStatus(l.getStatus());
        return vo;
    }
}
