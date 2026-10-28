package com.github.cocosoys.mc.custompanel.managed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MANAGED 实体处理器注册表（线程安全）：entityName → {@link ManagedEntityHandler}。
 *
 * <p>CustomPanel 内部持有单例注册表；业务插件经
 * {@code CustomPanel.getInstance().registerManagedEntity(handler)} 注册，
 * 页面定义校验（entityName 必须已注册）与托管数据路由均查本表。</p>
 */
public class ManagedEntityRegistry {

    private final Map<String, ManagedEntityHandler> handlers = new LinkedHashMap<>();

    /**
     * 注册处理器（幂等：同名 entityName 覆盖为新实现）。
     *
     * @param handler 处理器（entityName 为空则忽略）
     */
    public synchronized void register(ManagedEntityHandler handler) {
        if (handler == null || handler.entityName() == null || handler.entityName().isEmpty()) {
            return;
        }
        handlers.put(handler.entityName(), handler);
    }

    /** 按注册名取处理器；未注册返回 null。 */
    public synchronized ManagedEntityHandler get(String entityName) {
        return entityName == null ? null : handlers.get(entityName);
    }

    /** 是否已注册指定实体名。 */
    public synchronized boolean contains(String entityName) {
        return get(entityName) != null;
    }

    /** 已注册实体名列表（保持注册顺序）。 */
    public synchronized List<String> names() {
        return new ArrayList<>(handlers.keySet());
    }
}
