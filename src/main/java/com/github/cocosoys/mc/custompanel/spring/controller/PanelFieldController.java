package com.github.cocosoys.mc.custompanel.spring.controller;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelField;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelFieldService;
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
 * 字段定义管理控制器：路由 {@code /api/plugins/CustomPanel/panel/field/*}。
 *
 * <p>只做参数绑定与权限声明，方法体一律一行委托 {@link IPanelFieldService}。</p>
 */
@RequestMapping("/panel/field")
public class PanelFieldController {

    private final IPanelFieldService fieldService;

    public PanelFieldController(IPanelFieldService fieldService) {
        this.fieldService = fieldService;
    }

    @ApiName("字段列表")
    @ApiPermission("custompanel:field:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "pageId", required = false) Long pageId,
                              @RequestParam(name = "fieldKey", required = false) String fieldKey,
                              @RequestParam(name = "fieldName", required = false) String fieldName) {
        return fieldService.list(pageNum, pageSize, pageId, fieldKey, fieldName);
    }

    @ApiName("字段详情")
    @ApiPermission("custompanel:field:query")
    @GetMapping("/{fieldId}")
    public AjaxResult detail(@PathVariable(name = "fieldId") Long fieldId) {
        return fieldService.detail(fieldId);
    }

    @ApiName("新增字段")
    @ApiPermission("custompanel:field:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody PanelField field, CredentialPresentation credential) {
        return fieldService.add(field, credential);
    }

    @ApiName("修改字段")
    @ApiPermission("custompanel:field:edit")
    @PutMapping("/{fieldId}")
    public AjaxResult update(@PathVariable(name = "fieldId") Long fieldId,
                             @RequestBody PanelField field,
                             CredentialPresentation credential) {
        return fieldService.update(fieldId, field, credential);
    }

    @ApiName("删除字段")
    @ApiPermission("custompanel:field:remove")
    @DeleteMapping("/{fieldIds}")
    public AjaxResult remove(@PathVariable(name = "fieldIds") String fieldIds,
                             CredentialPresentation credential) {
        return fieldService.remove(fieldIds, credential);
    }
}
