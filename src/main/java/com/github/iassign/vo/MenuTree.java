/*
 * MIT License
 *
 * Copyright (c) 2024 Hongtao Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
