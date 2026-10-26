package com.luojiajun.chat.rag;

/**
 * 向量相似度与序列化。SQL 里不存 JSON 而存 float32 小端 BLOB，读写都在这一个地方。
 */
public final class Vectors {

    private Vectors() {
    }

    public static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0f;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0f;
        }
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    public static byte[] toBytes(float[] vector) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(vector.length * Float.BYTES)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    public static float[] fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < Float.BYTES) {
            return new float[0];
        }
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }
}
