# AGENTS.md

This file provides guidance to Codex when working with code in this repository.

## Project Overview

WeChat Mini-program AI customer service system with Spring Boot backend, WeChat mini-program frontend, and Vue admin panel.

## Build Commands

```bash
# Backend - compile and package
mvn clean package -DskipTests

# Backend - run directly
java -jar target/aiwechat-1.0.0-SNAPSHOT.jar

# Backend - run with Maven
mvn spring-boot:run

# Backend - run tests
mvn test
mvn test -Dtest=TokenServiceImplTest

# Database initialization
mysql -u root -p < src/main/resources/schema.sql

# Admin panel
cd admin && npm install && npm run dev
```

## Notes

- Use local services or dedicated server processes for Nacos, Redis, MySQL, and the application.
- Nacos-related local files are stored under `config/nacos`.
- Prefer local processes and local configuration management over container-based workflows.
