package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.QueryRouteResult;
import com.aiwechat.knowledge.service.QueryRoutingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QueryRoutingServiceImpl implements QueryRoutingService {

    @Override
    public QueryRouteResult route(String query) {
        String normalized = normalize(query);
        if (!StringUtils.hasText(normalized)) {
            return build(query, "hybrid", "hybrid", "空查询，使用混合检索兜底");
        }

        if (containsAny(normalized, "关联", "影响", "关系", "包含哪些", "哪些包含", "过敏原", "套餐包含", "适用哪些门店")) {
            return build(query, "lazy_graph", "lazy_graph", "疑似多实体关系问题，优先使用 LazyGraphRAG 概念图检索");
        }

        if (containsAny(normalized,
                "多少钱", "价格", "售价", "菜品", "菜单", "推荐菜", "热销", "分类",
                "配送", "配送费", "配送范围", "起送", "送达", "退款", "取消订单",
                "售后", "发票", "营业时间", "优惠")) {
            return build(query, "structured", "structured", "命中菜单、价格、配送、订单或规则类结构化查询");
        }

        if (containsAny(normalized, "说明", "介绍", "怎么", "如何", "为什么", "可以吗")) {
            return build(query, "hybrid", "hybrid", "说明性问题，使用混合检索");
        }

        return build(query, "hybrid", "hybrid", "未命中特定路由规则，使用混合检索");
    }

    private QueryRouteResult build(String query, String routeType, String searchType, String reason) {
        return QueryRouteResult.builder()
                .query(query)
                .routeType(routeType)
                .searchType(searchType)
                .reason(reason)
                .build();
    }

    private String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
