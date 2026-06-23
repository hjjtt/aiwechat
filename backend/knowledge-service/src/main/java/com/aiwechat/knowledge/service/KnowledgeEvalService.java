package com.aiwechat.knowledge.service;

import com.aiwechat.knowledge.model.dto.EvalReport;

import java.util.List;
import java.util.Map;

/**
 * 知识库评测服务接口
 */
public interface KnowledgeEvalService {

    /**
     * 运行评测
     * @param category 评测分类（null 则评测全部）
     * @return 评测报告
     */
    EvalReport runEval(String category);

    /**
     * 获取最近一次评测结果
     */
    List<Map<String, Object>> getLatestResults(int limit);

    /**
     * 初始化默认评测数据
     */
    int initDefaultEvalCases();
}
