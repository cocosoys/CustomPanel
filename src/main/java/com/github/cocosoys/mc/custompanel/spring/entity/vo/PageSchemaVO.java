package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 页面定义聚合展示对象（Schema）：前端渲染器一次拉取整页定义。
 *
 * <p>{@code GET /api/plugins/CustomPanel/panel/page/schema/{pageId}} 的返回体——
 * 页面自身 + 布局（confJson 单源） + 由布局派生的字段 + 事件 + 动作定义，
 * 一次请求即可驱动设计器画布与渲染页，避免前端多次往返。</p>
 *
 * <p>P1 起 {@code fields} 由 {@code layout.fieldsJson} 派生，旧 {@code panel_field}
 * 仅作迁移来源；{@code events} 仅下发 enabled=1 事件。</p>
 */
@Data
public class PageSchemaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页面定义 */
    private PageDefVO page;

    /** 当前生效布局（confJson 单源，DRAFT 不下发）；无布局时 null */
    private LayoutVO layout;

    /** 字段定义列表（由 layout.fieldsJson 派生，按 sortOrder 升序） */
    private List<FieldDefVO> fields;

    /** 事件绑定列表（仅 enabled=1，按 priority 升序） */
    private List<EventVO> events;

    /** 动作定义列表（按 sortOrder 升序） */
    private List<ActionDefVO> actions;
}
