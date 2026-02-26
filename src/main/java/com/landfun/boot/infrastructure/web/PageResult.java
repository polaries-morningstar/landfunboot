package com.landfun.boot.infrastructure.web;

import java.util.List;

import org.babyfish.jimmer.Page;

public record PageResult<T>(
        long total,
        List<T> rows) {
    public static <T> PageResult<T> of(Page<T> jimmerPage) {
        return new PageResult<>(jimmerPage.getTotalRowCount(), jimmerPage.getRows());
    }

    public static <T> PageResult<T> of(long total, List<T> rows) {
        return new PageResult<>(total, rows);
    }
}
