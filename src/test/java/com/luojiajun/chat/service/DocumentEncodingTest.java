package com.luojiajun.chat.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentEncodingTest {

    @Test
    void decodesUtf8() {
        String text = "这是一段 UTF-8 编码的中文文档。";
        assertThat(DocumentService.decode(text.getBytes(StandardCharsets.UTF_8))).isEqualTo(text);
    }

    @Test
    void decodesGbkWhenUtf8Fails() {
        String text = "这是一段 GBK 编码的中文文档。";
        byte[] gbk = text.getBytes(Charset.forName("GBK"));
        assertThat(DocumentService.decode(gbk)).isEqualTo(text);
    }

    @Test
    void emptyBytesDecodeToEmptyString() {
        assertThat(DocumentService.decode(new byte[0])).isEmpty();
    }
}
