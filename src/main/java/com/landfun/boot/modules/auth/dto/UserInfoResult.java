package com.landfun.boot.modules.auth.dto;

import java.util.Set;

public record UserInfoResult(
        LoginResult.LoginUserInfo user,
        Set<String> permissions) {
}
