package com.xx.marketing.service;

import com.xx.marketing.dto.Result;
import com.xx.marketing.dto.FriendRequestDTO;

import java.util.List;

public interface FriendService {

    /** 发送好友申请（通过手机号） */
    Result<Void> sendRequest(String targetPhone);

    /** 处理好友申请（接受/拒绝） */
    Result<Void> handleRequest(Long requestId, boolean accept);

    /** 查看收到的好友申请 */
    Result<List<FriendRequestDTO>> getReceivedRequests();

    /** 查看发出的好友申请 */
    Result<List<FriendRequestDTO>> getSentRequests();

    /** 获取好友列表 */
    Result<List<FriendRequestDTO>> getFriendList();

    /** 检查是否为好友 */
    Result<Boolean> isFriend(Long targetUserId);
}
