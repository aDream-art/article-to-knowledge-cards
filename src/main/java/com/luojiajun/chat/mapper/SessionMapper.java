package com.luojiajun.chat.mapper;

import com.luojiajun.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionMapper {

    int insert(ChatSession session);

    ChatSession selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    List<ChatSession> selectByUser(@Param("userId") Long userId);

    int updateTitle(@Param("id") Long id, @Param("userId") Long userId, @Param("title") String title);

    int updateModel(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("provider") String provider, @Param("model") String model);

    int updateDocument(@Param("id") Long id, @Param("userId") Long userId,
                       @Param("documentId") Long documentId);

    int touchLastMessage(@Param("id") Long id, @Param("userId") Long userId,
                         @Param("time") LocalDateTime time);

    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    long countByUser(@Param("userId") Long userId);

    /** 文档被删除时，把所有绑定它的会话解绑 */
    int clearDocumentById(@Param("documentId") Long documentId);
}
