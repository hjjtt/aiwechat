package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.EvalReport;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult.ChunkResult;
import com.aiwechat.knowledge.model.entity.KnowledgeEvalCase;
import com.aiwechat.knowledge.model.entity.KnowledgeEvalResult;
import com.aiwechat.knowledge.repository.KnowledgeEvalCaseRepository;
import com.aiwechat.knowledge.repository.KnowledgeEvalResultRepository;
import com.aiwechat.knowledge.service.HybridSearchService;
import com.aiwechat.knowledge.service.KnowledgeEvalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeEvalServiceImpl implements KnowledgeEvalService {

    private static final int REPORT_TOP_K = 5;
    private static final int FAILURE_SAMPLE_LIMIT = 12;
    private static final List<String> DEFAULT_CATEGORY_ORDER = List.of("menu", "faq", "delivery", "order_rule");

    private static final String FAILURE_NONE = "none";
    private static final String FAILURE_NO_RESULT = "no_result";
    private static final String FAILURE_HIT_AFTER_TOP3 = "hit_after_top3";
    private static final String FAILURE_SOURCE_MISS = "source_miss";
    private static final String FAILURE_PARTIAL_KEYWORD_MATCH = "partial_keyword_match";
    private static final String FAILURE_KEYWORD_MISS = "keyword_miss";

    private final HybridSearchService hybridSearchService;
    private final KnowledgeEvalCaseRepository evalCaseRepository;
    private final KnowledgeEvalResultRepository evalResultRepository;

    @Override
    public EvalReport runEval(String category) {
        List<KnowledgeEvalCase> cases = category != null
                ? evalCaseRepository.selectByCategory(category)
                : evalCaseRepository.selectAll();

        if (cases.isEmpty()) {
            log.warn("没有可用的评测用例");
            return createEmptyReport();
        }

        log.info("开始评测 - 用例数: {}, 分类: {}", cases.size(), category != null ? category : "全部");

        int top1Hits = 0;
        int top3Hits = 0;
        int top5Hits = 0;
        int noResults = 0;
        int wrongRecalls = 0;
        int rankingWeakCases = 0;
        long totalCostMs = 0;

        Map<String, EvalReport.CategoryStats> categoryStatsMap = createOrderedStatsMap(DEFAULT_CATEGORY_ORDER);
        Map<String, EvalReport.SearchStats> searchTypeStatsMap = new LinkedHashMap<>();
        Map<String, EvalReport.SearchStats> retrievalModeStatsMap = new LinkedHashMap<>();
        Map<String, Integer> failureReasonStats = new LinkedHashMap<>();
        List<EvalReport.FailureSample> failureSamples = new ArrayList<>();

        for (KnowledgeEvalCase evalCase : cases) {
            KnowledgeSearchResult searchResult = hybridSearchService.hybridSearch(evalCase.getQuestion(), REPORT_TOP_K);
            EvalOutcome outcome = evaluateCase(evalCase, searchResult);

            if (outcome.top1Hit) {
                top1Hits++;
            }
            if (outcome.top3Hit) {
                top3Hits++;
            }
            if (outcome.top5Hit) {
                top5Hits++;
            }
            if (outcome.noResult) {
                noResults++;
            }
            if (!outcome.noResult && !outcome.top3Hit) {
                wrongRecalls++;
            }
            if (outcome.top5Hit && !outcome.top3Hit) {
                rankingWeakCases++;
            }
            totalCostMs += safeCost(searchResult);

            saveEvalResult(evalCase, searchResult, outcome.top1Hit, outcome.top3Hit);

            String caseCategory = normalizeCategory(evalCase.getCategory());
            updateCategoryStats(categoryStatsMap, caseCategory, outcome);
            updateSearchStats(searchTypeStatsMap, outcome.searchType, outcome);
            updateSearchStats(retrievalModeStatsMap, outcome.retrievalMode, outcome);

            if (!FAILURE_NONE.equals(outcome.failureReason)) {
                failureReasonStats.merge(outcome.failureReason, 1, Integer::sum);
            }
            if (!outcome.top3Hit && failureSamples.size() < FAILURE_SAMPLE_LIMIT) {
                failureSamples.add(buildFailureSample(evalCase, outcome));
            }
        }

        finalizeCategoryStats(categoryStatsMap);
        finalizeSearchStats(searchTypeStatsMap);
        finalizeSearchStats(retrievalModeStatsMap);

        EvalReport report = new EvalReport();
        report.setTotalCases(cases.size());
        report.setTop1Hits(top1Hits);
        report.setTop3Hits(top3Hits);
        report.setTop5Hits(top5Hits);
        report.setNoResults(noResults);
        report.setWrongRecalls(wrongRecalls);
        report.setRankingWeakCases(rankingWeakCases);
        report.setTop1HitRate(rate(top1Hits, cases.size()));
        report.setTop3HitRate(rate(top3Hits, cases.size()));
        report.setTop5HitRate(rate(top5Hits, cases.size()));
        report.setNoAnswerRate(rate(noResults, cases.size()));
        report.setWrongRecallRate(rate(wrongRecalls, cases.size()));
        report.setRankingWeakRate(rate(rankingWeakCases, cases.size()));
        report.setResultCoverageRate(rate(cases.size() - noResults, cases.size()));
        report.setAvgCostMs(cases.isEmpty() ? 0 : (double) totalCostMs / cases.size());
        report.setCategoryStats(categoryStatsMap);
        report.setSearchTypeStats(searchTypeStatsMap);
        report.setRetrievalModeStats(retrievalModeStatsMap);
        report.setFailureReasonStats(failureReasonStats);
        report.setFailureSamples(failureSamples);

        log.info(
                "评测完成 - Top1: {}/{}, Top3: {}/{}, Top5: {}/{}, 无结果: {}, 排序偏后: {}, 平均耗时: {}ms",
                top1Hits,
                cases.size(),
                top3Hits,
                cases.size(),
                top5Hits,
                cases.size(),
                noResults,
                rankingWeakCases,
                report.getAvgCostMs()
        );
        return report;
    }

    @Override
    public List<Map<String, Object>> getLatestResults(int limit) {
        List<KnowledgeEvalResult> results = evalResultRepository.selectLatest(limit);
        Map<Long, KnowledgeEvalCase> evalCaseMap = evalCaseRepository.selectAll().stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(KnowledgeEvalCase::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Map<String, Object>> output = new ArrayList<>();
        for (KnowledgeEvalResult result : results) {
            KnowledgeEvalCase evalCase = evalCaseMap.get(result.getEvalCaseId());

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", result.getId());
            map.put("question", result.getQuestion());
            map.put("category", evalCase != null ? evalCase.getCategory() : null);
            map.put("difficulty", evalCase != null ? evalCase.getDifficulty() : null);
            map.put("searchType", result.getSearchType());
            map.put("top1Hit", result.getTop1Hit());
            map.put("top3Hit", result.getTop3Hit());
            map.put("resultStatus", buildResultStatus(result));
            map.put("topResultSource", result.getTopResultSource());
            map.put("topResult", result.getTopResult());
            map.put("topScore", result.getTopScore());
            map.put("costMs", result.getCostMs());
            map.put("resultCount", result.getResultCount());
            map.put("evaluatedAt", result.getEvaluatedAt());
            output.add(map);
        }
        return output;
    }

    @Override
    public int initDefaultEvalCases() {
        List<KnowledgeEvalCase> defaultCases = buildDefaultCases();
        Set<String> existingKeys = evalCaseRepository.selectAll().stream()
                .map(this::buildCaseKey)
                .collect(Collectors.toSet());

        int inserted = 0;
        int skipped = 0;
        LocalDateTime now = LocalDateTime.now();

        for (KnowledgeEvalCase evalCase : defaultCases) {
            String key = buildCaseKey(evalCase);
            if (existingKeys.contains(key)) {
                skipped++;
                continue;
            }

            evalCase.setEnabled(true);
            evalCase.setCreatedAt(now);
            evalCaseRepository.insert(evalCase);
            existingKeys.add(key);
            inserted++;
        }

        log.info("初始化默认评测用例完成 - 新增: {}, 跳过重复: {}, 总模板数: {}", inserted, skipped, defaultCases.size());
        return inserted;
    }

    private EvalReport createEmptyReport() {
        EvalReport report = new EvalReport();
        report.setCategoryStats(new LinkedHashMap<>());
        report.setSearchTypeStats(new LinkedHashMap<>());
        report.setRetrievalModeStats(new LinkedHashMap<>());
        report.setFailureReasonStats(new LinkedHashMap<>());
        report.setFailureSamples(List.of());
        return report;
    }

    private EvalOutcome evaluateCase(KnowledgeEvalCase evalCase, KnowledgeSearchResult searchResult) {
        EvalOutcome outcome = new EvalOutcome();
        outcome.searchType = normalizeSearchType(searchResult != null ? searchResult.getSearchType() : null);
        outcome.retrievalMode = determineRetrievalMode(searchResult);
        outcome.costMs = safeCost(searchResult);

        List<ChunkResult> results = searchResult != null && searchResult.getResults() != null
                ? searchResult.getResults()
                : List.of();

        if (results.isEmpty()) {
            outcome.noResult = true;
            outcome.failureReason = FAILURE_NO_RESULT;
            return outcome;
        }

        ChunkResult topResult = results.get(0);
        outcome.topResultSource = topResult.getSource();
        outcome.topResultExcerpt = trimText(topResult.getContent(), 160);

        int hitRank = 0;
        for (int i = 0; i < Math.min(REPORT_TOP_K, results.size()); i++) {
            if (isHit(evalCase, results.get(i))) {
                hitRank = i + 1;
                break;
            }
        }

        outcome.top1Hit = hitRank == 1;
        outcome.top3Hit = hitRank > 0 && hitRank <= 3;
        outcome.top5Hit = hitRank > 0;

        if (hitRank == 0) {
            outcome.failureReason = determineFailureReason(evalCase, results);
        } else if (hitRank > 3) {
            outcome.failureReason = FAILURE_HIT_AFTER_TOP3;
        } else {
            outcome.failureReason = FAILURE_NONE;
        }

        return outcome;
    }

    private boolean isHit(KnowledgeEvalCase evalCase, ChunkResult result) {
        return matchesExpectedSource(evalCase, result.getSource(), result.getContent())
                || matchesExpectedAnswer(evalCase, result.getSource(), result.getContent());
    }

    private boolean matchesExpectedSource(KnowledgeEvalCase evalCase, String source, String content) {
        if (isBlank(evalCase.getExpectedSource())) {
            return false;
        }

        String expectedSource = normalizeText(evalCase.getExpectedSource());
        String normalizedSource = normalizeText(source);
        String normalizedContent = normalizeText(content);
        return normalizedSource.contains(expectedSource) || normalizedContent.contains(expectedSource);
    }

    private boolean matchesExpectedAnswer(KnowledgeEvalCase evalCase, String source, String content) {
        if (isBlank(evalCase.getExpectedAnswer())) {
            return false;
        }

        String combined = normalizeText(joinNonBlank(source, content));
        String normalizedExpected = normalizeText(evalCase.getExpectedAnswer());
        if (!normalizedExpected.isEmpty() && combined.contains(normalizedExpected)) {
            return true;
        }

        MatchProfile profile = buildMatchProfile(evalCase.getExpectedAnswer());
        if (profile.tokens.isEmpty()) {
            return false;
        }

        int matches = countMatches(profile.tokens, combined);
        return matches >= profile.requiredMatches;
    }

    private String determineFailureReason(KnowledgeEvalCase evalCase, List<ChunkResult> results) {
        boolean sourceMatched = false;
        double bestCoverage = 0;

        MatchProfile profile = buildMatchProfile(evalCase.getExpectedAnswer());
        for (ChunkResult result : results) {
            String combined = normalizeText(joinNonBlank(result.getSource(), result.getContent()));
            if (matchesExpectedSource(evalCase, result.getSource(), result.getContent())) {
                sourceMatched = true;
            }
            if (!profile.tokens.isEmpty()) {
                bestCoverage = Math.max(bestCoverage, matchCoverage(profile.tokens, combined));
            }
        }

        if (!isBlank(evalCase.getExpectedSource()) && !sourceMatched) {
            return FAILURE_SOURCE_MISS;
        }
        if (bestCoverage > 0) {
            return FAILURE_PARTIAL_KEYWORD_MATCH;
        }
        return FAILURE_KEYWORD_MISS;
    }

    private void saveEvalResult(KnowledgeEvalCase evalCase, KnowledgeSearchResult searchResult, boolean top1Hit, boolean top3Hit) {
        KnowledgeEvalResult result = new KnowledgeEvalResult();
        result.setEvalCaseId(evalCase.getId());
        result.setQuestion(evalCase.getQuestion());
        result.setSearchType(searchResult != null ? searchResult.getSearchType() : "unknown");
        result.setTop1Hit(top1Hit);
        result.setTop3Hit(top3Hit);
        result.setCostMs(safeCost(searchResult));
        result.setResultCount(searchResult != null ? searchResult.getCount() : 0);
        result.setEvaluatedAt(LocalDateTime.now());

        if (searchResult != null && searchResult.getResults() != null && !searchResult.getResults().isEmpty()) {
            ChunkResult topResult = searchResult.getResults().get(0);
            result.setTopResult(trimText(topResult.getContent(), 200));
            result.setTopResultSource(topResult.getSource());
            result.setTopScore(topResult.getScore());
        }

        evalResultRepository.insert(result);
    }

    private void updateCategoryStats(Map<String, EvalReport.CategoryStats> categoryStatsMap, String category, EvalOutcome outcome) {
        EvalReport.CategoryStats stats = categoryStatsMap.computeIfAbsent(category, key -> new EvalReport.CategoryStats());
        stats.setTotal(stats.getTotal() + 1);
        if (outcome.top1Hit) {
            stats.setTop1Hits(stats.getTop1Hits() + 1);
        }
        if (outcome.top3Hit) {
            stats.setTop3Hits(stats.getTop3Hits() + 1);
        }
        if (outcome.top5Hit) {
            stats.setTop5Hits(stats.getTop5Hits() + 1);
        }
        if (outcome.noResult) {
            stats.setNoResults(stats.getNoResults() + 1);
        }
        if (!outcome.noResult && !outcome.top3Hit) {
            stats.setWrongRecalls(stats.getWrongRecalls() + 1);
        }
        stats.setAvgCostMs(stats.getAvgCostMs() + outcome.costMs);
    }

    private void updateSearchStats(Map<String, EvalReport.SearchStats> statsMap, String name, EvalOutcome outcome) {
        EvalReport.SearchStats stats = statsMap.computeIfAbsent(name, key -> new EvalReport.SearchStats());
        stats.setTotal(stats.getTotal() + 1);
        if (outcome.top1Hit) {
            stats.setTop1Hits(stats.getTop1Hits() + 1);
        }
        if (outcome.top3Hit) {
            stats.setTop3Hits(stats.getTop3Hits() + 1);
        }
        if (outcome.top5Hit) {
            stats.setTop5Hits(stats.getTop5Hits() + 1);
        }
        stats.setAvgCostMs(stats.getAvgCostMs() + outcome.costMs);
    }

    private void finalizeCategoryStats(Map<String, EvalReport.CategoryStats> categoryStatsMap) {
        for (EvalReport.CategoryStats stats : categoryStatsMap.values()) {
            stats.setTop1Rate(rate(stats.getTop1Hits(), stats.getTotal()));
            stats.setTop3Rate(rate(stats.getTop3Hits(), stats.getTotal()));
            stats.setTop5Rate(rate(stats.getTop5Hits(), stats.getTotal()));
            stats.setNoResultRate(rate(stats.getNoResults(), stats.getTotal()));
            stats.setAvgCostMs(stats.getTotal() == 0 ? 0 : stats.getAvgCostMs() / stats.getTotal());
        }
    }

    private void finalizeSearchStats(Map<String, EvalReport.SearchStats> statsMap) {
        for (EvalReport.SearchStats stats : statsMap.values()) {
            stats.setTop1Rate(rate(stats.getTop1Hits(), stats.getTotal()));
            stats.setTop3Rate(rate(stats.getTop3Hits(), stats.getTotal()));
            stats.setTop5Rate(rate(stats.getTop5Hits(), stats.getTotal()));
            stats.setAvgCostMs(stats.getTotal() == 0 ? 0 : stats.getAvgCostMs() / stats.getTotal());
        }
    }

    private Map<String, EvalReport.CategoryStats> createOrderedStatsMap(List<String> categories) {
        Map<String, EvalReport.CategoryStats> statsMap = new LinkedHashMap<>();
        for (String category : categories) {
            statsMap.put(category, new EvalReport.CategoryStats());
        }
        return statsMap;
    }

    private EvalReport.FailureSample buildFailureSample(KnowledgeEvalCase evalCase, EvalOutcome outcome) {
        EvalReport.FailureSample sample = new EvalReport.FailureSample();
        sample.setQuestion(evalCase.getQuestion());
        sample.setCategory(normalizeCategory(evalCase.getCategory()));
        sample.setDifficulty(evalCase.getDifficulty());
        sample.setSearchType(outcome.searchType);
        sample.setRetrievalMode(outcome.retrievalMode);
        sample.setFailureReason(outcome.failureReason);
        sample.setExpectedSource(evalCase.getExpectedSource());
        sample.setExpectedAnswer(evalCase.getExpectedAnswer());
        sample.setTopResultSource(outcome.topResultSource);
        sample.setTopResultExcerpt(outcome.topResultExcerpt);
        return sample;
    }

    private String determineRetrievalMode(KnowledgeSearchResult searchResult) {
        if (searchResult == null || searchResult.getResults() == null || searchResult.getResults().isEmpty()) {
            return "no_result";
        }
        if ("exact_menu".equalsIgnoreCase(searchResult.getSearchType())) {
            return "exact_menu";
        }

        boolean hasVectorScore = searchResult.getResults().stream().anyMatch(item -> item.getVectorScore() != null);
        boolean hasKeywordScore = searchResult.getResults().stream().anyMatch(item -> item.getKeywordScore() != null);

        if ("keyword".equalsIgnoreCase(searchResult.getSearchType())) {
            return "keyword_only";
        }
        if ("vector".equalsIgnoreCase(searchResult.getSearchType())) {
            return "vector_only";
        }
        if (hasKeywordScore && hasVectorScore) {
            return "hybrid_mixed";
        }
        if (hasKeywordScore) {
            return "keyword_degraded";
        }
        if (hasVectorScore) {
            return "vector_only";
        }
        return normalizeSearchType(searchResult.getSearchType());
    }

    private String normalizeSearchType(String searchType) {
        return isBlank(searchType) ? "unknown" : searchType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCategory(String category) {
        return isBlank(category) ? "other" : category.trim().toLowerCase(Locale.ROOT);
    }

    private String buildResultStatus(KnowledgeEvalResult result) {
        if (Boolean.TRUE.equals(result.getTop1Hit())) {
            return "top1_hit";
        }
        if (Boolean.TRUE.equals(result.getTop3Hit())) {
            return "top3_hit";
        }
        if (result.getResultCount() == null || result.getResultCount() == 0) {
            return FAILURE_NO_RESULT;
        }
        return "top3_miss";
    }

    private MatchProfile buildMatchProfile(String expectedAnswer) {
        if (isBlank(expectedAnswer)) {
            return new MatchProfile(List.of(), 0);
        }

        boolean explicit = expectedAnswer.matches(".*[|,，、；;\\n].*");
        Set<String> tokens = new LinkedHashSet<>();

        if (explicit) {
            Arrays.stream(expectedAnswer.split("[|,，、；;\\n]+"))
                    .map(this::normalizeText)
                    .filter(token -> token.length() >= 2)
                    .forEach(tokens::add);
        } else {
            String normalized = normalizeText(expectedAnswer);
            if (!normalized.isEmpty()) {
                tokens.add(normalized);
                if (normalized.length() > 4) {
                    for (int i = 0; i < normalized.length() - 1; i++) {
                        String bigram = normalized.substring(i, i + 2);
                        if (bigram.length() == 2) {
                            tokens.add(bigram);
                        }
                    }
                }
            }
        }

        List<String> tokenList = new ArrayList<>(tokens);
        int requiredMatches;
        if (tokenList.isEmpty()) {
            requiredMatches = 0;
        } else if (explicit) {
            requiredMatches = Math.max(1, (int) Math.ceil(tokenList.size() / 2.0));
        } else if (tokenList.size() == 1) {
            requiredMatches = 1;
        } else {
            requiredMatches = Math.min(2, tokenList.size());
        }
        return new MatchProfile(tokenList, requiredMatches);
    }

    private int countMatches(List<String> tokens, String actualContent) {
        int matches = 0;
        for (String token : tokens) {
            if (actualContent.contains(token)) {
                matches++;
            }
        }
        return matches;
    }

    private double matchCoverage(List<String> tokens, String actualContent) {
        if (tokens.isEmpty()) {
            return 0;
        }
        return (double) countMatches(tokens, actualContent) / tokens.size();
    }

    private String buildCaseKey(KnowledgeEvalCase evalCase) {
        return normalizeCategory(evalCase.getCategory()) + "::" + normalizeText(evalCase.getQuestion());
    }

    private long safeCost(KnowledgeSearchResult searchResult) {
        return searchResult != null ? searchResult.getCostMs() : 0;
    }

    private double rate(int numerator, int denominator) {
        return denominator == 0 ? 0 : (double) numerator / denominator;
    }

    private String trimText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String joinNonBlank(String... parts) {
        return Arrays.stream(parts)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。？！；：、“”‘’（）()【】《》<>]+", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<KnowledgeEvalCase> buildDefaultCases() {
        List<KnowledgeEvalCase> cases = new ArrayList<>();

        // 菜单
        addCase(cases, "有什么推荐菜", "推荐|招牌|特色|热销", null, "menu", "easy");
        addCase(cases, "宫保鸡丁多少钱", "宫保鸡丁|价格", "宫保鸡丁", "menu", "easy");
        addCase(cases, "鱼香肉丝多少钱一份", "鱼香肉丝|价格", "鱼香肉丝", "menu", "easy");
        addCase(cases, "有什么主食", "主食|米饭|面", null, "menu", "easy");
        addCase(cases, "有哪些汤类", "汤|例汤|汤类", null, "menu", "easy");
        addCase(cases, "有没有辣一点的菜", "辣|麻辣|香辣", null, "menu", "medium");
        addCase(cases, "最便宜的菜是什么", "便宜|最低价|价格", null, "menu", "medium");
        addCase(cases, "有没有适合一个人的套餐", "套餐|单人|组合", null, "menu", "medium");
        addCase(cases, "素菜有哪些", "素菜|蔬菜|清炒", null, "menu", "easy");
        addCase(cases, "招牌菜有哪些", "招牌|特色|热销", null, "menu", "easy");

        // FAQ
        addCase(cases, "营业时间是几点到几点", "营业时间|0900|2130", "常见问题-营业时间", "faq", "easy");
        addCase(cases, "可以开发票吗", "发票|开票|电子发票", "常见问题-发票", "faq", "easy");
        addCase(cases, "有没有优惠活动", "优惠|活动|满减|免配送费", "常见问题-优惠活动", "faq", "easy");
        addCase(cases, "能加辣吗", "加辣|少辣|口味", "常见问题-口味备注", "faq", "easy");
        addCase(cases, "可以备注不要香菜吗", "备注|口味|不要香菜", "常见问题-口味备注", "faq", "easy");
        addCase(cases, "支持到店自取吗", "自取|到店", "常见问题-自取", "faq", "easy");
        addCase(cases, "订单太久没到怎么办", "催单|客服|订单详情", "常见问题-催单", "faq", "medium");

        // 配送规则
        addCase(cases, "配送范围有哪些", "配送范围|5公里|地址", "配送规则-配送范围与起送价", "delivery", "easy");
        addCase(cases, "起送价是多少", "起送价|18元|25元", "配送规则-配送范围与起送价", "delivery", "easy");
        addCase(cases, "配送费怎么算", "配送费|3元|5元|免配送费", "配送规则-配送费与免配送费", "delivery", "easy");
        addCase(cases, "满多少可以免配送费", "59元|免配送费", "配送规则-配送费与免配送费", "delivery", "easy");
        addCase(cases, "多久能送到", "30到45分钟|60分钟", "配送规则-配送时效与异常处理", "delivery", "easy");
        addCase(cases, "下雨天配送会延迟吗", "高峰期|暴雨|60分钟", "配送规则-配送时效与异常处理", "delivery", "medium");
        addCase(cases, "超出配送范围怎么办", "暂不支持配送|到店自取|更换地址", "配送规则-配送范围与起送价", "delivery", "medium");

        // 订单规则
        addCase(cases, "怎么取消订单", "接单前|自行取消|退款", "订单规则-取消与退款", "order_rule", "easy");
        addCase(cases, "下单后能不能退款", "退款|1到3个工作日|原路退款", "订单规则-取消与退款", "order_rule", "easy");
        addCase(cases, "订单多久不确认会自动取消", "15分钟|未支付|自动取消", "订单规则-下单支付与自动取消", "order_rule", "easy");
        addCase(cases, "已经出餐还能取消吗", "制作中|不支持无理由取消|人工客服", "订单规则-取消与退款", "order_rule", "medium");
        addCase(cases, "订单提交后还能修改地址吗", "暂不支持在线改单|联系门店|人工客服", "订单规则-售后与改单", "order_rule", "medium");
        addCase(cases, "漏餐了怎么办", "漏餐|补送|部分退款|整单退款", "订单规则-售后与改单", "order_rule", "medium");
        addCase(cases, "售后多久内可以申请", "2小时内|售后反馈", "订单规则-售后与改单", "order_rule", "medium");

        return cases;
    }

    private void addCase(List<KnowledgeEvalCase> cases, String question, String expectedAnswer,
                         String expectedSource, String category, String difficulty) {
        KnowledgeEvalCase evalCase = new KnowledgeEvalCase();
        evalCase.setQuestion(question);
        evalCase.setExpectedAnswer(expectedAnswer);
        evalCase.setExpectedSource(expectedSource);
        evalCase.setCategory(category);
        evalCase.setDifficulty(difficulty);
        cases.add(evalCase);
    }

    private static class EvalOutcome {
        private boolean top1Hit;
        private boolean top3Hit;
        private boolean top5Hit;
        private boolean noResult;
        private long costMs;
        private String searchType = "unknown";
        private String retrievalMode = "unknown";
        private String failureReason = FAILURE_NONE;
        private String topResultSource;
        private String topResultExcerpt;
    }

    private static class MatchProfile {
        private final List<String> tokens;
        private final int requiredMatches;

        private MatchProfile(List<String> tokens, int requiredMatches) {
            this.tokens = tokens.stream().filter(Objects::nonNull).toList();
            this.requiredMatches = requiredMatches;
        }
    }
}
