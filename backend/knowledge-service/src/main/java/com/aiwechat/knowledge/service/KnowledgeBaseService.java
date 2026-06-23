package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库基础服务接口
 * 提供文档上传、文本添加、向量检索等核心功能
 */
public interface KnowledgeBaseService {

    /**
     * 文本条目记录
     */
    record TextItem(String title, String content, String bizType, String category, String source) {
        public TextItem(String title, String content) {
            this(title, content, null, null, null);
        }

        public TextItem(String title, String content, String bizType, String category) {
            this(title, content, bizType, category, null);
        }
    }

    /**
     * 上传结果记录
     */
    record UploadResult(String fileName, boolean success, String documentId, String error) {}

    /**
     * 文档信息记录
     */
    record DocumentInfo(String id, String title, String source, int chunkCount, LocalDateTime createdAt) {}

    /**
     * 知识初始化结果
     */
    record InitSummary(
            int menuCount,
            int deliveryRuleCount,
            int orderRuleCount,
            int faqCount,
            int totalCount,
            List<String> documentIds) {}

    /**
     * 上传单个文档
     * @param file 上传的文件
     * @param metadata 元数据信息
     * @return 文档ID列表
     */
    List<String> uploadDocument(MultipartFile file, String metadata);

    /**
     * 批量上传文档
     * @param files 上传的文件数组
     * @return 上传结果列表
     */
    List<UploadResult> uploadDocuments(MultipartFile[] files);

    /**
     * 添加单条文本到知识库
     * @param content 文本内容
     * @param title 标题
     * @return 文档ID
     */
    String addText(String content, String title);

    /**
     * 批量添加文本到知识库
     * @param texts 文本条目列表
     * @return 文档ID列表
     */
    List<String> addTexts(List<TextItem> texts);

    /**
     * 知识库检索
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @return 检索结果
     */
    KnowledgeSearchResult search(String query, int topK);

    /**
     * 列出所有文档信息
     * @return 文档信息列表
     */
    List<DocumentInfo> listDocuments();

    /**
     * 删除指定文档
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    boolean deleteDocument(String documentId);

    /**
     * 清空知识库
     */
    void clear();

    /**
     * 初始化菜单知识库
     * 从数据库加载菜品数据并构建向量索引
     */
    void initMenuKnowledge();

    /**
     * 初始化默认知识库
     * 一次性导入菜单、配送规则、订单规则、FAQ
     */
    InitSummary initDefaultKnowledge();
}
