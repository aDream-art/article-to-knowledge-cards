package com.luojiajun.chat.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    @Test
    void splitsByParagraphFirst() {
        List<String> chunks = TextChunker.chunk("第一段内容。\n\n第二段内容。", 400, 80);
        assertThat(chunks).containsExactly("第一段内容。", "第二段内容。");
    }

    @Test
    void longParagraphIsCutIntoSentences() {
        String paragraph = "这是一个句子。".repeat(60); // 420 字
        List<String> chunks = TextChunker.chunk(paragraph, 400, 80);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(400 + 80));
    }

    @Test
    void textWithoutPunctuationIsHardSplit() {
        String text = "字".repeat(1000);
        List<String> chunks = TextChunker.chunk(text, 400, 80);
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk).isNotBlank());
    }

    @Test
    void blankInputYieldsNothing() {
        assertThat(TextChunker.chunk(null, 400, 80)).isEmpty();
        assertThat(TextChunker.chunk("   \n\n  ", 400, 80)).isEmpty();
    }
}
