package com.github.cocosoys.mc.custompanel;

import com.github.cocosoys.mc.custompanel.i18n.PanelI18n;
import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import com.github.cocosoys.mc.soyshttpovermc.api.event.SoysReadyEvent;
import lombok.CustomLog;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CustomPanel —— 完全自定义的服务器控制面板（Spigot 1.12.2）。
 *
 * <ul>
 *   <li>硬依赖 SOYSHTTPOverMC：HTTP-over-MC 网关、注解式 API、页面托管、ORM 均由其提供；</li>
 *   <li>硬依赖 MCERP：作为「纯 MCERP 模块」接入统一控制台，菜单与路由由其中控合成；</li>
 *   <li>业务分层严格遵循 spring 写法：{@code spring/entity}、{@code spring/entity/vo}、
 *       {@code spring/service}（接口）、{@code spring/impl}（实现）、{@code spring/controller}
 *       （零逻辑，一行委托）；</li>
 *   <li>存储层不自行实现：统一经 SOYS {@code DATA} 门面（MySQL &gt; SQLite &gt; YAML 自动路由）。</li>
 * </ul>
 *
 * <p>启用时序：{@code depend} 已保证 SOYSHTTPOverMC 与 MCERP 先于本插件启用，
 * 故 onEnable 可立即登记；仍监听 {@link SoysReadyEvent} 作为主插件 reload 后的兜底重登记。</p>
 */
@CustomLog
@Getter
public final class CustomPanel extends JavaPlugin implements Listener {

    private @Getter static CustomPanel instance;

    private PanelExpansion expansion;

    @Override
    public void onEnable() {
        instance = this;
        PanelI18n.register(this);
        getServer().getPluginManager().registerEvents(this, this);
        // SOYS 可能已先启用（SoysReadyEvent 在本监听器注册前已广播）→ 就绪则直接登记
        if (HttpOverMcPlugin.getInstance() != null) {
            registerSoys();
        } else {
            log.infoT("custompanel.log.soys-not-ready", "SOYSHTTPOverMC 未就绪，等待 SoysReadyEvent 兜底注册");
        }
        log.infoT("custompanel.log.plugin-enabled", "已启用（自定义控制面板）");
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.unregister();
        }
        log.infoT("custompanel.log.plugin-disabled", "已禁用");
    }

    /** SOYS 就绪事件兜底：主插件 reload 重建门面后重新登记。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSoysReady(SoysReadyEvent event) {
        registerSoys();
    }

    /**
     * 登记面板扩展（幂等）：已登记则跳过；失败置空允许后续事件重试。
     */
    private void registerSoys() {
        if (expansion != null) {
            return;
        }
        expansion = new PanelExpansion(this);
        if (!expansion.register()) {
            log.warnT("custompanel.log.expansion-register-fail",
                    "面板扩展注册失败（identifier 冲突 / SOYS 未就绪）: {0}", expansion.getIdentifier());
            expansion = null;
            return;
        }
        log.infoT("custompanel.log.expansion-registered", "面板扩展注册完成: {0}", expansion.getIdentifier());
    }
}
