package com.github.cocosoys.mc.custompanel.spring.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.cocosoys.mc.custompanel.spring.entity.vo.InlineExecuteRequest;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

/**
 * 内联命令安全边界（不依赖 Spigot 运行时）：
 * 白名单仅 HTTP / INTERNAL；COMMAND、超长模板、环回 URL 一律拒绝，且均不进入异步分派。
 */
public class PanelActionInlineTest {

    private final PanelActionServiceImpl service = new PanelActionServiceImpl(null);

    @Test
    public void inlineTypeWhitelistExcludesCommand() {
        assertTrue(PanelActionServiceImpl.INLINE_TYPE.valid("HTTP"));
        assertTrue(PanelActionServiceImpl.INLINE_TYPE.valid("INTERNAL"));
        assertFalse("COMMAND 必须绑定已有 action", PanelActionServiceImpl.INLINE_TYPE.valid("COMMAND"));
        assertFalse(PanelActionServiceImpl.INLINE_TYPE.valid(""));
        assertFalse(PanelActionServiceImpl.INLINE_TYPE.valid(null));
    }

    @Test
    public void rejectsCommandInline() {
        InlineExecuteRequest req = request("COMMAND", "say hi");

        AjaxResult result = service.executeInline(req, null);

        assertEquals(AjaxResult.ERROR, code(result));
    }

    @Test
    public void rejectsEmptyTarget() {
        InlineExecuteRequest req = request("HTTP", "   ");

        AjaxResult result = service.executeInline(req, null);

        assertEquals(AjaxResult.ERROR, code(result));
    }

    @Test
    public void rejectsTargetOverDefaultByteLimit() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append('a');
        }
        InlineExecuteRequest req = request("INTERNAL", sb.toString());

        AjaxResult result = service.executeInline(req, null);

        assertEquals(AjaxResult.ERROR, code(result));
    }

    @Test
    public void rejectsLiteralLoopbackUrlBeforeAsyncDispatch() {
        InlineExecuteRequest req = request("HTTP", "http://127.0.0.1/api");

        AjaxResult result = service.executeInline(req, null);

        assertEquals(AjaxResult.ERROR, code(result));
    }

    // ===== 夹具 =====

    private static InlineExecuteRequest request(String type, String target) {
        InlineExecuteRequest r = new InlineExecuteRequest();
        r.setType(type);
        r.setTarget(target);
        return r;
    }

    private static int code(AjaxResult result) {
        return ((Number) result.get("code")).intValue();
    }
}
