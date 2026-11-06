package com.github.cocosoys.mc.custompanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.github.cocosoys.mc.custompanel.spring.controller.PanelActionController;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelFieldController;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelManagedController;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelPageController;
import com.github.cocosoys.mc.custompanel.spring.controller.PanelRecordController;
import com.github.cocosoys.mc.mcerp.ErpMenus;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;

/**
 * 菜单声明与按钮投影（不依赖 Spigot 运行时）。
 *
 * <p>与 {@code PanelExpansion.menus()} 保持同构，锁定两条 MCERP 规范：
 * <ul>
 *   <li>模块名由 {@code displayName()} 承担，{@code menus()} 只声明平铺子菜单，不得再套同名 dir；</li>
 *   <li>C 菜单不手写 component，由 {@code basePrefix + 父链 + menuId} 自动拼接；</li>
 *   <li>记录/托管实体的按钮权限挂在「页面管理」菜单下。</li>
 * </ul>
 */
public class PanelMenusTest {

    /** 与 PanelExpansion.menus() 同构的声明（改动菜单时必须同步本方法）。 */
    private static List<ErpMenuVO> buildMenus() {
        return ErpMenus.create()
                .menu("page", "页面管理", "build", PanelPageController.class)
                .permsFrom(PanelRecordController.class, PanelManagedController.class)
                .menu("field", "字段管理", "list", PanelFieldController.class)
                .menu("action", "动作管理", "flash-on", PanelActionController.class)
                .build();
    }

    private static void collectPerms(ErpMenuVO node, Set<String> out) {
        if (node.getPerms() != null && !node.getPerms().isEmpty()) {
            out.add(node.getPerms());
        }
        for (ErpMenuVO child : node.getChildren()) {
            collectPerms(child, out);
        }
    }

    private static Set<String> permsOf(ErpMenuVO node) {
        Set<String> out = new HashSet<>();
        collectPerms(node, out);
        return out;
    }

    @Test
    public void menusAreFlatModuleChildrenWithoutDuplicatedDir() {
        List<ErpMenuVO> roots = buildMenus();
        assertEquals("模块下应只有 3 条平铺菜单", 3, roots.size());
        assertEquals("page", roots.get(0).getMenuId());
        assertEquals("field", roots.get(1).getMenuId());
        assertEquals("action", roots.get(2).getMenuId());
        for (ErpMenuVO root : roots) {
            assertEquals("子菜单必须是 C 菜单（dir 会重复模块层级）", "C", root.getMenuType());
        }
    }

    @Test
    public void pageMenuCarriesRecordAndManagedButtons() {
        List<ErpMenuVO> roots = buildMenus();
        Set<String> pagePerms = permsOf(roots.get(0));
        assertTrue("页面管理缺少自身按钮: " + pagePerms,
                pagePerms.contains("custompanel:page:list"));
        assertTrue("页面管理缺少记录按钮: " + pagePerms,
                pagePerms.contains("custompanel:record:list"));
        assertTrue("页面管理缺少托管实体按钮: " + pagePerms,
                pagePerms.contains("custompanel:managed:list"));

        Set<String> fieldPerms = permsOf(roots.get(1));
        assertFalse("字段管理不应挂记录按钮", fieldPerms.contains("custompanel:record:list"));
        assertFalse("字段管理不应挂托管实体按钮", fieldPerms.contains("custompanel:managed:list"));
    }

    @Test
    public void componentIsAutoPrefixedByPageDirRoot() {
        List<ErpMenuVO> roots = ErpMenus.create()
                .setBasePrefix("/web/plugins/CustomPanel/custompanel")
                .setRootPrefix("/web/plugins/CustomPanel")
                .menu("page", "页面管理", "build", PanelPageController.class)
                .build();
        assertEquals("component 应由 basePrefix 自动拼接",
                "/web/plugins/CustomPanel/custompanel/page", roots.get(0).getComponent());
    }
}
