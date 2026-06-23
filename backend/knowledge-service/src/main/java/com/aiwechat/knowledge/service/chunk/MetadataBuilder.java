package com.aiwechat.knowledge.service.chunk;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一的 chunk metadata 构建器
 * 确保所有 chunk 携带标准化的元数据
 */
public class MetadataBuilder {

    private final Map<String, Object> metadata = new HashMap<>();

    public MetadataBuilder documentId(String documentId) {
        metadata.put("documentId", documentId);
        return this;
    }

    public MetadataBuilder source(String source) {
        metadata.put("source", source);
        return this;
    }

    public MetadataBuilder documentType(String documentType) {
        metadata.put("documentType", documentType);
        return this;
    }

    public MetadataBuilder bizType(String bizType) {
        metadata.put("bizType", bizType);
        return this;
    }

    public MetadataBuilder category(String category) {
        metadata.put("category", category);
        return this;
    }

    public MetadataBuilder menuId(String menuId) {
        metadata.put("menuId", menuId);
        return this;
    }

    public MetadataBuilder storeId(String storeId) {
        metadata.put("storeId", storeId);
        return this;
    }

    public MetadataBuilder version(int version) {
        metadata.put("version", version);
        return this;
    }

    public MetadataBuilder updatedAt(LocalDateTime updatedAt) {
        metadata.put("updatedAt", updatedAt.toString());
        return this;
    }

    public MetadataBuilder extra(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public Map<String, Object> build() {
        return new HashMap<>(metadata);
    }
}
