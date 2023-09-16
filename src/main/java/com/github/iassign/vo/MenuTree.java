package com.github.iassign.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.iassign.entity.SysMenu;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTree {
    public String id;
    public String pid;
    public String text;
    public Integer weight;
    // null不进行序列化
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String link;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String icon;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean checked;
    // 是否是叶子节点
    private Boolean isLeaf;

    public List<MenuTree> children = new ArrayList<>();

    public MenuTree() {

    }

    public MenuTree(SysMenu sysMenu) {
        this.id = sysMenu.id;
        this.pid = sysMenu.pid;
        this.text = sysMenu.text;
        this.weight = sysMenu.weight;
        this.link = sysMenu.link;
        this.icon = sysMenu.icon;
    }

    public static int compareWeight(MenuTree pre, MenuTree next) {
        if (pre.weight == null) {
            return -1;
        }
        if (next.weight == null) {
            return -1;
        }
        return pre.weight.compareTo(next.weight);
    }
}
