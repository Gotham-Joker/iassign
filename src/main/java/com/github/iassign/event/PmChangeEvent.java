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

package com.github.iassign.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.iassign.entity.SysPermission;
import lombok.Data;

/**
 * 权限变更事件，用于通知授权中心刷新权限
 */
@Data
public class PmChangeEvent {
    /**
     * 新权限会被加载
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SysPermission newPermission;
    /**
     * 旧权限会被移除
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SysPermission oldPermission;

    public PmChangeEvent() {

    }

    public PmChangeEvent(SysPermission newPermission, SysPermission oldPermission) {
        this.newPermission = newPermission;
        this.oldPermission = oldPermission;
    }
}
