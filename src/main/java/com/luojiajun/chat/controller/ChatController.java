package com.luojiajun.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.common.CommonResult;
import com.luojiajun.chat.dto.ChatStreamParam;
import com.luojiajun.chat.security.SecurityUtils;
import com.luojiajun.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "对话", description = "SSE 流式问答")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    /**
     * 用 POST 而不是 GET：EventSource 只能 GET，token 与提问内容会进 URL 和访问日志。
     * 前端用 fetch + ReadableStream 消费（见 web/src/composables/useSSE.ts）。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式提问（事件：meta / delta / usage / done / error）")
    public SseEmitter stream(@Valid @RequestBody ChatStreamParam param) {
        return chatService.stream(SecurityUtils.currentUserId(), param);
    }

    /**
     * 限流 / 配额这类"还没开始流"的失败必须返回 JSON，而不是 SSE 事件。
     * 本方法的 produces 是 text/event-stream，走全局异常处理器会被内容协商挡成 500 空响应，
     * 所以这里直接把 JSON 写进 response，绕开协商。
     */
    @ExceptionHandler(ApiException.class)
    public void handlePreStreamError(ApiException e, HttpServletResponse response) throws IOException {
        boolean throttled = e.getErrorCode().getCode() == 42901L || e.getErrorCode().getCode() == 42902L;
        response.setStatus(throttled ? 429 : HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=" + StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                CommonResult.failed(e.getErrorCode(), e.getMessage()));
    }
}
