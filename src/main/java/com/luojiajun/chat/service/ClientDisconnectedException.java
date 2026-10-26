package com.luojiajun.chat.service;

/**
 * 客户端断开（或 SSE 写失败）的内部信号，只用于中断生成，不外泄。
 */
public class ClientDisconnectedException extends RuntimeException {

    public ClientDisconnectedException() {
        super("client disconnected");
    }
}
