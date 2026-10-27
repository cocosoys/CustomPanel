package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 字段定义展示对象：字段管理的列表行与详情返回体（Controller 禁止 Map，一律 VO）。
 */
@Data
public class FieldDefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字段 ID */
    private Long fieldId;

    /** 所属页面 ID */
    private Long pageId;

    /** 字段键 */
    private String fieldKey;

    /** 字段名称 */
    private String fieldName;

    /** 字段类型 */
    private String fieldType;

    /** 是否必填（0 否 / 1 是） */
    private String required;

    /** 默认值 */
    private String defaultValue;

    /** 下拉选项 */
    private String options;

    /** 列表是否展示（0 否 / 1 是） */
    private String showInList;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
