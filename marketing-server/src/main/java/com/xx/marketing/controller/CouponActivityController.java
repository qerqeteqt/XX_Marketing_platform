package com.xx.marketing.controller;

import com.xx.marketing.dto.GrabCouponDTO;
import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.CouponActivity;
import com.xx.marketing.service.CouponActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponActivityController {

    private final CouponActivityService couponActivityService;

    /**
     * 获取活动列表（首页）
     */
    @GetMapping("/list")
    public Result<List<CouponActivity>> list() {
        return couponActivityService.listActiveCoupons();
    }

    /**
     * 按商家ID查询优惠券
     */
    @GetMapping("/merchant/{merchantId}")
    public Result<List<CouponActivity>> listByMerchant(@PathVariable Long merchantId) {
        return couponActivityService.listByMerchantId(merchantId);
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/detail/{id}")
    public Result<CouponActivity> detail(@PathVariable Long id) {
        return couponActivityService.getCouponDetail(id);
    }

    /**
     * 抢券（核心接口）
     */
    @PostMapping("/grab")
    public Result<String> grab(@RequestBody GrabCouponDTO dto) {
        return couponActivityService.grabCoupon(dto.getActivityId());
    }

    /**
     * 查询库存
     */
    @GetMapping("/stock/{id}")
    public Result<Long> stock(@PathVariable Long id) {
        return couponActivityService.getActivityStock(id);
    }
}
