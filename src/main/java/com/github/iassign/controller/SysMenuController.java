package com.github.iassign.controller;

import com.github.base.BaseController;
import com.github.core.Result;
import com.github.iassign.entity.SysMenu;
import com.github.iassign.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/menus")
public class SysMenuController extends BaseController<SysMenu> {
    @Autowired
    private SysMenuService sysMenuService;

    @GetMapping("parents")
    public Result findParentIds() {
        return Result.success(sysMenuService.findParentIds());
    }

}
