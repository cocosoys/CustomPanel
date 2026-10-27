package com.github.cocosoys.mc.custompanel.log;

import com.github.cocosoys.mc.soyshttpovermc.log.LogKit;

/**
 * CustomPanel 日志门面：继承 SOYS {@link LogKit}（统一经其全局级别过滤与热重载打印），
 * 定制本插件前缀 {@code [CustomPanel]}。
 *
 * <p>作为 Lombok 自定义日志的工厂（见项目根 {@code lombok.config}）：
 * 使用类标注 {@code @CustomLog} 即生成 {@code static final PanelLogKit log}，
 * 可调用 {@code log.infoT(key, fallback, args...)} 等 i18n 变体方法。</p>
 */
public class PanelLogKit extends LogKit {

    public PanelLogKit(String prefix, Class<?> sourceClass) {
        super(prefix, sourceClass);
    }

    /**
     * Lombok 自定义日志工厂（无 topic 重载）：前缀 {@code [CustomPanel]}。
     */
    public static PanelLogKit getLogger(Class<?> clazz) {
        return new PanelLogKit("[CustomPanel]", clazz);
    }

    /**
     * Lombok 自定义日志工厂（带 topic 重载）：前缀 {@code [topic]}，用于多主题细分。
     */
    public static PanelLogKit getLogger(Class<?> clazz, String topic) {
        return new PanelLogKit("[" + topic + "]", clazz);
    }
}
