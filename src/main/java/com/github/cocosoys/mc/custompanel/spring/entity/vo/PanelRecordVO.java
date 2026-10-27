package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 自由模式记录展示对象：记录管理的列表行与详情返回体（Controller 禁止 Map，一律 VO）。
 */
@Data
public class PanelRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录 ID */
    private Long recordId;

    /** 所属页面 ID */
    private Long pageId;

    /** 业务数据 JSON */
    private String dataJson;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
