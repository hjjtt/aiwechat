package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;

public interface StructuredKnowledgeService {

    KnowledgeSearchResult search(String query, int topK);
}
