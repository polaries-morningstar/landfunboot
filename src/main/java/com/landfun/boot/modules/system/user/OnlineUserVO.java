package com.landfun.boot.modules.system.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Online user info")
public record OnlineUserVO(
        @Schema(description = "User ID") long userId,
        @Schema(description = "Username") String username,
        @Schema(description = "Email") String email,
        @Schema(description = "Active status") boolean isActive) {
}
