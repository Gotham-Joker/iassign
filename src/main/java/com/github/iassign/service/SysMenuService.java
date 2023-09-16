package com.github.iassign.service;

import com.github.authorization.Authentication;
import com.github.authorization.AuthenticationContext;
import com.github.base.BaseService;
import com.github.core.ApiException;
import com.github.iassign.vo.SysMenuQuery;
import com.github.iassign.entity.SysMenu;
import com.github.iassign.vo.MenuTree;
import com.github.iassign.mapper.SysMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
public class SysMenuService extends BaseService<SysMenu> {
    public static final String ROOT_ID = "0";

    @Autowired
    private SysMenuMapper sysMenuMapper;


    /**
     * 获取用户所有菜单
     *
     * @return
     */
    public List<MenuTree> selectUserMenus() {
        Authentication authentication = AuthenticationContext.current();
        Stream<MenuTree> stream;
        Map<String, List<MenuTree>> group;
        if (authentication.isAdmin()) { // 超管拥有所有菜单
            stream = sysMenuMapper.selectList(null).stream()
                    .map(MenuTree::new).sorted(MenuTree::compareWeight);
        } else {
            // 其他用户
            List<MenuTree> list = sysMenuMapper.selectByUserId(authentication.getId());
            stream = list.stream().sorted(MenuTree::compareWeight);
        }
        // 根据pid进行分组
        group = stream.collect(groupingBy(MenuTree::getPid));
        // 已组装的节点id
        Set<String> existIds = new HashSet<>();
        // 遍历所有节点，装填孩子节点
        group.values().stream().flatMap(Collection::stream)
                .forEach(tree -> {
                    existIds.add(tree.id);
                    tree.children = group.get(tree.id);
                });
        // 取出真正的一级菜单
        List<MenuTree> rootMenus = group.get(ROOT_ID);
        if (rootMenus == null) {
            rootMenus = new ArrayList<>();
        }
        group.remove(ROOT_ID);
        // 把没有父亲节点的菜单当成一级菜单
        List<MenuTree> otherRootList = group.entrySet()
                .stream().filter(entry -> !existIds.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(toList());
        if (!CollectionUtils.isEmpty(otherRootList)) {
            rootMenus.addAll(otherRootList);
        }
        return rootMenus.stream().sorted(MenuTree::compareWeight).collect(Collectors.toList());
    }


    @Transactional(rollbackFor = Exception.class)
    public void delete(Serializable id) {
        List<SysMenu> subMenus = sysMenuMapper.selectByPid(id);
        if (subMenus != null && !subMenus.isEmpty()) {
            subMenus.forEach(menu -> sysMenuMapper.deleteById(menu.getId()));
        }
        // 解绑角色
        sysMenuMapper.unBindRole(id);
        sysMenuMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchIds(List<Integer> ids) {
        sysMenuMapper.deleteBatchIds(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateById(SysMenu sysMenu) {
        //不能修改父级菜单id
        SysMenu record = sysMenuMapper.selectById(sysMenu.getId());
        if (record == null) {
            throw new ApiException(404, "菜单不存在");
        }
        super.updateById(sysMenu);
    }

    public List<SysMenuQuery> findParentIds() {
        // 找出所有一级菜单和二级菜单
        List<SysMenu> menus = sysMenuMapper.selectByPid(ROOT_ID);
        return menus.stream().flatMap(m -> {
            List<SysMenu> list = sysMenuMapper.selectByPid(m.id);
            list.add(m);
            return list.stream();
        }).sorted(Comparator.comparing(SysMenu::getWeight))
                .map(m -> new SysMenuQuery(m.id, m.text)).collect(toList());
    }

    public List<SysMenu> selectByRoleId(String roleId) {
        return sysMenuMapper.selectByRoleId(roleId);
    }
}
