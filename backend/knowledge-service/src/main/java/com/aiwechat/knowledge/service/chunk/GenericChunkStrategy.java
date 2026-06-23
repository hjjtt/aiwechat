package com.aiwechat.knowledge.service.chunk;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用文档切分策略
 * 使用 TokenTextSplitter 进行标准切分
 */
public class GenericChunkStrategy implements ChunkStrategy {

    private final int chunkSize;
    private final int overlap;

    public GenericChunkStrategy() {
        this(500, 100);
    }

    public GenericChunkStrategy(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    public String getDocumentType() {
        return "generic";
    }

    @Override
    public List<Document> split(String content, Map<String, Object> metadata) {
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, overlap, 5, chunkSize, true);
        Document doc = new Document(content, metadata);
        List<Document> chunks = splitter.apply(List.of(doc));

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunkIndex", i);
            chunks.get(i).getMetadata().put("chunkType", "description");
        }

        return chunks;
    }

    @Override
    public List<String> extractKeywords(String content) {
        List<String> keywords = new ArrayList<>();
        // 通用关键词提取：按标点分句，取 2-6 字的片段
        String[] segments = content.split("[\\s,，。？！?!.！、：:；;\\n]+");
        for (String seg : segments) {
            if (seg.length() >= 2 && seg.length() <= 6) {
                keywords.add(seg);
            }
        }
        return keywords.stream().distinct().limit(10).toList();
    }
}
