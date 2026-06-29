package com.xx.marketing.service;

import com.xx.marketing.dto.*;

import java.util.List;

public interface UserService {
    Result<Void> sendSmsCode(String phone);
    Result<TokenPairDTO> login(LoginDTO loginDTO);
    Result<Void> register(RegisterDTO registerDTO);
    Result<TokenPairDTO> refreshToken(String refreshToken);
    Result<Void> logout(String accessToken, String refreshToken);
    UserDTO getCurrentUser();

    // ========== 关注功能 ==========
    Result<Void> followUser(Long targetUserId);
    Result<Void> unfollowUser(Long targetUserId);
    Result<List<UserDTO>> getFollowingList();
    Result<Boolean> isFollowing(Long targetUserId);
}
