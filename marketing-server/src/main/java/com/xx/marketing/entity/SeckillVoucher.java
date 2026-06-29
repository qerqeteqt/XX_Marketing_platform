package com.xx.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_seckill_voucher")
public class SeckillVoucher {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联优惠券活动ID */
    private Long activityId;

    /** 秒杀价格（优惠购买价） */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 每人限购数量 */
    private Integer maxPerUser;

    /** 秒杀开始时间 */
    private LocalDateTime beginTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /** 状态: 1-进行中, 0-已结束 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
