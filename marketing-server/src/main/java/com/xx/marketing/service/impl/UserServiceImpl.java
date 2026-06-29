package com.xx.marketing.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xx.marketing.common.RedisKeys;
import com.xx.marketing.common.UserContext;
import com.xx.marketing.dto.*;
import com.xx.marketing.entity.User;
import com.xx.marketing.mapper.UserMapper;
import com.xx.marketing.service.UserService;
import com.xx.marketing.utils.JwtUtil;
import com.xx.marketing.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    @Value("${marketing.sms.code-length:6}")
    private int codeLength;

    @Value("${marketing.sms.code-ttl:300}")
    private long codeTtl;

    @Value("${marketing.sms.send-interval:60}")
    private long sendInterval;

    @Override
    public Result<Void> sendSmsCode(String phone) {
        // 检查发送间隔
        if (redisUtil.hasSmsInterval(phone)) {
            return Result.fail("操作过于频繁，请稍后再试");
        }

        // 生成6位验证码
        String code = RandomUtil.randomNumbers(codeLength);

        // 存入 Redis，有效期5分钟
        redisUtil.saveSmsCode(phone, code, codeTtl);

        // 设置发送间隔
        redisUtil.setSmsInterval(phone, sendInterval);

        // 模拟发送（实际接入短信服务商）
        log.info("【模拟短信】手机号: {}, 验证码: {}", phone, code);

        return Result.okMsg("验证码已发送（演示环境验证码: " + code + "）");
    }

    @Override
    public Result<TokenPairDTO> login(LoginDTO loginDTO) {
        String phone = loginDTO.getPhone();

        // 查询或创建用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        if ("SMS".equals(loginDTO.getLoginType())) {
            // 验证码登录
            String cachedCode = redisUtil.getSmsCode(phone);
            if (cachedCode == null || !cachedCode.equals(loginDTO.getCode())) {
                return Result.fail(400, "验证码错误或已过期");
            }

            if (user == null) {
                // 新用户自动注册
                user = new User();
                user.setPhone(phone);
                user.setNickname("用户" + phone.substring(7));
                user.setRole("USER");
                user.setStatus(1);
                userMapper.insert(user);
            }

            // 删除验证码
            redisUtil.deleteSmsCode(phone);

        } else if ("PASSWORD".equals(loginDTO.getLoginType())) {
            // 密码登录
            if (user == null) {
                return Result.fail(400, "用户不存在");
            }
            // 实际项目中应使用 BCrypt 验证
            if (!loginDTO.getPassword().equals(user.getPassword())) {
                return Result.fail(400, "密码错误");
            }
        } else {
            return Result.fail(400, "不支持的登录类型");
        }

        // 生成 Token
        TokenPairDTO tokenPair = generateTokens(user);

        // 缓存用户信息
        UserDTO userDTO = toDTO(user);
        redisUtil.saveLoginUser(user.getId(), JSONUtil.toJsonStr(userDTO), 30 * 60);

        log.info("用户 {} 登录成功", phone);
        return Result.ok(tokenPair);
    }

    @Override
    public Result<TokenPairDTO> refreshToken(String refreshToken) {
        String userIdStr = redisUtil.getUserIdByRefreshToken(refreshToken);
        if (userIdStr == null) {
            return Result.fail(401, "Refresh Token 无效或已过期");
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }

        // 删除旧的 Refresh Token
        redisUtil.deleteRefreshToken(refreshToken);

        // 生成新的 Token 对
        TokenPairDTO newTokens = generateTokens(user);

        return Result.ok(newTokens);
    }

    @Override
    public Result<Void> logout(String accessToken, String refreshToken) {
        // Access Token 加入黑名单
        if (accessToken != null) {
            redisUtil.blacklistAccessToken(accessToken, 1800);
        }
        // 删除 Refresh Token
        if (refreshToken != null) {
            redisUtil.deleteRefreshToken(refreshToken);
        }
        UserContext.remove();
        return Result.ok();
    }

    @Override
    public Result<Void> register(RegisterDTO registerDTO) {
        String phone = registerDTO.getPhone();
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (existUser != null) {
            return Result.fail(400, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(phone);
        user.setPassword(registerDTO.getPassword());
        user.setNickname(registerDTO.getNickname());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        log.info("新用户注册: phone={}, nickname={}", phone, registerDTO.getNickname());
        return Result.okMsg("注册成功");
    }

    // ========== 关注功能（Redis Set） ==========

    @Override
    public Result<Void> followUser(Long targetUserId) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");
        if (userId.equals(targetUserId)) return Result.fail(400, "不能关注自己");
        redisUtil.set(RedisKeys.FOLLOW_USER + userId + ":" + targetUserId, "1");
        redisUtil.set(RedisKeys.FOLLOW_FANS + targetUserId + ":" + userId, "1");
        log.info("User {} followed {}", userId, targetUserId);
        return Result.okMsg("关注成功");
    }

    @Override
    public Result<Void> unfollowUser(Long targetUserId) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");
        redisUtil.delete(RedisKeys.FOLLOW_USER + userId + ":" + targetUserId);
        redisUtil.delete(RedisKeys.FOLLOW_FANS + targetUserId + ":" + userId);
        return Result.okMsg("已取消关注");
    }

    @Override
    public Result<List<UserDTO>> getFollowingList() {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");
        Set<String> keys = redisUtil.scanKeys(RedisKeys.FOLLOW_USER + userId + ":*");
        if (keys == null || keys.isEmpty()) return Result.ok(List.of());
        List<UserDTO> list = new ArrayList<>();
        for (String key : keys) {
            String targetId = key.substring(key.lastIndexOf(":") + 1);
            User u = userMapper.selectById(Long.parseLong(targetId));
            if (u != null) list.add(toDTO(u));
        }
        return Result.ok(list);
    }

    @Override
    public Result<Boolean> isFollowing(Long targetUserId) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.ok(false);
        return Result.ok(redisUtil.hasKey(RedisKeys.FOLLOW_USER + userId + ":" + targetUserId));
    }

    @Override
    public UserDTO getCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) return null;

        // 先从缓存获取
        String cached = redisUtil.getLoginUser(userId);
        if (cached != null) {
            return JSONUtil.toBean(cached, UserDTO.class);
        }

        // 从数据库获取
        User user = userMapper.selectById(userId);
        if (user == null) return null;

        UserDTO dto = toDTO(user);
        redisUtil.saveLoginUser(userId, JSONUtil.toJsonStr(dto), 30 * 60);
        return dto;
    }

    // ========== 私有方法 ==========

    private TokenPairDTO generateTokens(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("phone", user.getPhone());
        claims.put("nickname", user.getNickname());
        claims.put("role", user.getRole());

        String accessToken = jwtUtil.generateAccessToken(claims);
        String refreshToken = jwtUtil.generateRefreshToken(claims);

        // 存储 Refresh Token
        redisUtil.saveRefreshToken(refreshToken, user.getId(), 7 * 24 * 3600);

        TokenPairDTO dto = new TokenPairDTO();
        dto.setAccessToken(accessToken);
        dto.setRefreshToken(refreshToken);
        dto.setExpiresIn(1800L); // 30 minutes
        return dto;
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setPhone(user.getPhone());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        return dto;
    }
}
