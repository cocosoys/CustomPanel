package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 事件展示对象：渲染器按 trigger / targetKey 分组挂载事件。
 */
@Data
public class EventVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long eventId;
    private Long pageId;
    private String name;
    private String trigger;
    private String targetKey;
    private String conditionJson;
    private Long actionId;
    private String inlineType;
    private String inlineTarget;
    private Integer priority;
    private String enabled;
}
