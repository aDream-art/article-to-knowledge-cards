package com.luojiajun.chat.mapper;

import com.luojiajun.chat.entity.ChatTag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TagMapper {

    int insert(ChatTag tag);

    List<ChatTag> selectByUser(@Param("userId") Long userId);

    ChatTag selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    List<ChatTag> selectByIds(@Param("ids") List<Long> ids);

    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int deleteRelationsByTag(@Param("tagId") Long tagId);
}
