package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.LazyGraphIndexReport;
import com.aiwechat.knowledge.model.entity.KnowledgeChunk;
import com.aiwechat.knowledge.model.entity.KnowledgeConcept;
import com.aiwechat.knowledge.model.entity.KnowledgeConceptChunk;
import com.aiwechat.knowledge.model.entity.KnowledgeConceptEdge;
import com.aiwechat.knowledge.repository.KnowledgeChunkRepository;
import com.aiwechat.knowledge.repository.KnowledgeConceptChunkRepository;
import com.aiwechat.knowledge.repository.KnowledgeConceptEdgeRepository;
import com.aiwechat.knowledge.repository.KnowledgeConceptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LazyGraphRagServiceImplTest {

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    @Mock
    private KnowledgeConceptRepository conceptRepository;

    @Mock
    private KnowledgeConceptChunkRepository conceptChunkRepository;

    @Mock
    private KnowledgeConceptEdgeRepository conceptEdgeRepository;

    private LazyGraphRagServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LazyGraphRagServiceImpl(
                chunkRepository,
                conceptRepository,
                conceptChunkRepository,
                conceptEdgeRepository
        );
    }

    @Test
    @DisplayName("概念抽取应识别领域词和带标签字段")
    void extractConceptsShouldFindDomainAndLabeledConcepts() {
        List<String> concepts = service.extractConcepts("""
                菜品名称：宫保鸡丁
                分类：热菜
                配送费满 59 元免配送费，可开发票。
                """);

        assertTrue(concepts.contains("宫保鸡丁"));
        assertTrue(concepts.contains("热菜"));
        assertTrue(concepts.contains("配送费"));
        assertTrue(concepts.contains("免配送费"));
        assertTrue(concepts.contains("发票"));
    }

    @Test
    @DisplayName("重建索引应生成概念、chunk 关联和共现边")
    void rebuildIndexShouldCreateConceptMentionsAndEdges() {
        when(chunkRepository.selectList(null)).thenReturn(List.of(
                chunk("c1", 1L, "宫保鸡丁", "菜品名称：宫保鸡丁\n分类：热菜\n价格：28元", "menu"),
                chunk("c2", 2L, "配送规则", "配送费满 59 元免配送费，雨天配送时间可能延迟", "rule")
        ));
        AtomicLong idSequence = new AtomicLong(100L);
        when(conceptRepository.insert(any(KnowledgeConcept.class))).thenAnswer(invocation -> {
            KnowledgeConcept concept = invocation.getArgument(0);
            concept.setId(idSequence.incrementAndGet());
            return 1;
        });
        when(conceptChunkRepository.batchInsert(anyList())).thenReturn(1);
        when(conceptEdgeRepository.batchInsert(anyList())).thenReturn(1);

        LazyGraphIndexReport report = service.rebuildIndex();

        assertEquals(2, report.getChunksScanned());
        assertEquals(2, report.getChunksIndexed());
        assertTrue(report.getConceptsCreated() >= 4);
        assertTrue(report.getConceptMentionsCreated() >= 4);
        assertTrue(report.getEdgesCreated() >= 1);

        verify(conceptEdgeRepository).deleteAll();
        verify(conceptChunkRepository).deleteAll();
        verify(conceptRepository).deleteAll();
        verify(conceptRepository, times(report.getConceptsCreated())).incrementFrequency(any(), anyInt());
    }

    @Test
    @DisplayName("查询应根据概念和一跳邻居返回 lazy_graph evidence")
    void searchShouldReturnLazyGraphEvidence() {
        KnowledgeConcept combo = concept(1L, "套餐");
        KnowledgeConcept allergen = concept(2L, "过敏原");
        KnowledgeConcept menu = concept(3L, "宫保鸡丁");
        when(conceptRepository.selectByNormalizedNames(anyList())).thenReturn(List.of(combo, allergen));
        when(conceptEdgeRepository.selectNeighbors(anyList(), anyInt())).thenReturn(List.of(edge(1L, 3L), edge(2L, 3L)));
        when(conceptChunkRepository.selectByConceptIds(anyList(), anyInt())).thenReturn(List.of(
                conceptChunk(1L, "c1", 1.0),
                conceptChunk(3L, "c1", 0.8),
                conceptChunk(2L, "c2", 1.0)
        ));
        when(chunkRepository.selectById("c1")).thenReturn(chunk("c1", 10L, "套餐说明", "套餐A包含宫保鸡丁", "rule"));
        when(chunkRepository.selectById("c2")).thenReturn(chunk("c2", 11L, "过敏原说明", "宫保鸡丁含花生，请注意过敏原", "rule"));

        KnowledgeSearchResult result = service.search("套餐包含哪些菜品，哪些包含过敏原", 2);

        assertEquals("lazy_graph", result.getSearchType());
        assertEquals("lazy_graph", result.getRouteType());
        assertEquals(2, result.getCount());
        assertFalse(result.getEvidences().isEmpty());
        assertEquals("lazy_graph", result.getEvidences().get(0).getType());
        assertTrue(result.getEvidences().get(0).getMetadata().containsKey("queryConcepts"));
    }

    private KnowledgeChunk chunk(String id, Long documentId, String source, String content, String chunkType) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setSource(source);
        chunk.setContent(content);
        chunk.setChunkType(chunkType);
        return chunk;
    }

    private KnowledgeConcept concept(Long id, String name) {
        KnowledgeConcept concept = new KnowledgeConcept();
        concept.setId(id);
        concept.setName(name);
        concept.setNormalizedName(name);
        concept.setFrequency(1);
        return concept;
    }

    private KnowledgeConceptEdge edge(Long sourceId, Long targetId) {
        KnowledgeConceptEdge edge = new KnowledgeConceptEdge();
        edge.setSourceConceptId(sourceId);
        edge.setTargetConceptId(targetId);
        edge.setWeight(1);
        return edge;
    }

    private KnowledgeConceptChunk conceptChunk(Long conceptId, String chunkId, double weight) {
        KnowledgeConceptChunk conceptChunk = new KnowledgeConceptChunk();
        conceptChunk.setConceptId(conceptId);
        conceptChunk.setChunkId(chunkId);
        conceptChunk.setDocumentId(10L);
        conceptChunk.setWeight(weight);
        return conceptChunk;
    }
}
