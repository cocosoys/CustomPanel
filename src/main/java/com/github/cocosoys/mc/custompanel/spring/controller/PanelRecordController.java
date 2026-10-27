package com.github.cocosoys.mc.custompanel.spring.controller;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelRecord;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelRecordService;
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
 * 自由模式记录管理控制器：路由 {@code /api/plugins/CustomPanel/panel/record/*}。
 *
 * <p>只做参数绑定与权限声明，方法体一律一行委托 {@link IPanelRecordService}。</p>
 */
@RequestMapping("/panel/record")
public class PanelRecordController {

    private final IPanelRecordService recordService;

    public PanelRecordController(IPanelRecordService recordService) {
        this.recordService = recordService;
    }

    @ApiName("记录列表")
    @ApiPermission("custompanel:record:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "pageId", required = false) Long pageId) {
        return recordService.list(pageNum, pageSize, pageId);
    }

    @ApiName("记录详情")
    @ApiPermission("custompanel:record:query")
    @GetMapping("/{recordId}")
    public AjaxResult detail(@PathVariable(name = "recordId") Long recordId) {
        return recordService.detail(recordId);
    }

    @ApiName("新增记录")
    @ApiPermission("custompanel:record:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody PanelRecord record, CredentialPresentation credential) {
        return recordService.add(record, credential);
    }

    @ApiName("修改记录")
    @ApiPermission("custompanel:record:edit")
    @PutMapping("/{recordId}")
    public AjaxResult update(@PathVariable(name = "recordId") Long recordId,
                             @RequestBody PanelRecord record,
                             CredentialPresentation credential) {
        return recordService.update(recordId, record, credential);
    }

    @ApiName("删除记录")
    @ApiPermission("custompanel:record:remove")
    @DeleteMapping("/{recordIds}")
    public AjaxResult remove(@PathVariable(name = "recordIds") String recordIds,
                             CredentialPresentation credential) {
        return recordService.remove(recordIds, credential);
    }
}
