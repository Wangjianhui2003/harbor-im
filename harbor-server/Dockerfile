# ========== Runtime ==========
FROM eclipse-temurin:21-jre-alpine-3.23
WORKDIR /app

COPY target/harbor-server.jar /app

EXPOSE 8101

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:InitialRAMPercentage=50.0", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/harbor-server.jar"]
