package com.xx.marketing.utils;

import com.xx.marketing.common.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    // ========== 验证码相关 ==========

    public void saveSmsCode(String phone, String code, long ttlSeconds) {
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.SMS_CODE + phone, code, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getSmsCode(String phone) {
        return stringRedisTemplate.opsForValue().get(RedisKeys.SMS_CODE + phone);
    }

    public void deleteSmsCode(String phone) {
        stringRedisTemplate.delete(RedisKeys.SMS_CODE + phone);
    }

    public boolean hasSmsInterval(String phone) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeys.SMS_INTERVAL + phone));
    }

    public void setSmsInterval(String phone, long ttlSeconds) {
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.SMS_INTERVAL + phone, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    // ========== Token 相关 ==========

    public void blacklistAccessToken(String token, long ttlSeconds) {
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.TOKEN_BLACKLIST + token, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeys.TOKEN_BLACKLIST + token));
    }

    public void saveRefreshToken(String token, Long userId, long ttlSeconds) {
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.REFRESH_TOKEN + token, userId.toString(), ttlSeconds, TimeUnit.SECONDS);
    }

    public String getUserIdByRefreshToken(String token) {
        return stringRedisTemplate.opsForValue().get(RedisKeys.REFRESH_TOKEN + token);
    }

    public void deleteRefreshToken(String token) {
        stringRedisTemplate.delete(RedisKeys.REFRESH_TOKEN + token);
    }

    // ========== 用户信息缓存 ==========

    public void saveLoginUser(Long userId, String userJson, long ttlSeconds) {
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.LOGIN_USER + userId, userJson, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getLoginUser(Long userId) {
        return stringRedisTemplate.opsForValue().get(RedisKeys.LOGIN_USER + userId);
    }

    // ========== 优惠券库存相关 ==========

    public Long getCouponStock(Long activityId) {
        String stock = stringRedisTemplate.opsForValue().get(RedisKeys.COUPON_STOCK + activityId);
        return stock != null ? Long.parseLong(stock) : null;
    }

    public void setCouponStock(Long activityId, int stock) {
        stringRedisTemplate.opsForValue().set(RedisKeys.COUPON_STOCK + activityId, String.valueOf(stock));
    }

    public Boolean isUserGrabbedCoupon(Long userId, Long activityId) {
        return stringRedisTemplate.opsForSet().isMember(RedisKeys.USER_COUPON + activityId, userId.toString());
    }

    public void addUserCouponRecord(Long userId, Long activityId) {
        stringRedisTemplate.opsForSet().add(RedisKeys.USER_COUPON + activityId, userId.toString());
    }

    public Long getUserCouponCount(Long userId, Long activityId) {
        return stringRedisTemplate.opsForSet().size(RedisKeys.USER_COUPON + activityId);
    }

    // ========== 通用缓存方法 ==========

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, String value, long ttl, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public boolean setIfAbsent(String key, String value, long ttl, TimeUnit unit) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl, unit));
    }

    /** 按模式扫描 key（用于关注列表等场景） */
    public java.util.Set<String> scanKeys(String pattern) {
        return stringRedisTemplate.keys(pattern);
    }
}
