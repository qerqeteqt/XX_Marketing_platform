package com.xx.marketing.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private String role;
}
