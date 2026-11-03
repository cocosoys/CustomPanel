package com.github.cocosoys.mc.custompanel.spring.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import org.junit.Test;
import org.mockito.MockedStatic;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelRecord;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * FREE 模式不变量（不依赖 Spigot 运行时）：静态 mock {@link DATA} 门面。
 */
public class PanelRecordServiceImplTest {

    private final PanelRecordServiceImpl service = new PanelRecordServiceImpl();

    @Test
    public void addRejectsManagedPage() {
        PanelPage managed = page(1L, PanelPage.StoreMode.MANAGED);
        PanelRecord request = new PanelRecord();
        request.setPageId(1L);
        request.setDataJson("{\"name\":\"demo\"}");

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(managed);

            AjaxResult result = service.add(request, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelRecord.class)), never());
        }
    }

    @Test
    public void updateRejectsMigratingFreeRecordToManagedPage() {
        PanelRecord current = new PanelRecord();
        current.setRecordId(5L);
        current.setPageId(1L);
        current.setDataJson("{\"name\":\"demo\"}");

        PanelPage managed = page(2L, PanelPage.StoreMode.MANAGED);
        PanelRecord request = new PanelRecord();
        request.setPageId(2L);

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelRecord.class, 5L)).thenReturn(current);
            data.when(() -> DATA.get(PanelPage.class, 2L)).thenReturn(managed);

            AjaxResult result = service.update(5L, request, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.updateById(any(PanelRecord.class)), never());
        }
    }

    private static PanelPage page(Long pageId, String storeMode) {
        PanelPage page = new PanelPage();
        page.setPageId(pageId);
        page.setStoreMode(storeMode);
        return page;
    }

    private static int code(AjaxResult result) {
        return ((Number) result.get("code")).intValue();
    }
}
