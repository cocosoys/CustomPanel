package com.github.cocosoys.mc.custompanel.spring.entity;

import com.dlz.db.annotation.IdType;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * 自定义面板页面定义（panel_page 表）：配置驱动的元数据载体。
 *
 * <p>一条记录 = 侧边栏一个页面。字段声明（列定义/数据源/按钮）由 {@code PanelField}
 * 与 {@code PanelAction} 另行承载，本表只描述页面自身。</p>
 *
 * <p><b>存储模式 {@code storeMode}</b>（混合落库方案）：
 * <ul>
 *   <li>{@code MANAGED} 托管模式：绑定编译期已有实体（{@code entityName}），
 *       走 ORM 真实列，具备条件查询与索引能力；</li>
 *   <li>{@code FREE} 自由模式：走通用行存储 {@link PanelRecord}，
 *       任意字段结构均可存，零代码扩展。</li>
 * </ul>
 * 常量的合法值见 {@link StoreMode}。</p>
 *
 * <p>审计字段（createTime/updateTime/params）继承 {@link BaseEntity}；
 * {@code remark} 在此显式声明以落库（父类同名字段为 {@code exist=false}）。</p>
 */
@TableName("panel_page")
@Data
public class PanelPage extends BaseEntity {

    /** 页面 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long pageId;

    /** 页面标识（唯一，英文键，如 shop） */
    private String pageKey;

    /** 页面名称（侧边栏显示名） */
    private String pageName;

    /** 侧边栏图标 */
    private String icon;

    /** 存储模式：MANAGED 托管 / FREE 自由（见 {@link StoreMode}） */
    private String storeMode;

    /** 托管模式绑定的实体类名（自由模式为空） */
    private String entityName;

    /** 排序号（升序） */
    private Integer orderNum;

    /** 显示状态（0 显示 / 1 隐藏） */
    private String visible;

    /** 备注（落库） */
    private String remark;

    /**
     * 存储模式常量：与 {@link #storeMode} 字段取值一一对应。
     */
    public static final class StoreMode {

        /** 托管模式：绑定编译期已有实体。 */
        public static final String MANAGED = "MANAGED";

        /** 自由模式：通用行存储。 */
        public static final String FREE = "FREE";

        private StoreMode() {
        }

        /**
         * 判断是否合法存储模式。
         *
         * @param mode 待判定取值（可空）
         * @return true=MANAGED 或 FREE
         */
        public static boolean valid(String mode) {
            return MANAGED.equals(mode) || FREE.equals(mode);
        }
    }
}
