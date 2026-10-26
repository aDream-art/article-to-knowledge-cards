package com.luojiajun.chat.controller;

import com.luojiajun.chat.common.CommonPage;
import com.luojiajun.chat.common.CommonResult;
import com.luojiajun.chat.dto.BindDocumentParam;
import com.luojiajun.chat.dto.SessionCreateParam;
import com.luojiajun.chat.dto.SessionUpdateParam;
import com.luojiajun.chat.entity.ChatMessage;
import com.luojiajun.chat.entity.ChatSession;
import com.luojiajun.chat.security.SecurityUtils;
import com.luojiajun.chat.service.MessageService;
import com.luojiajun.chat.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "会话", description = "会话的增删改查与消息历史")
public class SessionController {

    private final SessionService sessionService;
    private final MessageService messageService;

    public SessionController(SessionService sessionService, MessageService messageService) {
        this.sessionService = sessionService;
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "会话列表（含最近一条消息摘要）")
    public CommonResult<CommonPage<ChatSession>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize) {
        return CommonResult.success(sessionService.list(SecurityUtils.currentUserId(), pageNum, pageSize));
    }

    @PostMapping
    @Operation(summary = "新建会话（可指定 provider / 绑定文档）")
    public CommonResult<ChatSession> create(@Valid @RequestBody SessionCreateParam param) {
        return CommonResult.success(sessionService.create(SecurityUtils.currentUserId(), param));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "重命名会话")
    public CommonResult<ChatSession> rename(@PathVariable Long id,
                                            @Valid @RequestBody SessionUpdateParam param) {
        return CommonResult.success(sessionService.rename(SecurityUtils.currentUserId(), id, param.getTitle()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话（物理删除，收藏中的快照不受影响）")
    public CommonResult<Void> delete(@PathVariable Long id) {
        sessionService.delete(SecurityUtils.currentUserId(), id);
        return CommonResult.success(null);
    }

    @PostMapping("/{id}/document")
    @Operation(summary = "绑定 / 解绑文档（documentId 传 null 解绑）")
    public CommonResult<Void> bindDocument(@PathVariable Long id, @RequestBody BindDocumentParam param) {
        sessionService.bindDocument(SecurityUtils.currentUserId(), id, param.getDocumentId());
        return CommonResult.success(null);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "历史消息（倒序分页）")
    public CommonResult<CommonPage<ChatMessage>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "30") int pageSize) {
        return CommonResult.success(messageService.list(SecurityUtils.currentUserId(), id, pageNum, pageSize));
    }
}
