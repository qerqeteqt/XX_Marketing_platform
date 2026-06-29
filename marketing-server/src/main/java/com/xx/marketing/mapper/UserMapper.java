package com.xx.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xx.marketing.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
