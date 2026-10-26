package com.luojiajun.chat.mapper;

import com.luojiajun.chat.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    int insert(User user);

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);
}
