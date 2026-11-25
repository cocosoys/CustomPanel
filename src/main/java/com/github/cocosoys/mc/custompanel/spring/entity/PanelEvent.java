package com.github.cocosoys.mc.custompanel.spring.entity;

import com.dlz.db.annotation.IdType;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * 页面事件绑定定义（panel_event 表）：把触发时机映射到动作。
 *
 * <p>一条记录 = 某页面的一个事件规则：{@code trigger} 决定何时触发，
 * {@code actionId}/{@code inlineType} 决定触发什么。内联仅允许 HTTP / INTERNAL；
 * COMMAND 必须绑定已有 action（安全边界，见 {@link InlineType}）。</p>
 *
 * <p>审计字段继承 {@link BaseEntity}。</p>
 */
@TableName("panel_event")
@Data
public class PanelEvent extends BaseEntity {

    /** 事件 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long eventId;

    /** 所属页面 ID */
    private Long pageId;

    /** 事件名称（设计器里便于识别） */
    private String name;

    /** 触发时机（见 {@link Trigger}） */
    private String trigger;

    /** 目标键：字段 vModel 或按钮标识；页面级事件留空 */
    private String targetKey;

    /** 执行条件 JSON（受限 DSL），空 = 无条件 */
    private String conditionJson;

    /** 绑定的已有动作 ID（COMMAND 唯一来源） */
    private Long actionId;

    /** 内联类型：仅 HTTP / INTERNAL（见 {@link InlineType}） */
    private String inlineType;

    /** 内联命令模板，支持 {fieldKey} 占位符 */
    private String inlineTarget;

    /** 同触发点多事件的执行顺序（升序） */
    private Integer priority;

    /** 是否启用（0 否 / 1 是） */
    private String enabled;

    /** 触发时机常量。 */
    public static final class Trigger {
        public static final String ON_LOAD = "onLoad";
        public static final String ON_CHANGE = "onChange";
        public static final String ON_CLICK = "onClick";
        public static final String BEFORE_SUBMIT = "beforeSubmit";
        public static final String AFTER_SUBMIT = "afterSubmit";

        private Trigger() {
        }

        public static boolean valid(String t) {
            return ON_LOAD.equals(t) || ON_CHANGE.equals(t) || ON_CLICK.equals(t)
                    || BEFORE_SUBMIT.equals(t) || AFTER_SUBMIT.equals(t);
        }
    }

    /** 内联类型常量：COMMAND 不在其列（必须绑定已有 action）。 */
    public static final class InlineType {
        public static final String HTTP = "HTTP";
        public static final String INTERNAL = "INTERNAL";

        private InlineType() {
        }

        public static boolean valid(String t) {
            return HTTP.equals(t) || INTERNAL.equals(t);
        }
    }
}
