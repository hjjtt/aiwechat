logging:
  level:
    root: INFO

spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR}
        namespace: ${NACOS_NAMESPACE}
        username: ${NACOS_USERNAME}
        password: ${NACOS_PASSWORD}
        group: ${NACOS_GROUP}

rate:
  limit:
    enabled: ${RATE_LIMIT_ENABLED}
    requests-per-minute: ${RATE_LIMIT_REQUESTS_PER_MINUTE}
    window-seconds: ${RATE_LIMIT_WINDOW_SECONDS}