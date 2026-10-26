package com.luojiajun.chat.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文长文本切分：段落优先 → 超长段落按句号/问号/换行切 → 目标 400 字、重叠 80 字。
 * 纯函数，单测覆盖边界（空文本、超长无标点、正好等于块大小）。
 */
public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null) {
            return chunks;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.isEmpty()) {
            return chunks;
        }
        for (String paragraph : normalized.split("\n{2,}")) {
            String p = paragraph.strip();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() <= chunkSize) {
                chunks.add(p);
            } else {
                chunks.addAll(splitLongParagraph(p, chunkSize, overlap));
            }
        }
        return chunks;
    }

    private static List<String> splitLongParagraph(String paragraph, int chunkSize, int overlap) {
        List<String> sentences = splitSentences(paragraph);
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > chunkSize && current.length() > 0) {
                result.add(current.toString());
                String tail = tail(current.toString(), overlap);
                current.setLength(0);
                current.append(tail);
            }
            if (sentence.length() > chunkSize) {
                // 单句仍然超长：硬切
                for (int i = 0; i < sentence.length(); i += chunkSize) {
                    result.add(sentence.substring(i, Math.min(sentence.length(), i + chunkSize)));
                }
                current.setLength(0);
            } else {
                current.append(sentence);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private static List<String> splitSentences(String paragraph) {
        List<String> sentences = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < paragraph.length(); i++) {
            char c = paragraph.charAt(i);
            buffer.append(c);
            boolean end = c == '。' || c == '！' || c == '？' || c == '；'
                    || c == '.' || c == '!' || c == '?' || c == ';' || c == '\n';
            if (end) {
                sentences.add(buffer.toString());
                buffer.setLength(0);
            }
        }
        if (buffer.length() > 0) {
            sentences.add(buffer.toString());
        }
        return sentences;
    }

    private static String tail(String text, int overlap) {
        if (text.length() <= overlap) {
            return text;
        }
        return text.substring(text.length() - overlap);
    }
}
