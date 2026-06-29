package com.xx.marketing.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ========== Canal 同步交换机/队列 ==========

    public static final String CANAL_EXCHANGE = "canal.exchange";
    public static final String CANAL_QUEUE_COUPON = "canal.queue.coupon";
    public static final String CANAL_QUEUE_MERCHANT = "canal.queue.merchant";
    public static final String CANAL_ROUTING_COUPON = "canal.routing.coupon";
    public static final String CANAL_ROUTING_MERCHANT = "canal.routing.merchant";

    @Bean
    public TopicExchange canalExchange() {
        return new TopicExchange(CANAL_EXCHANGE, true, false);
    }

    @Bean
    public Queue canalQueueCoupon() {
        return new Queue(CANAL_QUEUE_COUPON, true);
    }

    @Bean
    public Queue canalQueueMerchant() {
        return new Queue(CANAL_QUEUE_MERCHANT, true);
    }

    @Bean
    public Binding bindingCoupon() {
        return BindingBuilder.bind(canalQueueCoupon()).to(canalExchange()).with(CANAL_ROUTING_COUPON);
    }

    @Bean
    public Binding bindingMerchant() {
        return BindingBuilder.bind(canalQueueMerchant()).to(canalExchange()).with(CANAL_ROUTING_MERCHANT);
    }

    // ========== 抢券异步下单队列 ==========

    public static final String COUPON_ORDER_QUEUE = "coupon.order.queue";
    public static final String COUPON_ORDER_EXCHANGE = "coupon.order.exchange";
    public static final String COUPON_ORDER_ROUTING = "coupon.order.routing";

    @Bean
    public Queue couponOrderQueue() {
        return new Queue(COUPON_ORDER_QUEUE, true);
    }

    @Bean
    public DirectExchange couponOrderExchange() {
        return new DirectExchange(COUPON_ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Binding bindingCouponOrder() {
        return BindingBuilder.bind(couponOrderQueue()).to(couponOrderExchange()).with(COUPON_ORDER_ROUTING);
    }
}
