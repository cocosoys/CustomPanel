package com.github.cocosoys.mc.custompanel.spring.entity;

import com.dlz.db.annotation.IdType;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * 自定义面板自由模式通用行存储（panel_record 表）：任意页面配置的业务数据载体。
 *
 * <p>一条记录 = 某 FREE 自由模式页面（{@link PanelPage#getPageId()}，{@code storeMode=FREE}）下
 * 的一行业务数据。{@code dataJson} 承载任意字段结构（键值对象），实现零代码扩展：
 * 页面的字段声明（{@link PanelField}）只描述表单/列展示，实际数据一律落本表 JSON。</p>
 *
 * <p>MANAGED 托管模式页面不走本表（绑定编译期实体、走 ORM 真实列），
 * 写入前按页面 {@code storeMode} 校验，避免两种模式数据混存。</p>
 */
@TableName("panel_record")
@Data
public class PanelRecord extends BaseEntity {

    /** 记录 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /** 所属页面 ID（必须为 FREE 自由模式页面） */
    private Long pageId;

    /** 业务数据 JSON（任意字段结构，自由模式零代码扩展） */
    private String dataJson;
}
