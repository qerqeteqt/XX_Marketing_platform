package com.xx.marketing.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.xx.marketing.common.RedisKeys;
import com.xx.marketing.dto.UserDTO;
import com.xx.marketing.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 拦截器 — 实现无感刷新
 * 在 JwtInterceptor 之前执行，负责检测 Token 过期并刷新
 */
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenInterceptor implements org.springframework.web.servlet.HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        if (token == null) return true; // 没有 Token，交给 JwtInterceptor 处理

        JwtUtil jwtUtil = SpringContextHolder.getBean(JwtUtil.class);

        if (jwtUtil.isAccessTokenExpiring(token)) {
            // Token 即将过期，尝试刷新
            String refreshToken = request.getHeader("X-Refresh-Token");
            if (refreshToken != null) {
                String userId = stringRedisTemplate.opsForValue().get(RedisKeys.REFRESH_TOKEN + refreshToken);
                if (userId != null) {
                    Claims claims = jwtUtil.parseRefreshToken(refreshToken);
                    if (claims != null) {
                        // 生成新的 Access Token
                        String newAccessToken = jwtUtil.generateAccessToken(claims);
                        response.setHeader("X-New-Access-Token", newAccessToken);

                        // 将旧 Token 加入黑名单
                        stringRedisTemplate.opsForValue().set(
                                RedisKeys.TOKEN_BLACKLIST + token,
                                "1", 300, TimeUnit.SECONDS);
                    }
                }
            }
        }
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
