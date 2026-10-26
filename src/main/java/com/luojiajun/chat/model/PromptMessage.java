package com.luojiajun.chat.model;

/**
 * 提示词中的单条消息，role 取 user / assistant / system。
 */
public record PromptMessage(String role, String content) {

    public static PromptMessage system(String content) {
        return new PromptMessage("system", content);
    }

    public static PromptMessage user(String content) {
        return new PromptMessage("user", content);
    }

    public static PromptMessage assistant(String content) {
        return new PromptMessage("assistant", content);
    }
}
