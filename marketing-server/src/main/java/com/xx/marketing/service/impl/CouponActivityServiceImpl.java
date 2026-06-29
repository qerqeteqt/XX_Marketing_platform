package com.xx.marketing.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xx.marketing.common.RedisKeys;
import com.xx.marketing.common.UserContext;
import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.CouponActivity;
import com.xx.marketing.entity.CouponOrder;
import com.xx.marketing.mapper.CouponActivityMapper;
import com.xx.marketing.mapper.CouponOrderMapper;
import com.xx.marketing.service.CouponActivityService;
import com.xx.marketing.utils.BloomFilterUtil;
import com.xx.marketing.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponActivityServiceImpl implements CouponActivityService {

    private final CouponActivityMapper activityMapper;
    private final CouponOrderMapper orderMapper;
    private final RedisUtil redisUtil;
    private final BloomFilterUtil bloomFilterUtil;
    private final RedissonClient redissonClient;

    @Value("${marketing.coupon.max-per-user:3}")
    private int maxPerUser;

    @Value("${marketing.coupon.cache-ttl-min:1800}")
    private int cacheTtlMin;

    @Value("${marketing.coupon.cache-ttl-max:3600}")
    private int cacheTtlMax;

    private final Random random = new Random();

    // 预加载 Lua 脚本
    private String grabCouponScript;

    @jakarta.annotation.PostConstruct
    public void init() throws Exception {
        ClassPathResource resource = new ClassPathResource("lua/grab_coupon.lua");
        grabCouponScript = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        log.info("Lua script loaded successfully");
    }

    // ========== 查询活动列表（Cache Aside + 定时任务兜底） ==========

    @Override
    public Result<List<CouponActivity>> listActiveCoupons() {
        // 1. 尝试从缓存获取首页数据
        String cached = redisUtil.get(RedisKeys.HOME_SERVICE);
        if (cached != null) {
            List<CouponActivity> list = JSONUtil.toList(cached, CouponActivity.class);
            log.debug("Home service hit cache, count: {}", list.size());
            return Result.ok(list);
        }

        // 2. Cache miss — 查询数据库
        List<CouponActivity> activities = activityMapper.selectList(
                new LambdaQueryWrapper<CouponActivity>()
                        .eq(CouponActivity::getStatus, 1)
                        .orderByDesc(CouponActivity::getCreateTime));

        // 3. 写入缓存（随机TTL防雪崩）
        if (activities != null && !activities.isEmpty()) {
            int ttl = cacheTtlMin + random.nextInt(cacheTtlMax - cacheTtlMin + 1);
            redisUtil.set(RedisKeys.HOME_SERVICE, JSONUtil.toJsonStr(activities), ttl, TimeUnit.SECONDS);
            log.debug("Home service cached, TTL: {}s", ttl);
        } else {
            // 缓存空值（防穿透），短TTL
            redisUtil.set(RedisKeys.HOME_SERVICE, "[]", 60, TimeUnit.SECONDS);
        }

        return Result.ok(activities != null ? activities : List.<CouponActivity>of());
    }

    // ========== 按商家查询优惠券 ==========

    @Override
    public Result<List<CouponActivity>> listByMerchantId(Long merchantId) {
        String cacheKey = RedisKeys.MERCHANT_COUPONS + merchantId;
        String cached = redisUtil.get(cacheKey);
        if (cached != null) {
            if ("[]".equals(cached)) return Result.ok(List.of());
            return Result.ok(JSONUtil.toList(cached, CouponActivity.class));
        }

        List<CouponActivity> list = activityMapper.selectList(
                new LambdaQueryWrapper<CouponActivity>()
                        .eq(CouponActivity::getMerchantId, merchantId)
                        .eq(CouponActivity::getStatus, 1));

        if (list != null && !list.isEmpty()) {
            int ttl = cacheTtlMin + random.nextInt(cacheTtlMax - cacheTtlMin + 1);
            redisUtil.set(cacheKey, JSONUtil.toJsonStr(list), ttl, TimeUnit.SECONDS);
        } else {
            redisUtil.set(cacheKey, "[]", 60, TimeUnit.SECONDS);
        }

        return Result.ok(list != null ? list : List.<CouponActivity>of());
    }

    // ========== 查询活动详情（布隆过滤器 + 缓存永不过期 + TTL兜底） ==========

    @Override
    public Result<CouponActivity> getCouponDetail(Long id) {
        // 1. 查缓存
        String cached = redisUtil.get(RedisKeys.COUPON_DETAIL + id);
        if (cached != null) {
            if ("NULL".equals(cached)) return Result.fail(404, "活动不存在");
            return Result.ok(JSONUtil.toBean(cached, CouponActivity.class));
        }

        // 2. 查数据库
        CouponActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            redisUtil.set(RedisKeys.COUPON_DETAIL + id, "NULL", 60, TimeUnit.SECONDS);
            bloomFilterUtil.addCoupon(String.valueOf(id));  // 记录不存在
            return Result.fail(404, "活动不存在");
        }

        // 3. 写缓存 + 布隆过滤器
        bloomFilterUtil.addCoupon(String.valueOf(id));  // 记录存在

        // 4. 写缓存（逻辑过期 + 随机TTL兜底）
        int ttl = cacheTtlMin + random.nextInt(cacheTtlMax - cacheTtlMin + 1);
        redisUtil.set(RedisKeys.COUPON_DETAIL + id, JSONUtil.toJsonStr(activity), ttl, TimeUnit.SECONDS);

        // 同步库存到 Redis
        if (!redisUtil.hasKey(RedisKeys.COUPON_STOCK + id)) {
            redisUtil.setCouponStock(id, activity.getRemainStock());
        }

        return Result.ok(activity);
    }

    // ========== 抢券（Lua脚本原子操作） ==========

    @Override
    @Transactional
    public Result<String> grabCoupon(Long activityId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }

        // 兜底：Redis 库存不存在时从 DB 初始化
        String stockKey = RedisKeys.COUPON_STOCK + activityId;
        if (!redisUtil.hasKey(stockKey)) {
            CouponActivity act = activityMapper.selectById(activityId);
            if (act == null) return Result.fail("活动不存在");
            redisUtil.setCouponStock(activityId, act.getRemainStock());
        }

        // 执行 Lua 脚本（原子性：库存校验 + 扣减 + 用户资格检查）
        String userKey = RedisKeys.USER_COUPON + activityId;

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        List<Object> result = script.eval(
                RScript.Mode.READ_WRITE,
                grabCouponScript,
                RScript.ReturnType.MULTI,
                Collections.singletonList(stockKey),
                userKey,
                String.valueOf(userId),
                String.valueOf(maxPerUser)
        );

        int code = Integer.parseInt(result.get(0).toString());
        if (code != 1) {
            switch (code) {
                case -1: return Result.fail("库存不足");
                case -2: return Result.fail("您已达到领取上限（" + maxPerUser + "张）");
                case -3: return Result.fail("您已领取过该优惠券");
                default: return Result.fail("活动已结束");
            }
        }

        // ======== Redis 扣减成功，同步数据库 ========

        CouponActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return Result.fail("活动不存在");
        }

        // 1. 创建订单
        CouponOrder order = new CouponOrder();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setMerchantId(activity.getMerchantId());
        order.setAmount(activity.getAmount());
        order.setStatus(0);
        order.setExpireTime(LocalDateTime.now().plusDays(30));
        orderMapper.insert(order);

        // 2. 扣减数据库库存
        activity.setRemainStock(activity.getRemainStock() - 1);
        activityMapper.updateById(activity);

        // 3. 清除所有相关缓存
        redisUtil.delete(RedisKeys.COUPON_DETAIL + activityId);
        redisUtil.delete(RedisKeys.MERCHANT_COUPONS + activity.getMerchantId());
        redisUtil.delete(RedisKeys.HOME_SERVICE);

        log.info("User {} grabbed coupon {}, order={}", userId, activityId, order.getOrderNo());
        return Result.ok("抢券成功");
    }

    // ========== 查询库存 ==========

    @Override
    public Result<Long> getActivityStock(Long activityId) {
        Long stock = redisUtil.getCouponStock(activityId);
        if (stock == null) {
            CouponActivity activity = activityMapper.selectById(activityId);
            if (activity == null) return Result.fail(404, "活动不存在");
            stock = (long) activity.getRemainStock();
            redisUtil.setCouponStock(activityId, activity.getRemainStock());
        }
        return Result.ok(stock);
    }
}
