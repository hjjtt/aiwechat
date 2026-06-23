package com.aiwechat.knowledge.service.chunk;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则类型切分策略
 * 按规则条目切分，每条规则保持完整
 */
public class RuleChunkStrategy implements ChunkStrategy {

    @Override
    public String getDocumentType() {
        return "rule";
    }

    @Override
    public List<Document> split(String content, Map<String, Object> metadata) {
        List<Document> documents = new ArrayList<>();

        // 按编号规则条目切分（如 "1." "第一条" "规则一" 等）
        String[] rules = content.split("(?=\\d+[.、）)]|第[一二三四五六七八九十]+条|规则[一二三四五六七八九十\\d]+)");

        for (int i = 0; i < rules.length; i++) {
            String rule = rules[i].trim();
            if (rule.isEmpty()) continue;

            Map<String, Object> meta = new HashMap<>(metadata);
            meta.put("chunkIndex", i);
            meta.put("chunkType", "rule");
            documents.add(new Document(rule, meta));
        }

        // 如果未按规则格式分割，使用较小的 chunk size
        if (documents.isEmpty() || (documents.size() == 1 && documents.get(0).getText().length() > 800)) {
            TokenTextSplitter splitter = new TokenTextSplitter(400, 80, 5, 400, true);
            Document doc = new Document(content, metadata);
            return splitter.apply(List.of(doc));
        }

        return documents;
    }

    @Override
    public List<String> extractKeywords(String content) {
        List<String> keywords = new ArrayList<>();
        // 规则关键词提取：取每行前几个关键词
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // 去掉序号前缀
            trimmed = trimmed.replaceFirst("^[\\d]+[.、）)]\\s*", "");
            trimmed = trimmed.replaceFirst("^第[一二三四五六七八九十]+条\\s*", "");
            if (trimmed.length() >= 2 && trimmed.length() <= 20) {
                keywords.add(trimmed);
            }
        }
        return keywords.stream().distinct().limit(8).toList();
    }
}
