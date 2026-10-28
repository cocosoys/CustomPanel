package com.github.cocosoys.mc.custompanel.spring.controller;

import com.github.cocosoys.mc.custompanel.spring.entity.vo.ManagedEntityRequest;
import com.github.cocosoys.mc.custompanel.spring.service.IManagedEntityService;
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
 * 托管实体（MANAGED）数据通道控制器：路由 {@code /api/plugins/CustomPanel/panel/managed/*}。
 *
 * <p>只做参数绑定与权限声明，方法体一律一行委托 {@link IManagedEntityService}；
 * 路由前由服务层强制校验实体注册与页面托管绑定（封堵"任意字符串"缺口）。</p>
 */
@RequestMapping("/panel/managed")
public class PanelManagedController {

    private final IManagedEntityService managedService;

    public PanelManagedController(IManagedEntityService managedService) {
        this.managedService = managedService;
    }

    @ApiName("托管实体列表")
    @ApiPermission("custompanel:managed:list")
    @GetMapping("/entities")
    public AjaxResult entities(CredentialPresentation credential) {
        return managedService.entities(credential);
    }

    @ApiName("托管数据列表")
    @ApiPermission("custompanel:managed:list")
    @GetMapping("/{entityName}/list")
    public TableDataInfo list(@PathVariable(name = "entityName") String entityName,
                              @RequestParam(name = "pageId", required = false) Long pageId,
                              @RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              CredentialPresentation credential) {
        return managedService.list(entityName, pageId, pageNum, pageSize, credential);
    }

    @ApiName("托管数据详情")
    @ApiPermission("custompanel:managed:query")
    @GetMapping("/{entityName}/{id}")
    public AjaxResult detail(@PathVariable(name = "entityName") String entityName,
                             @PathVariable(name = "id") Long id,
                             @RequestParam(name = "pageId", required = false) Long pageId,
                             CredentialPresentation credential) {
        return managedService.detail(entityName, pageId, id, credential);
    }

    @ApiName("新增托管数据")
    @ApiPermission("custompanel:managed:add")
    @PostMapping("/{entityName}")
    public AjaxResult add(@PathVariable(name = "entityName") String entityName,
                          @RequestBody ManagedEntityRequest request,
                          CredentialPresentation credential) {
        return managedService.add(entityName, request == null ? null : request.getPageId(),
                request == null ? null : request.getData(), credential);
    }

    @ApiName("修改托管数据")
    @ApiPermission("custompanel:managed:edit")
    @PutMapping("/{entityName}/{id}")
    public AjaxResult update(@PathVariable(name = "entityName") String entityName,
                             @PathVariable(name = "id") Long id,
                             @RequestBody ManagedEntityRequest request,
                             CredentialPresentation credential) {
        return managedService.update(entityName, request == null ? null : request.getPageId(),
                id, request == null ? null : request.getData(), credential);
    }

    @ApiName("删除托管数据")
    @ApiPermission("custompanel:managed:remove")
    @DeleteMapping("/{entityName}/{ids}")
    public AjaxResult remove(@PathVariable(name = "entityName") String entityName,
                             @PathVariable(name = "ids") String ids,
                             @RequestParam(name = "pageId", required = false) Long pageId,
                             CredentialPresentation credential) {
        return managedService.remove(entityName, pageId, ids, credential);
    }
}
