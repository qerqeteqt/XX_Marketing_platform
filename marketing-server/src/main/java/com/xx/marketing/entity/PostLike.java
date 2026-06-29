package com.xx.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_post_like")
public class PostLike {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 点赞用户ID */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
