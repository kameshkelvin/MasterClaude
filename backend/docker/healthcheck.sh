#!/bin/bash

# Spring Boot 应用健康检查脚本

set -e

# 配置
HEALTH_URL="http://localhost:${SERVER_PORT:-8080}/actuator/health"
TIMEOUT=10
MAX_RETRIES=3

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] HEALTH CHECK:${NC} $1"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] HEALTH WARNING:${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] HEALTH ERROR:${NC} $1"
}

# 检查 Java 进程
check_java_process() {
    if ! pgrep -f "java.*app.jar" > /dev/null; then
        error "Java 进程未运行"
        return 1
    fi
    return 0
}

# 检查端口监听
check_port() {
    local port=${SERVER_PORT:-8080}
    if ! netcat -z localhost $port; then
        error "端口 $port 未监听"
        return 1
    fi
    return 0
}

# 检查 Spring Boot Actuator 健康端点
check_actuator_health() {
    local retry=0
    while [ $retry -lt $MAX_RETRIES ]; do
        if curl -f -s --max-time $TIMEOUT "$HEALTH_URL" > /dev/null 2>&1; then
            local status=$(curl -s --max-time $TIMEOUT "$HEALTH_URL" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
            if [ "$status" = "UP" ]; then
                log "应用健康状态：UP"
                return 0
            else
                warn "应用健康状态：$status"
            fi
        fi
        
        retry=$((retry + 1))
        if [ $retry -lt $MAX_RETRIES ]; then
            warn "健康检查失败，重试 $retry/$MAX_RETRIES"
            sleep 2
        fi
    done
    
    error "健康检查失败，已重试 $MAX_RETRIES 次"
    return 1
}

# 检查数据库连接（如果配置了）
check_database() {
    if [ -n "$DB_HOST" ] && [ -n "$DB_PORT" ]; then
        if ! netcat -z "$DB_HOST" "$DB_PORT"; then
            error "无法连接到数据库 $DB_HOST:$DB_PORT"
            return 1
        fi
        log "数据库连接正常"
    fi
    return 0
}

# 检查 Redis 连接（如果配置了）
check_redis() {
    if [ -n "$REDIS_HOST" ] && [ -n "$REDIS_PORT" ]; then
        if ! netcat -z "$REDIS_HOST" "$REDIS_PORT"; then
            error "无法连接到 Redis $REDIS_HOST:$REDIS_PORT"
            return 1
        fi
        log "Redis 连接正常"
    fi
    return 0
}

# 主健康检查函数
main() {
    log "开始健康检查..."
    
    # 基础检查
    if ! check_java_process; then
        exit 1
    fi
    
    if ! check_port; then
        exit 1
    fi
    
    # 应用健康检查
    if ! check_actuator_health; then
        exit 1
    fi
    
    # 依赖服务检查
    if ! check_database; then
        exit 1
    fi
    
    if ! check_redis; then
        exit 1
    fi
    
    log "所有健康检查通过 ✓"
    exit 0
}

# 执行主函数
main "$@"