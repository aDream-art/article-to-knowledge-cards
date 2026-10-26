-- 轻量 AI 智能问答助手 - 表结构
-- MySQL 8.0 / utf8mb4

CREATE DATABASE IF NOT EXISTS ai_chat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_chat;

DROP TABLE IF EXISTS chat_favorite_tag;
DROP TABLE IF EXISTS chat_tag;
DROP TABLE IF EXISTS chat_favorite;
DROP TABLE IF EXISTS chat_model_call_log;
DROP TABLE IF EXISTS chat_chunk;
DROP TABLE IF EXISTS chat_document;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_session;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL COMMENT '登录名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    nickname      VARCHAR(50)           DEFAULT NULL,
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 停用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT '用户';

CREATE TABLE chat_session
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    title           VARCHAR(100)          DEFAULT NULL,
    provider        VARCHAR(20)  NOT NULL DEFAULT 'deepseek',
    model           VARCHAR(50)  NOT NULL DEFAULT 'deepseek-chat',
    document_id     BIGINT                DEFAULT NULL COMMENT '可空，会话绑定的 TXT 文档',
    last_message_at DATETIME              DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_last (user_id, last_message_at)
) ENGINE = InnoDB COMMENT '会话';

CREATE TABLE chat_message
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    session_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL COMMENT 'user / assistant',
    content    LONGTEXT    NOT NULL,
    provider   VARCHAR(20)          DEFAULT NULL,
    model      VARCHAR(50)          DEFAULT NULL,
    token_in   INT                  DEFAULT NULL,
    token_out  INT                  DEFAULT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' COMMENT 'COMPLETED / ABORTED / FAILED',
    latency_ms INT                  DEFAULT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session_id (session_id, id)
) ENGINE = InnoDB COMMENT '消息';

CREATE TABLE chat_document
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    filename    VARCHAR(255) NOT NULL,
    byte_size   INT          NOT NULL,
    char_count  INT          NOT NULL DEFAULT 0,
    chunk_count INT          NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PARSING' COMMENT 'READY / EMBED_FAILED / PARSING',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT 'TXT 文档';

CREATE TABLE chat_chunk
(
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    document_id BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    seq         INT      NOT NULL,
    content     TEXT     NOT NULL,
    embedding   LONGBLOB          DEFAULT NULL COMMENT 'float32 小端序列',
    token_est   INT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_document (document_id)
) ENGINE = InnoDB COMMENT '文档片段';

CREATE TABLE chat_favorite
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    session_id BIGINT               DEFAULT NULL COMMENT '弱引用，仅作来源展示',
    question   TEXT        NOT NULL,
    answer     MEDIUMTEXT  NOT NULL,
    provider   VARCHAR(20)          DEFAULT NULL,
    model      VARCHAR(50)          DEFAULT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_created (user_id, created_at)
) ENGINE = InnoDB COMMENT '收藏（问答快照）';

CREATE TABLE chat_tag
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    name       VARCHAR(30) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_name (user_id, name)
) ENGINE = InnoDB COMMENT '标签';

CREATE TABLE chat_favorite_tag
(
    favorite_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (favorite_id, tag_id),
    KEY idx_tag (tag_id)
) ENGINE = InnoDB COMMENT '收藏-标签';

CREATE TABLE chat_model_call_log
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    session_id     BIGINT               DEFAULT NULL,
    message_id     BIGINT               DEFAULT NULL,
    request_id     VARCHAR(40) NOT NULL COMMENT '= 日志 traceId',
    provider       VARCHAR(20) NOT NULL,
    model          VARCHAR(50) NOT NULL,
    status         VARCHAR(20) NOT NULL COMMENT 'SUCCESS / FAILED',
    http_status    INT                  DEFAULT NULL,
    token_in       INT                  DEFAULT NULL,
    token_out      INT                  DEFAULT NULL,
    latency_ms     INT                  DEFAULT NULL,
    first_token_ms INT                  DEFAULT NULL,
    retry_count    INT         NOT NULL DEFAULT 0,
    error_code     VARCHAR(40)          DEFAULT NULL,
    error_msg      VARCHAR(255)         DEFAULT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_created (user_id, created_at)
) ENGINE = InnoDB COMMENT '模型调用日志';
