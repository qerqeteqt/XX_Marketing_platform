package com.xx.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_coupon_order")
public class CouponOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 优惠券活动ID */
    private Long activityId;

    /** 商家ID */
    private Long merchantId;

    /** 优惠券面值 */
    private BigDecimal amount;

    /** 状态: 0-未使用, 1-已使用, 2-已过期 */
    private Integer status;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
