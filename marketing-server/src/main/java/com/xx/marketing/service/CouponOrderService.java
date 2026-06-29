package com.xx.marketing.service;

import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.CouponOrder;
import com.xx.marketing.entity.Merchant;

import java.util.List;

public interface CouponOrderService {
    Result<List<CouponOrder>> getUserOrders();
    Result<Void> useCoupon(Long orderId);
}
