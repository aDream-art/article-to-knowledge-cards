package com.luojiajun.chat.controller;

import com.luojiajun.chat.common.CommonResult;
import com.luojiajun.chat.dto.TagCreateParam;
import com.luojiajun.chat.entity.ChatTag;
import com.luojiajun.chat.security.SecurityUtils;
import com.luojiajun.chat.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "标签", description = "标签的增删查")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "标签列表（含被收藏引用次数）")
    public CommonResult<List<ChatTag>> list() {
        return CommonResult.success(tagService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    @Operation(summary = "新建标签")
    public CommonResult<ChatTag> create(@Valid @RequestBody TagCreateParam param) {
        return CommonResult.success(tagService.create(SecurityUtils.currentUserId(), param));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签（同时解除与收藏的关联）")
    public CommonResult<Void> delete(@PathVariable Long id) {
        tagService.delete(SecurityUtils.currentUserId(), id);
        return CommonResult.success(null);
    }
}
