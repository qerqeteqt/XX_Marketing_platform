package com.xx.marketing.controller;

import com.xx.marketing.common.UserContext;
import com.xx.marketing.dto.*;
import com.xx.marketing.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 发送短信验证码
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestParam String phone) {
        return userService.sendSmsCode(phone);
    }

    /**
     * 登录（验证码 / 密码）
     */
    @PostMapping("/login")
    public Result<TokenPairDTO> login(@RequestBody LoginDTO loginDTO) {
        return userService.login(loginDTO);
    }

    /**
     * 刷新 Token（无感刷新）
     */
    @PostMapping("/refresh")
    public Result<TokenPairDTO> refresh(@RequestParam String refreshToken) {
        return userService.refreshToken(refreshToken);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String refreshToken) {
        String accessToken = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            accessToken = auth.substring(7);
        }
        return userService.logout(accessToken, refreshToken);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return userService.register(registerDTO);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserDTO> me() {
        UserDTO user = userService.getCurrentUser();
        if (user != null) {
            return Result.ok(user);
        }
        return Result.fail(401, "未登录");
    }

    // ========== 关注功能 ==========

    @PostMapping("/follow/{targetUserId}")
    public Result<Void> follow(@PathVariable Long targetUserId) {
        return userService.followUser(targetUserId);
    }

    @DeleteMapping("/follow/{targetUserId}")
    public Result<Void> unfollow(@PathVariable Long targetUserId) {
        return userService.unfollowUser(targetUserId);
    }

    @GetMapping("/following")
    public Result<List<UserDTO>> following() {
        return userService.getFollowingList();
    }

    @GetMapping("/is-following/{targetUserId}")
    public Result<Boolean> isFollowing(@PathVariable Long targetUserId) {
        return userService.isFollowing(targetUserId);
    }
}
