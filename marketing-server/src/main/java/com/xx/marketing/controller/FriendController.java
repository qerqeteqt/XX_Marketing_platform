package com.xx.marketing.controller;

import com.xx.marketing.dto.Result;
import com.xx.marketing.dto.FriendRequestDTO;
import com.xx.marketing.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /** 发送好友申请 */
    @PostMapping("/request")
    public Result<Void> sendRequest(@RequestBody Map<String, String> body) {
        return friendService.sendRequest(body.get("phone"));
    }

    /** 处理好友申请 */
    @PutMapping("/request/{requestId}")
    public Result<Void> handleRequest(@PathVariable Long requestId, @RequestBody Map<String, Boolean> body) {
        return friendService.handleRequest(requestId, body.get("accept"));
    }

    /** 收到的好友申请 */
    @GetMapping("/requests/received")
    public Result<List<FriendRequestDTO>> getReceivedRequests() {
        return friendService.getReceivedRequests();
    }

    /** 发出的好友申请 */
    @GetMapping("/requests/sent")
    public Result<List<FriendRequestDTO>> getSentRequests() {
        return friendService.getSentRequests();
    }

    /** 好友列表 */
    @GetMapping("/list")
    public Result<List<FriendRequestDTO>> getFriendList() {
        return friendService.getFriendList();
    }

    /** 检查是否为好友 */
    @GetMapping("/check/{userId}")
    public Result<Boolean> isFriend(@PathVariable Long userId) {
        return friendService.isFriend(userId);
    }
}
