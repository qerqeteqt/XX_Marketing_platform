package com.xx.marketing.interceptor;

import com.xx.marketing.common.UserContext;
import com.xx.marketing.dto.UserDTO;
import com.xx.marketing.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.xx.marketing.common.RedisKeys.TOKEN_BLACKLIST;

@Slf4j
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        if (token == null) {
            response.setStatus(401);
            return false;
        }

        // 检查 Token 是否在黑名单中
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(TOKEN_BLACKLIST + token))) {
            response.setStatus(401);
            return false;
        }

        // 解析 Token
        JwtUtil jwtUtil = SpringContextHolder.getBean(JwtUtil.class);
        Claims claims = jwtUtil.parseAccessToken(token);
        if (claims == null) {
            response.setStatus(401);
            return false;
        }

        // 设置用户上下文
        UserDTO user = new UserDTO();
        user.setId(claims.get("userId", Long.class));
        user.setPhone(claims.get("phone", String.class));
        user.setNickname(claims.get("nickname", String.class));
        user.setRole(claims.get("role", String.class));
        UserContext.set(user);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.remove();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
