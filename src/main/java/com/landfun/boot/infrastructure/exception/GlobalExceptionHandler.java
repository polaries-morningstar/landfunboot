package com.landfun.boot.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.landfun.boot.infrastructure.web.R;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException e) {
        log.warn("BizException: {}", e.getMessage());
        int code = e.getCode();
        R<Void> body = R.fail(code, e.getMessage());
        HttpStatus status = code >= 400 && code < 600 ? HttpStatus.valueOf(code) : HttpStatus.OK;
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        return R.fail(404, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled Exception at {}: ", request.getRequestURI(), e);
        return R.fail("服务器内部错误，请联系管理员");
    }
}
