package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 布局展示对象：设计器与渲染器读取的页面布局契约。
 *
 * <p>由 {@code PanelLayout} 实体投影而来，{@code formConfJson}/{@code fieldsJson}
 * 原样下发（前端按 RuoYi generator 契约解析），避免后端 hand-written Map。</p>
 */
@Data
public class LayoutVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long layoutId;
    private Long pageId;
    private String formConfJson;
    private String fieldsJson;
    private Integer version;
    private String status;
}
