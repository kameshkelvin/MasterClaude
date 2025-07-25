#!/bin/sh

# Next.js 应用健康检查脚本

set -e

# 配置
HEALTH_URL="http://localhost:${PORT:-3000}/api/health"
TIMEOUT=10
MAX_RETRIES=3

# 日志函数
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] FRONTEND HEALTH: $1"
}

warn() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] FRONTEND WARNING: $1"
}

error() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] FRONTEND ERROR: $1"
}

# 检查 Node.js 进程
check_node_process() {
    if ! pgrep -f "node.*server.js" > /dev/null; then
        error "Node.js 进程未运行"
        return 1
    fi
    return 0
}

# 检查端口监听
check_port() {
    local port=${PORT:-3000}
    if ! nc -z localhost $port; then
        error "端口 $port 未监听"
        return 1
    fi
    return 0
}

# 检查 Next.js 健康端点
check_health_endpoint() {
    local retry=0
    while [ $retry -lt $MAX_RETRIES ]; do
        if curl -f -s --max-time $TIMEOUT "$HEALTH_URL" > /dev/null 2>&1; then
            log "应用健康端点响应正常"
            return 0
        fi
        
        retry=$((retry + 1))
        if [ $retry -lt $MAX_RETRIES ]; then
            warn "健康端点检查失败，重试 $retry/$MAX_RETRIES"
            sleep 2
        fi
    done
    
    error "健康端点检查失败，已重试 $MAX_RETRIES 次"
    return 1
}

# 检查基础页面
check_basic_page() {
    local retry=0
    local base_url="http://localhost:${PORT:-3000}"
    
    while [ $retry -lt $MAX_RETRIES ]; do
        if curl -f -s --max-time $TIMEOUT "$base_url" > /dev/null 2>&1; then
            log "基础页面响应正常"
            return 0
        fi
        
        retry=$((retry + 1))
        if [ $retry -lt $MAX_RETRIES ]; then
            warn "基础页面检查失败，重试 $retry/$MAX_RETRIES"
            sleep 2
        fi
    done
    
    error "基础页面检查失败，已重试 $MAX_RETRIES 次"
    return 1
}

# 检查内存使用
check_memory_usage() {
    local mem_usage=$(ps -o pid,ppid,%mem,cmd -p $(pgrep -f "node.*server.js") | tail -n +2 | awk '{print $3}')
    if [ -n "$mem_usage" ]; then
        # 检查内存使用是否超过 90%
        if [ $(echo "$mem_usage > 90" | bc 2>/dev/null || echo 0) -eq 1 ]; then
            warn "内存使用率较高: ${mem_usage}%"
        else
            log "内存使用率正常: ${mem_usage}%"
        fi
    fi
    return 0
}

# 检查后端 API 连接
check_api_connection() {
    if [ -n "$NEXT_PUBLIC_API_BASE_URL" ]; then
        local api_health_url="${NEXT_PUBLIC_API_BASE_URL}/actuator/health"
        if curl -f -s --max-time $TIMEOUT "$api_health_url" > /dev/null 2>&1; then
            log "后端 API 连接正常"
        else
            warn "后端 API 连接失败: $api_health_url"
            # 前端可以继续运行，即使后端暂时不可用
        fi
    fi
    return 0
}

# 主健康检查函数
main() {
    log "开始前端健康检查..."
    
    # 基础检查
    if ! check_node_process; then
        exit 1
    fi
    
    if ! check_port; then
        exit 1
    fi
    
    # 应用健康检查
    if ! check_health_endpoint && ! check_basic_page; then
        error "应用无响应"
        exit 1
    fi
    
    # 资源使用检查
    check_memory_usage
    
    # 依赖服务检查（非强制）
    check_api_connection
    
    log "前端健康检查通过 ✓"
    exit 0
}

# 执行主函数
main "$@"