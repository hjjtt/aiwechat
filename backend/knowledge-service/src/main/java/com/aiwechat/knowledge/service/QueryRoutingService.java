package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.QueryRouteResult;

public interface QueryRoutingService {

    QueryRouteResult route(String query);
}
