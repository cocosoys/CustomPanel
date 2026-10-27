package com.github.cocosoys.mc.custompanel.spring.controller;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelAction;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelActionService;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 动作定义管理控制器：路由 {@code /api/plugins/CustomPanel/panel/action/*}。
 *
 * <p>只做参数绑定与权限声明，方法体一律一行委托 {@link IPanelActionService}。</p>
 */
@RequestMapping("/panel/action")
public class PanelActionController {

    private final IPanelActionService actionService;

    public PanelActionController(IPanelActionService actionService) {
        this.actionService = actionService;
    }

    @ApiName("动作列表")
    @ApiPermission("custompanel:action:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "pageId", required = false) Long pageId,
                              @RequestParam(name = "actionKey", required = false) String actionKey,
                              @RequestParam(name = "actionName", required = false) String actionName) {
        return actionService.list(pageNum, pageSize, pageId, actionKey, actionName);
    }

    @ApiName("动作详情")
    @ApiPermission("custompanel:action:query")
    @GetMapping("/{actionId}")
    public AjaxResult detail(@PathVariable(name = "actionId") Long actionId) {
        return actionService.detail(actionId);
    }

    @ApiName("新增动作")
    @ApiPermission("custompanel:action:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody PanelAction action, CredentialPresentation credential) {
        return actionService.add(action, credential);
    }

    @ApiName("修改动作")
    @ApiPermission("custompanel:action:edit")
    @PutMapping("/{actionId}")
    public AjaxResult update(@PathVariable(name = "actionId") Long actionId,
                             @RequestBody PanelAction action,
                             CredentialPresentation credential) {
        return actionService.update(actionId, action, credential);
    }

    @ApiName("删除动作")
    @ApiPermission("custompanel:action:remove")
    @DeleteMapping("/{actionIds}")
    public AjaxResult remove(@PathVariable(name = "actionIds") String actionIds,
                             CredentialPresentation credential) {
        return actionService.remove(actionIds, credential);
    }
}
