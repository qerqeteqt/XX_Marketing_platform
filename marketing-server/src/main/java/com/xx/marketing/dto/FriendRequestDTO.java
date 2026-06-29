package com.xx.marketing.dto;

import lombok.Data;

@Data
public class FriendRequestDTO {
    private Long id;
    private Long fromUserId;
    private String fromNickname;
    private String fromPhone;
    private String status;
    private String createTime;
}
