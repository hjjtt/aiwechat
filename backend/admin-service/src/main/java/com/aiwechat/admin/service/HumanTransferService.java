package com.aiwechat.admin.service;

import java.util.List;

/**
 * 人工转接服务接口
 */
public interface HumanTransferService {

    /**
     * 转接结果记录
     */
    record TransferResult(String message) {}

    /**
     * 转接队列记录
     */
    record TransferQueue(List<?> queue, int count) {}

    /**
     * 获取转接队列
     *
     * @return 转接队列信息
     */
    TransferQueue getTransferQueue();

    /**
     * 接受转接请求
     *
     * @param transferId 转接ID
     * @param agentId    客服ID
     * @return 是否接受成功
     */
    boolean acceptTransfer(String transferId, String agentId);
}
