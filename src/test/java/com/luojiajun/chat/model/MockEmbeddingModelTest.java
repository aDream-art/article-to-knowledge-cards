package com.luojiajun.chat.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MockEmbeddingModelTest {

    @Test
    void sameTextAlwaysProducesSameVector() {
        MockEmbeddingModel model = new MockEmbeddingModel();
        float[] first = model.embed("Redis 缓存穿透的三个解决方案");
        float[] second = model.embed("Redis 缓存穿透的三个解决方案");
        for (int i = 0; i < first.length; i++) {
            assertThat(second[i]).isCloseTo(first[i], within(1e-6f));
        }
    }

    @Test
    void vectorIsNormalized() {
        float[] vector = new MockEmbeddingModel().embed("anything");
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, within(1e-4));
    }

    @Test
    void differentTextProducesDifferentVector() {
        MockEmbeddingModel model = new MockEmbeddingModel();
        assertThat(model.embed("缓存穿透")).isNotEqualTo(model.embed("消息队列"));
    }
}
