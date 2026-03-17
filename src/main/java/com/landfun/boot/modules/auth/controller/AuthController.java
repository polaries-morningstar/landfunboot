package com.landfun.boot.modules.auth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.auth.dto.LoginReq;
import com.landfun.boot.modules.auth.dto.LoginResult;
import com.landfun.boot.modules.auth.dto.UserInfoResult;
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
    public R<LoginResult> login(@RequestBody LoginReq req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "Get User Info")
    @GetMapping("/info")
    public R<UserInfoResult> info() {
        return R.ok(authService.info());
    }

    @Operation(summary = "Get current user menu tree (for sidebar, no sys:menu:list required)")
    @GetMapping("/menus")
    public R<List<Map<String, Object>>> menus() {
        return R.ok(authService.menus());
    }
}
