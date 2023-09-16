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
    public Result delete(@RequestParam("id") Integer id) {
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
