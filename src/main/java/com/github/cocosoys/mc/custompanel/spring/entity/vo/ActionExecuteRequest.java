package com.github.cocosoys.mc.custompanel.spring.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 动作执行请求（{@code POST /panel/action/execute/{actionId}} 的请求体）。
 *
 * <p>可选携带记录上下文（recordId → dataJson 键值用于动作目标占位符插值）与
 * 额外参数（params 覆盖/补充记录字段，同名优先 params）。Controller 只做参数绑定，
 * 请求体中的 Map 由 VO 承载（不违反「禁止 Map 手撕」——手撕指 Controller 拼装响应）。</p>
 */
@Data
public class ActionExecuteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录上下文（可空）：取该 FREE 记录 dataJson 的键值对参与插值 */
    private Long recordId;

    /** 额外参数（可空）：键值对直接参与插值，同名优先于记录字段 */
    private Map<String, String> params;
}
