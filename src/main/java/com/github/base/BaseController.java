package com.github.base;

import com.github.core.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

public class BaseController<T> extends QueryController {

    @PostMapping
    public Result save(@Validated @RequestBody T entity) {
        this.baseService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result update(@Validated @RequestBody T entity) {
        this.baseService.updateById(entity);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(@RequestParam Serializable id) {
        this.baseService.delete(id);
        return Result.success();
    }
}
