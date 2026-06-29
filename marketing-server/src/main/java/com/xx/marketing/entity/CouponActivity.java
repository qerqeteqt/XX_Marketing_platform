package com.xx.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_coupon_activity")
public class CouponActivity {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商家ID */
    private Long merchantId;

    /** 活动名称 */
    private String name;

    /** 活动描述 */
    private String description;

    /** 优惠券面值 */
    private BigDecimal amount;

    /** 最低消费金额 */
    private BigDecimal minAmount;

    /** 总库存 */
    private Integer totalStock;

    /** 剩余库存 */
    private Integer remainStock;

    /** 每人限领数量 */
    private Integer maxPerUser;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 状态: 1-进行中, 0-已结束, 2-未开始 */
    private Integer status;

    /** 类型: FULL_REDUCTION-满减, DISCOUNT-折扣, CASH-代金券 */
    private String type;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
