server:
  port: 9095

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
      max-request-size: 50MB
      file-size-threshold: 0
      location: ./uploads
  ai:
    openai:
      api-key: ${EMBEDDING_API_KEY}
      base-url: ${EMBEDDING_BASE_URL}
      embedding:
        options:
          model: ${EMBEDDING_MODEL}

vectorstore:
  path: ${VECTORSTORE_PATH}

knowledge:
  sync:
    enabled: ${KNOWLEDGE_SYNC_ENABLED}
    cron: ${KNOWLEDGE_SYNC_CRON}

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.aiwechat.knowledge.model.entity
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
