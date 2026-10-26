package com.luojiajun.chat.service;

import com.luojiajun.chat.mapper.ModelLogMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private static final int TOP_USERS = 5;
    private static final int DAYS = 14;

    private final ModelLogMapper modelLogMapper;

    public StatsService(ModelLogMapper modelLogMapper) {
        this.modelLogMapper = modelLogMapper;
    }

    public Map<String, Object> overview() {
        Map<String, Object> totals = modelLogMapper.selectTotals();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCalls", totals == null ? 0 : totals.getOrDefault("totalCalls", 0));
        result.put("totalTokens", totals == null ? 0 : totals.getOrDefault("totalTokens", 0));
        result.put("topUsers", modelLogMapper.selectTopUsers(TOP_USERS));
        result.put("daily", normalizeDaily(modelLogMapper.selectDaily(DAYS)));
        return result;
    }

    /**
     * SQL 里的列名是 day（DATE_FORMAT 的别名），对外契约是 date —— 在这里收敛，
     * 避免前端为了一个字段名去兼容两套。
     */
    private List<Map<String, Object>> normalizeDaily(List<Map<String, Object>> rows) {
        List<Map<String, Object>> daily = new ArrayList<>();
        if (rows == null) {
            return daily;
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", row.get("day"));
            item.put("calls", row.get("calls"));
            item.put("tokens", row.get("tokens"));
            daily.add(item);
        }
        return daily;
    }
}
