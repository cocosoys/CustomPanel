package com.github.cocosoys.mc.custompanel.spring.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.github.cocosoys.mc.custompanel.CustomPanel;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelAction;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelEvent;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 事件绑定服务：trigger / inlineType 白名单、内联长度、动作存在性与 enabled 过滤。
 */
public class PanelEventServiceImplTest {

    @Test
    public void rejectsInvalidTrigger() {
        PanelEventServiceImpl service = new PanelEventServiceImpl(null);
        PanelEvent e = validHttpEvent();
        e.setTrigger("onBogus");

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page(1L));

            AjaxResult result = service.add(e, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelEvent.class)), never());
        }
    }

    @Test
    public void rejectsCommandInlineType() {
        PanelEventServiceImpl service = new PanelEventServiceImpl(null);
        PanelEvent e = validHttpEvent();
        e.setInlineType("COMMAND");

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page(1L));

            AjaxResult result = service.add(e, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelEvent.class)), never());
        }
    }

    @Test
    public void rejectsInlineTargetOverConfiguredLimit() {
        CustomPanel plugin = mock(CustomPanel.class);
        FileConfiguration cfg = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(cfg);
        when(cfg.getInt("designer.max-inline-target-bytes", 4096)).thenReturn(5);
        PanelEventServiceImpl service = new PanelEventServiceImpl(plugin);
        PanelEvent e = validHttpEvent();

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page(1L));

            AjaxResult result = service.add(e, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelEvent.class)), never());
        }
    }

    @Test
    public void rejectsUnknownBoundAction() {
        PanelEventServiceImpl service = new PanelEventServiceImpl(null);
        PanelEvent e = new PanelEvent();
        e.setPageId(1L);
        e.setTrigger(PanelEvent.Trigger.ON_LOAD);
        e.setActionId(999L);

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page(1L));
            data.when(() -> DATA.get(PanelAction.class, 999L)).thenReturn(null);

            AjaxResult result = service.add(e, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelEvent.class)), never());
        }
    }

    @Test
    public void rejectsEventWithoutActionOrInline() {
        PanelEventServiceImpl service = new PanelEventServiceImpl(null);
        PanelEvent e = new PanelEvent();
        e.setPageId(1L);
        e.setTrigger(PanelEvent.Trigger.ON_LOAD);

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page(1L));

            AjaxResult result = service.add(e, null);

            assertEquals(AjaxResult.ERROR, code(result));
            data.verify(() -> DATA.insert(any(PanelEvent.class)), never());
        }
    }

    @Test
    public void acceptsHttpInlineAndDefaultsEnabled() {
        PanelEventServiceImpl service = new PanelEventServiceImpl(null);
        PanelEvent e = validHttpEvent();

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.get(PanelPage.class, 1L)).thenReturn(page(1L));

            AjaxResult result = service.add(e, null);

            assertEquals(AjaxResult.SUCCESS, code(result));
            data.verify(() -> DATA.insert(any(PanelEvent.class)));
        }
    }

    @Test
    public void listByPageReturnsOnlyEnabledSortedByPriority() {
        PanelEventServiceImpl service = new PanelEventServiceImpl(null);
        PanelEvent disabled = event(1L, "0", 0);
        PanelEvent late = event(2L, "1", 5);
        PanelEvent early = event(3L, "1", 1);
        PanelEvent otherPage = event(4L, "1", 0);
        otherPage.setPageId(2L);

        try (MockedStatic<DATA> data = mockStatic(DATA.class)) {
            data.when(() -> DATA.select(PanelEvent.class))
                    .thenReturn(Arrays.asList(disabled, late, early, otherPage));

            List<PanelEvent> rows = service.listByPage(1L);

            assertEquals(2, rows.size());
            assertEquals(Long.valueOf(3L), rows.get(0).getEventId());
            assertEquals(Long.valueOf(2L), rows.get(1).getEventId());
        }
    }

    // ===== 夹具 =====

    private static PanelEvent validHttpEvent() {
        PanelEvent e = new PanelEvent();
        e.setPageId(1L);
        e.setName("加载时拉取");
        e.setTrigger(PanelEvent.Trigger.ON_LOAD);
        e.setInlineType(PanelEvent.InlineType.HTTP);
        e.setInlineTarget("http://example.com/api");
        return e;
    }

    private static PanelEvent event(Long id, String enabled, int priority) {
        PanelEvent e = new PanelEvent();
        e.setEventId(id);
        e.setPageId(1L);
        e.setTrigger(PanelEvent.Trigger.ON_CLICK);
        e.setEnabled(enabled);
        e.setPriority(priority);
        e.setActionId(1L);
        return e;
    }

    private static PanelPage page(Long id) {
        PanelPage p = new PanelPage();
        p.setPageId(id);
        return p;
    }

    private static int code(AjaxResult result) {
        return ((Number) result.get("code")).intValue();
    }
}
