package com.luojiajun.chat.mapper;

import com.luojiajun.chat.entity.ChatModelCallLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ModelLogMapper {

    int insert(ChatModelCallLog log);

    Map<String, Object> selectTotals();

    List<Map<String, Object>> selectTopUsers(@Param("limit") int limit);

    List<Map<String, Object>> selectDaily(@Param("days") int days);
}
