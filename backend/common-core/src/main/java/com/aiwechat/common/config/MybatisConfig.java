package com.aiwechat.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.aiwechat.**.repository")
public class MybatisConfig {
}
