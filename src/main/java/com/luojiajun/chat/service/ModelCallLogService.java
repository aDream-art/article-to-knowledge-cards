package com.luojiajun.chat.service;

import com.luojiajun.chat.entity.ChatModelCallLog;
import com.luojiajun.chat.mapper.ModelLogMapper;
import com.luojiajun.chat.model.ChatResult;
import com.luojiajun.chat.model.ModelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 调用日志：写库失败不能影响对话，所以整段 try/catch 掉。
 */
@Slf4j
@Service
public class ModelCallLogService {

    private final ModelLogMapper modelLogMapper;

    public ModelCallLogService(ModelLogMapper modelLogMapper) {
        this.modelLogMapper = modelLogMapper;
    }

    public void logSuccess(Long userId, Long sessionId, Long messageId, String requestId,
                           String provider, String model, ChatResult result, int retryCount) {
        ChatModelCallLog entity = base(userId, sessionId, messageId, requestId, provider, model, retryCount);
        entity.setStatus("SUCCESS");
        entity.setHttpStatus(200);
        entity.setTokenIn(result.getTokenIn());
        entity.setTokenOut(result.getTokenOut());
        entity.setLatencyMs(result.getLatencyMs());
        entity.setFirstTokenMs(result.getFirstTokenMs());
        insert(entity);
    }

    public void logFailure(Long userId, Long sessionId, Long messageId, String requestId,
                           String provider, String model, ModelException error,
                           int retryCount, long latencyMs) {
        ChatModelCallLog entity = base(userId, sessionId, messageId, requestId, provider, model, retryCount);
        entity.setStatus("FAILED");
        entity.setHttpStatus(error.getHttpStatus());
        entity.setLatencyMs((int) latencyMs);
        entity.setErrorCode(error.getCode());
        entity.setErrorMsg(truncate(error.getMessage(), 250));
        insert(entity);
    }

    private ChatModelCallLog base(Long userId, Long sessionId, Long messageId, String requestId,
                                  String provider, String model, int retryCount) {
        ChatModelCallLog entity = new ChatModelCallLog();
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setMessageId(messageId);
        entity.setRequestId(requestId);
        entity.setProvider(provider);
        entity.setModel(model);
        entity.setRetryCount(retryCount);
        return entity;
    }

    private void insert(ChatModelCallLog entity) {
        try {
            modelLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("写调用日志失败: {}", e.getMessage());
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
