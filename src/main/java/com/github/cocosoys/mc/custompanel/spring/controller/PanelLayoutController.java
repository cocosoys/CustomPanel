package com.github.cocosoys.mc.custompanel.spring.controller;

import com.github.cocosoys.mc.custompanel.spring.entity.vo.PanelLayoutSaveRequest;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelLayoutService;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 页面设计器布局控制器：路由 {@code /api/plugins/CustomPanel/panel/layout/*}。
 *
 * <p>只做参数绑定与权限声明，方法体一律一行委托 {@link IPanelLayoutService}。</p>
 */
@RequestMapping("/panel/layout")
public class PanelLayoutController {

    private final IPanelLayoutService layoutService;

    public PanelLayoutController(IPanelLayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @ApiName("布局详情")
    @ApiPermission("custompanel-erp:layout:query")
    @GetMapping("/detail/{pageId}")
    public AjaxResult detail(@PathVariable(name = "pageId") Long pageId) {
        return layoutService.detail(pageId);
    }

    @ApiName("保存布局草稿")
    @ApiPermission("custompanel-erp:layout:add")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody PanelLayoutSaveRequest request, CredentialPresentation credential) {
        return layoutService.save(request, credential);
    }

    @ApiName("发布布局")
    @ApiPermission("custompanel-erp:layout:edit")
    @PutMapping("/publish/{pageId}")
    public AjaxResult publish(@PathVariable(name = "pageId") Long pageId, CredentialPresentation credential) {
        return layoutService.publish(pageId, credential);
    }

    @ApiName("删除布局")
    @ApiPermission("custompanel-erp:layout:remove")
    @DeleteMapping("/remove/{layoutId}")
    public AjaxResult remove(@PathVariable(name = "layoutId") Long layoutId) {
        return layoutService.remove(layoutId);
    }
}
