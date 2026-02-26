package com.landfun.boot.modules.auth.dto;

public record LoginReq(
        String email,
        String password) {
}
