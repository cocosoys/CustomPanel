package com.github.cocosoys.mc.custompanel.spring.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.github.cocosoys.mc.custompanel.CustomPanel;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelField;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelLayout;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.PanelLayoutSaveRequest;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 布局服务（confJson 单源）：配置上限、版本唯一、旧字段迁移（不依赖 Spigot 运行时）。
 */
public class PanelLayoutServiceImplTest {

    @Test
    public void saveRejectsFieldCountOverConfiguredLimit() {
        CustomPanel plugin = mock(CustomPanel.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(cfg);
        when(cfg.getInt("designer.max-fields", 200)).thenReturn(1);
        when(cfg.getInt("designer.max-conf-bytes", 262144)).thenReturn(262144);
        PanelLayoutServiceImpl service = new PanelLayoutServiceImpl(plugin);
        PanelLayoutSaveRequest req = request(1L,
                "[{\"vModel\":\"a\"},{\"vModel\":\"b\"}]");

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page(1L));

            AjaxResult result = service.save(req, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelLayout.class)), never());
        }
    }

    @Test
    public void saveRejectsConfBytesOverConfiguredLimit() {
        CustomPanel plugin = mock(CustomPanel.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(cfg);
        when(cfg.getInt("designer.max-fields", 200)).thenReturn(200);
        when(cfg.getInt("designer.max-conf-bytes", 262144)).thenReturn(10);
        PanelLayoutServiceImpl service = new PanelLayoutServiceImpl(plugin);
        PanelLayoutSaveRequest req = request(1L, "[{\"vModel\":\"abcdefghijklmnopqrst\"}]");

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page(1L));

            AjaxResult result = service.save(req, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelLayout.class)), never());
        }
    }

    @Test
    public void publishLeavesExactlyOnePublishedVersion() {
        PanelLayoutServiceImpl service = new PanelLayoutServiceImpl(null);
        PanelLayout v1 = layout(1L, 1, PanelLayout.Status.PUBLISHED);
        PanelLayout v2 = layout(2L, 2, PanelLayout.Status.DRAFT);
        PanelLayout v3 = layout(3L, 3, PanelLayout.Status.DRAFT);

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.select(PanelLayout.class)).thenReturn(Arrays.asList(v1, v2, v3));

            AjaxResult result = service.publish(1L, null);

            assertEquals(AjaxResult.SUCCESS, code(result));
            assertEquals(PanelLayout.Status.DRAFT, v1.getStatus());
            assertEquals(PanelLayout.Status.DRAFT, v2.getStatus());
            assertEquals(PanelLayout.Status.PUBLISHED, v3.getStatus());
            data.verify(() -> DATA.updateById(v1), times(1));
            data.verify(() -> DATA.updateById(v3), times(1));
            data.verify(() -> DATA.updateById(v2), never());
        }
    }

    @Test
    public void ensureMigratedMapsLegacyFieldTypesAndIsIdempotent() {
        PanelLayoutServiceImpl service = new PanelLayoutServiceImpl(null);
        PanelField stringField = field(1L, "name", PanelField.FieldType.STRING, 1);
        PanelField numberField = field(2L, "amount", PanelField.FieldType.NUMBER, 2);
        PanelField selectField = field(3L, "level", PanelField.FieldType.SELECT, 3);
        selectField.setOptions("A,B");
        PanelLayout migrated = null;

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.select(PanelLayout.class)).thenReturn(new ArrayList<>());
            data.when(() -> DATA.select(PanelField.class))
                    .thenReturn(Arrays.asList(stringField, numberField, selectField));

            service.ensureMigrated(1L);

            ArgumentCaptor<PanelLayout> captor = ArgumentCaptor.forClass(PanelLayout.class);
            data.verify(() -> DATA.insert(captor.capture()));
            migrated = captor.getValue();
            assertEquals(Integer.valueOf(1), migrated.getVersion());
            assertEquals(PanelLayout.Status.PUBLISHED, migrated.getStatus());
            String json = migrated.getFieldsJson();
            assertTrue("STRING 应映射为 el-input", json.contains("el-input"));
            assertTrue("NUMBER 应映射为 el-input-number", json.contains("el-input-number"));
            assertTrue("SELECT 应映射为 el-select", json.contains("el-select"));
            assertTrue("SELECT 应带 options", json.contains("\"options\""));
        }

        // 幂等：同页面已有布局后，再次调用不得重复迁移
        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.select(PanelLayout.class)).thenReturn(Collections.singletonList(migrated));

            service.ensureMigrated(1L);

            data.verify(() -> DATA.insert(any(PanelLayout.class)), never());
        }
    }

    // ===== 夹具 =====

    private static PanelPage page(Long id) {
        PanelPage p = new PanelPage();
        p.setPageId(id);
        return p;
    }

    private static PanelLayout layout(Long id, int version, String status) {
        PanelLayout l = new PanelLayout();
        l.setLayoutId(id);
        l.setPageId(1L);
        l.setVersion(version);
        l.setStatus(status);
        return l;
    }

    private static PanelField field(Long id, String key, String type, int order) {
        PanelField f = new PanelField();
        f.setFieldId(id);
        f.setPageId(1L);
        f.setFieldKey(key);
        f.setFieldName(key);
        f.setFieldType(type);
        f.setSortOrder(order);
        return f;
    }

    private static PanelLayoutSaveRequest request(Long pageId, String fieldsJson) {
        PanelLayoutSaveRequest r = new PanelLayoutSaveRequest();
        r.setPageId(pageId);
        r.setFormConfJson("{}");
        r.setFieldsJson(fieldsJson);
        return r;
    }

    private static int code(AjaxResult result) {
        return ((Number) result.get("code")).intValue();
    }
}
