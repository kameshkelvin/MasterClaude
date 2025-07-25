# Spring Boot 后端服务 Dockerfile
# 多阶段构建，优化镜像大小和构建速度

# ====================
# 构建阶段
# ====================
FROM maven:3.9-openjdk-17-slim AS builder

LABEL maintainer="Exam System Team <team@exam-system.com>"
LABEL description="在线考试系统 Spring Boot 后端服务"

# 设置工作目录
WORKDIR /app

# 复制 Maven 配置文件（利用 Docker 缓存层）
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖（这一层会被缓存，除非 pom.xml 改变）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw clean package -DskipTests -B && \
    mv target/*.jar app.jar

# ====================
# 运行时阶段
# ====================
FROM openjdk:17-jre-slim

# 安装必要的系统工具
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    netcat-traditional \
    && rm -rf /var/lib/apt/lists/*

# 创建非 root 用户
RUN groupadd -r spring && useradd -r -g spring spring

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/app.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown -R spring:spring /app

# 设置 JVM 参数和环境变量
ENV JAVA_OPTS="-server \
    -Xms512m \
    -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai"

# Spring Boot 配置
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

# 健康检查脚本
COPY --chown=spring:spring docker/healthcheck.sh /app/healthcheck.sh
RUN chmod +x /app/healthcheck.sh

# 切换到非 root 用户
USER spring

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD ./healthcheck.sh

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]