package com.xx.marketing.controller;

import com.xx.marketing.dto.Result;
import com.xx.marketing.dto.PostDTO;
import com.xx.marketing.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** 发布帖子 */
    @PostMapping
    public Result<Void> createPost(@RequestBody Map<String, String> body) {
        return postService.createPost(body.get("content"));
    }

    /** 查看朋友圈 */
    @GetMapping("/feed")
    public Result<List<PostDTO>> getFeed() {
        return postService.getFeed();
    }

    /** 点赞/取消点赞 */
    @PostMapping("/{postId}/like")
    public Result<Void> toggleLike(@PathVariable Long postId) {
        return postService.toggleLike(postId);
    }
}
