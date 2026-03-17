package com.landfun.boot.modules.auth.dto;

public record LoginResult(
        String token,
        LoginUserInfo user) {

    public record LoginUserInfo(
            long id,
            String username,
            String email,
            boolean isSuperuser) {
    }
}
