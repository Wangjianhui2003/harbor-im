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
# 空构建 harbor-server 模块，触发依赖下载
RUN mvn -s /usr/share/maven/ref/settings-docker.xml \
    package \
    -DskipTests \
    -pl harbor-server -am \
    || true


# 4. 拷贝所有源码
# 即使这里因为代码修改导致缓存失效，上面的依赖层依然是从缓存读取的
COPY harbor-common/src ./harbor-common/src
COPY harbor-server/src ./harbor-server/src

# 5. 构建
RUN mvn -s /usr/share/maven/ref/settings-docker.xml \
    clean package \
    -pl harbor-server -am \
    -DskipTests

# ========== Stage 2: 运行环境==========
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/harbor-server/target/harbor-server.jar /app/harbor-server.jar
EXPOSE 8101
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", \
            "-Dspring.profiles.active=prod", \
            "/app/harbor-server.jar"]
