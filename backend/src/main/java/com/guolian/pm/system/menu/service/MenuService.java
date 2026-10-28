package com.guolian.pm.system.menu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.system.menu.entity.SysMenu;
import com.guolian.pm.system.menu.mapper.SysMenuMapper;
import com.guolian.pm.system.role.entity.SysRoleMenu;
import com.guolian.pm.system.role.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理：目录 → 菜单 → 按钮三级，权限标识 module:resource:action。
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public static class Node {
        public Long id;
        public Long parentId;
        public String menuName;
        public Integer menuType;
        public String path;
        public String component;
        public String perms;
        public String icon;
        public Integer sortNo;
        public Integer visible;
        public Integer status;
        public List<Node> children = new ArrayList<>();
    }

    public List<Node> tree() {
        List<SysMenu> all = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getSortNo));
        Map<Long, Node> nodes = new LinkedHashMap<>();
        for (SysMenu menu : all) {
            nodes.put(menu.getId(), toNode(menu));
        }
        List<Node> roots = new ArrayList<>();
        for (SysMenu menu : all) {
            Node node = nodes.get(menu.getId());
            Node parent = nodes.get(menu.getParentId());
            if (parent != null) {
                parent.children.add(node);
            } else if (menu.getParentId() == null || menu.getParentId() == 0) {
                roots.add(node);
            }
        }
        sortRecursively(roots);
        return roots;
    }

    @Transactional
    public Long save(SysMenu menu) {
        if (!StringUtils.hasText(menu.getMenuName())) {
            throw BizException.badRequest("请输入菜单名称");
        }
        if (menu.getMenuType() == null) {
            menu.setMenuType(2);
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getId() == null) {
            menuMapper.insert(menu);
        } else {
            if (menu.getId().equals(menu.getParentId())) {
                throw BizException.biz("上级菜单不能是自己");
            }
            menuMapper.updateById(menu);
        }
        return menu.getId();
    }

    @Transactional
    public void delete(Long id) {
        Long children = menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.biz("该菜单下仍有 " + children + " 个子节点，请先删除子节点");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, id));
    }

    private Node toNode(SysMenu menu) {
        Node node = new Node();
        node.id = menu.getId();
        node.parentId = menu.getParentId();
        node.menuName = menu.getMenuName();
        node.menuType = menu.getMenuType();
        node.path = menu.getPath();
        node.component = menu.getComponent();
        node.perms = menu.getPerms();
        node.icon = menu.getIcon();
        node.sortNo = menu.getSortNo();
        node.visible = menu.getVisible();
        node.status = menu.getStatus();
        return node;
    }

    private void sortRecursively(List<Node> nodes) {
        nodes.sort(Comparator.comparing(n -> n.sortNo == null ? 0 : n.sortNo));
        for (Node node : nodes) {
            sortRecursively(node.children);
        }
    }
}
