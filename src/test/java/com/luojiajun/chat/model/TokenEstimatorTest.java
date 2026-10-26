package com.luojiajun.chat.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenEstimatorTest {

    @Test
    void estimatesChineseByCharacter() {
        // 4 个汉字 × 0.7 ≈ 2.8 → 向上取整 3
        assertThat(TokenEstimator.estimate("你好世界")).isEqualTo(3);
    }

    @Test
    void emptyTextIsZero() {
        assertThat(TokenEstimator.estimate(null)).isZero();
        assertThat(TokenEstimator.estimate("")).isZero();
    }

    @Test
    void englishCostsLessThanChineseForSameLength() {
        assertThat(TokenEstimator.estimate("abcdefghij"))
                .isLessThan(TokenEstimator.estimate("一二三四五六七八九十"));
    }
}
