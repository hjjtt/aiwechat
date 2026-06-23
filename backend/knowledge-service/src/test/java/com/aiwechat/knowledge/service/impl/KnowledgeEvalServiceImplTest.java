package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.EvalReport;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.entity.KnowledgeEvalCase;
import com.aiwechat.knowledge.model.entity.KnowledgeEvalResult;
import com.aiwechat.knowledge.repository.KnowledgeEvalCaseRepository;
import com.aiwechat.knowledge.repository.KnowledgeEvalResultRepository;
import com.aiwechat.knowledge.service.HybridSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
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
    @DisplayName("初始化默认评测用例应补足扩充后的基线并跳过重复项")
    void initDefaultEvalCasesShouldInsertExpandedCasesAndSkipDuplicates() {
        when(evalCaseRepository.selectAll()).thenReturn(List.of(
                evalCase(1L, "有什么推荐菜", "menu", "easy", "推荐|招牌|特色|热销", null)
        ));

        int inserted = service.initDefaultEvalCases();

        assertEquals(30, inserted);

        ArgumentCaptor<KnowledgeEvalCase> captor = ArgumentCaptor.forClass(KnowledgeEvalCase.class);
        verify(evalCaseRepository, times(30)).insert(captor.capture());

        List<KnowledgeEvalCase> insertedCases = captor.getAllValues();
        assertTrue(insertedCases.stream().anyMatch(item -> "faq".equals(item.getCategory())));
        assertTrue(insertedCases.stream().anyMatch(item -> "delivery".equals(item.getCategory())));
        assertTrue(insertedCases.stream().anyMatch(item -> "order_rule".equals(item.getCategory())));
        assertTrue(insertedCases.stream().allMatch(item -> Boolean.TRUE.equals(item.getEnabled())));
        assertTrue(insertedCases.stream().allMatch(item -> item.getCreatedAt() != null));
    }

    @Test
    @DisplayName("评测报告应输出关键统计、分类统计和失败样本")
    void runEvalShouldBuildKeyStatistics() {
        List<KnowledgeEvalCase> cases = List.of(
                evalCase(1L, "宫保鸡丁多少钱", "menu", "easy", "宫保鸡丁|价格", "宫保鸡丁"),
                evalCase(2L, "可以开发票吗", "faq", "easy", "发票|开票", null),
                evalCase(3L, "下雨天配送会延迟吗", "delivery", "medium", "下雨|延迟|配送时间", null),
                evalCase(4L, "怎么取消订单", "order_rule", "easy", "取消订单|取消|订单", null)
        );
        when(evalCaseRepository.selectAll()).thenReturn(cases);
        when(hybridSearchService.hybridSearch(anyString(), anyInt())).thenReturn(
                top1HitResult(),
                noResult(),
                hitAfterTop3Result(),
                keywordMissResult()
        );

        EvalReport report = service.runEval(null);

        assertEquals(4, report.getTotalCases());
        assertEquals(1, report.getTop1Hits());
        assertEquals(1, report.getTop3Hits());
        assertEquals(2, report.getTop5Hits());
        assertEquals(1, report.getNoResults());
        assertEquals(2, report.getWrongRecalls());
        assertEquals(1, report.getRankingWeakCases());
        assertEquals(0.25, report.getTop1HitRate(), 0.0001);
        assertEquals(0.25, report.getTop3HitRate(), 0.0001);
        assertEquals(0.5, report.getTop5HitRate(), 0.0001);
        assertEquals(0.25, report.getNoAnswerRate(), 0.0001);
        assertEquals(0.5, report.getWrongRecallRate(), 0.0001);
        assertEquals(0.25, report.getRankingWeakRate(), 0.0001);
        assertEquals(0.75, report.getResultCoverageRate(), 0.0001);
        assertEquals(45.0, report.getAvgCostMs(), 0.0001);

        assertEquals(1, report.getCategoryStats().get("menu").getTop1Hits());
        assertEquals(1, report.getCategoryStats().get("faq").getNoResults());
        assertEquals(1, report.getCategoryStats().get("delivery").getTop5Hits());
        assertEquals(1, report.getCategoryStats().get("order_rule").getWrongRecalls());

        assertEquals(1, report.getSearchTypeStats().get("exact_menu").getTotal());
        assertEquals(2, report.getSearchTypeStats().get("keyword").getTotal());
        assertEquals(1, report.getSearchTypeStats().get("hybrid").getTotal());

        assertEquals(1, report.getRetrievalModeStats().get("exact_menu").getTotal());
        assertEquals(1, report.getRetrievalModeStats().get("no_result").getTotal());
        assertEquals(1, report.getRetrievalModeStats().get("hybrid_mixed").getTotal());
        assertEquals(1, report.getRetrievalModeStats().get("keyword_only").getTotal());

        assertEquals(1, report.getFailureReasonStats().get("no_result"));
        assertEquals(1, report.getFailureReasonStats().get("hit_after_top3"));
        assertEquals(1, report.getFailureReasonStats().get("keyword_miss"));

        assertEquals(3, report.getFailureSamples().size());
        assertEquals("可以开发票吗", report.getFailureSamples().get(0).getQuestion());
        assertEquals("no_result", report.getFailureSamples().get(0).getFailureReason());

        verify(evalResultRepository, times(4)).insert(org.mockito.ArgumentMatchers.any(KnowledgeEvalResult.class));
    }

    @Test
    @DisplayName("最近评测结果应带出分类、难度和结果状态")
    void getLatestResultsShouldExposeEnhancedFields() {
        when(evalCaseRepository.selectAll()).thenReturn(List.of(
                evalCase(11L, "营业时间是几点到几点", "faq", "easy", "营业时间|开门|打烊", null)
        ));
        when(evalResultRepository.selectLatest(5)).thenReturn(List.of(
                evalResult(101L, 11L, "营业时间是几点到几点", "keyword", false, true, "常见问题-营业时间", "09:00 到 21:30", 0.88, 2, 36L),
                evalResult(102L, 999L, "未知问题", "hybrid", false, false, "未知来源", "无命中", 0.12, 0, 48L)
        ));

        List<Map<String, Object>> results = service.getLatestResults(5);

        assertEquals(2, results.size());

        Map<String, Object> first = results.get(0);
        assertEquals("faq", first.get("category"));
        assertEquals("easy", first.get("difficulty"));
        assertEquals("top3_hit", first.get("resultStatus"));
        assertEquals("常见问题-营业时间", first.get("topResultSource"));

        Map<String, Object> second = results.get(1);
        assertEquals(null, second.get("category"));
        assertEquals(null, second.get("difficulty"));
        assertEquals("no_result", second.get("resultStatus"));
        assertNotNull(second.get("evaluatedAt"));
    }

    private KnowledgeEvalCase evalCase(Long id, String question, String category, String difficulty,
                                       String expectedAnswer, String expectedSource) {
        KnowledgeEvalCase evalCase = new KnowledgeEvalCase();
        evalCase.setId(id);
        evalCase.setQuestion(question);
        evalCase.setCategory(category);
        evalCase.setDifficulty(difficulty);
        evalCase.setExpectedAnswer(expectedAnswer);
        evalCase.setExpectedSource(expectedSource);
        return evalCase;
    }

    private KnowledgeSearchResult top1HitResult() {
        return searchResult("exact_menu", 30,
                chunk("宫保鸡丁的价格是 28 元", "宫保鸡丁", 0.95, null, null));
    }

    private KnowledgeSearchResult noResult() {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setSearchType("keyword");
        result.setCount(0);
        result.setCostMs(40);
        result.setResults(List.of());
        return result;
    }

    private KnowledgeSearchResult hitAfterTop3Result() {
        return searchResult("hybrid", 50,
                chunk("系统公告", "首页公告", 0.70, 0.70, 0.40),
                chunk("订单进度说明", "订单状态说明", 0.65, 0.65, 0.42),
                chunk("配送费说明", "配送规则-配送费与免配送费", 0.63, 0.63, 0.50),
                chunk("正常情况下接单后 30 到 45 分钟送达，下雨天可能延迟", "配送规则-配送时效与异常处理", 0.61, 0.61, 0.55)
        );
    }

    private KnowledgeSearchResult keywordMissResult() {
        return searchResult("keyword", 60,
                chunk("本店支持电子发票和营业时间查询", "常见问题-发票", 0.55, null, 0.55));
    }

    private KnowledgeSearchResult searchResult(String searchType, long costMs, ChunkResult... chunks) {
        KnowledgeSearchResult result = new KnowledgeSearchResult();
        result.setSearchType(searchType);
        result.setCostMs(costMs);
        result.setResults(List.of(chunks));
        result.setCount(chunks.length);
        return result;
    }

    private ChunkResult chunk(String content, String source, double score, Double vectorScore, Double keywordScore) {
        ChunkResult chunk = new ChunkResult();
        chunk.setContent(content);
        chunk.setSource(source);
        chunk.setScore(score);
        chunk.setVectorScore(vectorScore);
        chunk.setKeywordScore(keywordScore);
        return chunk;
    }

    private KnowledgeEvalResult evalResult(Long id, Long evalCaseId, String question, String searchType,
                                           boolean top1Hit, boolean top3Hit, String topResultSource,
                                           String topResult, double topScore, int resultCount, long costMs) {
        KnowledgeEvalResult result = new KnowledgeEvalResult();
        result.setId(id);
        result.setEvalCaseId(evalCaseId);
        result.setQuestion(question);
        result.setSearchType(searchType);
        result.setTop1Hit(top1Hit);
        result.setTop3Hit(top3Hit);
        result.setTopResultSource(topResultSource);
        result.setTopResult(topResult);
        result.setTopScore(topScore);
        result.setResultCount(resultCount);
        result.setCostMs(costMs);
        result.setEvaluatedAt(java.time.LocalDateTime.now());
        return result;
    }
}
