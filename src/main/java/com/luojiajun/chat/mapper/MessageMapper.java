package com.luojiajun.chat.mapper;

import com.luojiajun.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageMapper {

    int insert(ChatMessage message);

    int updateResult(@Param("id") Long id,
                     @Param("content") String content,
                     @Param("tokenIn") Integer tokenIn,
                     @Param("tokenOut") Integer tokenOut,
                     @Param("status") String status,
                     @Param("latencyMs") Integer latencyMs,
                     @Param("provider") String provider,
                     @Param("model") String model);

    List<ChatMessage> selectPageBySession(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    List<ChatMessage> selectRecent(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    int deleteBySession(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
