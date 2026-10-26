package com.luojiajun.chat.service;

import com.luojiajun.chat.config.ChatProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextServiceTest {

    private ContextService contextService(int maxRounds, int maxTokens) {
        ChatProperties properties = new ChatProperties();
        properties.getContext().setMaxRounds(maxRounds);
        properties.getContext().setMaxTokens(maxTokens);
        return new ContextService(null, null, null, properties, null);
    }

    @Test
    void keepsOnlyLastRounds() {
        ContextService service = contextService(3, 10_000);
        LinkedList<ContextService.CachedMessage> window = new LinkedList<>();
        for (int i = 0; i < 20; i++) {
            window.add(new ContextService.CachedMessage(i % 2 == 0 ? "user" : "assistant", "消息" + i, null));
        }
        var trimmed = service.trim(window);
        assertThat(trimmed).hasSize(6);
        assertThat(trimmed.get(trimmed.size() - 1).content()).isEqualTo("消息19");
    }

    @Test
    void dropsOldestMessagesWhenTokenBudgetExceeded() {
        ContextService service = contextService(10, 10);
        LinkedList<ContextService.CachedMessage> window = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            window.add(new ContextService.CachedMessage("user", "一二三四五", null));
        }
        var trimmed = service.trim(window);
        assertThat(trimmed).isNotEmpty();
        assertThat(trimmed.size()).isLessThan(window.size());
        assertThat(trimmed.get(trimmed.size() - 1).content()).isEqualTo("一二三四五");
    }

    @Test
    void emptyWindowStaysEmpty() {
        assertThat(contextService(10, 100).trim(new LinkedList<>())).isEmpty();
    }

    /**
     * 切换成真实模型后，历史里的 mock 回答不能进入 prompt，
     * 否则真模型会读到"我是 Mock 模型"并顺着扮演（线上踩过）。
     */
    @Test
    void realModelDoesNotSeeMockHistory() {
        ContextService service = contextService(10, 10_000);
        List<ContextService.CachedMessage> window = List.of(
                new ContextService.CachedMessage("user", "问题一", null),
                new ContextService.CachedMessage("assistant", "Mock 模式回答", "mock"),
                new ContextService.CachedMessage("user", "问题二", null),
                new ContextService.CachedMessage("assistant", "真实回答", "deepseek"));

        var visible = service.visibleFor("deepseek", window);

        assertThat(visible).hasSize(3);
        assertThat(visible).noneMatch(message -> "mock".equals(message.provider()));
        assertThat(visible.get(0).content()).isEqualTo("问题一");
        assertThat(visible.get(2).content()).isEqualTo("真实回答");
    }

    @Test
    void mockModeKeepsItsOwnHistory() {
        ContextService service = contextService(10, 10_000);
        List<ContextService.CachedMessage> window = List.of(
                new ContextService.CachedMessage("user", "问题一", null),
                new ContextService.CachedMessage("assistant", "Mock 模式回答", "mock"));

        assertThat(service.visibleFor("mock", window)).hasSize(2);
    }
}
