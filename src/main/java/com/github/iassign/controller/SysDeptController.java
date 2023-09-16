package com.github.iassign.controller;

import com.github.base.BaseController;
import com.github.iassign.entity.SysDept;
import com.github.iassign.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门管理模块
 */
@RestController
@RequestMapping("/api/depts")
public class SysDeptController extends BaseController<SysDept> {
    @Autowired
    SysDeptService sysDeptService;
}
