package com.aiwechat.aichat.service;

import com.aiwechat.aichat.model.dto.KnowledgeSearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库管理服务接口
 * 当前为内存实现，后续改为 Feign 远程调用 knowledge-service
 */
public interface KnowledgeBaseService {

    /**
     * 上传文档并向量化
     *
     * @param file     文档文件
     * @param metadata 元数据（JSON格式）
     * @return 文档ID列表
     */
    List<String> uploadDocument(MultipartFile file, String metadata);

    /**
     * 搜索知识库
     *
     * @param query 查询文本
     * @param topK  返回结果数量
     * @return 搜索结果
     */
    KnowledgeSearchResult search(String query, int topK);

    /**
     * 批量添加文本
     *
     * @param texts 文本列表（标题-内容对）
     * @return 文档ID列表
     */
    List<String> addTexts(List<TextItem> texts);

    /**
     * 清空知识库
     */
    void clear();

    /**
     * 文本项
     *
     * @param title   标题
     * @param content 内容
     */
    record TextItem(String title, String content) {}

    /**
     * 上传结果
     *
     * @param fileName   文件名
     * @param success    是否成功
     * @param documentId 文档ID
     * @param error      错误信息
     */
    record UploadResult(String fileName, boolean success, String documentId, String error) {}

    /**
     * 文档信息
     *
     * @param id         文档ID
     * @param title      标题
     * @param source     来源
     * @param chunkCount 分块数量
     * @param createdAt  创建时间
     */
    record DocumentInfo(String id, String title, String source, int chunkCount, LocalDateTime createdAt) {}
}
