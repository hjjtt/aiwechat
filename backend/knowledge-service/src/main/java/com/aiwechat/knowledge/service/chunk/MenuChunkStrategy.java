package com.aiwechat.knowledge.service.chunk;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单类型切分策略
 * 每个菜品作为一个独立 chunk，保留完整结构化信息
 */
public class MenuChunkStrategy implements ChunkStrategy {

    @Override
    public String getDocumentType() {
        return "menu";
    }

    @Override
    public List<Document> split(String content, Map<String, Object> metadata) {
        List<Document> documents = new ArrayList<>();
        // 菜单文本按菜品分隔（双换行或"菜品名称"标记）
        String[] items = content.split("\n\n+");

        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (item.isEmpty()) continue;

            Map<String, Object> meta = new HashMap<>(metadata);
            meta.put("chunkIndex", i);
            meta.put("chunkType", "menu");

            // 尝试提取菜品名作为额外 metadata
            String menuName = extractMenuName(item);
            if (menuName != null) {
                meta.put("menuName", menuName);
            }

            // 尝试提取分类
            String category = extractField(item, "分类");
            if (category != null) {
                meta.put("category", category);
            }

            documents.add(new Document(item, meta));
        }

        // 如果按空行没分出来，整段作为一个 chunk
        if (documents.isEmpty()) {
            Map<String, Object> meta = new HashMap<>(metadata);
            meta.put("chunkIndex", 0);
            meta.put("chunkType", "menu");
            documents.add(new Document(content, meta));
        }

        return documents;
    }

    @Override
    public List<String> extractKeywords(String content) {
        List<String> keywords = new ArrayList<>();
        // 菜单关键词：菜品名、分类、价格相关
        String menuName = extractMenuName(content);
        if (menuName != null) keywords.add(menuName);

        String category = extractField(content, "分类");
        if (category != null) keywords.add(category);

        // 提取描述中的关键词
        String desc = extractField(content, "描述");
        if (desc != null) {
            String[] words = desc.split("[\\s,，。、]+");
            for (String w : words) {
                if (w.length() >= 2 && w.length() <= 6) keywords.add(w);
            }
        }

        return keywords.stream().distinct().limit(8).toList();
    }

    private String extractMenuName(String content) {
        return extractField(content, "菜品名称");
    }

    private String extractField(String content, String fieldName) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(fieldName + "：") || trimmed.startsWith(fieldName + ":")) {
                return trimmed.substring(fieldName.length() + 1).trim();
            }
        }
        return null;
    }
}
