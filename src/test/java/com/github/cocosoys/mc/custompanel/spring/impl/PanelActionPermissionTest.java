package com.github.cocosoys.mc.custompanel.spring.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.Test;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelAction;

/**
 * 动作级权限三分支（放行 / 拒绝 / 服务不可用），纯 JVM 单元测试。
 */
public class PanelActionPermissionTest {

    @Test
    public void blankPermissionFallsBackToPagePermission() {
        assertEquals(Boolean.TRUE, PanelActionServiceImpl.actionPermissionGranted((String) null, null));
        assertEquals(Boolean.TRUE, PanelActionServiceImpl.actionPermissionGranted("  ", null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void grantedCheckerAllowsAndNormalizesNodeSeparator() {
        Function<String, Boolean> checker = mock(Function.class);
        when(checker.apply("custompanel.action.reset")).thenReturn(Boolean.TRUE);

        assertEquals(Boolean.TRUE,
                PanelActionServiceImpl.actionPermissionGranted("custompanel:action:reset", checker));
        verify(checker).apply("custompanel.action.reset");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deniedCheckerRejects() {
        Function<String, Boolean> checker = mock(Function.class);
        when(checker.apply("custompanel.action.reset")).thenReturn(Boolean.FALSE);

        assertEquals(Boolean.FALSE,
                PanelActionServiceImpl.actionPermissionGranted("custompanel.action.reset", checker));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void unavailablePermissionServiceReturnsNull() {
        assertNull(PanelActionServiceImpl.actionPermissionGranted("custompanel.action.reset", null));

        Function<String, Boolean> broken = mock(Function.class);
        when(broken.apply(anyString())).thenThrow(new IllegalStateException("permission service down"));

        assertNull(PanelActionServiceImpl.actionPermissionGranted("custompanel.action.reset", broken));
    }

    @Test
    public void actionWithoutPermissionInheritsPagePermission() {
        PanelAction action = new PanelAction();
        action.setPermission(null);

        assertEquals(Boolean.TRUE,
                PanelActionServiceImpl.actionPermissionGranted(action.getPermission(), null));
    }
}
