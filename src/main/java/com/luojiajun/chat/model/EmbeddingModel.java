package com.luojiajun.chat.model;

import java.util.List;

public interface EmbeddingModel {

    float[] embed(String text);

    default List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }
}
