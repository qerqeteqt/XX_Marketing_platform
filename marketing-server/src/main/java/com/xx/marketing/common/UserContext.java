package com.xx.marketing.common;

import com.xx.marketing.dto.UserDTO;

/**
 * ThreadLocal 线程级别的用户上下文
 */
public class UserContext {
    private static final ThreadLocal<UserDTO> USER_HOLDER = new ThreadLocal<>();

    public static void set(UserDTO user) {
        USER_HOLDER.set(user);
    }

    public static UserDTO get() {
        return USER_HOLDER.get();
    }

    public static Long getUserId() {
        UserDTO user = USER_HOLDER.get();
        return user != null ? user.getId() : null;
    }

    public static void remove() {
        USER_HOLDER.remove();
    }
}
