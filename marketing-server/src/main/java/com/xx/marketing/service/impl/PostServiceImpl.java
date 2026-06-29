package com.xx.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xx.marketing.dto.Result;
import com.xx.marketing.common.UserContext;
import com.xx.marketing.dto.PostDTO;
import com.xx.marketing.entity.Post;
import com.xx.marketing.entity.PostLike;
import com.xx.marketing.entity.User;
import com.xx.marketing.mapper.PostLikeMapper;
import com.xx.marketing.mapper.PostMapper;
import com.xx.marketing.mapper.UserMapper;
import com.xx.marketing.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserMapper userMapper;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public Result<Void> createPost(String content) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");
        if (content == null || content.trim().isEmpty()) return Result.fail(400, "内容不能为空");
        if (content.length() > 2000) return Result.fail(400, "内容过长");

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content.trim());
        post.setLikeCount(0);
        postMapper.insert(post);

        log.info("用户 {} 发布了帖子 id={}", userId, post.getId());
        return Result.okMsg("发布成功");
    }

    @Override
    public Result<List<PostDTO>> getFeed() {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");

        // 查询好友和自己的帖子
        List<Post> posts = postMapper.selectFriendsAndOwnPosts(userId);
        if (posts.isEmpty()) return Result.ok(new ArrayList<>());

        // 收集所有帖子ID和用户ID
        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());

        // 查询当前用户点赞了哪些帖子
        List<PostLike> myLikes = postLikeMapper.selectList(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .in(PostLike::getPostId, postIds));
        Set<Long> likedPostIds = myLikes.stream().map(PostLike::getPostId).collect(Collectors.toSet());

        // 查询用户昵称
        List<User> users = userMapper.selectBatchIds(userIds);
        java.util.Map<Long, String> nickMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : ""));

        // 组装 DTO
        List<PostDTO> dtoList = new ArrayList<>();
        for (Post post : posts) {
            PostDTO dto = new PostDTO();
            dto.setId(post.getId());
            dto.setUserId(post.getUserId());
            dto.setNickname(nickMap.getOrDefault(post.getUserId(), ""));
            dto.setContent(post.getContent());
            dto.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0);
            dto.setLiked(likedPostIds.contains(post.getId()));
            dto.setCreateTime(post.getCreateTime() != null ? post.getCreateTime().format(DTF) : "");
            dtoList.add(dto);
        }

        return Result.ok(dtoList);
    }

    @Override
    @Transactional
    public Result<Void> toggleLike(Long postId) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.fail(401, "请先登录");

        Post post = postMapper.selectById(postId);
        if (post == null) return Result.fail(404, "帖子不存在");

        PostLike existing = postLikeMapper.selectOne(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId)
                        .eq(PostLike::getUserId, userId));

        if (existing != null) {
            // 取消点赞
            postLikeMapper.deleteById(existing.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postMapper.updateById(post);
            log.info("用户 {} 取消点赞帖子 {}", userId, postId);
            return Result.okMsg("已取消点赞");
        } else {
            // 点赞
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            postLikeMapper.insert(like);
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            log.info("用户 {} 点赞帖子 {}", userId, postId);
            return Result.okMsg("点赞成功");
        }
    }
}
