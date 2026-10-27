package com.github.cocosoys.mc.custompanel.spring.entity;

import com.dlz.db.annotation.IdType;
import com.dlz.db.annotation.TableId;
import com.dlz.db.annotation.TableName;
import com.github.cocosoys.mc.soyshttpovermc.spring.entity.BaseEntity;
import lombok.Data;

/**
 * 自定义面板字段定义（panel_field 表）：页面表单与列表列的字段元数据。
 *
 * <p>一条记录 = 所属页面（{@link PanelPage#getPageId()}）的一个字段。
 * 自由模式下字段即通用行存储的键；托管模式下字段需与绑定实体的属性对齐。</p>
 *
 * <p>合法字段类型见 {@link FieldType}。</p>
 */
@TableName("panel_field")
@Data
public class PanelField extends BaseEntity {

    /** 字段 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long fieldId;

    /** 所属页面 ID */
    private Long pageId;

    /** 字段键（英文字段名，如 price） */
    private String fieldKey;

    /** 字段名称（表头 / 表单标签） */
    private String fieldName;

    /** 字段类型（见 {@link FieldType}） */
    private String fieldType;

    /** 是否必填（0 否 / 1 是） */
    private String required;

    /** 默认值 */
    private String defaultValue;

    /** 下拉选项（SELECT 类型用，逗号分隔；其余类型留空） */
    private String options;

    /** 列表是否展示（0 否 / 1 是） */
    private String showInList;

    /** 排序号（升序） */
    private Integer sortOrder;

    /** 备注（落库） */
    private String remark;

    /**
     * 字段类型常量：与 {@link #fieldType} 字段取值一一对应。
     */
    public static final class FieldType {

        /** 单行文本 */
        public static final String STRING = "STRING";
        /** 数字 */
        public static final String NUMBER = "NUMBER";
        /** 布尔 */
        public static final String BOOLEAN = "BOOLEAN";
        /** 日期 */
        public static final String DATE = "DATE";
        /** 多行文本 */
        public static final String TEXT = "TEXT";
        /** 下拉选择 */
        public static final String SELECT = "SELECT";

        private FieldType() {
        }

        /**
         * 判断是否合法字段类型。
         *
         * @param type 待判定取值（可空）
         * @return true=属于内置类型之一
         */
        public static boolean valid(String type) {
            return STRING.equals(type) || NUMBER.equals(type) || BOOLEAN.equals(type)
                    || DATE.equals(type) || TEXT.equals(type) || SELECT.equals(type);
        }
    }
}
