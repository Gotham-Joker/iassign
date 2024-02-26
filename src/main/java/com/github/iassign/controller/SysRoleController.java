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

import com.github.base.QueryController;
import com.github.core.Result;
import com.github.iassign.entity.SysRole;
import com.github.iassign.vo.SysRoleRequest;
import com.github.iassign.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/roles")
public class SysRoleController extends QueryController<SysRole> {
    @Autowired
    private SysRoleService sysRoleService;

    @GetMapping("list")
    public Result list() {
        return Result.success(sysRoleService.list());
    }

    @PostMapping
    public Result save(@Valid @RequestBody SysRoleRequest request) {
        sysRoleService.save(request);
        return Result.success();
    }

    @PutMapping
    public Result update(@Valid @RequestBody SysRoleRequest request) {
        sysRoleService.update(request);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(@RequestParam("id") String id) {
        sysRoleService.delete(id);
        return Result.success();
    }

    /**
     * 查找角色详情（包含角色关联的菜单ID和权限ID）
     *
     * @param id
     * @return
     */
    @GetMapping("{id}/details")
    public Result rolePermissions(@PathVariable String id) {
        return Result.success(sysRoleService.selectDetails(id));
    }

}
