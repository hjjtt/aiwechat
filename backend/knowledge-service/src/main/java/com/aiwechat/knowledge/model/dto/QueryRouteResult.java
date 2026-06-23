package com.aiwechat.knowledge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRouteResult {

    private String query;

    /** structured, hybrid, vector, graph */
    private String routeType;

    /** 最终执行的检索类型 */
    private String searchType;

    /** 路由原因 */
    private String reason;
}
