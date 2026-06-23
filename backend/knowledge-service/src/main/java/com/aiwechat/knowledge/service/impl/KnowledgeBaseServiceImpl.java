package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.entity.KnowledgeChunk;
import com.aiwechat.knowledge.model.entity.KnowledgeDocument;
import com.aiwechat.knowledge.model.entity.MenuItem;
import com.aiwechat.knowledge.repository.KnowledgeChunkRepository;
import com.aiwechat.knowledge.repository.KnowledgeDocumentRepository;
import com.aiwechat.knowledge.repository.MenuItemRepository;
import com.aiwechat.knowledge.service.KnowledgeBaseService;
import com.aiwechat.knowledge.service.chunk.ChunkStrategy;
import com.aiwechat.knowledge.service.chunk.ChunkStrategyFactory;
import com.aiwechat.knowledge.service.chunk.MetadataBuilder;
import com.aiwechat.knowledge.service.seed.DefaultKnowledgeSeedProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final VectorStore vectorStore;
    private final MenuItemRepository menuItemRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final ChunkStrategyFactory chunkStrategyFactory;
    private final DefaultKnowledgeSeedProvider defaultKnowledgeSeedProvider;
    private final ObjectMapper objectMapper;

    /** 向量存储是否可用 */
    private boolean vectorAvailable = true;

    @Override
    public List<String> uploadDocument(MultipartFile file, String metadata) {
        String originalFilename = file.getOriginalFilename();
        log.info("开始处理上传文档: {}", originalFilename);
        List<String> documentIds = new ArrayList<>();

        try {
            Path tempDir = Files.createTempDirectory("knowledge-upload");
            String suffix = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".tmp";
            Path tempFile = Files.createTempFile(tempDir, "upload-", suffix);
            file.transferTo(tempFile.toFile());

            String content = Files.readString(tempFile, StandardCharsets.UTF_8);
            String contentHash = computeHash(content);

            KnowledgeDocument existing = documentRepository.selectByContentHash(contentHash);
            if (existing != null) {
                log.info("文档内容重复，跳过入库: existingId={}", existing.getId());
                Files.deleteIfExists(tempFile);
                Files.deleteIfExists(tempDir);
                documentIds.add(String.valueOf(existing.getId()));
                return documentIds;
            }

            List<Document> documents;
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
                documents = loadPdfDocuments(tempFile.toFile());
            } else {
                documents = loadTextDocuments(tempFile.toFile());
            }

            String fileType = originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf") ? "pdf" : "txt";
            String bizType = detectBizType(originalFilename, metadata);
            String source = StringUtils.hasText(originalFilename) ? originalFilename : "上传文档";

            KnowledgeDocument docRecord = new KnowledgeDocument();
            docRecord.setTitle(originalFilename);
            docRecord.setContent(content.length() > 5000 ? content.substring(0, 5000) : content);
            docRecord.setSource(source);
            docRecord.setFilePath(tempFile.toString());
            docRecord.setFileType(fileType);
            docRecord.setBizType(bizType);
            docRecord.setStatus("active");
            docRecord.setVersion(1);
            docRecord.setChunkCount(0);
            docRecord.setContentHash(contentHash);
            docRecord.setCreatedAt(LocalDateTime.now());
            docRecord.setUpdatedAt(LocalDateTime.now());
            documentRepository.insert(docRecord);

            Long docId = docRecord.getId();
            String docIdStr = String.valueOf(docId);

            Map<String, Object> docMeta = buildDocumentMetadata(
                    docIdStr, source, fileType, bizType, null, metadata);
            docRecord.setMetadata(toJson(docMeta));
            documentRepository.updateById(docRecord);

            for (Document doc : documents) {
                doc.getMetadata().putAll(docMeta);
            }

            ChunkStrategy strategy = chunkStrategyFactory.getStrategy(bizType);
            List<Document> chunks = strategy.split(
                    documents.stream().map(Document::getText).collect(Collectors.joining("\n")),
                    docMeta
            );

            boolean vectorSynced = tryVectorAdd(chunks);

            documentRepository.updateChunkCount(docIdStr, chunks.size());
            saveChunksToDb(docId, chunks, strategy, vectorSynced);

            documentIds.add(docIdStr);
            log.info("文档上传完成: {}, 生成 {} 个分块, 向量同步: {}", originalFilename, chunks.size(), vectorSynced);

            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempDir);

        } catch (Exception e) {
            log.error("文档上传失败: {}", originalFilename, e);
            throw new RuntimeException("文档上传失败: " + e.getMessage(), e);
        }

        return documentIds;
    }

    @Override
    public List<UploadResult> uploadDocuments(MultipartFile[] files) {
        log.info("开始批量上传 {} 个文档", files.length);
        List<UploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                List<String> ids = uploadDocument(file, null);
                results.add(new UploadResult(file.getOriginalFilename(), true,
                        ids.isEmpty() ? null : ids.get(0), null));
            } catch (Exception e) {
                log.error("文件上传失败: {}", file.getOriginalFilename(), e);
                results.add(new UploadResult(file.getOriginalFilename(), false, null, e.getMessage()));
            }
        }
        return results;
    }

    @Override
    @Transactional
    public String addText(String content, String title) {
        return addTextInternal(new TextItem(title, content));
    }

    @Override
    public List<String> addTexts(List<TextItem> texts) {
        log.info("批量添加 {} 条文本到知识库", texts.size());
        List<String> documentIds = new ArrayList<>();
        for (TextItem item : texts) {
            try {
                String id = addTextInternal(item);
                documentIds.add(id);
            } catch (Exception e) {
                log.error("添加文本失败: {}, error: {}", item.title(), e.getMessage());
            }
        }
        return documentIds;
    }

    @Override
    public KnowledgeSearchResult search(String query, int topK) {
        log.info("执行知识库检索: query={}, topK={}", query, topK);

        if (vectorAvailable) {
            try {
                SearchRequest searchRequest = SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build();

                List<Document> results = vectorStore.similaritySearch(searchRequest);

                KnowledgeSearchResult searchResult = new KnowledgeSearchResult();
                searchResult.setQuery(query);
                searchResult.setCount(results.size());
                searchResult.setSearchType("vector");

                List<ChunkResult> chunkResults = new ArrayList<>();
                for (Document doc : results) {
                    ChunkResult chunk = new ChunkResult();
                    chunk.setContent(doc.getText());
                    chunk.setSource(doc.getMetadata().get("source") != null
                            ? doc.getMetadata().get("source").toString() : "unknown");
                    chunk.setScore(doc.getMetadata().get("distance") != null
                            ? Double.parseDouble(doc.getMetadata().get("distance").toString()) : 0.0);
                    chunkResults.add(chunk);
                }
                searchResult.setResults(chunkResults);

                log.info("向量检索完成，返回 {} 条结果", chunkResults.size());
                return searchResult;
            } catch (Exception e) {
                log.warn("向量检索失败，降级到关键词检索: {}", e.getMessage());
                vectorAvailable = false;
            }
        }

        return keywordFallback(query, topK);
    }

    @Override
    public List<DocumentInfo> listDocuments() {
        log.info("列出所有文档信息");
        List<KnowledgeDocument> docs = documentRepository.selectList();
        return docs.stream()
                .map(d -> new DocumentInfo(
                        String.valueOf(d.getId()), d.getTitle(), d.getSource(),
                        d.getChunkCount() != null ? d.getChunkCount() : 0,
                        d.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public boolean deleteDocument(String documentId) {
        log.info("删除文档: documentId={}", documentId);
        chunkRepository.deleteByDocumentId(documentId);
        documentRepository.updateStatus(documentId, "deleted");
        return true;
    }

    @Override
    @Transactional
    public void clear() {
        log.info("清空知识库");
        List<KnowledgeDocument> docs = documentRepository.selectList();
        for (KnowledgeDocument doc : docs) {
            documentRepository.updateStatus(String.valueOf(doc.getId()), "deleted");
            chunkRepository.deleteByDocumentId(String.valueOf(doc.getId()));
        }
    }

    @Override
    public void initMenuKnowledge() {
        log.info("开始初始化菜单知识库");
        List<MenuItem> menuItems = menuItemRepository.findAllAvailable();
        log.info("从数据库加载到 {} 个上架菜品", menuItems.size());

        List<TextItem> menuTexts = buildMenuKnowledge(menuItems);
        List<String> ids = addTexts(menuTexts);

        log.info("菜单知识库初始化完成，共添加 {} 条知识文本", ids.size());
    }

    @Override
    public InitSummary initDefaultKnowledge() {
        log.info("开始初始化默认知识库");
        List<MenuItem> menuItems = menuItemRepository.findAllAvailable();
        List<TextItem> menuTexts = buildMenuKnowledge(menuItems);
        DefaultKnowledgeSeedProvider.SeedBundle seedBundle = defaultKnowledgeSeedProvider.buildDefaultSeeds();

        List<TextItem> allTexts = new ArrayList<>(menuTexts);
        allTexts.addAll(seedBundle.allTexts());

        List<String> ids = addTexts(allTexts);
        InitSummary summary = new InitSummary(
                menuTexts.size(),
                seedBundle.deliveryRules().size(),
                seedBundle.orderRules().size(),
                seedBundle.faqs().size(),
                ids.size(),
                ids
        );
        log.info("默认知识库初始化完成: menu={}, deliveryRule={}, orderRule={}, faq={}, total={}",
                summary.menuCount(), summary.deliveryRuleCount(), summary.orderRuleCount(),
                summary.faqCount(), summary.totalCount());
        return summary;
    }

    private String addTextInternal(TextItem item) {
        String content = item.content();
        String title = item.title();
        String source = StringUtils.hasText(item.source()) ? item.source() : title;
        String bizType = StringUtils.hasText(item.bizType())
                ? item.bizType()
                : detectBizType(title, null);
        String category = StringUtils.hasText(item.category()) ? item.category() : null;

        log.info("添加文本到知识库: title={}, bizType={}", title, bizType);

        String contentHash = computeHash(content);

        KnowledgeDocument existing = documentRepository.selectByContentHash(contentHash);
        if (existing != null) {
            log.info("文本内容重复，跳过: existingId={}", existing.getId());
            return String.valueOf(existing.getId());
        }

        KnowledgeDocument docRecord = new KnowledgeDocument();
        docRecord.setTitle(title);
        docRecord.setContent(content.length() > 5000 ? content.substring(0, 5000) : content);
        docRecord.setSource(source);
        docRecord.setFileType("text");
        docRecord.setBizType(bizType);
        docRecord.setCategory(category);
        docRecord.setStatus("active");
        docRecord.setVersion(1);
        docRecord.setChunkCount(0);
        docRecord.setContentHash(contentHash);
        docRecord.setCreatedAt(LocalDateTime.now());
        docRecord.setUpdatedAt(LocalDateTime.now());
        documentRepository.insert(docRecord);

        Long docId = docRecord.getId();
        String docIdStr = String.valueOf(docId);

        Map<String, Object> docMeta = buildDocumentMetadata(
                docIdStr, source, "text", bizType, category, title);
        docRecord.setMetadata(toJson(docMeta));
        documentRepository.updateById(docRecord);

        ChunkStrategy strategy = chunkStrategyFactory.getStrategy(bizType);
        List<Document> chunks = strategy.split(content, docMeta);

        boolean vectorSynced = tryVectorAdd(chunks);

        documentRepository.updateChunkCount(docIdStr, chunks.size());
        saveChunksToDb(docId, chunks, strategy, vectorSynced);

        log.info("文本添加完成: {}, 生成 {} 个分块, 向量同步: {}", title, chunks.size(), vectorSynced);
        return docIdStr;
    }

    private boolean tryVectorAdd(List<Document> chunks) {
        if (!vectorAvailable) {
            return false;
        }
        try {
            vectorStore.add(chunks);
            return true;
        } catch (Exception e) {
            log.warn("向量存储写入失败，数据仅写入数据库: {}", e.getMessage());
            vectorAvailable = false;
            return false;
        }
    }

    private KnowledgeSearchResult keywordFallback(String query, int topK) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setQuery(query);
        result.setSearchType("keyword_fallback");

        List<KnowledgeChunk> chunks = chunkRepository.searchByKeywords(query, topK);
        List<ChunkResult> chunkResults = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            ChunkResult cr = new ChunkResult();
            cr.setContent(chunk.getContent());
            cr.setSource(chunk.getSource());
            cr.setScore(0.8);
            cr.setChunkType(chunk.getChunkType());
            cr.setDocumentId(String.valueOf(chunk.getDocumentId()));
            chunkResults.add(cr);
        }

        result.setResults(chunkResults);
        result.setCount(chunkResults.size());
        log.info("关键词降级检索完成，返回 {} 条结果", chunkResults.size());
        return result;
    }

    private List<Document> loadPdfDocuments(File file) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(file));
        return List.copyOf(reader.get());
    }

    private List<Document> loadTextDocuments(File file) {
        TextReader reader = new TextReader(new FileSystemResource(file));
        return reader.get();
    }

    private List<TextItem> buildMenuKnowledge(List<MenuItem> menuItems) {
        List<TextItem> texts = new ArrayList<>();
        for (MenuItem item : menuItems) {
            StringBuilder content = new StringBuilder();
            content.append("菜品名称：").append(item.getName()).append("\n");
            content.append("价格：").append(item.getPrice()).append("元\n");
            content.append("分类：").append(item.getCategory()).append("\n");
            if (StringUtils.hasText(item.getDescription())) {
                content.append("描述：").append(item.getDescription()).append("\n");
            }
            if (item.getSalesCount() != null) {
                content.append("销量：").append(item.getSalesCount()).append("份\n");
            }
            texts.add(new TextItem(
                    item.getName(),
                    content.toString(),
                    "menu",
                    item.getCategory(),
                    item.getName()
            ));
        }
        return texts;
    }

    private void saveChunksToDb(Long docId, List<Document> chunks, ChunkStrategy strategy, boolean vectorSynced) {
        List<KnowledgeChunk> dbChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            String text = chunk.getText();

            KnowledgeChunk dbChunk = new KnowledgeChunk();
            dbChunk.setId(UUID.randomUUID().toString());
            dbChunk.setDocumentId(docId);
            dbChunk.setContent(text);
            dbChunk.setChunkIndex(i);
            dbChunk.setChunkType((String) chunk.getMetadata().get("chunkType"));
            dbChunk.setSource((String) chunk.getMetadata().get("source"));
            dbChunk.setVectorSynced(vectorSynced);
            dbChunk.setKeywords(String.join(",", strategy.extractKeywords(text)));
            dbChunk.setMetadata(toJson(chunk.getMetadata()));
            dbChunk.setCreatedAt(LocalDateTime.now());
            dbChunk.setUpdatedAt(LocalDateTime.now());
            dbChunks.add(dbChunk);
        }

        if (!dbChunks.isEmpty()) {
            chunkRepository.batchInsert(dbChunks);
        }
    }

    private Map<String, Object> buildDocumentMetadata(
            String documentId,
            String source,
            String documentType,
            String bizType,
            String category,
            String rawMetadata) {
        MetadataBuilder builder = new MetadataBuilder()
                .documentId(documentId)
                .source(source)
                .documentType(documentType)
                .bizType(bizType)
                .version(1)
                .updatedAt(LocalDateTime.now());
        if (StringUtils.hasText(category)) {
            builder.category(category);
        }
        if (StringUtils.hasText(rawMetadata)) {
            builder.extra("rawMetadata", rawMetadata);
        }
        return builder.build();
    }

    private String detectBizType(String filename, String metadata) {
        if (filename == null && metadata == null) {
            return "generic";
        }
        String combined = (filename != null ? filename.toLowerCase() : "")
                + " "
                + (metadata != null ? metadata.toLowerCase() : "");
        if (combined.contains("菜单") || combined.contains("menu")) {
            return "menu";
        }
        if (combined.contains("faq") || combined.contains("常见问题")) {
            return "faq";
        }
        if (combined.contains("订单") || combined.contains("退款") || combined.contains("售后")) {
            return "order_rule";
        }
        if (combined.contains("配送") || combined.contains("delivery")) {
            return "delivery_rule";
        }
        if (combined.contains("规则") || combined.contains("rule")) {
            return "rule";
        }
        if (combined.contains("优惠") || combined.contains("promotion")) {
            return "promotion";
        }
        return "generic";
    }

    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("metadata 序列化失败: {}", e.getMessage());
            return null;
        }
    }
}
