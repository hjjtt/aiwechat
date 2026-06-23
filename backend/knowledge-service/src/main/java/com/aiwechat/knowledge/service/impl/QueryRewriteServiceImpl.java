package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.service.QueryRewriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 查询改写实现
 * 基于规则的查询扩展和规范化
 */
@Slf4j
@Service
public class QueryRewriteServiceImpl implements QueryRewriteService {

    /** 同义词扩展映射 */
    private static final Map<String, List<String>> SYNONYM_MAP = Map.of(
            "多少钱", List.of("价格", "价钱", "多少钱", "费用"),
            "配送", List.of("配送", "外卖", "送餐", "送货", "配送费"),
            "推荐", List.of("推荐", "招牌", "特色", "热销", "热门"),
            "辣", List.of("辣", "麻辣", "香辣", "微辣", "特辣"),
            "套餐", List.of("套餐", "组合", "搭配", "优惠套餐"),
            "取消", List.of("取消", "退单", "取消订单"),
            "退款", List.of("退款", "退钱", "退货", "退"),
            "地址", List.of("地址", "配送地址", "收货地址", "送餐地址")
    );

    /** 业务关键词白名单（用于强匹配） */
    private static final List<String> BUSINESS_KEYWORDS = List.of(
            "菜单", "菜品", "主食", "热菜", "素菜", "汤类", "饮品",
            "配送费", "起送价", "配送范围", "配送时间",
            "营业时间", "下单", "订单", "支付"
    );

    @Override
    public String rewrite(String query) {
        if (query == null || query.trim().isEmpty()) return query;

        String rewritten = query.trim();

        // 1. 去除常见口语化前缀
        rewritten = rewritten.replaceFirst("^(请问|你好|我想知道|能不能|可不可以|能不能告诉我)\\s*", "");

        // 2. 同义词扩展：如果 query 中包含同义词表中的词，扩展为多词查询
        rewritten = expandSynonyms(rewritten);

        // 3. 提取业务关键词
        List<String> matchedKeywords = BUSINESS_KEYWORDS.stream()
                .filter(rewritten::contains)
                .toList();

        if (!matchedKeywords.isEmpty()) {
            // 将匹配到的业务关键词追加到查询中（增强召回）
            log.debug("匹配到业务关键词: {}", matchedKeywords);
        }

        log.debug("查询改写: '{}' -> '{}'", query, rewritten);
        return rewritten;
    }

    @Override
    public boolean needsRewrite(String query) {
        if (query == null || query.trim().isEmpty()) return false;

        // 口语化前缀
        if (query.matches("^(请问|你好|我想知道|能不能|可不可以).*")) return true;

        // 长度过短（单字查询需要扩展）
        if (query.length() <= 2) return true;

        // 包含同义词表中的词
        for (String key : SYNONYM_MAP.keySet()) {
            if (query.contains(key)) return true;
        }

        return false;
    }

    private String expandSynonyms(String query) {
        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            if (query.contains(entry.getKey())) {
                // 只取前 2 个同义词追加，避免查询过长
                List<String> synonyms = entry.getValue().stream()
                        .filter(s -> !s.equals(entry.getKey()))
                        .limit(2)
                        .toList();
                if (!synonyms.isEmpty()) {
                    return query + " " + String.join(" ", synonyms);
                }
            }
        }
        return query;
    }
}
