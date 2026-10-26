package com.luojiajun.chat.dto;

import lombok.Data;

@Data
public class BindDocumentParam {
    /** 传 null 表示解绑 */
    private Long documentId;
}
