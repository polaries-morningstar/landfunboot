package com.landfun.boot.infrastructure.web;

import org.springframework.lang.Nullable;

public record BasePageQuery(
        @Nullable Integer page,
        @Nullable Integer size,
        @Nullable String sort) {
    public int pageIndex() {
        return page == null || page < 1 ? 0 : page - 1; // 0-based
    }

    public int pageSize() {
        return size == null || size < 1 ? 10 : size;
    }
}
