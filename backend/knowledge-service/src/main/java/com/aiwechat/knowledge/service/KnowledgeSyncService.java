package com.aiwechat.knowledge.service;

/**
 * 知识库同步服务接口
 * 负责定时同步菜单数据到知识库
 */
public interface KnowledgeSyncService {

    /**
     * 同步状态记录
     */
    record SyncStatus(long lastSyncTime, int menuCount, int syncedCount, String lastResult) {}

    /**
     * 同步菜单数据到知识库
     */
    void syncMenuToKnowledge();

    /**
     * 全量同步（清空后重建）
     */
    void syncAll();

    /**
     * 获取同步状态
     * @return 同步状态信息
     */
    SyncStatus getSyncStatus();
}
