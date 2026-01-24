# syntax=docker/dockerfile:1
# context要设为harbor-im根目录
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. 拷贝 Maven 配置
COPY docker-settings.xml /usr/share/maven/ref/settings-docker.xml

# 2. 拷贝所有 pom.xml 文件
COPY pom.xml .
COPY harbor-common/pom.xml ./harbor-common/
COPY harbor-client/pom.xml ./harbor-client/
COPY harbor-platform/pom.xml ./harbor-platform/
COPY harbor-server/pom.xml ./harbor-server/

# 3. 单独下载依赖
# 空构建 harbor-platform 模块，触发依赖下载
RUN mvn -s /usr/share/maven/ref/settings-docker.xml \
    package \
    -DskipTests \
    -pl harbor-platform -am \
    || true

# 4. 拷贝所有源码
COPY harbor-common/src ./harbor-common/src
COPY harbor-client/src ./harbor-client/src
COPY harbor-platform/src ./harbor-platform/src

# 5. 构建
RUN mvn -s /usr/share/maven/ref/settings-docker.xml \
    clean package \
    -pl harbor-platform -am \
    -DskipTests

# ========== Stage 2: 运行环境  ==========
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/harbor-platform/target/harbor-platform.jar /app/harbor-platform.jar
EXPOSE 8100
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", \
            "-Dspring.profiles.active=prod", \
            "/app/harbor-platform.jar"]
