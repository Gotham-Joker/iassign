package com.github.core;

import com.github.pagehelper.Page;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class PageResult<T> {

    public List<T> list;
    public int totalPages;
    public long total;
    public int size;
    public int page;

    public PageResult() {
    }

    public PageResult(Page<T> page) {
        this.page = page.getPageNum(); // 当前页
        this.size = page.getPageSize(); // 每页的数量
        this.total = page.getTotal(); // 总记录数
        this.totalPages = page.getPages(); // 总页数
        this.list = page; // 结果集
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.list = Collections.emptyList();
        pageResult.total = 0;
        pageResult.page = 1;
        pageResult.size = 10;
        return pageResult;
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page);
    }

    public static <T> PageResult<T> of(List<T> page) {
        return new PageResult<>((Page<T>) page);
    }

    /**
     * 分页转换成另一个分页，保留页码属性
     *
     * @param page   需要转换的分页对象
     * @param mapper 转换的具体逻辑
     * @return
     */
    public static <T, R> PageResult<R> convert(Page<T> page, Function<T, R> mapper) {
        PageResult<R> pageResult = new PageResult<>();
        pageResult.page = page.getPageNum();
        pageResult.setSize(page.getPageSize());
        pageResult.total = page.getTotal();
        pageResult.setTotalPages(page.getPages());
        pageResult.size = page.size();
        pageResult.list = page.stream().map(mapper).collect(Collectors.toList());
        return pageResult;
    }

}
