package com.luojiajun.chat.service;

import java.util.List;

/**
 * 轻量 RAG 的召回：不动向量库，把该文档的分片全取出来在内存里算余弦。
 * 分片规模只有几百，这个"不够 fancy"的选择才是这个体量下的正确工程判断。
 */
public interface RagService {

    List<String> retrieve(Long documentId, String question);
}
