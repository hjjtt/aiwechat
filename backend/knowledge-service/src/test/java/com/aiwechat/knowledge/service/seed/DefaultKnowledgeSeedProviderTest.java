package com.aiwechat.knowledge.service.seed;

import com.aiwechat.knowledge.service.KnowledgeBaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultKnowledgeSeedProviderTest {

    private final DefaultKnowledgeSeedProvider provider = new DefaultKnowledgeSeedProvider();

    @Test
    @DisplayName("默认知识种子应覆盖配送规则、订单规则和 FAQ")
    void shouldBuildDefaultSeedsWithExpectedCoverage() {
        DefaultKnowledgeSeedProvider.SeedBundle bundle = provider.buildDefaultSeeds();

        assertEquals(3, bundle.deliveryRules().size());
        assertEquals(3, bundle.orderRules().size());
        assertEquals(6, bundle.faqs().size());
        assertEquals(12, bundle.allTexts().size());
    }

    @Test
    @DisplayName("默认知识种子应带上业务类型、分类和来源")
    void shouldBuildSeedsWithStructuredMetadata() {
        DefaultKnowledgeSeedProvider.SeedBundle bundle = provider.buildDefaultSeeds();

        KnowledgeBaseService.TextItem deliveryRule = bundle.deliveryRules().get(0);
        KnowledgeBaseService.TextItem orderRule = bundle.orderRules().get(0);
        KnowledgeBaseService.TextItem faq = bundle.faqs().get(0);

        assertEquals("delivery_rule", deliveryRule.bizType());
        assertEquals("delivery_scope", deliveryRule.category());
        assertEquals(deliveryRule.title(), deliveryRule.source());
        assertTrue(deliveryRule.content().contains("配送范围"));

        assertEquals("order_rule", orderRule.bizType());
        assertEquals("order_lifecycle", orderRule.category());
        assertEquals(orderRule.title(), orderRule.source());
        assertTrue(orderRule.content().contains("自动取消"));

        assertEquals("faq", faq.bizType());
        assertEquals("store_hours", faq.category());
        assertEquals(faq.title(), faq.source());
        assertTrue(faq.content().contains("营业时间"));
    }

    @Test
    @DisplayName("默认知识种子汇总顺序应保持稳定")
    void shouldKeepCombinedSeedOrderStable() {
        DefaultKnowledgeSeedProvider.SeedBundle bundle = provider.buildDefaultSeeds();

        List<KnowledgeBaseService.TextItem> allTexts = bundle.allTexts();

        assertEquals(bundle.deliveryRules().get(0).title(), allTexts.get(0).title());
        assertEquals(bundle.orderRules().get(0).title(), allTexts.get(3).title());
        assertEquals(bundle.faqs().get(0).title(), allTexts.get(6).title());
    }
}
