server:
  port: 9096

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

admin:
  auth:
    secret: ${ADMIN_AUTH_SECRET}
    username: ${ADMIN_AUTH_USERNAME}
    password: ${ADMIN_AUTH_PASSWORD}
    jwt-expiry-hours: ${ADMIN_JWT_EXPIRY_HOURS:24}

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.aiwechat.admin.model.entity
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
