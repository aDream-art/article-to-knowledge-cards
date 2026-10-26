package com.luojiajun.chat.mapper;

import com.luojiajun.chat.entity.ChatDocument;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DocumentMapper {

    int insert(ChatDocument document);

    List<ChatDocument> selectByUser(@Param("userId") Long userId);

    ChatDocument selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int updateAfterParse(@Param("id") Long id, @Param("status") String status,
                         @Param("charCount") int charCount, @Param("chunkCount") int chunkCount);

    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    long countByUser(@Param("userId") Long userId);
}
