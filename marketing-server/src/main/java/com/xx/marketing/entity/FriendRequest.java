package com.xx.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_friend_request")
public class FriendRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起方用户ID */
    private Long fromUserId;

    /** 接收方用户ID */
    private Long toUserId;

    /** 发起方手机号 */
    private String fromPhone;

    /** 接收方手机号 */
    private String toPhone;

    /** 状态: PENDING-待处理, ACCEPTED-已通过, REJECTED-已拒绝 */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
