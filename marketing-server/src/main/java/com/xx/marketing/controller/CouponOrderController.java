package com.xx.marketing.controller;

import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.CouponOrder;
import com.xx.marketing.service.CouponOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class CouponOrderController {

    private final CouponOrderService orderService;

    /**
     * 我的优惠券订单
     */
    @GetMapping("/my")
    public Result<List<CouponOrder>> myOrders() {
        return orderService.getUserOrders();
    }

    /**
     * 使用优惠券
     */
    @PostMapping("/use/{orderId}")
    public Result<Void> useCoupon(@PathVariable Long orderId) {
        return orderService.useCoupon(orderId);
    }
}
