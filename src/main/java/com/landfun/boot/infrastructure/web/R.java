package com.landfun.boot.infrastructure.web;

import lombok.Builder;

/**
 * Universal Response Wrapper
 *
 * @param <T> Data type
 */
@Builder
public record R<T>(
        int code,
        String message,
        T data) {
    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }
}
