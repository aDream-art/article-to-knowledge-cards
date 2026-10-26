# RAG 召回用向量 + Java 内暴力余弦，不引入向量数据库

TXT 上传后按 400 字 / 80 字重叠切分成片段，用通义 `text-embedding-v3` 生成 1024 维向量存 MySQL；提问时按 `document_id` 一次性取出该文档的全部分片，在 Java 里算余弦相似度取 top-5，相似度低于 0.3 视为"文档里没有"。

## Considered Options

- **BM25 / 词法召回**：需要中文分词（jieba 之类）且效果对长文档一般，也不体现"LLM 应用"的叙事。
- **向量库（Milvus / pgvector / Elasticsearch）**：与既定的 MySQL + Redis 栈冲突，且多一个要运维的组件；一个文档切完只有 200~500 个分片，暴力遍历在毫秒级完成，上 ANN 索引是纯负担。
- **混合检索（RRF 融合）**：两套打分、两套阈值、两套失败分支，对这个体量没有回报。

## Consequences

- 依赖通义（DeepSeek 无 embedding 接口）；embedding 失败重试一次仍失败则文档标 `EMBED_FAILED` 并提示重传。
- 检索前会整篇文档的分片向量进内存，因此限制单文档 1MB / 每用户 10 份；分片数超过 5000 时才需要重新考虑 ANN。
- `chat_chunk` 表里存向量（1024 × float32 ≈ 4KB/行），200~500 行对 MySQL 毫无压力。
