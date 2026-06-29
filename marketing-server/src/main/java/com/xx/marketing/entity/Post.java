package com.xx.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_post")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作者用户ID */
    private Long userId;

    /** 帖子内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
