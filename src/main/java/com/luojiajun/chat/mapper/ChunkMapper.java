package com.luojiajun.chat.mapper;

import com.luojiajun.chat.entity.ChatChunk;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChunkMapper {

    int batchInsert(@Param("list") List<ChatChunk> chunks);

    List<ChatChunk> selectByDocument(@Param("documentId") Long documentId);

    int deleteByDocument(@Param("documentId") Long documentId);
}
