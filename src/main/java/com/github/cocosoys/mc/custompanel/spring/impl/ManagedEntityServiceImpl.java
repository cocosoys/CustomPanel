package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.managed.ManagedEntityHandler;
import com.github.cocosoys.mc.custompanel.managed.ManagedEntityRegistry;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.service.IManagedEntityService;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;

import java.util.Map;

/**
 * 托管实体（MANAGED）数据通道实现：路由与前置校验。
 *
 * <p>前置校验（任何托管操作前）：实体已注册 → 目标页面存在 → 页面为 MANAGED →
 * 页面 entityName 与请求实体一致。任一不满足即拒绝，不触碰处理器。</p>
 */
@CustomLog
public class ManagedEntityServiceImpl implements IManagedEntityService {

    private final ManagedEntityRegistry registry;

    public ManagedEntityServiceImpl(ManagedEntityRegistry registry) {
        this.registry = registry;
    }

    @Override
    public AjaxResult entities(CredentialPresentation credential) {
        return AjaxResult.success(registry.names());
    }

    @Override
    public TableDataInfo list(String entityName, Long pageId, Integer pageNum, Integer pageSize,
                              CredentialPresentation credential) {
        ManagedEntityHandler h = requireHandler(entityName);
        if (h == null) {
            return TableDataInfo.error(t("custompanel.managed.entity-not-found", "托管实体未注册：{0}", entityName));
        }
        PanelPage p = requireManagedPage(pageId, entityName);
        if (p == null) {
            return TableDataInfo.error(t("custompanel.managed.page-not-managed",
                    "目标页面不存在或非托管模式/实体不匹配（pageId={0}, entity={1}）", pageId, entityName));
        }
        return h.list(credential, pageId, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(String entityName, Long pageId, Long id, CredentialPresentation credential) {
        ManagedEntityHandler h = requireHandler(entityName);
        if (h == null) {
            return errEntity(entityName);
        }
        if (requireManagedPage(pageId, entityName) == null) {
            return errPage(pageId, entityName);
        }
        return h.detail(credential, pageId, id);
    }

    @Override
    public AjaxResult add(String entityName, Long pageId, Map<String, String> data,
                          CredentialPresentation credential) {
        ManagedEntityHandler h = requireHandler(entityName);
        if (h == null) {
            return errEntity(entityName);
        }
        if (requireManagedPage(pageId, entityName) == null) {
            return errPage(pageId, entityName);
        }
        return h.add(credential, pageId, data);
    }

    @Override
    public AjaxResult update(String entityName, Long pageId, Long id, Map<String, String> data,
                             CredentialPresentation credential) {
        ManagedEntityHandler h = requireHandler(entityName);
        if (h == null) {
            return errEntity(entityName);
        }
        if (requireManagedPage(pageId, entityName) == null) {
            return errPage(pageId, entityName);
        }
        return h.update(credential, pageId, id, data);
    }

    @Override
    public AjaxResult remove(String entityName, Long pageId, String ids, CredentialPresentation credential) {
        ManagedEntityHandler h = requireHandler(entityName);
        if (h == null) {
            return errEntity(entityName);
        }
        if (requireManagedPage(pageId, entityName) == null) {
            return errPage(pageId, entityName);
        }
        return h.remove(credential, pageId, ids);
    }

    // ===== 内部 =====

    private ManagedEntityHandler requireHandler(String entityName) {
        return entityName == null ? null : registry.get(entityName);
    }

    /** 校验页面：存在 + MANAGED + entityName 与请求一致。 */
    private PanelPage requireManagedPage(Long pageId, String entityName) {
        if (pageId == null) {
            return null;
        }
        PanelPage p = DATA.get(PanelPage.class, pageId);
        if (p == null || !PanelPage.StoreMode.MANAGED.equals(p.getStoreMode())) {
            return null;
        }
        return entityName != null && entityName.equals(p.getEntityName()) ? p : null;
    }

    private static AjaxResult errEntity(String entityName) {
        return AjaxResult.error(t("custompanel.managed.entity-not-found", "托管实体未注册：{0}", entityName));
    }

    private static AjaxResult errPage(Long pageId, String entityName) {
        return AjaxResult.error(t("custompanel.managed.page-not-managed",
                "目标页面不存在或非托管模式/实体不匹配（pageId={0}, entity={1}）", pageId, entityName));
    }
}
