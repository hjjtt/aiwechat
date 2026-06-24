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

    /** 转接升级关键词 */
    private static final Set<String> ESCALATION_KEYWORDS = Set.of(
            "投诉", "严重", "紧急", "退款", "差评",
            "食品问题", "食品安全", "过敏", "中毒",
            "律师", "媒体", "监管部门", "12315"
    );

    /** 情绪负面关键词 */
    private static final Set<String> NEGATIVE_KEYWORDS = Set.of(
            "太差", "垃圾", "废物", "投诉", "举报",
            "再也不", "太差了", "非常不满意", "愤怒"
    );

    /** 内存转接队列 */
    private final Map<String, TransferQueueItem> transferMap = new ConcurrentHashMap<>();
    /** 用户重复提问计数器 */
    private final Map<String, Integer> repeatCountMap = new ConcurrentHashMap<>();

    @Override
    public boolean shouldTransfer(String userId, String question, String aiResponse) {
        // 1. 检查问题中是否包含升级关键词
        String lowerQuestion = question.toLowerCase();
        for (String keyword : ESCALATION_KEYWORDS) {
            if (lowerQuestion.contains(keyword.toLowerCase())) {
                log.info("检测到升级关键词: {}", keyword);
                return true;
            }
        }

        // 2. 检查是否连续多次提问同一问题（用户不满迹象）
        int repeatCount = repeatCountMap.merge(userId, 1, Integer::sum);
        if (repeatCount >= 3) {
            log.info("用户重复提问次数过多: {}", repeatCount);
            return true;
        }

        // 3. 检查问题长度（过短的问题可能表达不清）
        if (question.length() < 5) {
            log.debug("问题过短，可能需要人工介入");
        }

        // 4. 检查是否涉及复杂问题
        if (isComplexQuestion(question)) {
            log.info("检测到复杂问题");
            return true;
        }

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
