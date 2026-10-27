package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 页面定义展示对象：页面管理的列表行与详情返回体。
 *
 * <p>Controller 禁止以 Map 承载数据，一律使用本 VO；
 * 由 {@code PanelPageServiceImpl} 从实体投影而来。</p>
 */
@Data
public class PageDefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页面 ID */
    private Long pageId;

    /** 页面标识 */
    private String pageKey;

    /** 页面名称 */
    private String pageName;

    /** 侧边栏图标 */
    private String icon;

    /** 存储模式（MANAGED / FREE） */
    private String storeMode;

    /** 托管模式绑定的实体类名 */
    private String entityName;

    /** 排序号 */
    private Integer orderNum;

    /** 显示状态（0 显示 / 1 隐藏） */
    private String visible;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
