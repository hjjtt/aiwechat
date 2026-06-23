package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.KnowledgeSearchResult;
import com.aiwechat.knowledge.model.dto.QueryRouteResult;

public interface KnowledgeRouterService {

    QueryRouteResult route(String query);

    KnowledgeSearchResult retrieve(String query, int topK);
}
