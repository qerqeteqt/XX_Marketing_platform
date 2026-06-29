package com.xx.marketing.dto;

import lombok.Data;

@Data
public class PostDTO {
    private Long id;
    private Long userId;
    private String nickname;
    private String content;
    private Integer likeCount;
    private Boolean liked;       // 当前用户是否已点赞
    private String createTime;
}
