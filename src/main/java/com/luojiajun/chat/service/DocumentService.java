package com.luojiajun.chat.service;

import com.luojiajun.chat.common.ApiException;
import com.luojiajun.chat.common.Asserts;
import com.luojiajun.chat.common.ResultCode;
import com.luojiajun.chat.config.ChatProperties;
import com.luojiajun.chat.entity.ChatChunk;
import com.luojiajun.chat.entity.ChatDocument;
import com.luojiajun.chat.mapper.ChunkMapper;
import com.luojiajun.chat.mapper.DocumentMapper;
import com.luojiajun.chat.mapper.SessionMapper;
import com.luojiajun.chat.model.EmbeddingRouter;
import com.luojiajun.chat.model.ModelException;
import com.luojiajun.chat.rag.TextChunker;
import com.luojiajun.chat.rag.Vectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * TXT 上传 → 解码 → 切分 → 向量化 → 落库。同步处理：1MB 的文本秒级完成，异步队列是过度设计。
 */
@Slf4j
@Service
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final ChunkMapper chunkMapper;
    private final SessionMapper sessionMapper;
    private final EmbeddingRouter embeddingRouter;
    private final ChatProperties properties;

    public DocumentService(DocumentMapper documentMapper, ChunkMapper chunkMapper,
                           SessionMapper sessionMapper, EmbeddingRouter embeddingRouter,
                           ChatProperties properties) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.sessionMapper = sessionMapper;
        this.embeddingRouter = embeddingRouter;
        this.properties = properties;
    }

    public ChatDocument upload(Long userId, MultipartFile file) {
        validateFile(userId, file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new ApiException(ResultCode.FAILED, "读取文件失败");
        }
        String text = decode(bytes);
        if (text.isBlank()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, "文档内容为空");
        }

        ChatDocument document = new ChatDocument();
        document.setUserId(userId);
        document.setFilename(file.getOriginalFilename());
        document.setByteSize(bytes.length);
        document.setCharCount(text.length());
        document.setChunkCount(0);
        document.setStatus("PARSING");
        documentMapper.insert(document);

        List<String> parts = TextChunker.chunk(text,
                properties.getRag().getChunkSize(), properties.getRag().getOverlap());
        if (parts.isEmpty()) {
            documentMapper.updateAfterParse(document.getId(), "EMBED_FAILED", text.length(), 0);
            document.setStatus("EMBED_FAILED");
            return document;
        }

        List<float[]> vectors;
        try {
            vectors = embeddingRouter.model().embedBatch(parts);
        } catch (ModelException e) {
            log.warn("文档向量化失败 documentId={}: {}", document.getId(), e.getMessage());
            documentMapper.updateAfterParse(document.getId(), "EMBED_FAILED", text.length(), 0);
            document.setStatus("EMBED_FAILED");
            return document;
        }

        List<ChatChunk> chunks = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            ChatChunk chunk = new ChatChunk();
            chunk.setDocumentId(document.getId());
            chunk.setUserId(userId);
            chunk.setSeq(i);
            chunk.setContent(parts.get(i));
            chunk.setEmbedding(vectors.size() > i ? Vectors.toBytes(vectors.get(i)) : new byte[0]);
            chunk.setTokenEst(com.luojiajun.chat.model.TokenEstimator.estimate(parts.get(i)));
            chunks.add(chunk);
        }
        chunkMapper.batchInsert(chunks);
        documentMapper.updateAfterParse(document.getId(), "READY", text.length(), chunks.size());

        document.setStatus("READY");
        document.setChunkCount(chunks.size());
        return document;
    }

    public List<ChatDocument> list(Long userId) {
        return documentMapper.selectByUser(userId);
    }

    public void delete(Long userId, Long documentId) {
        ChatDocument document = documentMapper.selectByIdAndUser(documentId, userId);
        Asserts.notNull(document, "文档不存在");
        chunkMapper.deleteByDocument(documentId);
        documentMapper.deleteByIdAndUser(documentId, userId);
        sessionMapper.clearDocumentById(documentId);
    }

    public ChatDocument require(Long userId, Long documentId) {
        ChatDocument document = documentMapper.selectByIdAndUser(documentId, userId);
        Asserts.notNull(document, "文档不存在");
        return document;
    }

    private void validateFile(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, "请选择要上传的 TXT 文件");
        }
        if (file.getSize() > properties.getUpload().getMaxBytes()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, "文件超过大小限制（1MB）");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".txt")) {
            throw new ApiException(ResultCode.VALIDATE_FAILED, "目前只支持 .txt 文件");
        }
        if (documentMapper.countByUser(userId) >= properties.getUpload().getMaxDocsPerUser()) {
            throw new ApiException(ResultCode.VALIDATE_FAILED,
                    "最多上传 " + properties.getUpload().getMaxDocsPerUser() + " 份文档，请先删除旧文档");
        }
    }

    /**
     * 先按 UTF-8 严格解码，失败再按 GBK —— 中文 txt 的两种常见编码。
     */
    static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            try {
                return Charset.forName("GBK").newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ex) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }
}
