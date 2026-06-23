package com.aiwechat.knowledge.service.impl;

import com.aiwechat.knowledge.service.KnowledgeBaseService;
import com.aiwechat.knowledge.service.KnowledgeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSyncServiceImpl implements KnowledgeSyncService {

    private final KnowledgeBaseService knowledgeBaseService;

    private long lastSyncTime = 0;
    private int lastMenuCount = 0;
    private int lastSyncedCount = 0;
    private String lastResult = "未同步";

    @Override
    @Scheduled(cron = "${knowledge.sync.cron:0 0 3 * * ?}")
    public void syncMenuToKnowledge() {
        log.info("开始定时同步默认知识到知识库");
        try {
            KnowledgeBaseService.InitSummary summary = knowledgeBaseService.initDefaultKnowledge();
            lastMenuCount = summary.menuCount();
            lastSyncedCount = summary.totalCount();
            lastSyncTime = System.currentTimeMillis();
            lastResult = String.format(
                    "成功: 菜单%d, 配送规则%d, 订单规则%d, FAQ%d",
                    summary.menuCount(),
                    summary.deliveryRuleCount(),
                    summary.orderRuleCount(),
                    summary.faqCount()
            );

            log.info("默认知识同步完成，共同步 {} 条", lastSyncedCount);
        } catch (Exception e) {
            lastResult = "失败: " + e.getMessage();
            log.error("默认知识同步失败", e);
        }
    }

    @Override
    public void syncAll() {
        log.info("开始全量同步默认知识");
        try {
            knowledgeBaseService.clear();
            log.info("已清空现有知识库");
            syncMenuToKnowledge();
            log.info("全量同步完成");
        } catch (Exception e) {
            log.error("全量同步失败", e);
            throw new RuntimeException("全量同步失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SyncStatus getSyncStatus() {
        return new SyncStatus(lastSyncTime, lastMenuCount, lastSyncedCount, lastResult);
    }
}
