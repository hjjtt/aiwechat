package com.aiwechat.menu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 菜品服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aiwechat.menu", "com.aiwechat.common"})
@MapperScan("com.aiwechat.menu.repository")
public class MenuServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MenuServiceApplication.class, args);
    }
}
