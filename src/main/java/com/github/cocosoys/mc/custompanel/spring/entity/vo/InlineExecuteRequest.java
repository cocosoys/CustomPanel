package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 内联命令执行请求体：设计器事件里直接写的 HTTP / INTERNAL 命令。
 *
 * <p>{@code type} 仅允许 HTTP / INTERNAL（COMMAND 必须绑定已有 action）；
 * {@code params} 为插值上下文（fieldKey → 值），与 {@code execute} 的
 * {@link ActionExecuteRequest#getParams()} 等价，复用同一分派引擎。</p>
 */
@Data
public class InlineExecuteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 内联类型：HTTP / INTERNAL */
    private String type;
    /** 命令模板：HTTP URL 或 INTERNAL 逻辑路径 */
    private String target;
    /** 插值上下文（fieldKey → 值） */
    private Map<String, String> params;
    /** 所属页面 ID（上下文注入用） */
    private Long pageId;
    /** 当前记录 ID（上下文注入用） */
    private Long recordId;
}
