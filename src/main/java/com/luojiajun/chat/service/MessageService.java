package com.luojiajun.chat.service;

import com.github.pagehelper.PageHelper;
import com.luojiajun.chat.common.CommonPage;
import com.luojiajun.chat.entity.ChatMessage;
import com.luojiajun.chat.mapper.MessageMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final MessageMapper messageMapper;

    public MessageService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public CommonPage<ChatMessage> list(Long userId, Long sessionId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return CommonPage.restPage(messageMapper.selectPageBySession(sessionId, userId));
    }

    public List<ChatMessage> recent(Long sessionId, int limit) {
        return messageMapper.selectRecent(sessionId, limit);
    }

    public ChatMessage save(Long userId, Long sessionId, String role, String content,                            String provider, String model, Integer tokenIn, Integer tokenOut,
                            String status, Integer latencyMs) {
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setProvider(provider);
        message.setModel(model);
        message.setTokenIn(tokenIn);
        message.setTokenOut(tokenOut);
        message.setStatus(status);
        message.setLatencyMs(latencyMs);
        messageMapper.insert(message);
        return message;
    }

    /**
     * 流式结束后回填：状态（COMPLETED / ABORTED / FAILED）、token 用量，
     * 以及**实际生效**的 provider / model（会话里记的是用户选择，不一定等于真正跑的模型）。
     */
    public void updateResult(Long id, String content, Integer tokenIn, Integer tokenOut,
                             String status, Integer latencyMs, String provider, String model) {
        messageMapper.updateResult(id, content, tokenIn, tokenOut, status, latencyMs, provider, model);
    }
}
