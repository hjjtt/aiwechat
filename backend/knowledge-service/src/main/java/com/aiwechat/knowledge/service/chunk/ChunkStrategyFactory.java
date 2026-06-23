package com.aiwechat.knowledge.service.chunk;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 切分策略工厂
 * 根据文档类型返回对应的切分策略
 */
@Component
public class ChunkStrategyFactory {

    private final Map<String, ChunkStrategy> strategies = new HashMap<>();

    public ChunkStrategyFactory() {
        ChunkStrategy generic = new GenericChunkStrategy();
        ChunkStrategy rule = new RuleChunkStrategy();
        strategies.put("generic", generic);
        strategies.put("pdf", generic);
        strategies.put("txt", generic);

        strategies.put("faq", new FaqChunkStrategy());
        strategies.put("menu", new MenuChunkStrategy());
        strategies.put("rule", rule);
        strategies.put("delivery_rule", rule);
        strategies.put("order_rule", rule);
    }

    public ChunkStrategy getStrategy(String documentType) {
        return strategies.getOrDefault(documentType, strategies.get("generic"));
    }

    public void registerStrategy(String documentType, ChunkStrategy strategy) {
        strategies.put(documentType, strategy);
    }
}
