server:
  port: 9094

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 20MB
      file-size-threshold: 0
      location: ./uploads
  ai:
    openai:
      api-key: ${MODELSCOPE_API_KEY}
      base-url: ${AI_BASE_URL}
      chat:
        options:
          model: ${AI_MODEL}
          temperature: ${AI_TEMPERATURE}

chat:
  history:
    limit: ${CHAT_HISTORY_LIMIT}

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.aiwechat.aichat.model.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

logging:
  level:
    root: INFO
    com.aiwechat: DEBUG
