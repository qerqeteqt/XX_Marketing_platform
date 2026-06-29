package com.xx.marketing.task;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xx.marketing.common.RedisKeys;
import com.xx.marketing.entity.CouponActivity;
import com.xx.marketing.mapper.CouponActivityMapper;
import com.xx.marketing.utils.BloomFilterUtil;
import com.xx.marketing.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务
 * 1. 首页数据定时刷新（Cache Aside 兜底）
 * 2. 布隆过滤器定时重建
 * 3. 优惠券库存定时同步
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheSyncTask {

    private final CouponActivityMapper activityMapper;
    private final RedisUtil redisUtil;
    private final BloomFilterUtil bloomFilterUtil;

    private final Random random = new Random();

    /**
     * 每5分钟刷新首页热门活动缓存
     * — 保证Cache Aside模式的最终一致性
     */
    @Scheduled(fixedRate = 300_000)
    public void refreshHomeService() {
        try {
            List<CouponActivity> activities = activityMapper.selectList(
                    new LambdaQueryWrapper<CouponActivity>()
                            .eq(CouponActivity::getStatus, 1)
                            .orderByDesc(CouponActivity::getCreateTime));

            if (activities != null && !activities.isEmpty()) {
                int ttl = 1800 + random.nextInt(1800);
                redisUtil.set(RedisKeys.HOME_SERVICE, JSONUtil.toJsonStr(activities), ttl, TimeUnit.SECONDS);
                log.debug("Scheduled: Home service cache refreshed, count: {}", activities.size());
            }
        } catch (Exception e) {
            log.error("Refresh home service cache failed", e);
        }
    }

    /**
     * 每30分钟同步活动库存到Redis
     * — 保证Redis库存与数据库一致
     */
    @Scheduled(fixedRate = 1_800_000)
    public void syncCouponStock() {
        try {
            List<CouponActivity> activities = activityMapper.selectList(
                    new LambdaQueryWrapper<CouponActivity>()
                            .eq(CouponActivity::getStatus, 1));

            for (CouponActivity activity : activities) {
                String key = RedisKeys.COUPON_STOCK + activity.getId();
                if (!redisUtil.hasKey(key)) {
                    redisUtil.setCouponStock(activity.getId(), activity.getRemainStock());
                }
            }
            log.debug("Scheduled: Stock synced, activities: {}", activities.size());
        } catch (Exception e) {
            log.error("Sync coupon stock failed", e);
        }
    }
}
