package com.aiwechat.aichat.service;

import java.util.List;

/**
 * 人工客服转接服务接口
 */
public interface HumanTransferService {

    /**
     * 判断是否需要转接人工客服
     *
     * @param userId     用户ID
     * @param question   用户问题
     * @param aiResponse AI回复
     * @return 是否需要转接
     */
    boolean shouldTransfer(String userId, String question, String aiResponse);

    /**
     * 执行转接
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param reason    转接原因
     * @return 转接结果
     */
    TransferResult transferToHuman(String userId, String sessionId, String reason);

    /**
     * 获取转接队列
     *
     * @return 转接队列信息
     */
    TransferQueue getTransferQueue();

    /**
     * 客服接单
     *
     * @param transferId 转接ID
     * @param agentId    客服ID
     * @return 接单是否成功
     */
    boolean acceptTransfer(String transferId, String agentId);

    /**
     * 转接状态枚举
     */
    enum TransferStatus {
        /** 待处理 */
        PENDING,
        /** 已接单 */
        ACCEPTED,
        /** 已完成 */
        COMPLETED,
        /** 已过期 */
        EXPIRED
    }

    /**
     * 转接结果
     *
     * @param transferId 转接ID
     * @param success    是否成功
     * @param message    提示消息
     * @param status     转接状态
     */
    record TransferResult(
            String transferId,
            boolean success,
            String message,
            TransferStatus status
    ) {}

    /**
     * 转接队列项
     *
     * @param transferId 转接ID
     * @param userId     用户ID
     * @param sessionId  会话ID
     * @param question   用户问题
     * @param reason     转接原因
     * @param status     转接状态
     * @param createdAt  创建时间戳
     */
    record TransferQueueItem(
            String transferId,
            String userId,
            String sessionId,
            String question,
            String reason,
            TransferStatus status,
            long createdAt
    ) {}

    /**
     * 转接队列
     *
     * @param pendingCount 待处理数量
     * @param items        队列项列表
     */
    record TransferQueue(
            int pendingCount,
            List<TransferQueueItem> items
    ) {}
}
