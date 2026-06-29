package com.xx.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xx.marketing.dto.Result;
import com.xx.marketing.common.UserContext;
import com.xx.marketing.dto.FriendRequestDTO;
import com.xx.marketing.entity.FriendRequest;
import com.xx.marketing.entity.User;
import com.xx.marketing.mapper.FriendRequestMapper;
import com.xx.marketing.mapper.UserMapper;
import com.xx.marketing.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserMapper userMapper;
    private final FriendRequestMapper friendRequestMapper;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public Result<Void> sendRequest(String targetPhone) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");

        // 查找目标用户
        User target = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, targetPhone));
        if (target == null) return Result.fail(404, "未找到该手机号的用户");
        if (target.getId().equals(userId)) return Result.fail(400, "不能添加自己为好友");

        // 检查是否已经是好友
        boolean alreadyFriend = friendRequestMapper.exists(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getStatus, "ACCEPTED")
                        .and(w -> w
                            .eq(FriendRequest::getFromUserId, userId).eq(FriendRequest::getToUserId, target.getId())
                            .or()
                            .eq(FriendRequest::getFromUserId, target.getId()).eq(FriendRequest::getToUserId, userId)));
        if (alreadyFriend) return Result.fail(400, "已经是好友了");

        // 检查是否已有待处理的申请
        FriendRequest existing = friendRequestMapper.selectOne(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getStatus, "PENDING")
                        .and(w -> w
                            .eq(FriendRequest::getFromUserId, userId).eq(FriendRequest::getToUserId, target.getId())
                            .or()
                            .eq(FriendRequest::getFromUserId, target.getId()).eq(FriendRequest::getToUserId, userId)));
        if (existing != null) {
            if (existing.getFromUserId().equals(userId)) {
                return Result.fail(400, "已发送过好友申请，请等待对方处理");
            } else {
                // 对方已经给你发了申请，直接通过
                existing.setStatus("ACCEPTED");
                friendRequestMapper.updateById(existing);
                log.info("双向申请自动通过: user {} <-> {}", userId, target.getId());
                return Result.okMsg("已自动成为好友");
            }
        }

        // 查找当前用户信息（获取手机号）
        User me = userMapper.selectById(userId);
        if (me == null) return Result.fail(500, "用户信息异常");

        // 创建好友申请
        FriendRequest req = new FriendRequest();
        req.setFromUserId(userId);
        req.setToUserId(target.getId());
        req.setFromPhone(me.getPhone());
        req.setToPhone(targetPhone);
        req.setStatus("PENDING");
        friendRequestMapper.insert(req);

        log.info("好友申请已发送: {} -> {}", userId, target.getId());
        return Result.okMsg("好友申请已发送");
    }

    @Override
    @Transactional
    public Result<Void> handleRequest(Long requestId, boolean accept) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");

        FriendRequest req = friendRequestMapper.selectById(requestId);
        if (req == null) return Result.fail(404, "申请不存在");
        if (!req.getToUserId().equals(userId)) return Result.fail(403, "无权处理此申请");
        if (!"PENDING".equals(req.getStatus())) return Result.fail(400, "该申请已处理");

        req.setStatus(accept ? "ACCEPTED" : "REJECTED");
        friendRequestMapper.updateById(req);

        log.info("好友申请处理: requestId={}, accept={}", requestId, accept);
        return Result.okMsg(accept ? "已添加好友" : "已拒绝");
    }

    @Override
    public Result<List<FriendRequestDTO>> getReceivedRequests() {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");

        List<FriendRequest> requests = friendRequestMapper.selectList(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getToUserId, userId)
                        .eq(FriendRequest::getStatus, "PENDING")
                        .orderByDesc(FriendRequest::getCreateTime));

        return Result.ok(toDTOList(requests, userId));
    }

    @Override
    public Result<List<FriendRequestDTO>> getSentRequests() {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");

        List<FriendRequest> requests = friendRequestMapper.selectList(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getFromUserId, userId)
                        .orderByDesc(FriendRequest::getCreateTime));

        return Result.ok(toDTOList(requests, userId));
    }

    @Override
    public Result<List<FriendRequestDTO>> getFriendList() {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");

        // 查询所有 ACCEPTED 状态、且包含当前用户的好友关系
        List<FriendRequest> friends = friendRequestMapper.selectList(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getStatus, "ACCEPTED")
                        .and(w -> w.eq(FriendRequest::getFromUserId, userId)
                                  .or().eq(FriendRequest::getToUserId, userId))
                        .orderByDesc(FriendRequest::getUpdateTime));

        return Result.ok(toDTOList(friends, userId));
    }

    @Override
    public Result<Boolean> isFriend(Long targetUserId) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");

        boolean isFriend = friendRequestMapper.exists(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getStatus, "ACCEPTED")
                        .and(w -> w
                            .eq(FriendRequest::getFromUserId, userId).eq(FriendRequest::getToUserId, targetUserId)
                            .or()
                            .eq(FriendRequest::getFromUserId, targetUserId).eq(FriendRequest::getToUserId, userId)));

        return Result.ok(isFriend);
    }

    private List<FriendRequestDTO> toDTOList(List<FriendRequest> requests, Long myId) {
        List<FriendRequestDTO> dtoList = new ArrayList<>();
        for (FriendRequest req : requests) {
            FriendRequestDTO dto = new FriendRequestDTO();
            dto.setId(req.getId());
            dto.setStatus(req.getStatus());

            // 判断对方是发起方还是接收方
            boolean isFromMe = req.getFromUserId().equals(myId);
            Long friendId = isFromMe ? req.getToUserId() : req.getFromUserId();

            dto.setFromUserId(isFromMe ? myId : req.getFromUserId());
            dto.setFromPhone(isFromMe ? req.getToPhone() : req.getFromPhone());
            dto.setCreateTime(req.getCreateTime() != null ? req.getCreateTime().format(DTF) : "");

            // 查对方昵称
            User friend = userMapper.selectById(friendId);
            if (friend != null) {
                dto.setFromNickname(friend.getNickname());
            }

            dtoList.add(dto);
        }
        return dtoList;
    }
}
