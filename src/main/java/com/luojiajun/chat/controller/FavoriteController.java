package com.luojiajun.chat.controller;

import com.luojiajun.chat.common.CommonPage;
import com.luojiajun.chat.common.CommonResult;
import com.luojiajun.chat.dto.FavoriteCreateParam;
import com.luojiajun.chat.entity.ChatFavorite;
import com.luojiajun.chat.security.SecurityUtils;
import com.luojiajun.chat.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏", description = "问答快照 + 标签检索")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @Operation(summary = "收藏列表（可按标签、关键词过滤）")
    public CommonResult<CommonPage<ChatFavorite>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(favoriteService.list(SecurityUtils.currentUserId(),
                pageNum, pageSize, tagId, keyword));
    }

    @PostMapping
    @Operation(summary = "收藏一轮问答")
    public CommonResult<ChatFavorite> create(@Valid @RequestBody FavoriteCreateParam param) {
        return CommonResult.success(favoriteService.create(SecurityUtils.currentUserId(), param));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除收藏")
    public CommonResult<Void> delete(@PathVariable Long id) {
        favoriteService.delete(SecurityUtils.currentUserId(), id);
        return CommonResult.success(null);
    }
}
