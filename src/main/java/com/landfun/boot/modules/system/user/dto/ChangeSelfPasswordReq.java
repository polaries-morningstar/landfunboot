package com.landfun.boot.modules.system.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeSelfPasswordReq(
        @NotBlank(message = "请输入当前密码")
        String oldPassword,

        @NotBlank(message = "请输入新密码")
        @Size(min = 6, message = "密码至少6个字符")
        String password) {
}
