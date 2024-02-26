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

package com.github.iassign.controller;

import com.github.base.BaseController;
import com.github.core.Result;
import com.github.iassign.entity.SysRole;
import com.github.iassign.entity.SysUser;
import com.github.iassign.service.SysRoleService;
import com.github.iassign.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("api/users")
public class SysUserController extends BaseController<SysUser> {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysRoleService sysRoleService;

    @GetMapping("list")
    public Result list() {
        return Result.success(sysUserService.list());
    }


    @Override
    @GetMapping("{id}")
    public Result queryById(@PathVariable String id) {
        Result result = super.queryById(id);
        SysUser user = (SysUser) result.data;
        user.setRoleIds(sysRoleService.selectBySysUserId(id).stream().map(SysRole::getId).collect(Collectors.toSet()));
        return result;
    }

    @GetMapping("base/{id}")
    public Result baseInfo(@PathVariable String id) {
        return super.queryById(id);
    }


    @PutMapping("set-admin")
    public Result setAdmin(@RequestParam String userId, @RequestParam Boolean isAdmin) {
        sysUserService.setAdmin(userId, isAdmin);
        return Result.success();
    }

}

