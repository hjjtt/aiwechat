package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.entity.KnowledgeDocument;
import com.aiwechat.knowledge.model.entity.MenuItem;
import com.aiwechat.knowledge.repository.KnowledgeChunkRepository;
import com.aiwechat.knowledge.repository.KnowledgeDocumentRepository;
import com.aiwechat.knowledge.repository.MenuItemRepository;
import com.aiwechat.knowledge.service.KnowledgeBaseService;
import com.aiwechat.knowledge.service.chunk.ChunkStrategyFactory;
import com.aiwechat.knowledge.service.seed.DefaultKnowledgeSeedProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    private KnowledgeBaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseServiceImpl(
                vectorStore,
                menuItemRepository,
                documentRepository,
                chunkRepository,
                new ChunkStrategyFactory(),
                new DefaultKnowledgeSeedProvider(),
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("初始化默认知识库应导入菜单、配送规则、订单规则和 FAQ")
    void initDefaultKnowledgeShouldSeedMenuRulesAndFaq() {
        when(menuItemRepository.findAllAvailable()).thenReturn(List.of(
                menuItem("宫保鸡丁", "热菜", "招牌下饭菜", new BigDecimal("28"), 120),
                menuItem("酸辣汤", "汤类", "开胃汤品", new BigDecimal("12"), 60)
        ));
        when(documentRepository.selectByContentHash(anyString())).thenReturn(null);
        when(documentRepository.updateById(any(KnowledgeDocument.class))).thenReturn(1);
        when(documentRepository.updateChunkCount(anyString(), anyInt())).thenReturn(1);
        when(chunkRepository.batchInsert(anyList())).thenReturn(1);
        doNothing().when(vectorStore).add(anyList());

        AtomicLong idSequence = new AtomicLong(1000L);
        when(documentRepository.insert(any(KnowledgeDocument.class))).thenAnswer(invocation -> {
            KnowledgeDocument document = invocation.getArgument(0);
            document.setId(idSequence.incrementAndGet());
            return 1;
        });

        KnowledgeBaseService.InitSummary summary = service.initDefaultKnowledge();

        assertEquals(2, summary.menuCount());
        assertEquals(3, summary.deliveryRuleCount());
        assertEquals(3, summary.orderRuleCount());
        assertEquals(6, summary.faqCount());
        assertEquals(14, summary.totalCount());
        assertEquals(14, summary.documentIds().size());
        assertEquals("1001", summary.documentIds().get(0));
        assertEquals("1014", summary.documentIds().get(13));
        assertTrue(summary.documentIds().stream().allMatch(id -> id.matches("\\d+")));

        verify(menuItemRepository).findAllAvailable();
        verify(documentRepository, times(14)).insert(any(KnowledgeDocument.class));
        verify(chunkRepository, times(14)).batchInsert(anyList());
        verify(vectorStore, times(14)).add(anyList());
    }

    private MenuItem menuItem(String name, String category, String description, BigDecimal price, int salesCount) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setCategory(category);
        item.setDescription(description);
        item.setPrice(price);
        item.setSalesCount(salesCount);
        item.setIsAvailable(1);
        return item;
    }
}
