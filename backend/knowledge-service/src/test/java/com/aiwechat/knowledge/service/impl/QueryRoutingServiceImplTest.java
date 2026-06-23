package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.model.dto.QueryRouteResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryRoutingServiceImplTest {

    private final QueryRoutingServiceImpl service = new QueryRoutingServiceImpl();

    @Test
    @DisplayName("价格和规则类问题应路由到结构化检索")
    void routeShouldUseStructuredForMenuAndRuleQueries() {
        QueryRouteResult priceRoute = service.route("宫保鸡丁多少钱");
        QueryRouteResult deliveryRoute = service.route("配送费怎么算");

        assertEquals("structured", priceRoute.getRouteType());
        assertEquals("structured", priceRoute.getSearchType());
        assertEquals("structured", deliveryRoute.getRouteType());
    }

    @Test
    @DisplayName("关系链问题应识别为 LazyGraphRAG 意图")
    void routeShouldMarkLazyGraphIntent() {
        QueryRouteResult route = service.route("套餐包含哪些菜品，哪些包含过敏原");

        assertEquals("lazy_graph", route.getRouteType());
        assertEquals("lazy_graph", route.getSearchType());
        assertTrue(route.getReason().contains("LazyGraphRAG"));
    }

    @Test
    @DisplayName("普通说明性问题应使用混合检索")
    void routeShouldUseHybridForGeneralQuestion() {
        QueryRouteResult route = service.route("怎么联系客服");

        assertEquals("hybrid", route.getRouteType());
        assertEquals("hybrid", route.getSearchType());
    }
}
