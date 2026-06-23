package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.LazyGraphIndexReport;

import java.util.List;

public interface LazyGraphRagService {

    List<String> extractConcepts(String text);

    LazyGraphIndexReport rebuildIndex();

    KnowledgeSearchResult search(String query, int topK);
}
