package com.luojiajun.chat.controller;

import com.luojiajun.chat.common.CommonResult;
import com.luojiajun.chat.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 极简管理视图：只有只读聚合，没有任何用户管理/封禁能力（这是刻意的取舍，见 spec §12）。
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "统计", description = "调用量与 token 消耗的只读聚合")
public class AdminController {

    private final StatsService statsService;

    public AdminController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    @Operation(summary = "总调用 / 总 token / Top 用户 / 近 14 天趋势")
    public CommonResult<Map<String, Object>> stats() {
        return CommonResult.success(statsService.overview());
    }
}
