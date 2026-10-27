package com.github.cocosoys.mc.custompanel.i18n;

import com.github.cocosoys.mc.soyshttpovermc.i18n.I18n;
import com.github.cocosoys.mc.soyshttpovermc.spi.Platforms;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * CustomPanel 国际化门面：桥接主插件 SOYSHTTPOverMC 的 {@link I18n} 机制（与 MCERP 同构）。
 *
 * <p><b>注册</b>：{@link #register(JavaPlugin)} 在 {@code onEnable} 时调用——
 * 把 {@code resources/language/zh_cn.yml} 释放到 {@code plugins/CustomPanel/language/}
 * （已存在不覆盖，保留运维修改），再注册为语言源（绑定 zh_cn，立即重载生效）。</p>
 *
 * <p><b>取值</b>：{@link #t(String, String, Object...)} 委托 {@link I18n#t}，
 * key 统一 {@code custompanel.} 前缀避免与主插件 / MCERP 的键冲突；
 * 未命中时回退 fallback（内联中文原文），天然兜底。</p>
 */
public final class PanelI18n {

    /** 语言源名称（/soyshttp lang sources 展示用） */
    private static final String SOURCE_NAME = "custompanel";
    /** 语言源描述 */
    private static final String SOURCE_DESC = "CustomPanel 消息";
    /** 当前支持的语言（仅中文） */
    private static final String LANGUAGE = "zh_cn";

    private PanelI18n() {
    }

    /**
     * 注册 CustomPanel 语言包到主插件语言资源包（幂等：重复调用仅重复登记同名源）。
     *
     * @param plugin CustomPanel 主类（用于释放语言资源 + 定位语言文件夹）
     */
    public static void register(JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }
        try {
            plugin.saveResource("language/zh_cn.yml", false);
        } catch (IllegalArgumentException e) {
            // jar 内无该资源（异常情况）：跳过释放，后续注册仍可用已存在的文件
        }
        File folder = new File(plugin.getDataFolder(), "language");
        I18n.registerLanguageSource(
                Platforms.getOrNull(), SOURCE_NAME, SOURCE_DESC, LANGUAGE, folder.getAbsolutePath());
    }

    /**
     * 取翻译文本（默认作用域，key 须带 {@code custompanel.} 前缀）；未命中回退 fallback。
     *
     * @param key      i18n 键（如 {@code custompanel.page.not-found}）
     * @param fallback 回退文本（内联中文原文）
     * @param args     {@code {0} {1}...} 占位符参数
     */
    public static String t(String key, String fallback, Object... args) {
        return I18n.t(key, fallback, args);
    }
}
