package com.luojiajun.chat.service;

import com.github.pagehelper.PageHelper;
import com.luojiajun.chat.common.Asserts;
import com.luojiajun.chat.common.CommonPage;
import com.luojiajun.chat.dto.SessionCreateParam;
import com.luojiajun.chat.entity.ChatDocument;
import com.luojiajun.chat.entity.ChatSession;
import com.luojiajun.chat.mapper.DocumentMapper;
import com.luojiajun.chat.mapper.MessageMapper;
import com.luojiajun.chat.mapper.SessionMapper;
import com.luojiajun.chat.model.ModelRouter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SessionService {

    private static final String DEFAULT_TITLE = "新会话";

    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final DocumentMapper documentMapper;
    private final ModelRouter modelRouter;
    private final ContextService contextService;

    public SessionService(SessionMapper sessionMapper, MessageMapper messageMapper,
                          DocumentMapper documentMapper, ModelRouter modelRouter,
                          ContextService contextService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.documentMapper = documentMapper;
        this.modelRouter = modelRouter;
        this.contextService = contextService;
    }

    @Transactional
    public ChatSession create(Long userId, SessionCreateParam param) {
        String provider = modelRouter.normalize(param.getProvider());
        String model = param.getModel() == null || param.getModel().isBlank()
                ? modelRouter.defaultModel(provider) : param.getModel();
        Long documentId = null;
        if (param.getDocumentId() != null) {
            ChatDocument document = documentMapper.selectByIdAndUser(param.getDocumentId(), userId);
            Asserts.notNull(document, "文档不存在");
            documentId = document.getId();
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(param.getTitle() == null || param.getTitle().isBlank() ? DEFAULT_TITLE : param.getTitle().trim());
        session.setProvider(provider);
        session.setModel(model);
        session.setDocumentId(documentId);
        sessionMapper.insert(session);
        return session;
    }

    public CommonPage<ChatSession> list(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return CommonPage.restPage(sessionMapper.selectByUser(userId));
    }

    public ChatSession require(Long userId, Long sessionId) {
        ChatSession session = sessionMapper.selectByIdAndUser(sessionId, userId);
        Asserts.notNull(session, "会话不存在");
        return session;
    }

    public ChatSession rename(Long userId, Long sessionId, String title) {
        ChatSession session = require(userId, sessionId);
        sessionMapper.updateTitle(sessionId, userId, title.trim());
        session.setTitle(title.trim());
        return session;
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        require(userId, sessionId);
        messageMapper.deleteBySession(sessionId, userId);
        sessionMapper.deleteByIdAndUser(sessionId, userId);
        contextService.invalidate(sessionId);
    }

    public void bindDocument(Long userId, Long sessionId, Long documentId) {
        require(userId, sessionId);
        if (documentId != null) {
            ChatDocument document = documentMapper.selectByIdAndUser(documentId, userId);
            Asserts.notNull(document, "文档不存在");
        }
        sessionMapper.updateDocument(sessionId, userId, documentId);
    }

    public void touch(Long userId, Long sessionId) {
        sessionMapper.touchLastMessage(sessionId, userId, LocalDateTime.now());
    }

    /**
     * 首条提问自动生成标题：截断 20 字，不做 LLM 生成（不为一个 UI 装饰多打一次 API）。
     * 用户手动改过标题就不再覆盖。
     */
    public void autoTitleIfDefault(Long userId, ChatSession session, String question) {
        String current = session.getTitle();
        if (current != null && !current.isBlank() && !DEFAULT_TITLE.equals(current)) {
            return;
        }
        String title = question.replaceAll("\\s+", " ").trim();
        if (title.length() > 20) {
            title = title.substring(0, 20);
        }
        if (title.isEmpty()) {
            return;
        }
        sessionMapper.updateTitle(session.getId(), userId, title);
        session.setTitle(title);
    }
}
