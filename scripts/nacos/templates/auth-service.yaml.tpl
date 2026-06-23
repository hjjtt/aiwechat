server:
  port: 9091

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

wechat:
  appId: ${WECHAT_APP_ID}
  appSecret: ${WECHAT_APP_SECRET}
  sessionHost: ${WECHAT_SESSION_HOST}

app:
  token:
    expire-hours: ${TOKEN_EXPIRE_HOURS}