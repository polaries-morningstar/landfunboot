package com.landfun.boot.modules.auth.controller;

import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.auth.dto.LoginReq;
import com.landfun.boot.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Object> login(@RequestBody LoginReq req) {
        return R.ok(authService.login(req));
    }
}
