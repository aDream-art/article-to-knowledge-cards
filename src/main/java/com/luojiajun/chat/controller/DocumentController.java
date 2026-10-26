package com.luojiajun.chat.controller;

import com.luojiajun.chat.common.CommonResult;
import com.luojiajun.chat.entity.ChatDocument;
import com.luojiajun.chat.security.SecurityUtils;
import com.luojiajun.chat.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档", description = "TXT 上传与轻量 RAG 语料")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传 TXT（≤1MB，同步解析并向量化）")
    public CommonResult<ChatDocument> upload(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(documentService.upload(SecurityUtils.currentUserId(), file));
    }

    @GetMapping
    @Operation(summary = "我的文档列表")
    public CommonResult<List<ChatDocument>> list() {
        return CommonResult.success(documentService.list(SecurityUtils.currentUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档（同时解绑引用它的会话）")
    public CommonResult<Void> delete(@PathVariable Long id) {
        documentService.delete(SecurityUtils.currentUserId(), id);
        return CommonResult.success(null);
    }
}
