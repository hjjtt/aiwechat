package com.aiwechat.admin.service;

/**
 * 性能监控服务接口
 */
public interface PerformanceMonitorService {

    /**
     * 性能指标记录
     */
    record PerformanceMetrics(
            long totalMemory,
            long freeMemory,
            long usedMemory,
            double memoryUsagePercent,
            int availableProcessors,
            long uptimeMillis,
            String jvmVersion
    ) {}

    /**
     * 健康状态记录
     */
    record HealthStatus(
            String status,
            String service,
            long timestamp,
            PerformanceMetrics metrics
    ) {}

    /**
     * 获取性能指标
     *
     * @return 性能指标数据
     */
    PerformanceMetrics getMetrics();

    /**
     * 获取健康状态
     *
     * @return 健康状态数据
     */
    HealthStatus getHealthStatus();
}
