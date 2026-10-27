package com.github.cocosoys.mc.custompanel.util;

import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 凭证解析工具：从请求凭证取当前玩家名（审计字段用）。
 *
 * <p>{@link CredentialPresentation} 本身只携带 apiKey / bearer / basic / cookies，
 * 不含主体信息，需经主插件权限服务反查：</p>
 * <pre>
 *   HttpOverMcPlugin.getInstance().getCombinedPermissionService().subjectOf(credential)
 * </pre>
 * 与 MCERP 的 {@code AuthServiceImpl#currentPlayer} 同款实现。
 */
public final class PanelAuth {

    private PanelAuth() {
    }

    /**
     * 解析当前请求玩家名。
     *
     * @param credential 请求凭证（可空）
     * @return 玩家名；未携带凭证或主插件不可用时返回 null
     */
    public static String currentPlayer(CredentialPresentation credential) {
        if (credential == null || !credential.hasAnyCredential()) {
            return null;
        }
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        if (soys == null || soys.getCombinedPermissionService() == null) {
            return null;
        }
        return soys.getCombinedPermissionService().subjectOf(credential);
    }
}
