package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 动作定义展示对象：动作管理的列表行与详情返回体（Controller 禁止 Map，一律 VO）。
 */
@Data
public class ActionDefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 动作 ID */
    private Long actionId;

    /** 所属页面 ID */
    private Long pageId;

    /** 动作键 */
    private String actionKey;

    /** 动作名称 */
    private String actionName;

    /** 动作类型 */
    private String actionType;

    /** 动作目标 */
    private String actionTarget;

    /** 是否需要二次确认（0 否 / 1 是） */
    private String confirm;

    /** 触发所需权限标识 */
    private String permission;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
