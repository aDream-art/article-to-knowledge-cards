package com.luojiajun.chat.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VectorsTest {

    @Test
    void cosineOfIdenticalVectorsIsOne() {
        float[] a = {0.6f, 0.8f};
        assertThat(Vectors.cosine(a, a)).isCloseTo(1f, within(1e-6f));
    }

    @Test
    void cosineOfOrthogonalVectorsIsZero() {
        assertThat(Vectors.cosine(new float[]{1, 0}, new float[]{0, 1})).isCloseTo(0f, within(1e-6f));
    }

    @Test
    void cosineOfOppositeVectorsIsMinusOne() {
        assertThat(Vectors.cosine(new float[]{1, 0}, new float[]{-1, 0})).isCloseTo(-1f, within(1e-6f));
    }

    @Test
    void bytesRoundTripKeepsValues() {
        float[] vector = {0.1f, -0.25f, 3.5f, 0f};
        float[] restored = Vectors.fromBytes(Vectors.toBytes(vector));
        assertThat(restored).hasSize(vector.length);
        for (int i = 0; i < vector.length; i++) {
            assertThat(restored[i]).isCloseTo(vector[i], within(1e-6f));
        }
    }

    @Test
    void mismatchedDimensionsAreZeroNotExplosion() {
        assertThat(Vectors.cosine(new float[]{1, 0}, new float[]{1, 0, 0})).isEqualTo(0f);
        assertThat(Vectors.cosine(null, new float[]{1})).isEqualTo(0f);
    }
}
