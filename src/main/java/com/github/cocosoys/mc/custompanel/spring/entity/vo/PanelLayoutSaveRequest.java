package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 布局保存请求体：设计器画布产出的一次快照。
 *
 * <p>{@code save} 落 DRAFT 草稿，{@code publish} 把指定版本置 PUBLISHED。
 * 字段数 / confJson 大小由 config.yml 上限校验，代码不硬编码。</p>
 */
@Data
public class PanelLayoutSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 所属页面 ID */
    private Long pageId;
    /** RuoYi formConf 全局配置（JSON 字符串） */
    private String formConfJson;
    /** RuoYi fields[] 控件数组（JSON 字符串） */
    private String fieldsJson;
    /** 备注 */
    private String remark;
}
