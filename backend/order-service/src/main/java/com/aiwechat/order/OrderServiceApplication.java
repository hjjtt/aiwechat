package com.aiwechat.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aiwechat.order", "com.aiwechat.common"})
@MapperScan("com.aiwechat.order.repository")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
