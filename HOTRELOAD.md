# Spring Boot DevTools 热加载配置指南

## ✅ 已配置功能

项目已配置 Spring Boot DevTools，支持以下热加载功能：

### 1. **自动重启 (Automatic Restart)**
- **触发条件**: 修改 `src/main/java` 下的 Java 文件并保存
- **重启延迟**: 文件保存后 1 秒自动重启
- **排除路径**: `static/**`, `public/**` 下的文件修改不会触发重启

### 2. **LiveReload**
- **功能**: 浏览器自动刷新（需要安装 LiveReload 浏览器插件）
- **状态**: 已启用

### 3. **触发文件重启**
- **触发文件**: `.reloadtrigger`
- **用法**: 修改此文件内容可手动触发重启

## 🚀 使用方法

### 方式一：Maven 运行（推荐）
```bash
# 开发模式运行（支持热加载）
mvn spring-boot:run

# 或者先编译再运行
mvn clean package -DskipTests
java -jar target/aiwechat-1.0.0-SNAPSHOT.jar
```

### 方式二：IDEA 运行
1. 确保已安装 Lombok 插件
2. 启用注解处理器：`Settings → Build → Compiler → Annotation Processors`
3. 运行 `AiwechatApplication.java`
4. 修改代码后按 `Ctrl+F9` (Windows) 或 `Cmd+F9` (Mac) 重新编译

### 方式三：Eclipse 运行
1. 确保已安装 Lombok
2. 启用自动构建：`Project → Build Automatically`
3. 运行 `AiwechatApplication.java`
4. 修改代码后自动保存即可触发重启

## ⚙️ 配置说明

### application.yml 配置
```yaml
spring:
  devtools:
    restart:
      enabled: true              # 启用重启
      additional-paths: src/main/java  # 监控路径
      exclude: static/**,public/**     # 排除路径
    livereload:
      enabled: true              # 启用 LiveReload
```

### pom.xml 配置
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

## 📋 热加载测试

1. **启动应用**:
   ```bash
   mvn spring-boot:run
   ```

2. **修改代码**:
   - 打开任意 Controller 或 Service 文件
   - 修改某个方法的返回值或日志
   - 保存文件

3. **观察日志**:
   ```
   2026-03-21T00:00:00.000+08:00  INFO 12345 --- [ restartedMain] com.aiwechat.AiwechatApplication : Starting AiwechatApplication...
   2026-03-21T00:00:01.000+08:00  INFO 12345 --- [ restartedMain] com.aiwechat.AiwechatApplication : Started AiwechatApplication in 1.5s
   ```
   看到 `restartedMain` 表示热加载成功！

## ⚠️ 注意事项

1. **生产环境禁用**: DevTools 在生产环境会自动禁用
2. **Lombok 兼容**: 确保 IDE 已安装 Lombok 插件并启用注解处理
3. **静态资源**: 修改 `static/` 或 `public/` 下的文件不会触发重启
4. **配置文件**: 修改 `application.yml` 需要手动重启
5. **依赖变更**: 添加/修改 Maven 依赖需要重新编译

## 🔧 故障排查

### 问题：热加载不生效

**解决方案**:
1. 检查 `application.yml` 中 `spring.devtools.restart.enabled=true`
2. 确保使用 `mvn spring-boot:run` 运行，而不是直接运行 jar
3. 检查 IDE 是否启用了自动编译
4. 尝试修改 `.reloadtrigger` 文件手动触发

### 问题：重启速度慢

**优化方案**:
1. 在 `application.yml` 中添加排除路径：
   ```yaml
   spring:
     devtools:
       restart:
         exclude: src/main/resources/templates/**,src/main/resources/static/**
   ```
2. 减少监控的文件数量
3. 增加 JVM 内存：`-Xmx512m`

## 📚 参考资料

- [Spring Boot DevTools 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#using.devtools)
- [Maven Spring Boot Plugin](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/)
