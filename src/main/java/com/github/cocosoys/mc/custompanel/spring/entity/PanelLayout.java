package com.github.cocosoys.mc.custompanel.spring.entity;

import com.dlz.db.annotation.IdType;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * 页面设计器布局定义（panel_layout 表）：confJson 单源。
 *
 * <p>一条记录 = 一个页面的一个版本布局。{@code formConfJson} 存 RuoYi 表单全局配置，
 * {@code fieldsJson} 存控件数组（RuoYi generator 的 fields[] 契约），二者共同构成
 * 页面字段与布局的<b>唯一真源</b>——旧 {@code panel_field} 表自 P1 起停写，仅作迁移来源。</p>
 *
 * <p>{@code status=PUBLISHED} 为唯一生效版本；发布新版本时旧版本自动置 DRAFT。
 * 审计字段继承 {@link BaseEntity}；{@code remark} 显式声明以落库。</p>
 */
@TableName("panel_layout")
@Data
public class PanelLayout extends BaseEntity {

    /** 状态常量：与 {@link #status} 取值一一对应。 */
    public static final class Status {
        public static final String DRAFT = "DRAFT";
        public static final String PUBLISHED = "PUBLISHED";

        private Status() {
        }

        public static boolean valid(String status) {
            return DRAFT.equals(status) || PUBLISHED.equals(status);
        }
    }

    /** 布局 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long layoutId;

    /** 所属页面 ID */
    private Long pageId;

    /** RuoYi formConf 全局配置（JSON 字符串） */
    private String formConfJson;

    /** RuoYi fields[] 控件数组（JSON 字符串，字段定义唯一真源） */
    private String fieldsJson;

    /** 版本号（同页面内递增） */
    private Integer version;

    /** 状态：DRAFT / PUBLISHED（见 {@link Status}） */
    private String status;

    /** 备注（落库） */
    private String remark;
}
