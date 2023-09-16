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

