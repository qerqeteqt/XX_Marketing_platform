package com.xx.marketing.dto;

import lombok.Data;

@Data
public class TokenPairDTO {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
