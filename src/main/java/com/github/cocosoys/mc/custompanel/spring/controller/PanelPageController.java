package com.github.cocosoys.mc.custompanel.spring.controller;

import com.github.cocosoys.mc.custompanel.spring.entity.PanelPage;
import com.github.cocosoys.mc.custompanel.spring.service.IPanelPageService;
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
 * 页面定义管理控制器：路由 {@code /api/plugins/CustomPanel/panel/page/*}。
 *
 * <p>本类<b>只做参数绑定与权限声明</b>，方法体一律一行委托
 * {@link IPanelPageService}，不含任何业务逻辑与数据加工。</p>
 *
 * <p>{@code @ApiName} + {@code @ApiPermission} 会被 MCERP 的
 * {@code ControllerPermScanner} 自动投影为 F 按钮菜单节点，无需手写按钮。</p>
 */
@RequestMapping("/panel/page")
public class PanelPageController {

    private final IPanelPageService pageService;

    public PanelPageController(IPanelPageService pageService) {
        this.pageService = pageService;
    }

    @ApiName("页面列表")
    @ApiPermission("custompanel:page:list")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                              @RequestParam(name = "pageSize", required = false) Integer pageSize,
                              @RequestParam(name = "pageKey", required = false) String pageKey,
                              @RequestParam(name = "pageName", required = false) String pageName,
                              @RequestParam(name = "storeMode", required = false) String storeMode) {
        return pageService.list(pageNum, pageSize, pageKey, pageName, storeMode);
    }

    @ApiName("页面定义聚合（Schema）")
    @ApiPermission("custompanel:page:query")
    @GetMapping("/schema/{pageId}")
    public AjaxResult schema(@PathVariable(name = "pageId") Long pageId) {
        return pageService.schema(pageId);
    }

    @ApiName("页面详情")
    @ApiPermission("custompanel:page:query")
    @GetMapping("/{pageId}")
    public AjaxResult detail(@PathVariable(name = "pageId") Long pageId) {
        return pageService.detail(pageId);
    }

    @ApiName("新增页面")
    @ApiPermission("custompanel:page:add")
    @PostMapping("")
    public AjaxResult add(@RequestBody PanelPage page, CredentialPresentation credential) {
        return pageService.add(page, credential);
    }

    @ApiName("修改页面")
    @ApiPermission("custompanel:page:edit")
    @PutMapping("/{pageId}")
    public AjaxResult update(@PathVariable(name = "pageId") Long pageId,
                             @RequestBody PanelPage page,
                             CredentialPresentation credential) {
        return pageService.update(pageId, page, credential);
    }

    @ApiName("删除页面")
    @ApiPermission("custompanel:page:remove")
    @DeleteMapping("/{pageIds}")
    public AjaxResult remove(@PathVariable(name = "pageIds") String pageIds,
                             CredentialPresentation credential) {
        return pageService.remove(pageIds, credential);
    }
}
