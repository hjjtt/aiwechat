package com.aiwechat.knowledge.service;

/**
 * 查询改写服务接口
 * 对用户原始 query 进行优化后再送入检索
 */
public interface QueryRewriteService {

    /**
     * 改写查询
     * @param query 原始查询
     * @return 改写后的查询
     */
    String rewrite(String query);

    /**
     * 判断是否需要改写
     */
    boolean needsRewrite(String query);
}
