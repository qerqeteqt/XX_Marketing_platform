package com.xx.marketing.config;

import com.xx.marketing.interceptor.JwtInterceptor;
import com.xx.marketing.interceptor.RefreshTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // RefreshToken 拦截器: 所有请求都经过，用于无感刷新
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/api/**")
                .order(0);

        // JWT 拦截器: 排除登录/验证码等不需要认证的接口
        registry.addInterceptor(new JwtInterceptor(stringRedisTemplate))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/send-code",
                        "/api/user/refresh",
                        "/api/home/**",
                        "/api/service/**"
                )
                .order(1);
    }
}
