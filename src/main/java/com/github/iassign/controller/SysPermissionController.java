package com.github.iassign.controller;

import com.github.base.BaseController;
import com.github.iassign.entity.SysPermission;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
public class SysPermissionController extends BaseController<SysPermission> {

}
