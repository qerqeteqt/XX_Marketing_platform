package com.xx.marketing.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xx.marketing.common.RedisKeys;
import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.Merchant;
import com.xx.marketing.mapper.MerchantMapper;
import com.xx.marketing.service.MerchantService;
import com.xx.marketing.utils.BloomFilterUtil;
import com.xx.marketing.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final RedisUtil redisUtil;
    private final BloomFilterUtil bloomFilterUtil;

    private final Random random = new Random();

    @Override
    public Result<List<Merchant>> getHotMerchants() {
        // Cache Aside 模式
        String cached = redisUtil.get(RedisKeys.HOME_SERVICE + "merchant");
        if (cached != null) {
            if ("[]".equals(cached)) return Result.ok(List.of());
            return Result.ok(JSONUtil.toList(cached, Merchant.class));
        }

        List<Merchant> merchants = merchantMapper.selectList(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getStatus, 1)
                        .orderByDesc(Merchant::getSales)
                        .last("LIMIT 10"));

        if (merchants != null && !merchants.isEmpty()) {
            int ttl = 1800 + random.nextInt(1800); // 30~60分钟随机TTL
            redisUtil.set(RedisKeys.HOME_SERVICE + "merchant", JSONUtil.toJsonStr(merchants), ttl, TimeUnit.SECONDS);
        } else {
            redisUtil.set(RedisKeys.HOME_SERVICE + "merchant", "[]", 60, TimeUnit.SECONDS);
        }

        return Result.ok(merchants != null ? merchants : List.<Merchant>of());
    }

    @Override
    public Result<Merchant> getMerchantDetail(Long id) {
        // Cache Aside 详情查询
        String key = RedisKeys.MERCHANT_INFO + id;
        String cached = redisUtil.get(key);
        if (cached != null) {
            if ("NULL".equals(cached)) return Result.fail(404, "商家不存在");
            return Result.ok(JSONUtil.toBean(cached, Merchant.class));
        }

        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            // 缓存空值 + 加入布隆过滤器
            redisUtil.set(key, "NULL", 60, TimeUnit.SECONDS);
            bloomFilterUtil.addMerchant(String.valueOf(id));  // 记录：这个ID不存在
            return Result.fail(404, "商家不存在");
        }

        bloomFilterUtil.addMerchant(String.valueOf(id));  // 记录：这个ID存在

        int ttl = 1800 + random.nextInt(1800);
        redisUtil.set(key, JSONUtil.toJsonStr(merchant), ttl, TimeUnit.SECONDS);

        return Result.ok(merchant);
    }
}
