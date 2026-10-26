package com.luojiajun.chat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("轻量 AI 智能问答助手 API")
                .description("会话隔离 / SSE 流式 / 轻量 RAG / 收藏标签")
                .version("1.0.0"));
    }
}
