# syntax=docker/dockerfile:1
# context要设为harbor-im根目录
FROM maven:3.9.12-eclipse-temurin-25-alpine AS builder

WORKDIR /build

# 1. 拷贝 Maven 配置
COPY docker-settings.xml /usr/share/maven/ref/settings-docker.xml

# 2. 拷贝所有 pom.xml 文件
COPY pom.xml .
COPY common/pom.xml ./common/
COPY platform/pom.xml ./platform/
COPY server/pom.xml ./server/

# 3. 单独下载依赖
# 空构建 harbor-platform 模块，触发依赖下载
RUN mvn -s /usr/share/maven/ref/settings-docker.xml \
    package \
    -DskipTests \
    -pl platform -am \
    || true

# 4. 拷贝所有源码
COPY common/src ./common/src
COPY platform/src ./platform/src

# 5. 构建
RUN mvn -s /usr/share/maven/ref/settings-docker.xml \
    clean package \
    -pl platform -am \
    -DskipTests

# ========== Stage 2: 运行环境  ==========
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /build/platform/target/platform.jar /app/platform.jar
EXPOSE 8100
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", \
            "-Dspring.profiles.active=prod", \
            "/app/platform.jar"]
