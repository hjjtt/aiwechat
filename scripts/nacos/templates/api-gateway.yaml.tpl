server:
  port: 9090

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
        - id: order-service-admin
          uri: lb://order-service
          predicates:
            - Path=/api/admin/orders/**
        - id: menu-service-admin
          uri: lb://menu-service
          predicates:
            - Path=/api/admin/menus/**
        - id: knowledge-service-admin
          uri: lb://knowledge-service
          predicates:
            - Path=/api/admin/knowledge/**
        - id: admin-service
          uri: lb://admin-service
          predicates:
            - Path=/api/admin/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
        - id: order-service-addresses
          uri: lb://order-service
          predicates:
            - Path=/api/addresses/**
        - id: menu-service
          uri: lb://menu-service
          predicates:
            - Path=/api/menu/**
        - id: menu-service-upload
          uri: lb://menu-service
          predicates:
            - Path=/api/upload/**
        - id: menu-service-favorites
          uri: lb://menu-service
          predicates:
            - Path=/api/favorites/**
        - id: admin-service-feedback
          uri: lb://admin-service
          predicates:
            - Path=/api/feedback/**
        - id: ai-chat-service
          uri: lb://ai-chat-service
          predicates:
            - Path=/api/ai-chat/**
        - id: knowledge-service
          uri: lb://knowledge-service
          predicates:
            - Path=/api/knowledge/**
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns:
              - "http://localhost:*"
              - "https://servicewechat.com"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true

security:
  auth-service-url: ${AUTH_SERVICE_URL}

management:
  endpoints:
    web:
      exposure:
        include: health,info
