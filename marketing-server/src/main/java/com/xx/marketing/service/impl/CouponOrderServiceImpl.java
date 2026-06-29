package com.xx.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xx.marketing.common.UserContext;
import com.xx.marketing.dto.Result;
import com.xx.marketing.entity.CouponOrder;
import com.xx.marketing.mapper.CouponOrderMapper;
import com.xx.marketing.service.CouponOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponOrderServiceImpl implements CouponOrderService {

    private final CouponOrderMapper orderMapper;

    @Override
    public Result<List<CouponOrder>> getUserOrders() {
        Long userId = UserContext.getUserId();
        List<CouponOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<CouponOrder>()
                        .eq(CouponOrder::getUserId, userId)
                        .orderByDesc(CouponOrder::getCreateTime));
        return Result.ok(orders);
    }

    @Override
    public Result<Void> useCoupon(Long orderId) {
        CouponOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        if (!order.getUserId().equals(UserContext.getUserId())) {
            return Result.fail(403, "无权操作此订单");
        }
        if (order.getStatus() != 0) {
            return Result.fail("该优惠券已使用或已过期");
        }
        order.setStatus(1);
        orderMapper.updateById(order);
        return Result.ok();
    }
}
