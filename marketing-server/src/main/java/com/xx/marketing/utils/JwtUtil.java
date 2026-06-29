package com.xx.marketing.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.access-token.secret}")
    private String accessSecret;

    @Value("${jwt.access-token.ttl}")
    private Long accessTtl;

    @Value("${jwt.refresh-token.secret}")
    private String refreshSecret;

    @Value("${jwt.refresh-token.ttl}")
    private Long refreshTtl;

    private SecretKey getAccessKey() {
        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     */
    public String generateAccessToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTtl))
                .signWith(getAccessKey())
                .compact();
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTtl))
                .signWith(getRefreshKey())
                .compact();
    }

    /**
     * 解析 Access Token
     */
    public Claims parseAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getAccessKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * 解析 Refresh Token
     */
    public Claims parseRefreshToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getRefreshKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * 检查 Access Token 是否即将过期（剩余时间小于5分钟）
     */
    public boolean isAccessTokenExpiring(String token) {
        Claims claims = parseAccessToken(token);
        if (claims == null) return true;
        Date expiration = claims.getExpiration();
        return expiration.getTime() - System.currentTimeMillis() < 300_000; // 5 min
    }
}
