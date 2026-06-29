package com.xx.marketing.common;

/**
 * Redis Key 前缀常量
 */
public class RedisKeys {
    /** 验证码前缀 */
    public static final String SMS_CODE = "sms:code:";

    /** 验证码发送间隔 */
    public static final String SMS_INTERVAL = "sms:interval:";

    /** JWT Access Token 黑名单 */
    public static final String TOKEN_BLACKLIST = "token:blacklist:";

    /** 登录用户信息 */
    public static final String LOGIN_USER = "login:user:";

    /** Refresh Token */
    public static final String REFRESH_TOKEN = "refresh:token:";

    /** 优惠券活动库存 */
    public static final String COUPON_STOCK = "coupon:stock:";

    /** 用户已抢券集合 */
    public static final String USER_COUPON = "coupon:user:";

    /** 优惠券活动详情缓存 */
    public static final String COUPON_DETAIL = "coupon:detail:";

    /** 首页服务列表缓存 */
    public static final String HOME_SERVICE = "home:service:";

    /** 商家信息缓存 */
    public static final String MERCHANT_INFO = "merchant:info:";

    /** 服务详情缓存 */
    public static final String SERVICE_DETAIL = "service:detail:";

    /** 布隆过滤器 Key（优惠券活动ID） */
    public static final String BLOOM_COUPON = "bloom:coupon";

    /** 布隆过滤器 Key（商家ID） */
    public static final String BLOOM_MERCHANT = "bloom:merchant";

    /** 用户关注集合 */
    public static final String FOLLOW_USER = "follow:user:";

    /** 用户粉丝集合（被关注） */
    public static final String FOLLOW_FANS = "follow:fans:";

    /** 商家优惠券列表缓存 */
    public static final String MERCHANT_COUPONS = "merchant:coupons:";
}
