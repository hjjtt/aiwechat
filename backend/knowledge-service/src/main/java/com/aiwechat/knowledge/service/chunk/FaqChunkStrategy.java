package com.aiwechat.knowledge.service.chunk;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FAQ 类型切分策略
 * 每个 Q&A 对作为一个独立 chunk，不做额外切分
 */
public class FaqChunkStrategy implements ChunkStrategy {

    @Override
    public String getDocumentType() {
        return "faq";
    }

    @Override
    public List<Document> split(String content, Map<String, Object> metadata) {
        List<Document> documents = new ArrayList<>();
        // 按 "Q:" / "问题：" 或空行分割 FAQ 对
        String[] faqPairs = content.split("(?=Q[:：]|问题[：:])");
        for (int i = 0; i < faqPairs.length; i++) {
            String pair = faqPairs[i].trim();
            if (pair.isEmpty()) continue;

            Map<String, Object> meta = new HashMap<>(metadata);
            meta.put("chunkIndex", i);
            meta.put("chunkType", "faq");
            documents.add(new Document(pair, meta));
        }

        // 如果没有按 FAQ 格式分割成功，按普通文本切分
        if (documents.isEmpty()) {
            TokenTextSplitter splitter = new TokenTextSplitter(300, 50, 5, 300, true);
            Document doc = new Document(content, metadata);
            return splitter.apply(List.of(doc));
        }

        return documents;
    }

    @Override
    public List<String> extractKeywords(String content) {
        // FAQ 的关键词主要是问句中的核心词
        List<String> keywords = new ArrayList<>();
        if (content.contains("Q:") || content.contains("问题：")) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Q:") || trimmed.startsWith("Q：") || trimmed.startsWith("问题：")) {
                    String question = trimmed.replaceFirst("^[Q问题][：:]", "").trim();
                    // 简单提取：取问句中 2-4 字的片段
                    keywords.addAll(extractSegments(question, 2, 4));
                }
            }
        }
        return keywords.stream().distinct().limit(10).toList();
    }

    private List<String> extractSegments(String text, int minLen, int maxLen) {
        List<String> segments = new ArrayList<>();
        // 按标点和空格分词
        String[] words = text.split("[\\s,，。？！?!.！、：:；;]+");
        for (String word : words) {
            if (word.length() >= minLen && word.length() <= maxLen) {
                segments.add(word);
            }
        }
        return segments;
    }
}
