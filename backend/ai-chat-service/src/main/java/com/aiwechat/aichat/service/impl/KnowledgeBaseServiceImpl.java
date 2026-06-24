package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.model.dto.KnowledgeSearchResult;
import com.aiwechat.aichat.service.KnowledgeBaseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务 - 远程调用 knowledge-service
 * 通过 REST 调用 knowledge-service 的混合检索 API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${knowledge.service.url:http://localhost:9095}")
    private String knowledgeServiceUrl;

    @Override
    public List<String> uploadDocument(MultipartFile file, String metadata) {
        log.warn("知识库上传文档请直接调用 knowledge-service");
        return Collections.emptyList();
    }

    @Override
    public KnowledgeSearchResult search(String query, int topK) {
        return search(query, topK, "retrieve");
    }

    /**
     * 远程检索 - 支持指定检索类型
     */
    public KnowledgeSearchResult search(String query, int topK, String searchType) {
        try {
            String url = buildSearchUrl(searchType, query, topK);
            log.debug("调用 knowledge-service 检索: url={}", url);

            String responseJson = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            return parseSearchResult(responseJson);

        } catch (Exception e) {
            log.error("调用 knowledge-service 检索失败: query={}, error={}", query, e.getMessage());
            // 降级：返回空结果
            KnowledgeSearchResult fallback = new KnowledgeSearchResult();
            fallback.setQuery(query);
            fallback.setCount(0);
            fallback.setResults(new ArrayList<>());
            fallback.setSearchType("fallback");
            return fallback;
        }
    }

    @Override
    public List<String> addTexts(List<TextItem> texts) {
        try {
            String url = knowledgeServiceUrl + "/api/admin/knowledge/add-texts";
            String jsonBody = objectMapper.writeValueAsString(texts);

            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.get("data");
            if (dataNode != null && dataNode.isArray()) {
                List<String> ids = new ArrayList<>();
                for (JsonNode node : dataNode) {
                    ids.add(node.asText());
                }
                return ids;
            }
        } catch (Exception e) {
            log.error("远程添加文本失败: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public void clear() {
        try {
            String url = knowledgeServiceUrl + "/api/admin/knowledge/clear";
            restClient.delete()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            log.info("远程清空知识库完成");
        } catch (Exception e) {
            log.error("远程清空知识库失败: {}", e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private String buildSearchUrl(String searchType, String query, int topK) {
        return switch (searchType) {
            case "vector" -> knowledgeServiceUrl + "/api/knowledge/vector-search?query=" +
                    java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) + "&topK=" + topK;
            case "keyword" -> knowledgeServiceUrl + "/api/knowledge/keyword-search?query=" +
                    java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) + "&topK=" + topK;
            case "menu" -> knowledgeServiceUrl + "/api/knowledge/menu-search?name=" +
                    java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            case "retrieve" -> knowledgeServiceUrl + "/api/knowledge/retrieve?query=" +
                    java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) + "&topK=" + topK;
            default -> knowledgeServiceUrl + "/api/knowledge/retrieve?query=" +
                    java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) + "&topK=" + topK;
        };
    }

    private KnowledgeSearchResult parseSearchResult(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode data = root.get("data");
            if (data == null) {
                log.warn("knowledge-service 返回数据为空");
                return new KnowledgeSearchResult();
            }

            KnowledgeSearchResult result = new KnowledgeSearchResult();
            result.setQuery(getTextValue(data, "query"));
            result.setCount(getIntValue(data, "count"));
            result.setCostMs(getLongValue(data, "costMs"));
            result.setSearchType(getTextValue(data, "searchType"));
            result.setRouteType(getTextValue(data, "routeType"));
            result.setRouteReason(getTextValue(data, "routeReason"));
            result.setFallback(getBooleanValue(data, "fallback"));

            List<KnowledgeSearchResult.ChunkResult> chunkResults = new ArrayList<>();
            JsonNode resultsNode = data.get("results");
            if (resultsNode != null && resultsNode.isArray()) {
                for (JsonNode node : resultsNode) {
                    KnowledgeSearchResult.ChunkResult cr = new KnowledgeSearchResult.ChunkResult();
                    cr.setContent(getTextValue(node, "content"));
                    cr.setSource(getTextValue(node, "source"));
                    cr.setScore(getDoubleValue(node, "score"));
                    cr.setChunkType(getTextValue(node, "chunkType"));
                    cr.setDocumentId(getTextValue(node, "documentId"));
                    cr.setEvidenceType(getTextValue(node, "evidenceType"));
                    cr.setTitle(getTextValue(node, "title"));

                    if (node.has("vectorScore") && !node.get("vectorScore").isNull()) {
                        cr.setVectorScore(node.get("vectorScore").asDouble());
                    }
                    if (node.has("keywordScore") && !node.get("keywordScore").isNull()) {
                        cr.setKeywordScore(node.get("keywordScore").asDouble());
                    }

                    chunkResults.add(cr);
                }
            }
            result.setResults(chunkResults);

            List<KnowledgeSearchResult.KnowledgeEvidence> evidences = new ArrayList<>();
            JsonNode evidenceNode = data.get("evidences");
            if (evidenceNode != null && evidenceNode.isArray()) {
                for (JsonNode node : evidenceNode) {
                    KnowledgeSearchResult.KnowledgeEvidence evidence = new KnowledgeSearchResult.KnowledgeEvidence();
                    evidence.setType(getTextValue(node, "type"));
                    evidence.setSource(getTextValue(node, "source"));
                    evidence.setTitle(getTextValue(node, "title"));
                    evidence.setContent(getTextValue(node, "content"));
                    evidence.setScore(getDoubleValue(node, "score"));
                    evidences.add(evidence);
                }
            }
            result.setEvidences(evidences);
            return result;

        } catch (Exception e) {
            log.error("解析 knowledge-service 响应失败: {}", e.getMessage());
            return new KnowledgeSearchResult();
        }
    }

    private String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private int getIntValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asInt() : 0;
    }

    private long getLongValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : 0L;
    }

    private double getDoubleValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asDouble() : 0.0;
    }

    private boolean getBooleanValue(JsonNode node, String field) {
        return node.has(field) && node.get(field).asBoolean();
    }
}
