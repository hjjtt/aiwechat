package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.entity.KnowledgeChunk;
import com.aiwechat.knowledge.model.entity.MenuItem;
import com.aiwechat.knowledge.repository.KnowledgeChunkRepository;
import com.aiwechat.knowledge.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructuredKnowledgeServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    private StructuredKnowledgeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StructuredKnowledgeServiceImpl(menuItemRepository, chunkRepository);
    }

    @Test
    @DisplayName("菜品价格问题应直接从菜单结构化数据生成证据")
    void searchShouldReturnMenuEvidenceForPriceQuestion() {
        when(menuItemRepository.findAllAvailable()).thenReturn(List.of(
                menuItem(1L, "宫保鸡丁", "热菜", "招牌下饭菜", "28.00", 120),
                menuItem(2L, "酸辣汤", "汤类", "开胃汤品", "12.00", 80)
        ));

        KnowledgeSearchResult result = service.search("宫保鸡丁多少钱", 3);

        assertEquals("structured", result.getSearchType());
        assertEquals(1, result.getCount());
        assertEquals("menu_item", result.getEvidences().get(0).getSource());
        assertEquals("宫保鸡丁", result.getEvidences().get(0).getTitle());
        assertTrue(result.getEvidences().get(0).getContent().contains("28.00元"));
        assertEquals("structured", result.getResults().get(0).getEvidenceType());
    }

    @Test
    @DisplayName("推荐菜问题没有精确菜名时应按销量返回菜单证据")
    void searchShouldReturnTopSalesMenuForRecommendQuestion() {
        when(menuItemRepository.findAllAvailable()).thenReturn(List.of(
                menuItem(1L, "宫保鸡丁", "热菜", "招牌下饭菜", "28.00", 120),
                menuItem(2L, "酸辣汤", "汤类", "开胃汤品", "12.00", 180)
        ));

        KnowledgeSearchResult result = service.search("有什么推荐菜", 1);

        assertEquals(1, result.getCount());
        assertEquals("酸辣汤", result.getEvidences().get(0).getTitle());
    }

    @Test
    @DisplayName("配送规则问题应返回规则类证据")
    void searchShouldReturnRuleEvidenceForDeliveryQuestion() {
        when(chunkRepository.searchByKeywords(anyString(), anyInt())).thenReturn(List.of(
                chunk("c1", "配送规则-配送费与免配送费", "满 59 元免配送费", "rule")
        ));

        KnowledgeSearchResult result = service.search("配送费怎么算", 3);

        assertFalse(result.getEvidences().isEmpty());
        assertEquals("rule_delivery", result.getEvidences().get(0).getSource());
        assertEquals("配送规则-配送费与免配送费", result.getEvidences().get(0).getTitle());
    }

    @Test
    @DisplayName("非结构化问题不应查询菜单和规则表")
    void searchShouldSkipRepositoriesForUnstructuredQuestion() {
        KnowledgeSearchResult result = service.search("你好", 3);

        assertEquals(0, result.getCount());
        verify(menuItemRepository, never()).findAllAvailable();
        verify(chunkRepository, never()).searchByKeywords(anyString(), anyInt());
    }

    private MenuItem menuItem(Long id, String name, String category, String description, String price, int salesCount) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName(name);
        item.setCategory(category);
        item.setDescription(description);
        item.setPrice(new BigDecimal(price));
        item.setSalesCount(salesCount);
        item.setIsAvailable(1);
        return item;
    }

    private KnowledgeChunk chunk(String id, String source, String content, String chunkType) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setDocumentId(10L);
        chunk.setSource(source);
        chunk.setContent(content);
        chunk.setChunkType(chunkType);
        return chunk;
    }
}
