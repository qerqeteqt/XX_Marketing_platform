package com.xx.marketing.service;

import com.xx.marketing.dto.Result;
import com.xx.marketing.dto.PostDTO;

import java.util.List;

public interface PostService {

    /** 发布帖子 */
    Result<Void> createPost(String content);

    /** 查看朋友圈（好友 + 自己的帖子） */
    Result<List<PostDTO>> getFeed();

    /** 点赞/取消点赞 */
    Result<Void> toggleLike(Long postId);
}
