# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. 拷贝 Docker 专用的 Maven 配置（使用阿里云镜像加速）
COPY docker-settings.xml /usr/share/maven/ref/settings-docker.xml

# 2. 拷贝所有 pom.xml 文件（保持目录结构）
COPY pom.xml .
COPY harbor-common/pom.xml ./harbor-common/
COPY harbor-client/pom.xml ./harbor-client/
COPY harbor-platform/pom.xml ./harbor-platform/
COPY harbor-server/pom.xml ./harbor-server/

# 3. 拷贝所有源码
COPY harbor-common/src ./harbor-common/src
COPY harbor-client/src ./harbor-client/src
COPY harbor-platform/src ./harbor-platform/src

# 4. 构建 harbor-platform 及其依赖模块
# -pl harbor-platform: 只构建 harbor-platform 模块
# -am: 同时构建其依赖的模块 (harbor-common, harbor-client)
# -DskipTests: 跳过测试以加快构建速度
RUN mvn -s /usr/share/maven/ref/settings-docker.xml clean package -pl harbor-platform -am -DskipTests

# ========== Stage 2: 运行环境 ==========
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 拷贝构建产物
COPY --from=builder /build/harbor-platform/target/harbor-platform.jar /app/harbor-platform.jar

# 暴露端口
EXPOSE 8100

# 启动命令
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", \
            "-Dspring.profiles.active=prod", \
            "/app/harbor-platform.jar"]
