package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 托管数据操作请求体（{@code POST/PUT /panel/managed/{entityName}[/{id}]}）。
 *
 * <p>{@code data} 键 = 页面字段 fieldKey，值为字符串（渲染器动态表单产出）；
 * 请求体中的 Map 由 VO 承载（不违反「禁止 Map 手撕」——手撕指 Controller 拼装响应）。</p>
 */
@Data
public class ManagedEntityRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标托管页面 ID（路由前强校验：存在 + MANAGED + entityName 一致） */
    private Long pageId;

    /** 字段数据（键 = fieldKey） */
    private Map<String, String> data;
}
