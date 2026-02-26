package com.landfun.boot.infrastructure.web;

import org.babyfish.jimmer.Page;

import java.util.List;

public record PageResult<T>(
        long total,
        List<T> rows) {
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getTotalRowCount(), page.getRows());
    }

    public static <T> PageResult<T> of(long total, List<T> rows) {
        return new PageResult<>(total, rows);
    }
}
