package com.landfun.boot.infrastructure.web;

import com.landfun.boot.modules.system.user.User;

public class AuthContext {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<User> USER_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setUser(User user) {
        USER_HOLDER.set(user);
    }

    public static User getUser() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USER_HOLDER.remove();
    }
}
