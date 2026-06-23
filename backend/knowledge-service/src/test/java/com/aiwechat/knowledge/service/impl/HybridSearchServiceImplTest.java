package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.entity.KnowledgeChunk;
import com.aiwechat.knowledge.repository.KnowledgeChunkRepository;
import com.aiwechat.knowledge.service.QueryRewriteService;
import com.aiwechat.knowledge.service.RerankService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridSearchServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    @Mock
    private RerankService rerankService;

    @Mock
    private QueryRewriteService queryRewriteService;

    private HybridSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HybridSearchServiceImpl(
                vectorStore,
                chunkRepository,
                rerankService,
                queryRewriteService,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("混合检索应按 metadata 过滤关键词召回结果")
    void hybridSearchShouldApplyMetadataFilters() {
        KnowledgeChunk ruleChunk = chunk("1", "配送规则", "配送费说明", "rule", "{\"bizType\":\"delivery_rule\"}");
        KnowledgeChunk faqChunk = chunk("2", "常见问题", "配送咨询入口", "faq", "{\"bizType\":\"faq\"}");

        when(queryRewriteService.needsRewrite(anyString())).thenReturn(false);
        when(chunkRepository.searchByKeywords(anyString(), anyInt())).thenReturn(List.of(ruleChunk, faqChunk));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(rerankService.rerank(anyString(), anyList(), anyInt())).thenAnswer(invocation -> {
            List<ChunkResult> candidates = invocation.getArgument(1);
            int topK = invocation.getArgument(2);
            return candidates.stream()
                    .sorted(Comparator.comparingDouble(ChunkResult::getScore).reversed())
                    .limit(topK)
                    .toList();
        });

        KnowledgeSearchResult result = service.hybridSearch("配送", 5, Map.of("bizType", "delivery_rule"));

        assertEquals(1, result.getResults().size());
        assertEquals("配送规则", result.getResults().get(0).getSource());
        assertEquals("delivery_rule", result.getResults().get(0).getMetadata().get("bizType"));
    }

    private KnowledgeChunk chunk(String id, String source, String content, String chunkType, String metadata) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setDocumentId(10L);
        chunk.setSource(source);
        chunk.setContent(content);
        chunk.setChunkType(chunkType);
        chunk.setMetadata(metadata);
        return chunk;
    }
}
