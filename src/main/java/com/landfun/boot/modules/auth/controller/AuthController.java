package com.landfun.boot.modules.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.auth.dto.LoginReq;
import com.landfun.boot.modules.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Authentication", description = "Authentication APIs")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User Login")
    @PostMapping("/login")
    public R<Object> login(@RequestBody LoginReq req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "Get User Info")
    @org.springframework.web.bind.annotation.GetMapping("/info")
    public R<Object> info() {
        return R.ok(authService.info());
    }

    @Operation(summary = "Get current user menu tree (for sidebar, no sys:menu:list required)")
    @org.springframework.web.bind.annotation.GetMapping("/menus")
    public R<Object> menus() {
        return R.ok(authService.menus());
    }
}
