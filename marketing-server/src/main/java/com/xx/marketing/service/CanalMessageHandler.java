package com.xx.marketing.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xx.marketing.common.RedisKeys;
import com.xx.marketing.config.RabbitMQConfig;
import com.xx.marketing.entity.CouponActivity;
import com.xx.marketing.entity.Merchant;
import com.xx.marketing.mapper.CouponActivityMapper;
import com.xx.marketing.mapper.MerchantMapper;
import com.xx.marketing.utils.BloomFilterUtil;
import com.xx.marketing.utils.RedisUtil;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Canal 消息处理器
 * 监听 Canal 发送到 RabbitMQ 的数据库变更消息，同步更新 Redis 缓存
 *
 * 需要在 Canal 配置中设置 RabbitMQ 为消息投递目标
 * 当 canal.enabled=true 时启用
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "canal.enabled", havingValue = "true")
public class CanalMessageHandler {

    private final RedisUtil redisUtil;
    private final BloomFilterUtil bloomFilterUtil;
    private final CouponActivityMapper activityMapper;
    private final MerchantMapper merchantMapper;

    private final Random random = new Random();

    /**
     * 处理优惠券活动的变更消息
     */
    @RabbitListener(queues = RabbitMQConfig.CANAL_QUEUE_COUPON)
    public void handleCouponChange(Message message, Channel channel) throws IOException {
        try {
            String body = new String(message.getBody());
            JSONObject json = JSONUtil.parseObj(body);

            String type = json.getStr("type"); // INSERT / UPDATE / DELETE
            String table = json.getStr("table");
            List<JSONObject> data = json.getJSONArray("data").toList(JSONObject.class);

            log.info("Canal sync: table={}, type={}, rows={}", table, type, data.size());

            for (JSONObject row : data) {
                Long activityId = row.getLong("id");

                if ("DELETE".equals(type)) {
                    // 删除缓存
                    redisUtil.delete(RedisKeys.COUPON_DETAIL + activityId);
                    redisUtil.delete(RedisKeys.COUPON_STOCK + activityId);
                } else {
                    // INSERT / UPDATE: 重新加载数据并更新缓存
                    CouponActivity activity = activityMapper.selectById(activityId);
                    if (activity != null) {
                        // 更新详情缓存
                        int ttl = 1800 + random.nextInt(1800);
                        redisUtil.set(RedisKeys.COUPON_DETAIL + activityId,
                                JSONUtil.toJsonStr(activity), ttl, java.util.concurrent.TimeUnit.SECONDS);

                        // 同步库存
                        redisUtil.setCouponStock(activityId, activity.getRemainStock());

                        // 更新布隆过滤器
                        bloomFilterUtil.add(String.valueOf(activityId));
                    }
                }
            }

            // 删除首页缓存，下次请求时重建
            redisUtil.delete(RedisKeys.HOME_SERVICE);

            // 手动ACK
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("Canal message handle error", e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * 处理商家信息的变更消息
     */
    @RabbitListener(queues = RabbitMQConfig.CANAL_QUEUE_MERCHANT)
    public void handleMerchantChange(Message message, Channel channel) throws IOException {
        try {
            String body = new String(message.getBody());
            JSONObject json = JSONUtil.parseObj(body);

            String type = json.getStr("type");
            List<JSONObject> data = json.getJSONArray("data").toList(JSONObject.class);

            log.info("Canal sync merchant: type={}, rows={}", type, data.size());

            for (JSONObject row : data) {
                Long merchantId = row.getLong("id");

                if ("DELETE".equals(type)) {
                    redisUtil.delete(RedisKeys.MERCHANT_INFO + merchantId);
                } else {
                    Merchant merchant = merchantMapper.selectById(merchantId);
                    if (merchant != null) {
                        int ttl = 1800 + random.nextInt(1800);
                        redisUtil.set(RedisKeys.MERCHANT_INFO + merchantId,
                                JSONUtil.toJsonStr(merchant), ttl, java.util.concurrent.TimeUnit.SECONDS);
                    }
                }
            }

            redisUtil.delete(RedisKeys.HOME_SERVICE + "merchant");
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("Canal merchant message handle error", e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
