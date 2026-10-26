package com.luojiajun.chat.model;

import java.util.function.Consumer;

/**
 * 统一模型调用层：新增一家模型只需要实现这个接口，其余（重试、超时、日志、降级）都不动。
 */
public interface ChatModel {

    String provider();

    /**
     * 流式调用，阻塞直到结束；每个增量回调 onDelta。
     */
    ChatResult stream(ChatRequest request, Consumer<String> onDelta) throws ModelException;

    /**
     * 非流式调用，用于文档标题之外的短任务与测试。
     */
    ChatResult complete(ChatRequest request) throws ModelException;
}
