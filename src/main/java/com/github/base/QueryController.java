package com.github.base;

import com.github.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public class QueryController<T> {
    @Autowired
    protected BaseService<T> baseService;

    @GetMapping
    public Result pageQuery(@RequestParam Map<String, String> queryParams) {
        return Result.success(baseService.pageQuery(queryParams));
    }

    @GetMapping("{id}")
    public Result queryById(@PathVariable String id) {
        return Result.success(baseService.selectById(id));
    }

}
