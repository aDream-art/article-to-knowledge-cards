# 轻量 AI 问答助手

一个面向普通用户的独立 AI 问答工具：用户在自己的入口里提问、按会话隔离上下文、把值得留存的问答收藏起来，并可以上传 TXT 文档后基于文档内容提问。

## Language

**会话 (Session)**：
用户与助手的一段隔离对话，拥有自己的消息序列与上下文窗口，不与其他会话共享。
_Avoid_: 对话、聊天、conversation、chat、thread

**消息 (Message)**：
会话中的一次发言，角色为用户或助手；一问一答构成一轮。
_Avoid_: 记录、聊天记录、chat message、record

**收藏 (Favorite)**：
用户标记为值得留存的一轮问答，脱离原会话也可独立检索。
_Avoid_: 书签、收藏夹、bookmark、star

**标签 (Tag)**：
用户自有的分类词，只挂在收藏上，用于之后找回。
_Avoid_: 分类、分类目录、category、分组

**文档 (Document)**：
用户上传的 TXT 原文，是按用户隔离的资料，会话至多绑定一份。
_Avoid_: 知识库、知识、附件、file、knowledge base

**片段 (Chunk)**：
文档切分后的最小检索单位，是回答时被引用进提示词的原文片段。
_Avoid_: 分块、块、paragraph、segment

**上下文窗口 (Context Window)**：
一次提问时真正喂给模型的近期消息集合，按轮数与 token 双重上限裁剪，不含系统提示与检索片段。
_Avoid_: 历史、记忆、memory、history

**模型提供方 (Provider)**：
被统一调用层封装的一家大模型服务，例如 DeepSeek、通义千问。
_Avoid_: 模型厂商、渠道、channel、vendor
