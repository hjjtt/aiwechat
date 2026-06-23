server:
  port: 9093

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
      max-file-size: 2MB
      max-request-size: 10MB
      file-size-threshold: 0
      location: ./uploads

admin:
  auth:
    secret: ${ADMIN_AUTH_SECRET}
    username: ${ADMIN_AUTH_USERNAME}
    password: ${ADMIN_AUTH_PASSWORD}

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.aiwechat.menu.model.entity
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
