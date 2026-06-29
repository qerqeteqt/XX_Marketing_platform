package com.xx.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** 店铺名称 */
    private String shopName;

    /** 店铺Logo */
    private String logo;

    /** 店铺描述 */
    private String description;

    /** 店铺类型 */
    private String type;

    /** 评分 */
    private Double rating;

    /** 销量 */
    private Integer sales;

    /** 地址 */
    private String address;

    /** 营业时间 */
    private String businessHours;

    /** 状态: 1-营业, 0-歇业 */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
