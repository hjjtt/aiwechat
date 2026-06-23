package com.aiwechat.admin.service.impl;

import com.aiwechat.admin.service.HumanTransferService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单的人工转接服务实现（内存数据）
 */
@Service
public class SimpleHumanTransferServiceImpl implements HumanTransferService {

    /** 内存中的转接队列 */
    private final List<Object> transferQueue = new ArrayList<>();

    @Override
    public TransferQueue getTransferQueue() {
        return new TransferQueue(new ArrayList<>(transferQueue), transferQueue.size());
    }

    @Override
    public boolean acceptTransfer(String transferId, String agentId) {
        // 简单实现：直接返回true表示接受成功
        return true;
    }
}
