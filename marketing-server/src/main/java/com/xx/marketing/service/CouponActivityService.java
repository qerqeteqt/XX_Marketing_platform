package com.xx.marketing.service;

import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.CouponActivity;
import java.util.List;

public interface CouponActivityService {
    Result<List<CouponActivity>> listActiveCoupons();
    Result<List<CouponActivity>> listByMerchantId(Long merchantId);
    Result<CouponActivity> getCouponDetail(Long id);
    Result<String> grabCoupon(Long activityId);
    Result<Long> getActivityStock(Long activityId);
}
