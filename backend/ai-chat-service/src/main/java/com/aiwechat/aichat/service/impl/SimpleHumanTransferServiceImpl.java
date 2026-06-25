package com.aiwechat.aichat.service.impl;

import com.aiwechat.aichat.service.HumanTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 人工客服转接服务实现 - 内存版
 * 使用内存队列管理转接请求，适合单机部署
 */
@Slf4j
@Service
@Primary
public class SimpleHumanTransferServiceImpl implements HumanTransferService {

    /** 转接过期时间（分钟） */
    private static final long TRANSFER_EXPIRY_MINUTES = 30;

    /** 内存转接队列 */
    private final Map<String, TransferQueueItem> transferMap = new ConcurrentHashMap<>();

    @Override
    public boolean shouldTransfer(String userId, String question, String aiResponse) {
        return false;
    }

    @Override
    public TransferResult transferToHuman(String userId, String sessionId, String reason) {
        String transferId = UUID.randomUUID().toString();

        TransferQueueItem item = new TransferQueueItem(
                transferId,
                userId,
                sessionId,
                null,
                reason,
                TransferStatus.PENDING,
                System.currentTimeMillis()
        );

        // 保存到内存队列
        transferMap.put(transferId, item);

        log.info("人工客服转接已创建 - transferId: {}, userId: {}", transferId, userId);

        return new TransferResult(transferId, true, "已为您转接人工客服，请稍候", TransferStatus.PENDING);
    }

    @Override
    public TransferQueue getTransferQueue() {
        List<TransferQueueItem> items = transferMap.values().stream()
                .filter(item -> item.status() == TransferStatus.PENDING)
                .sorted((a, b) -> Long.compare(b.createdAt(), a.createdAt()))
                .collect(Collectors.toList());

        return new TransferQueue(items.size(), items);
    }

    @Override
    public boolean acceptTransfer(String transferId, String agentId) {
        TransferQueueItem item = transferMap.get(transferId);
        if (item == null || item.status() != TransferStatus.PENDING) {
            log.warn("转接单不存在或已处理 - transferId: {}", transferId);
            return false;
        }

        TransferQueueItem updated = new TransferQueueItem(
                item.transferId(),
                item.userId(),
                item.sessionId(),
                item.question(),
                item.reason(),
                TransferStatus.ACCEPTED,
                item.createdAt()
        );

        transferMap.put(transferId, updated);
        log.info("客服已接单 - transferId: {}, agentId: {}", transferId, agentId);

        return true;
    }

    /**
     * 判断是否为复杂问题（涉及多个主题）
     */
    private boolean isComplexQuestion(String question) {
        String lower = question.toLowerCase();

        int topicCount = 0;
        if (lower.contains("订单") || lower.contains("配送") || lower.contains("快递")) {
            topicCount++;
        }
        if (lower.contains("菜品") || lower.contains("味道") || lower.contains("分量")) {
            topicCount++;
        }
        if (lower.contains("价格") || lower.contains("优惠") || lower.contains("退款")) {
            topicCount++;
        }

        return topicCount >= 5;
    }
}
