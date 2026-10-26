package com.luojiajun.chat.service;

import com.luojiajun.chat.config.ChatProperties;
import com.luojiajun.chat.entity.ChatChunk;
import com.luojiajun.chat.mapper.ChunkMapper;
import com.luojiajun.chat.model.EmbeddingRouter;
import com.luojiajun.chat.rag.Vectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class RagServiceImpl implements RagService {

    private final ChunkMapper chunkMapper;
    private final EmbeddingRouter embeddingRouter;
    private final ChatProperties properties;

    public RagServiceImpl(ChunkMapper chunkMapper, EmbeddingRouter embeddingRouter, ChatProperties properties) {
        this.chunkMapper = chunkMapper;
        this.embeddingRouter = embeddingRouter;
        this.properties = properties;
    }

    @Override
    public List<String> retrieve(Long documentId, String question) {
        List<ChatChunk> chunks = chunkMapper.selectByDocument(documentId);
        if (chunks.isEmpty()) {
            return List.of();
        }
        float[] questionVector = embeddingRouter.model().embed(question);
        List<ScoredChunk> scored = new ArrayList<>();
        for (ChatChunk chunk : chunks) {
            float score = Vectors.cosine(questionVector, Vectors.fromBytes(chunk.getEmbedding()));
            scored.add(new ScoredChunk(score, chunk.getSeq(), chunk.getContent()));
        }
        List<String> result = scored.stream()
                .filter(s -> s.score() >= properties.getRag().getThreshold())
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(properties.getRag().getTopK())
                .map(ScoredChunk::content)
                .toList();
        log.debug("RAG 召回 documentId={} 分片={} 命中={}", documentId, chunks.size(), result.size());
        return result;
    }

    private record ScoredChunk(float score, int seq, String content) {
    }
}
