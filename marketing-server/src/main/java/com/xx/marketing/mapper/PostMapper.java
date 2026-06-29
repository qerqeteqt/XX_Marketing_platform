package com.xx.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xx.marketing.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT * FROM tb_post WHERE user_id IN " +
            "(SELECT CASE WHEN from_user_id = #{userId} THEN to_user_id ELSE from_user_id END " +
            " FROM tb_friend_request WHERE status = 'ACCEPTED' AND (from_user_id = #{userId} OR to_user_id = #{userId})) " +
            "OR user_id = #{userId} ORDER BY create_time DESC")
    List<Post> selectFriendsAndOwnPosts(Long userId);
}
