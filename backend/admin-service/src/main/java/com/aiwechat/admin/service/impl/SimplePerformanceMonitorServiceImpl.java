package com.aiwechat.admin.service.impl;

import com.aiwechat.admin.service.PerformanceMonitorService;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * 简单的性能监控服务实现（基于JVM内存数据）
 */
@Service
public class SimplePerformanceMonitorServiceImpl implements PerformanceMonitorService {

    @Override
    public PerformanceMetrics getMetrics() {
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = totalMemory > 0
                ? (double) usedMemory / totalMemory * 100
                : 0;

        return new PerformanceMetrics(
                totalMemory,
                freeMemory,
                usedMemory,
                Math.round(memoryUsagePercent * 100.0) / 100.0,
                runtime.availableProcessors(),
                runtimeMXBean.getUptime(),
                System.getProperty("java.version")
        );
    }

    @Override
    public HealthStatus getHealthStatus() {
        return new HealthStatus(
                "UP",
                "admin-service",
                System.currentTimeMillis(),
                getMetrics()
        );
    }
}
