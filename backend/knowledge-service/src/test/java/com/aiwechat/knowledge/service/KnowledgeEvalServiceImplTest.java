package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.EvalReport;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.entity.KnowledgeEvalCase;
import com.aiwechat.knowledge.model.entity.KnowledgeEvalResult;
import com.aiwechat.knowledge.repository.KnowledgeEvalCaseRepository;
import com.aiwechat.knowledge.repository.KnowledgeEvalResultRepository;
import com.aiwechat.knowledge.service.impl.KnowledgeEvalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeEvalServiceImplTest {

    @Mock
    private HybridSearchService hybridSearchService;

    @Mock
    private KnowledgeEvalCaseRepository evalCaseRepository;

    @Mock
    private KnowledgeEvalResultRepository evalResultRepository;

    private KnowledgeEvalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeEvalServiceImpl(hybridSearchService, evalCaseRepository, evalResultRepository);
    }

    @Test
    @DisplayName("runEval 应按分类、检索模式和失败原因输出增强报告")
    void runEvalShouldBuildEnhancedReport() {
        KnowledgeEvalCase menuCase = buildCase(1L, "宫保鸡丁多少钱", "宫保鸡丁|价格", "宫保鸡丁", "menu", "easy");
        KnowledgeEvalCase faqCase = buildCase(2L, "营业时间是几点到几点", "营业时间|开门|打烊", null, "faq", "easy");
        KnowledgeEvalCase deliveryCase = buildCase(3L, "配送范围有哪些", "配送范围|区域|地址", null, "delivery", "medium");
        KnowledgeEvalCase orderCase = buildCase(4L, "怎么取消订单", "取消订单|取消|订单", null, "order_rule", "easy");

        when(evalCaseRepository.selectAll()).thenReturn(List.of(menuCase, faqCase, deliveryCase, orderCase));
        when(hybridSearchService.hybridSearch(eq(menuCase.getQuestion()), eq(5)))
                .thenReturn(buildSearchResult("exact_menu", 12,
                        chunk("菜品名称：宫保鸡丁，价格 28 元", "宫保鸡丁", 1.0, 1.0, null)));
        when(hybridSearchService.hybridSearch(eq(faqCase.getQuestion()), eq(5)))
                .thenReturn(buildSearchResult("hybrid", 18,
                        chunk("门店营业时间为 10:00-22:00，开门后持续营业至打烊", "营业时间说明", 0.9, 0.8, null),
                        chunk("支持微信支付", "支付方式", 0.5, 0.3, null)));
        when(hybridSearchService.hybridSearch(eq(deliveryCase.getQuestion()), eq(5)))
                .thenReturn(buildSearchResult("hybrid", 20,
                        chunk("配送时间一般 30 分钟左右", "配送时间说明", 0.7, 0.9, null),
                        chunk("超出服务时建议提前电话确认", "配送补充", 0.6, 0.6, null),
                        chunk("起送价以门店展示为准", "起送价规则", 0.55, 0.5, null),
                        chunk("当前配送范围覆盖 3 公里内区域", "配送范围规则", 0.5, 0.8, null)));
        when(hybridSearchService.hybridSearch(eq(orderCase.getQuestion()), eq(5)))
                .thenReturn(buildSearchResult("hybrid", 22,
                        chunk("联系客服可以咨询发票问题", "发票说明", 0.6, 0.5, null),
                        chunk("已出餐订单退款规则说明", "退款规则", 0.5, 0.4, null)));

        EvalReport report = service.runEval(null);
        assertEquals(4, report.getTotalCases());
        assertEquals(2, report.getTop1Hits());
        assertEquals(2, report.getTop3Hits());
        assertEquals(3, report.getTop5Hits());
        assertEquals(0, report.getNoResults());
        assertEquals(2, report.getWrongRecalls());
        assertEquals(1, report.getRankingWeakCases());
        assertEquals(0.5, report.getTop1HitRate());
        assertEquals(0.5, report.getTop3HitRate());
        assertEquals(0.75, report.getTop5HitRate());

        assertEquals(1, report.getCategoryStats().get("menu").getTop1Hits());
        assertEquals(1, report.getCategoryStats().get("faq").getTop1Hits());
        assertEquals(0, report.getCategoryStats().get("delivery").getTop3Hits());
        assertEquals(1, report.getCategoryStats().get("order_rule").getWrongRecalls());

        assertEquals(1, report.getSearchTypeStats().get("exact_menu").getTotal());
        assertEquals(3, report.getSearchTypeStats().get("hybrid").getTotal());
        assertEquals(1, report.getRetrievalModeStats().get("exact_menu").getTotal());
        assertEquals(3, report.getRetrievalModeStats().get("keyword_degraded").getTotal());
        assertEquals(1, report.getFailureReasonStats().get("hit_after_top3"));
        assertEquals(1, report.getFailureReasonStats().get("partial_keyword_match"));
        assertEquals(2, report.getFailureSamples().size());
        assertEquals("delivery", report.getFailureSamples().get(0).getCategory());
        assertEquals("hit_after_top3", report.getFailureSamples().get(0).getFailureReason());
        assertEquals("order_rule", report.getFailureSamples().get(1).getCategory());
        assertEquals("partial_keyword_match", report.getFailureSamples().get(1).getFailureReason());

        ArgumentCaptor<KnowledgeEvalResult> captor = ArgumentCaptor.forClass(KnowledgeEvalResult.class);
        verify(evalResultRepository, org.mockito.Mockito.times(4)).insert(captor.capture());
        assertEquals(4, captor.getAllValues().size());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(item -> "exact_menu".equals(item.getSearchType()) && Boolean.TRUE.equals(item.getTop1Hit())));
    }

    @Test
    @DisplayName("getLatestResults 应补充 category、difficulty 和 resultStatus")
    void getLatestResultsShouldExposeEnhancedFields() {
        KnowledgeEvalCase evalCase = buildCase(10L, "可以开发票吗", "发票|开票", null, "faq", "easy");
        KnowledgeEvalResult result = new KnowledgeEvalResult();
        result.setId(100L);
        result.setEvalCaseId(10L);
        result.setQuestion("可以开发票吗");
        result.setSearchType("hybrid");
        result.setTop1Hit(false);
        result.setTop3Hit(false);
        result.setTopResultSource("支付规则");
        result.setTopResult("支持微信支付");
        result.setTopScore(0.42);
        result.setCostMs(15L);
        result.setResultCount(2);
        result.setEvaluatedAt(LocalDateTime.of(2026, 4, 24, 16, 0));

        when(evalResultRepository.selectLatest(5)).thenReturn(List.of(result));
        when(evalCaseRepository.selectAll()).thenReturn(List.of(evalCase));

        List<Map<String, Object>> output = service.getLatestResults(5);

        assertEquals(1, output.size());
        Map<String, Object> item = output.get(0);
        assertEquals("faq", item.get("category"));
        assertEquals("easy", item.get("difficulty"));
        assertEquals("top3_miss", item.get("resultStatus"));
        assertEquals("支付规则", item.get("topResultSource"));
        assertEquals(2, item.get("resultCount"));
    }

    @Test
    @DisplayName("initDefaultEvalCases 应跳过重复题目，只插入新增默认集")
    void initDefaultEvalCasesShouldSkipExistingCases() {
        KnowledgeEvalCase existing = buildCase(20L, "有什么推荐菜", "推荐|招牌|特色|热销", null, "menu", "easy");
        when(evalCaseRepository.selectAll()).thenReturn(List.of(existing));

        int inserted = service.initDefaultEvalCases();

        assertEquals(30, inserted);

        ArgumentCaptor<KnowledgeEvalCase> captor = ArgumentCaptor.forClass(KnowledgeEvalCase.class);
        verify(evalCaseRepository, org.mockito.Mockito.times(30)).insert(captor.capture());
        List<KnowledgeEvalCase> insertedCases = captor.getAllValues();
        assertTrue(insertedCases.stream().allMatch(item -> Boolean.TRUE.equals(item.getEnabled())));
        assertTrue(insertedCases.stream().allMatch(item -> item.getCreatedAt() != null));
        assertFalse(insertedCases.stream().anyMatch(item -> "有什么推荐菜".equals(item.getQuestion())));
    }

    @Test
    @DisplayName("runEval 没有用例时应返回空报告且不写结果")
    void runEvalShouldReturnEmptyReportWhenNoCases() {
        when(evalCaseRepository.selectAll()).thenReturn(List.of());

        EvalReport report = service.runEval(null);

        assertEquals(0, report.getTotalCases());
        assertNotNull(report.getCategoryStats());
        assertTrue(report.getFailureSamples().isEmpty());
        verify(evalResultRepository, never()).insert(any());
    }

    private KnowledgeEvalCase buildCase(Long id, String question, String expectedAnswer,
                                        String expectedSource, String category, String difficulty) {
        KnowledgeEvalCase evalCase = new KnowledgeEvalCase();
        evalCase.setId(id);
        evalCase.setQuestion(question);
        evalCase.setExpectedAnswer(expectedAnswer);
        evalCase.setExpectedSource(expectedSource);
        evalCase.setCategory(category);
        evalCase.setDifficulty(difficulty);
        evalCase.setEnabled(true);
        return evalCase;
    }

    private KnowledgeSearchResult buildSearchResult(String searchType, long costMs, ChunkResult... chunks) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setQuery("test");
        result.setSearchType(searchType);
        result.setResults(List.of(chunks));
        result.setCount(chunks.length);
        result.setCostMs(costMs);
        return result;
    }

    private ChunkResult chunk(String content, String source, double score, Double keywordScore, Double vectorScore) {
        ChunkResult result = new ChunkResult();
        result.setContent(content);
        result.setSource(source);
        result.setScore(score);
        result.setKeywordScore(keywordScore);
        result.setVectorScore(vectorScore);
        return result;
    }
}
