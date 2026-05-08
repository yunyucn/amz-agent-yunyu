# ==============================================================================
# AI Interview Platform - Backend Dockerfile (Production)
# ==============================================================================
# 直接使用 CI 预编译的 JAR 包，不做 Gradle 源码编译。
# JAR 由 GitHub Actions 编译后通过 SCP 传输到服务器 app/build/libs/ 目录。
# 本地开发使用 ./gradlew bootRun，无需通过 Docker 构建后端。
# ==============================================================================

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY app/build/libs/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
