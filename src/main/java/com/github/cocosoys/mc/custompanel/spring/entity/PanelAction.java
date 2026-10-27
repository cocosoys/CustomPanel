package com.github.cocosoys.mc.custompanel.spring.entity;

import com.dlz.db.annotation.IdType;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * 自定义面板动作定义（panel_action 表）：页面按钮触发的动作元数据。
 *
 * <p>一条记录 = 所属页面（{@link PanelPage#getPageId()}）的一个可触发动作，
 * 与 Controller 上的 {@code ControllerPermScanner} 自动按钮互为补充：
 * 前者面向内置端点，本表面向「服主自己在配置里声明的动作」。</p>
 *
 * <p>合法动作类型见 {@link ActionType}。本批次只落地动作定义的 CRUD，
 * 执行引擎（命令派发 / HTTP 回调）在后续批次接入。</p>
 */
@TableName("panel_action")
@Data
public class PanelAction extends BaseEntity {

    /** 动作 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long actionId;

    /** 所属页面 ID */
    private Long pageId;

    /** 动作键（英文标识，如 reset） */
    private String actionKey;

    /** 动作名称（按钮文案） */
    private String actionName;

    /** 动作类型（见 {@link ActionType}） */
    private String actionType;

    /** 动作目标（命令串 / URL / 内部处理器名） */
    private String actionTarget;

    /** 是否需要二次确认（0 否 / 1 是） */
    private String confirm;

    /** 触发所需权限标识（可空 = 沿用页面权限） */
    private String permission;

    /** 排序号（升序） */
    private Integer sortOrder;

    /** 备注（落库） */
    private String remark;

    /**
     * 动作类型常量：与 {@link #actionType} 字段取值一一对应。
     */
    public static final class ActionType {

        /** 执行后台命令 */
        public static final String COMMAND = "COMMAND";
        /** 回调外部 HTTP */
        public static final String HTTP = "HTTP";
        /** 调用插件内部处理器 */
        public static final String INTERNAL = "INTERNAL";

        private ActionType() {
        }

        /**
         * 判断是否合法动作类型。
         *
         * @param type 待判定取值（可空）
         * @return true=属于内置类型之一
         */
        public static boolean valid(String type) {
            return COMMAND.equals(type) || HTTP.equals(type) || INTERNAL.equals(type);
        }
    }
}
