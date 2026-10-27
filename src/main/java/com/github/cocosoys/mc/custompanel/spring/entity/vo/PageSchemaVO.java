package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 页面定义聚合展示对象（Schema）：前端渲染器一次拉取整页定义。
 *
 * <p>{@code GET /api/plugins/CustomPanel/panel/page/schema/{pageId}} 的返回体——
 * 页面自身 + 其下全部字段定义 + 全部动作定义，一次请求即可驱动一个配置页面的
 * 列表 / 表单 / 按钮渲染，避免前端多次往返。</p>
 */
@Data
public class PageSchemaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页面定义 */
    private PageDefVO page;

    /** 字段定义列表（按 sortOrder 升序） */
    private List<FieldDefVO> fields;

    /** 动作定义列表（按 sortOrder 升序） */
    private List<ActionDefVO> actions;
}
