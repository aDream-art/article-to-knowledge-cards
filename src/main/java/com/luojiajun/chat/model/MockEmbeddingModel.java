package com.luojiajun.chat.model;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * 确定性 mock 向量：字符 bigram 哈希到 512 维再归一化。
 * 同一段文本永远得到同一个向量，测试可以断言；语义相似度不可用，仅用于跑通链路。
 */
public class MockEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSION = 512;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSION];
        if (text == null || text.isEmpty()) {
            vector[0] = 1f;
            return vector;
        }
        String normalized = text.replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length() - 1; i++) {
            String bigram = normalized.substring(i, i + 2);
            int slot = Math.floorMod(bigram.hashCode(), DIMENSION);
            Random random = new Random(bigram.hashCode());
            vector[slot] += random.nextFloat() * 2 - 1;
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            vector[0] = 1f;
            return vector;
        }
        for (int i = 0; i < DIMENSION; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
        return vector;
    }

    public static byte[] toBytes(float[] vector) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(vector.length * 4)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    public static float[] fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return new float[0];
        }
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[bytes.length / 4];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }

    @SuppressWarnings("unused")
    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
