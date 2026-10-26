package com.luojiajun.chat.model;

/**
 * 中文场景的粗估：1 个汉字约 0.7 token，1 个英文单词约 1.3 token。
 * 只在上游不返回 usage 时兜底，代码里所有估算都必须走这里，方便一处调整。
 */
public final class TokenEstimator {

    private TokenEstimator() {
    }

    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                cjk++;
            } else if (!Character.isWhitespace(c)) {
                other++;
            }
        }
        return (int) Math.ceil(cjk * 0.7 + other * 0.3);
    }
}
