package com.github.cocosoys.mc.custompanel.spring.impl;

import static com.github.cocosoys.mc.custompanel.i18n.PanelI18n.t;

import com.github.cocosoys.mc.custompanel.CustomPanel;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelAction;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.entity.PanelRecord;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.ActionDefVO;
import com.github.cocosoys.mc.custompanel.spring.entity.vo.ActionExecuteRequest;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelActionService;
import com.github.cocosoys.mc.custompanel.util.PanelAuth;
import com.github.cocosoys.mc.soyshttpovermc.HttpOverMcPlugin;
import com.github.cocosoys.mc.soyshttpovermc.api.HttpClientApi;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.JsonReader;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.HttpResponse;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;
import lombok.CustomLog;
import org.bukkit.Bukkit;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 动作定义管理实现：CRUD、所属页面校验、类型合法性校验、按页面级联清理与执行分派。
 *
 * <p>执行分派（{@link #execute}）：COMMAND 经 Bukkit 调度到主线程派发控制台命令
 * （可能操作世界数据）；HTTP 与 INTERNAL 经 SOYS {@link HttpClientApi} 在异步线程执行
 * （复用主插件出站 HTTP 客户端，不自行实现网络栈），避免阻塞网关线程。
 * 动作目标支持 {@code {fieldKey}} 占位符插值，来源为记录 dataJson 与请求参数。</p>
 *
 * <p>动作级权限：{@code panel_action.permission} 非空时，执行前经主插件组合权限服务校验当前凭证
 * 是否持有该节点（端点权限 {@code custompanel:action:execute} 只决定能否调用执行接口，不放宽动作自身约束）；
 * 为空则沿用页面权限。</p>
 *
 * <p>持久化经 SOYS {@link DATA} 门面，本插件不实现存储层。</p>
 */
@CustomLog
public class PanelActionServiceImpl implements IPanelActionService {

    private final CustomPanel plugin;

    public PanelActionServiceImpl(CustomPanel plugin) {
        this.plugin = plugin;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, Long pageId, String actionKey, String actionName) {
        List<PanelAction> all = DATA.select(PanelAction.class);
        List<PanelAction> filtered = new ArrayList<>();
        for (PanelAction a : all) {
            if (pageId != null && !pageId.equals(a.getPageId())) {
                continue;
            }
            if (!contains(a.getActionKey(), actionKey)) {
                continue;
            }
            if (!contains(a.getActionName(), actionName)) {
                continue;
            }
            filtered.add(a);
        }
        filtered.sort(Comparator.comparing(PanelAction::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PanelAction::getActionId, Comparator.nullsLast(Long::compareTo)));
        List<Object> rows = new ArrayList<>();
        for (PanelAction a : filtered) {
            rows.add(toVO(a));
        }
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(Long actionId) {
        PanelAction a = actionId == null ? null : DATA.get(PanelAction.class, actionId);
        if (a == null) {
            return AjaxResult.error(t("custompanel.action.not-found", "动作不存在"));
        }
        return AjaxResult.success(toVO(a));
    }

    @Override
    public AjaxResult add(PanelAction action, CredentialPresentation credential) {
        if (action == null) {
            return AjaxResult.error(t("custompanel.action.key-empty", "动作键不能为空"));
        }
        String key = action.getActionKey() == null ? "" : action.getActionKey().trim();
        String name = action.getActionName() == null ? "" : action.getActionName().trim();
        if (key.isEmpty()) {
            return AjaxResult.error(t("custompanel.action.key-empty", "动作键不能为空"));
        }
        if (name.isEmpty()) {
            return AjaxResult.error(t("custompanel.action.name-empty", "动作名称不能为空"));
        }
        String type = action.getActionType() == null || action.getActionType().trim().isEmpty()
                ? PanelAction.ActionType.COMMAND : action.getActionType().trim().toUpperCase();
        if (!PanelAction.ActionType.valid(type)) {
            return AjaxResult.error(t("custompanel.action.type-invalid", "动作类型非法（COMMAND/HTTP/INTERNAL）"));
        }
        if (action.getPageId() == null || DATA.get(PanelPage.class, action.getPageId()) == null) {
            return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
        }
        // 同页动作键唯一（忽略大小写）：重复会导致按钮冲突与执行歧义
        if (findByKey(action.getPageId(), key, null) != null) {
            return AjaxResult.error(t("custompanel.action.key-exists", "动作键已存在"));
        }
        PanelAction a = new PanelAction();
        a.setPageId(action.getPageId());
        a.setActionKey(key);
        a.setActionName(name);
        a.setActionType(type);
        a.setActionTarget(action.getActionTarget() == null ? "" : action.getActionTarget());
        a.setConfirm(action.getConfirm() == null || action.getConfirm().isEmpty() ? "0" : action.getConfirm());
        a.setPermission(action.getPermission() == null ? "" : action.getPermission());
        a.setSortOrder(action.getSortOrder() == null ? 0 : action.getSortOrder());
        a.setRemark(action.getRemark() == null ? "" : action.getRemark());
        a.setCreateTime(new Date());
        a.setCreateBy(PanelAuth.currentPlayer(credential));
        DATA.insert(a);
        return AjaxResult.success(t("custompanel.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult update(Long actionId, PanelAction action, CredentialPresentation credential) {
        if (actionId == null) {
            return AjaxResult.error(t("custompanel.action.missing-actionId", "缺少 actionId"));
        }
        PanelAction a = DATA.get(PanelAction.class, actionId);
        if (a == null) {
            return AjaxResult.error(t("custompanel.action.not-found", "动作不存在"));
        }
        // 目标页面：更新请求携带新 pageId 则以其为准（须存在），否则沿用当前记录所属页
        Long targetPageId = a.getPageId();
        if (action.getPageId() != null && !action.getPageId().equals(a.getPageId())) {
            if (DATA.get(PanelPage.class, action.getPageId()) == null) {
                return AjaxResult.error(t("custompanel.common.page-not-found", "所属页面不存在"));
            }
            targetPageId = action.getPageId();
        }
        // 目标动作键：请求携带非空 actionKey 则以其为准，否则沿用原键
        boolean keyChanged = action.getActionKey() != null && !action.getActionKey().trim().isEmpty()
                && !action.getActionKey().trim().equals(a.getActionKey());
        String targetKey = keyChanged ? action.getActionKey().trim() : a.getActionKey();
        // 同页动作键唯一（忽略大小写，排除自身）：键或所属页变化都要重新校验，
        // 否则“只换页不改键”可把重复键搬进目标页
        if (keyChanged || !Objects.equals(targetPageId, a.getPageId())) {
            if (findByKey(targetPageId, targetKey, actionId) != null) {
                return AjaxResult.error(t("custompanel.action.key-exists", "动作键已存在"));
            }
        }
        if (keyChanged) {
            a.setActionKey(targetKey);
        }
        if (action.getActionName() != null && !action.getActionName().trim().isEmpty()) {
            a.setActionName(action.getActionName().trim());
        }
        if (action.getActionType() != null && !action.getActionType().trim().isEmpty()) {
            String type = action.getActionType().trim().toUpperCase();
            if (!PanelAction.ActionType.valid(type)) {
                return AjaxResult.error(t("custompanel.action.type-invalid", "动作类型非法（COMMAND/HTTP/INTERNAL）"));
            }
            a.setActionType(type);
        }
        if (action.getActionTarget() != null) {
            a.setActionTarget(action.getActionTarget());
        }
        if (action.getConfirm() != null && !action.getConfirm().isEmpty()) {
            a.setConfirm(action.getConfirm());
        }
        if (action.getPermission() != null) {
            a.setPermission(action.getPermission());
        }
        if (action.getSortOrder() != null) {
            a.setSortOrder(action.getSortOrder());
        }
        if (action.getRemark() != null) {
            a.setRemark(action.getRemark());
        }
        a.setPageId(targetPageId);
        a.setUpdateTime(new Date());
        a.setUpdateBy(PanelAuth.currentPlayer(credential));
        DATA.updateById(a);
        return AjaxResult.success(t("custompanel.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult remove(String actionIds, CredentialPresentation credential) {
        if (actionIds == null || actionIds.trim().isEmpty()) {
            return AjaxResult.error(t("custompanel.action.missing-actionId", "缺少 actionId"));
        }
        for (String id : actionIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            Long aid = parseId(id);
            if (aid != null) {
                DATA.deleteById(PanelAction.class, aid);
            }
        }
        return AjaxResult.success(t("custompanel.common.delete-success", "删除成功"));
    }

    @Override
    public int removeByPageId(Long pageId) {
        if (pageId == null) {
            return 0;
        }
        int n = 0;
        for (PanelAction a : DATA.select(PanelAction.class)) {
            if (pageId.equals(a.getPageId()) && a.getActionId() != null) {
                DATA.deleteById(PanelAction.class, a.getActionId());
                n++;
            }
        }
        return n;
    }

    @Override
    public List<ActionDefVO> listByPageId(Long pageId) {
        if (pageId == null) {
            return new ArrayList<>();
        }
        List<ActionDefVO> rows = new ArrayList<>();
        for (PanelAction a : DATA.select(PanelAction.class)) {
            if (pageId.equals(a.getPageId())) {
                rows.add(toVO(a));
            }
        }
        rows.sort(Comparator.comparing(ActionDefVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ActionDefVO::getActionId, Comparator.nullsLast(Long::compareTo)));
        return rows;
    }

    @Override
    public AjaxResult execute(Long actionId, ActionExecuteRequest request, CredentialPresentation credential) {
        PanelAction a = actionId == null ? null : DATA.get(PanelAction.class, actionId);
        if (a == null) {
            return AjaxResult.error(t("custompanel.action.not-found", "动作不存在"));
        }
        // 动作级权限：配置了 permission 时必须由当前凭证持有（端点权限只决定能否调用执行接口，
        // 不能覆盖动作自身约束）；未配置则沿用页面权限（放行）
        Boolean granted = actionPermissionGranted(a, credential);
        if (granted == null) {
            return AjaxResult.errorT(503, "custompanel.action.permission-service-unavailable",
                    "权限服务不可用，无法校验动作权限");
        }
        if (!granted) {
            return AjaxResult.forbiddenT("custompanel.action.permission-denied", "无执行该动作的权限");
        }
        Map<String, String> context = buildContext(request);
        String type = a.getActionType() == null ? PanelAction.ActionType.COMMAND : a.getActionType();
        if (PanelAction.ActionType.COMMAND.equals(type)) {
            return executeCommand(a, context);
        }
        if (PanelAction.ActionType.HTTP.equals(type)) {
            return executeHttp(a, context);
        }
        if (PanelAction.ActionType.INTERNAL.equals(type)) {
            return executeInternal(a, context);
        }
        return AjaxResult.error(t("custompanel.action.type-invalid", "动作类型非法（COMMAND/HTTP/INTERNAL）"));
    }

    // ===== 执行引擎（分派） =====

    /** 构建插值上下文：请求参数优先，记录 dataJson 键值补充（同名不覆盖）。 */
    private Map<String, String> buildContext(ActionExecuteRequest request) {
        Map<String, String> context = new HashMap<>();
        if (request == null) {
            return context;
        }
        if (request.getParams() != null) {
            for (Map.Entry<String, String> e : request.getParams().entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    context.put(e.getKey(), e.getValue());
                }
            }
        }
        if (request.getRecordId() != null) {
            PanelRecord rec = DATA.get(PanelRecord.class, request.getRecordId());
            if (rec != null && rec.getDataJson() != null && !rec.getDataJson().trim().isEmpty()) {
                try {
                    Map<String, Object> data = JsonReader.parseObject(rec.getDataJson());
                    for (Map.Entry<String, Object> e : data.entrySet()) {
                        if (e.getKey() != null && e.getValue() != null && !context.containsKey(e.getKey())) {
                            context.put(e.getKey(), String.valueOf(e.getValue()));
                        }
                    }
                } catch (Exception ignored) {
                    // 记录 JSON 解析失败不影响动作执行（仅少一个插值来源）
                }
            }
        }
        return context;
    }

    /** 占位符插值：target 中 {@code {key}} ← context.get(key)；未命中保留原样。 */
    private String interpolate(String target, Map<String, String> context) {
        if (target == null || target.isEmpty()) {
            return "";
        }
        String out = target;
        for (Map.Entry<String, String> e : context.entrySet()) {
            String placeholder = "{" + e.getKey() + "}";
            if (out.contains(placeholder)) {
                out = out.replace(placeholder, e.getValue());
            }
        }
        return out;
    }

    /** 解析 "METHOD URL"；无方法前缀默认 GET；非法方法头回退 GET。 */
    private String[] splitMethodTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            return null;
        }
        String t = target.trim();
        int sp = t.indexOf(' ');
        if (sp <= 0) {
            return new String[]{"GET", t};
        }
        String head = t.substring(0, sp).trim().toUpperCase();
        if ("GET".equals(head) || "POST".equals(head) || "PUT".equals(head)
                || "DELETE".equals(head) || "PATCH".equals(head)) {
            return new String[]{head, t.substring(sp + 1).trim()};
        }
        return new String[]{"GET", t};
    }

    /** COMMAND：调度到主线程派发控制台命令（命令可能操作世界数据）。 */
    private AjaxResult executeCommand(PanelAction a, Map<String, String> context) {
        final String cmd = interpolate(a.getActionTarget(), context);
        if (cmd.isEmpty()) {
            return AjaxResult.error(t("custompanel.action.target-empty", "动作目标不能为空"));
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                log.infoT("custompanel.log.action-executed", "动作已执行 -> {0}（COMMAND），命令：{1}，结果：{2}",
                        a.getActionKey(), cmd, ok ? "OK" : "REJECTED");
            } catch (Exception e) {
                log.warnT("custompanel.log.action-execute-failed", "动作执行异常 -> {0}，原因：{1}",
                        a.getActionKey(), String.valueOf(e.getMessage()));
            }
        });
        return AjaxResult.success(t("custompanel.action.execute-queued", "动作已派发执行"));
    }

    /** HTTP：复用 SOYS 出站 HTTP 客户端，异步执行；同步只做协议与字面量环回拦截。 */
    private AjaxResult executeHttp(PanelAction a, Map<String, String> context) {
        String target = interpolate(a.getActionTarget(), context);
        String[] parts = splitMethodTarget(target);
        if (parts == null) {
            return AjaxResult.error(t("custompanel.action.target-empty", "动作目标不能为空"));
        }
        String method = parts[0];
        String url = parts[1];
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            return AjaxResult.error(t("custompanel.action.http-url-invalid", "HTTP 动作目标必须是 http/https URL"));
        }
        if (isLiteralLoopback(url)) {
            return AjaxResult.error(t("custompanel.action.http-url-blocked", "HTTP 动作目标被安全策略拒绝（环回地址）"));
        }
        final String fMethod = method, fUrl = url;
        CompletableFuture.runAsync(() -> {
            try {
                // 异步阶段做 DNS 解析级 SSRF 防护（域名指向环回/内网段一律拒绝）
                if (isPrivateResolvedHost(fUrl)) {
                    log.warnT("custompanel.log.action-execute-failed", "动作执行拒绝（SSRF 防护）-> {0}，URL：{1}",
                            a.getActionKey(), fUrl);
                    return;
                }
                HttpClientApi client = httpClient();
                HttpResponse resp = client == null ? null : client.sendHttp(fMethod, fUrl, null, null);
                log.infoT("custompanel.log.action-executed", "动作已执行 -> {0}（HTTP），URL：{1}，状态：{2}",
                        a.getActionKey(), fUrl, resp == null ? "CLIENT-NA" : String.valueOf(resp.getStatus()));
            } catch (Exception e) {
                log.warnT("custompanel.log.action-execute-failed", "动作执行异常 -> {0}，原因：{1}",
                        a.getActionKey(), String.valueOf(e.getMessage()));
            }
        });
        return AjaxResult.success(t("custompanel.action.execute-queued", "动作已派发执行"));
    }

    /** INTERNAL：本地分发调用本插件已注册 API（绕网络，注解权限照常生效）。 */
    private AjaxResult executeInternal(PanelAction a, Map<String, String> context) {
        String target = interpolate(a.getActionTarget(), context);
        String[] parts = splitMethodTarget(target);
        if (parts == null) {
            return AjaxResult.error(t("custompanel.action.target-empty", "动作目标不能为空"));
        }
        String method = parts[0];
        String path = parts[1];
        if (!path.startsWith("/")) {
            return AjaxResult.error(t("custompanel.action.internal-path-invalid", "INTERNAL 动作目标必须是 / 开头的逻辑路径"));
        }
        final String fMethod = method, fPath = path;
        CompletableFuture.runAsync(() -> {
            try {
                HttpClientApi client = httpClient();
                Object result = client == null ? null : client.sendApi(fMethod, fPath, null, null);
                log.infoT("custompanel.log.action-executed", "动作已执行 -> {0}（INTERNAL），路径：{1}，结果：{2}",
                        a.getActionKey(), fPath, result == null ? "null" : String.valueOf(result));
            } catch (Exception e) {
                log.warnT("custompanel.log.action-execute-failed", "动作执行异常 -> {0}，原因：{1}",
                        a.getActionKey(), String.valueOf(e.getMessage()));
            }
        });
        return AjaxResult.success(t("custompanel.action.execute-queued", "动作已派发执行"));
    }

    /** SOYS 出站 HTTP 客户端（主插件未就绪时返回 null，调用方按空处理）。 */
    private HttpClientApi httpClient() {
        if (HttpOverMcPlugin.getInstance() == null) {
            return null;
        }
        com.github.cocosoys.mc.soyshttpovermc.api.SoysHttpOverMcApi api = HttpOverMcPlugin.getInstance().getApi();
        return api == null ? null : api.getHttpClient();
    }

    /** 字面量环回 host（同步快速拦截）：localhost / 127.0.0.1 / 0.0.0.0 / ::1。 */
    private boolean isLiteralLoopback(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return true;
            }
            String h = host.toLowerCase();
            return "localhost".equals(h) || "127.0.0.1".equals(h)
                    || "0.0.0.0".equals(h) || "::1".equals(h) || "[::1]".equals(h);
        } catch (Exception e) {
            return true;
        }
    }

    /** DNS 解析级 SSRF 防护（异步阶段）：域名解析为环回 / 链路本地 / 私网段（10/172.16-31/192.168/169.254）一律拒绝。 */
    private boolean isPrivateResolvedHost(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return true;
            }
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                return true;
            }
            byte[] b = addr.getAddress();
            if (b.length == 4) {
                int first = b[0] & 0xFF;
                if (first == 10 || first == 192 || first == 169
                        || (first == 172 && (b[1] & 0xFF) >= 16 && (b[1] & 0xFF) <= 31)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true; // 解析失败一律拒绝
        }
    }

    // ===== 内部 =====

    /**
     * 动作级权限判定：{@code permission} 为空视为沿用页面权限（直接放行）；非空则经主插件组合权限服务
     * 判定当前凭证（会话令牌 / X-API-Key）是否持有该节点。节点按 SOYS 约定归一（{@code ':' ≡ '.'}），
     * 支持全量 {@code *} 与段级尾通配。
     *
     * @param a          动作定义
     * @param credential 请求凭证
     * @return true=有权限；false=无权限；null=主插件权限服务不可用（调用方按失败关闭处理）
     */
    private static Boolean actionPermissionGranted(PanelAction a, CredentialPresentation credential) {
        String node = a.getPermission() == null ? "" : a.getPermission().trim();
        if (node.isEmpty()) {
            return Boolean.TRUE;
        }
        HttpOverMcPlugin soys = HttpOverMcPlugin.getInstance();
        if (soys == null || soys.getCombinedPermissionService() == null) {
            return null;
        }
        return soys.getCombinedPermissionService().hasPermission(credential, node.replace(':', '.'));
    }

    /** 同页动作键查重（忽略大小写）；excludeActionId 非空时排除该动作（修改场景排除自身），命中返回记录。 */
    private PanelAction findByKey(Long pageId, String key, Long excludeActionId) {
        for (PanelAction a : DATA.select(PanelAction.class)) {
            if (pageId != null && pageId.equals(a.getPageId())
                    && a.getActionKey() != null && a.getActionKey().equalsIgnoreCase(key)
                    && (excludeActionId == null || !excludeActionId.equals(a.getActionId()))) {
                return a;
            }
        }
        return null;
    }

    /** 实体 → 展示 VO。 */
    private ActionDefVO toVO(PanelAction a) {
        ActionDefVO vo = new ActionDefVO();
        vo.setActionId(a.getActionId());
        vo.setPageId(a.getPageId());
        vo.setActionKey(a.getActionKey());
        vo.setActionName(a.getActionName());
        vo.setActionType(a.getActionType());
        vo.setActionTarget(a.getActionTarget());
        vo.setConfirm(a.getConfirm());
        vo.setPermission(a.getPermission());
        vo.setSortOrder(a.getSortOrder());
        vo.setRemark(a.getRemark());
        vo.setCreateTime(a.getCreateTime());
        vo.setUpdateTime(a.getUpdateTime());
        return vo;
    }

    private static boolean contains(String source, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    private static Long parseId(String id) {
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
