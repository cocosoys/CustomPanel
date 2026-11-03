package com.github.cocosoys.mc.custompanel.spring.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.MockedStatic;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelField;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 字段唯一性校验（不依赖 Spigot 运行时）：静态 mock {@link DATA} 门面。
 */
public class PanelFieldServiceImplTest {

    private final PanelFieldServiceImpl service = new PanelFieldServiceImpl();

    @Test
    public void addRejectsDuplicateKeyOnSamePageIgnoringCase() {
        PanelPage page = page(1L, PanelPage.StoreMode.FREE);
        PanelField existing = field(2L, 1L, "name");
        PanelField request = field(null, 1L, "NAME");

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page);
            data.when(() -> DATA.select(PanelField.class)).thenReturn(Arrays.asList(existing));

            AjaxResult result = service.add(request, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelField.class)), never());
        }
    }

    @Test
    public void updateRejectsMovingFieldIntoPageThatAlreadyHasSameKey() {
        PanelField current = field(10L, 1L, "nick");
        PanelField collision = field(11L, 2L, "nick");
        PanelPage targetPage = page(2L, PanelPage.StoreMode.FREE);

        PanelField request = new PanelField();
        request.setPageId(2L);
        request.setFieldKey("nick");

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelField.class, 10L)).thenReturn(current);
            data.when(() -> DATA.get(PanelPage.class, 2L)).thenReturn(targetPage);
            data.when(() -> DATA.select(PanelField.class)).thenReturn(Arrays.asList(current, collision));

            AjaxResult result = service.update(10L, request, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.updateById(any(PanelField.class)), never());
        }
    }

    private static PanelPage page(Long pageId, String storeMode) {
        PanelPage page = new PanelPage();
        page.setPageId(pageId);
        page.setStoreMode(storeMode);
        return page;
    }

    private static PanelField field(Long fieldId, Long pageId, String key) {
        PanelField field = new PanelField();
        field.setFieldId(fieldId);
        field.setPageId(pageId);
        field.setFieldKey(key);
        field.setFieldName(key);
        return field;
    }

    private static int code(AjaxResult result) {
        return ((Number) result.get("code")).intValue();
    }
}
