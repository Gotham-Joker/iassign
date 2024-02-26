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

package com.github.base;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.core.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Map;

public class BaseService<T> {
    @Autowired
    protected BaseMapper<T> baseMapper;

    public static <T> QueryWrapper<T> wrapper(Map<String, String> queryParams) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        queryParams.forEach((key, value) -> {
            // 空的参数不参与查询
            if (value == null || StringUtils.isBlank(value)) {
                return;
            }
            String[] strArr = key.split("_");
            String col = StringUtils.camelToUnderline(strArr[0]);
            if (strArr.length == 2) {
                switch (strArr[1]) {
                    case "like":
                        wrapper.like(col, value);
                        break;
                    case "likeLeft":
                        wrapper.likeLeft(col, value);
                        break;
                    case "likeRight":
                        wrapper.likeRight(col, value);
                        break;
                    case "ge":
                        wrapper.ge(col, value);
                        break;
                    case "gt":
                        wrapper.gt(col, value);
                        break;
                    case "le":
                        wrapper.le(col, value);
                        break;
                    case "lt":
                        wrapper.lt(col, value);
                        break;
                    case "eq":
                        wrapper.eq(col, value);
                        break;
                    case "oda":
                        wrapper.orderByAsc(col);
                        break;
                    case "odd":
                        wrapper.orderByDesc(col);
                        break;
                }
            } else {
                wrapper.eq(col, value);
            }
        });
        return wrapper;
    }

    public static <T> Page<T> pageHelper(Map<String, String> queryParams) {
        int page = getInt(queryParams, "page", 1);
        queryParams.remove("page");
        int size = getInt(queryParams, "size", 10);
        queryParams.remove("size");
        return PageHelper.startPage(page, size);
    }


    public PageResult<T> pageQuery(Map<String, String> queryParams) {
        pageHelper(queryParams);
        QueryWrapper<T> wrapper = wrapper(queryParams);
        return PageResult.of(baseMapper.selectList(wrapper));
    }

    private static Integer getInt(Map<String, String> queryParams, String key, Integer defaultValue) {
        Object obj = queryParams.get(key);
        return obj == null ? defaultValue : Integer.parseInt(obj.toString());
    }

    public T selectById(Serializable id) {
        return baseMapper.selectById(id);
    }

    @Transactional
    public void updateById(T entity) {
        baseMapper.updateById(entity);
    }

    @Transactional
    public void save(T entity) {
        baseMapper.insert(entity);
    }

    @Transactional
    public void delete(Serializable id) {
        baseMapper.deleteById(id);
    }
}
