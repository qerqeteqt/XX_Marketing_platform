package com.xx.marketing.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class LoginDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 登录类型: SMS-验证码登录, PASSWORD-密码登录 */
    @NotBlank(message = "登录类型不能为空")
    private String loginType;

    /** 验证码 (SMS登录时必填) */
    private String code;

    /** 密码 (PASSWORD登录时必填) */
    private String password;
}
