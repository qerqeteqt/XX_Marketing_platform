package com.xx.marketing.utils;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.xx.marketing.common.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import java.nio.charset.StandardCharsets;

/**
 * 布隆过滤器工具类 — 优惠券 + 商家 双过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterUtil {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int EXPECTED_INSERTIONS = 10_000;
    private static final double FPP = 0.01;

    /** 优惠券活动布隆过滤器 */
    private BloomFilter<String> couponBloomFilter;

    /** 商家布隆过滤器 */
    private BloomFilter<String> merchantBloomFilter;

    @PostConstruct
    public void init() {
        couponBloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), EXPECTED_INSERTIONS, FPP);
        merchantBloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), EXPECTED_INSERTIONS, FPP);
        log.info("BloomFilters initialized (coupon + merchant), expected insertions: {}, fpp: {}", EXPECTED_INSERTIONS, FPP);
    }

    // ========== 优惠券 ==========

    public void addCoupon(String key) {
        couponBloomFilter.put(key);
        stringRedisTemplate.opsForSet().add(RedisKeys.BLOOM_COUPON, key);
    }

    /** 判断优惠券ID是否可能存在 */
    public boolean mightContainCoupon(String key) {
        return couponBloomFilter.mightContain(key);
    }

    // ========== 商家 ==========

    public void addMerchant(String key) {
        merchantBloomFilter.put(key);
        stringRedisTemplate.opsForSet().add(RedisKeys.BLOOM_MERCHANT, key);
    }

    /** 判断商家ID是否可能存在 */
    public boolean mightContainMerchant(String key) {
        return merchantBloomFilter.mightContain(key);
    }

    // ========== 旧接口兼容 ==========

    /** @deprecated 使用 addCoupon 替代 */
    @Deprecated
    public void add(String key) {
        addCoupon(key);
    }

    /** @deprecated 使用 mightContainCoupon 替代 */
    @Deprecated
    public boolean mightContain(String key) {
        return mightContainCoupon(key);
    }
}
