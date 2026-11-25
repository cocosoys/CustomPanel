package com.github.cocosoys.mc.custompanel.spring.controller;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelEvent;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelEventService;
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
 * 页面事件绑定控制器：路由 {@code /api/plugins/CustomPanel/panel/event/*}。
 *
 * <p>只做参数绑定与权限声明，方法体一律一行委托 {@link IPanelEventService}。</p>
 */
@RequestMapping("/panel/event")
public class PanelEventController {

    private final IPanelEventService eventService;

    public PanelEventController(IPanelEventService eventService) {
        this.eventService = eventService;
    }

    @ApiName("事件列表")
    @ApiPermission("custompanel-erp:event:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "pageId", required = false) Long pageId,
                              @RequestParam(name = "trigger", required = false) String trigger) {
        return eventService.list(pageNum, pageSize, pageId, trigger);
    }

    @ApiName("事件详情")
    @ApiPermission("custompanel-erp:event:query")
    @GetMapping("/detail/{eventId}")
    public AjaxResult detail(@PathVariable(name = "eventId") Long eventId) {
        return eventService.detail(eventId);
    }

    @ApiName("新增事件")
    @ApiPermission("custompanel-erp:event:add")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody PanelEvent event, CredentialPresentation credential) {
        return eventService.add(event, credential);
    }

    @ApiName("修改事件")
    @ApiPermission("custompanel-erp:event:edit")
    @PutMapping("/update/{eventId}")
    public AjaxResult update(@PathVariable(name = "eventId") Long eventId,
                             @RequestBody PanelEvent event,
                             CredentialPresentation credential) {
        return eventService.update(eventId, event, credential);
    }

    @ApiName("删除事件")
    @ApiPermission("custompanel-erp:event:remove")
    @DeleteMapping("/remove/{eventIds}")
    public AjaxResult remove(@PathVariable(name = "eventIds") String eventIds) {
        return eventService.remove(eventIds);
    }
}
