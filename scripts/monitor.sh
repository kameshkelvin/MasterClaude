#!/bin/bash

# 在线考试系统监控脚本
# 提供实时监控、性能分析和告警功能

set -e

# ====================
# 全局变量配置
# ====================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${PROJECT_ROOT}/logs/monitor-${TIMESTAMP}.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# 监控配置
COMPOSE_FILE="docker-compose.yml"
ALERT_EMAIL=""
SLACK_WEBHOOK=""
CHECK_INTERVAL=30
METRICS_RETENTION_DAYS=30
DASHBOARD_PORT=8888

# 阈值配置
CPU_THRESHOLD=80
MEMORY_THRESHOLD=85
DISK_THRESHOLD=90
RESPONSE_TIME_THRESHOLD=5000
ERROR_RATE_THRESHOLD=5

# ====================
# 日志和输出函数
# ====================

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARN:${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" | tee -a "$LOG_FILE"
}

alert() {
    echo -e "${PURPLE}[$(date +'%Y-%m-%d %H:%M:%S')] ALERT:${NC} $1" | tee -a "$LOG_FILE"
    send_alert "$1"
}

debug() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] DEBUG:${NC} $1" | tee -a "$LOG_FILE"
    fi
}

# ====================
# 帮助信息
# ====================

show_help() {
    cat << EOF
在线考试系统监控工具

用法: $0 [选项] <操作>

操作:
    status      显示服务状态
    health      健康检查
    metrics     性能指标
    logs        查看日志
    dashboard   启动监控面板
    watch       持续监控模式
    alert       测试告警系统

选项:
    -h, --help          显示帮助信息
    -v, --verbose       详细输出模式
    -i, --interval <秒>  监控间隔 (默认: 30)
    --compose-file <文件> 指定 docker-compose 文件
    --email <邮箱>      告警邮箱地址
    --slack <webhook>   Slack 告警 webhook
    --no-color          禁用颜色输出

示例:
    $0 status                   # 显示服务状态
    $0 watch -i 10             # 每10秒监控一次
    $0 dashboard               # 启动监控面板
    $0 metrics --verbose       # 详细性能指标

EOF
}

# ====================
# 参数解析
# ====================

parse_arguments() {
    OPERATION=""
    VERBOSE=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -i|--interval)
                CHECK_INTERVAL="$2"
                shift 2
                ;;
            --compose-file)
                COMPOSE_FILE="$2"
                shift 2
                ;;
            --email)
                ALERT_EMAIL="$2"
                shift 2
                ;;
            --slack)
                SLACK_WEBHOOK="$2"
                shift 2
                ;;
            --no-color)
                RED=""
                GREEN=""
                YELLOW=""
                BLUE=""
                PURPLE=""
                NC=""
                shift
                ;;
            status|health|metrics|logs|dashboard|watch|alert)
                OPERATION="$1"
                shift
                ;;
            *)
                error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    if [ -z "$OPERATION" ]; then
        error "请指定操作"
        show_help
        exit 1
    fi
}

# ====================
# 告警函数
# ====================

send_alert() {
    local message="$1"
    local timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    
    # 邮件告警
    if [ -n "$ALERT_EMAIL" ]; then
        echo "告警时间: $timestamp" | mail -s "考试系统告警" "$ALERT_EMAIL" || warn "邮件发送失败"
    fi
    
    # Slack 告警
    if [ -n "$SLACK_WEBHOOK" ]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"🚨 考试系统告警\\n时间: $timestamp\\n详情: $message\"}" \
            "$SLACK_WEBHOOK" || warn "Slack 通知发送失败"
    fi
}

# ====================
# 服务状态检查
# ====================

check_service_status() {
    log "检查服务状态..."
    
    local services=(
        "mysql-primary:MySQL数据库"
        "redis-master:Redis缓存"
        "rabbitmq:消息队列"
        "elasticsearch:搜索引擎"
        "user-service:用户服务"
        "exam-service:考试服务"
        "question-service:题库服务"
        "proctoring-service:监考服务"
        "frontend:前端应用"
        "api-gateway:API网关"
    )
    
    local failed_services=0
    
    echo
    printf "%-20s %-15s %-10s %-15s\n" "服务名称" "状态" "健康度" "运行时间"
    echo "================================================================="
    
    for service_info in "${services[@]}"; do
        IFS=':' read -r service_name display_name <<< "$service_info"
        
        local status="停止"
        local health="未知"
        local uptime="N/A"
        local color=$RED
        
        if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q "$service_name" &> /dev/null; then
            local container_id=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q "$service_name")
            
            if [ -n "$container_id" ]; then
                local container_status=$(docker inspect --format='{{.State.Status}}' "$container_id")
                
                if [ "$container_status" = "running" ]; then
                    status="运行中"
                    color=$GREEN
                    
                    # 获取运行时间
                    uptime=$(docker inspect --format='{{.State.StartedAt}}' "$container_id" | xargs date -d | xargs -I {} bash -c 'echo $(( ($(date +%s) - $(date -d "{}" +%s)) / 60 ))分钟')
                    
                    # 健康检查
                    health=$(get_service_health "$service_name")
                else
                    failed_services=$((failed_services + 1))
                fi
            fi
        else
            failed_services=$((failed_services + 1))
        fi
        
        printf "${color}%-20s %-15s %-10s %-15s${NC}\n" "$display_name" "$status" "$health" "$uptime"
    done
    
    echo
    if [ $failed_services -eq 0 ]; then
        log "✅ 所有服务运行正常"
    else
        alert "❌ 有 $failed_services 个服务异常"
    fi
    
    return $failed_services
}

# ====================
# 服务健康检查
# ====================

get_service_health() {
    local service_name="$1"
    
    case $service_name in
        mysql-primary)
            if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T "$service_name" mysqladmin ping -h localhost &> /dev/null; then
                echo "健康"
            else
                echo "异常"
            fi
            ;;
        redis-master)
            if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T "$service_name" redis-cli ping &> /dev/null; then
                echo "健康"
            else
                echo "异常"
            fi
            ;;
        *-service|frontend|api-gateway)
            local port=$(get_service_port "$service_name")
            if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T "$service_name" nc -z localhost "$port" &> /dev/null; then
                echo "健康"
            else
                echo "异常"
            fi
            ;;
        *)
            echo "未知"
            ;;
    esac
}

get_service_port() {
    case $1 in
        *-service) echo "8080" ;;
        frontend) echo "3000" ;;
        api-gateway) echo "80" ;;
        *) echo "80" ;;
    esac
}

# ====================
# 性能指标收集
# ====================

collect_metrics() {
    log "收集性能指标..."
    
    local metrics_file="${PROJECT_ROOT}/logs/metrics-$(date +%Y%m%d-%H%M%S).json"
    
    cat > "$metrics_file" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "system": $(get_system_metrics),
  "docker": $(get_docker_metrics),
  "services": $(get_service_metrics),
  "database": $(get_database_metrics),
  "application": $(get_application_metrics)
}
EOF
    
    if [ "$VERBOSE" = true ]; then
        log "性能指标已保存到: $metrics_file"
        jq '.' "$metrics_file" 2>/dev/null || cat "$metrics_file"
    fi
    
    # 检查阈值
    check_thresholds "$metrics_file"
}

get_system_metrics() {
    cat << EOF
{
  "cpu_usage": $(top -bn1 | grep "Cpu(s)" | awk '{print $2+$4}' | sed 's/%us,//'),
  "memory": {
    "total": $(free -m | awk 'NR==2{printf "%.2f", $2/1024}'),
    "used": $(free -m | awk 'NR==2{printf "%.2f", $3/1024}'),
    "usage_percent": $(free | awk 'NR==2{printf "%.2f", $3*100/$2}')
  },
  "disk": {
    "total": $(df -BG ${PROJECT_ROOT} | awk 'NR==2{print $2}' | sed 's/G//'),
    "used": $(df -BG ${PROJECT_ROOT} | awk 'NR==2{print $3}' | sed 's/G//'),
    "usage_percent": $(df ${PROJECT_ROOT} | awk 'NR==2{print $5}' | sed 's/%//')
  },
  "load_average": "$(uptime | awk -F'load average:' '{print $2}' | tr -d ' ')"
}
EOF
}

get_docker_metrics() {
    local containers=$(docker ps --format "table {{.Names}}" | tail -n +2 | tr '\n' ' ')
    
    echo "["
    local first=true
    for container in $containers; do
        if [ "$first" = false ]; then
            echo ","
        fi
        first=false
        
        local stats=$(docker stats --no-stream --format "table {{.CPUPerc}},{{.MemUsage}},{{.NetIO}},{{.BlockIO}}" "$container" | tail -n +2)
        IFS=',' read -r cpu mem net block <<< "$stats"
        
        cat << EOF
    {
      "name": "$container",
      "cpu_percent": "$(echo $cpu | sed 's/%//')",
      "memory_usage": "$mem",
      "network_io": "$net",
      "block_io": "$block"
    }
EOF
    done
    echo "]"
}

get_service_metrics() {
    cat << EOF
{
  "response_times": {
    "api_gateway": $(measure_response_time "http://localhost/health"),
    "user_service": $(measure_response_time "http://localhost:8080/actuator/health"),
    "exam_service": $(measure_response_time "http://localhost:8081/actuator/health"),
    "frontend": $(measure_response_time "http://localhost:3000/api/health")
  },
  "error_rates": {
    "last_hour": $(calculate_error_rate 60)
  }
}
EOF
}

get_database_metrics() {
    if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary mysql -e "SELECT 1" &> /dev/null; then
        local connections=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary mysql -e "SHOW STATUS LIKE 'Threads_connected'" | tail -n +2 | awk '{print $2}')
        local queries=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary mysql -e "SHOW STATUS LIKE 'Queries'" | tail -n +2 | awk '{print $2}')
        
        cat << EOF
{
  "connections": $connections,
  "queries": $queries,
  "status": "healthy"
}
EOF
    else
        echo '{"status": "unhealthy"}'
    fi
}

get_application_metrics() {
    cat << EOF
{
  "active_exams": $(count_active_exams),
  "concurrent_users": $(count_concurrent_users),
  "system_load": "normal"
}
EOF
}

measure_response_time() {
    local url="$1"
    local time=$(curl -o /dev/null -s -w '%{time_total}' --max-time 10 "$url" 2>/dev/null || echo "timeout")
    
    if [ "$time" = "timeout" ]; then
        echo "null"
    else
        echo "$(echo "$time * 1000" | bc -l | cut -d. -f1)"
    fi
}

calculate_error_rate() {
    # 简化版错误率计算（实际应该从日志中分析）
    echo "0"
}

count_active_exams() {
    # 简化版活跃考试计数
    echo "0"
}

count_concurrent_users() {
    # 简化版并发用户计数
    echo "0"
}

# ====================
# 阈值检查
# ====================

check_thresholds() {
    local metrics_file="$1"
    
    if ! command -v jq &> /dev/null; then
        warn "jq 未安装，跳过阈值检查"
        return
    fi
    
    # CPU 使用率检查
    local cpu_usage=$(jq -r '.system.cpu_usage // 0' "$metrics_file" | cut -d. -f1)
    if [ "$cpu_usage" -gt "$CPU_THRESHOLD" ]; then
        alert "CPU 使用率过高: ${cpu_usage}% (阈值: ${CPU_THRESHOLD}%)"
    fi
    
    # 内存使用率检查
    local mem_usage=$(jq -r '.system.memory.usage_percent // 0' "$metrics_file" | cut -d. -f1)
    if [ "$mem_usage" -gt "$MEMORY_THRESHOLD" ]; then
        alert "内存使用率过高: ${mem_usage}% (阈值: ${MEMORY_THRESHOLD}%)"
    fi
    
    # 磁盘使用率检查
    local disk_usage=$(jq -r '.system.disk.usage_percent // 0' "$metrics_file")
    if [ "$disk_usage" -gt "$DISK_THRESHOLD" ]; then
        alert "磁盘使用率过高: ${disk_usage}% (阈值: ${DISK_THRESHOLD}%)"
    fi
    
    # 响应时间检查
    local api_response_time=$(jq -r '.services.response_times.api_gateway // 0' "$metrics_file")
    if [ "$api_response_time" != "null" ] && [ "$api_response_time" -gt "$RESPONSE_TIME_THRESHOLD" ]; then
        alert "API 响应时间过长: ${api_response_time}ms (阈值: ${RESPONSE_TIME_THRESHOLD}ms)"
    fi
}

# ====================
# 日志查看
# ====================

view_logs() {
    log "查看服务日志..."
    
    echo "选择要查看的服务日志:"
    echo "1) 所有服务"
    echo "2) API 网关"
    echo "3) 用户服务"
    echo "4) 考试服务"
    echo "5) 数据库"
    echo "6) 系统日志"
    
    read -p "请选择 (1-6): " choice
    
    case $choice in
        1)
            docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" logs --tail=100 -f
            ;;
        2)
            docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" logs --tail=100 -f api-gateway
            ;;
        3)
            docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" logs --tail=100 -f user-service
            ;;
        4)
            docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" logs --tail=100 -f exam-service
            ;;
        5)
            docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" logs --tail=100 -f mysql-primary
            ;;
        6)
            tail -f "${PROJECT_ROOT}/logs/deploy-"*.log 2>/dev/null || echo "没有找到系统日志文件"
            ;;
        *)
            error "无效选择"
            exit 1
            ;;
    esac
}

# ====================
# 监控面板
# ====================

start_dashboard() {
    log "启动监控面板..."
    
    # 创建简单的 HTML 监控面板
    local dashboard_file="${PROJECT_ROOT}/logs/dashboard.html"
    
    cat > "$dashboard_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>在线考试系统监控面板</title>
    <meta charset="utf-8">
    <meta http-equiv="refresh" content="30">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .header { background: #2c3e50; color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
        .metric-card { background: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
        .metric-title { font-size: 18px; font-weight: bold; margin-bottom: 10px; color: #2c3e50; }
        .metric-value { font-size: 24px; font-weight: bold; }
        .status-ok { color: #27ae60; }
        .status-warning { color: #f39c12; }
        .status-error { color: #e74c3c; }
        .timestamp { text-align: center; margin-top: 20px; color: #7f8c8d; }
    </style>
</head>
<body>
    <div class="header">
        <h1>在线考试系统监控面板</h1>
        <p>实时监控系统运行状态和性能指标</p>
    </div>
    
    <div class="metrics">
        <div class="metric-card">
            <div class="metric-title">服务状态</div>
            <div class="metric-value status-ok" id="service-status">检查中...</div>
        </div>
        
        <div class="metric-card">
            <div class="metric-title">CPU 使用率</div>
            <div class="metric-value" id="cpu-usage">---%</div>
        </div>
        
        <div class="metric-card">
            <div class="metric-title">内存使用率</div>
            <div class="metric-value" id="memory-usage">---%</div>
        </div>
        
        <div class="metric-card">
            <div class="metric-title">响应时间</div>
            <div class="metric-value" id="response-time">---ms</div>
        </div>
        
        <div class="metric-card">
            <div class="metric-title">活跃考试</div>
            <div class="metric-value" id="active-exams">---</div>
        </div>
        
        <div class="metric-card">
            <div class="metric-title">并发用户</div>
            <div class="metric-value" id="concurrent-users">---</div>
        </div>
    </div>
    
    <div class="timestamp">
        最后更新: <span id="last-update">$(date)</span>
    </div>
    
    <script>
        // 这里可以添加 JavaScript 来动态更新数据
        setTimeout(function() {
            location.reload();
        }, 30000);
    </script>
</body>
</html>
EOF
    
    log "监控面板已生成: $dashboard_file"
    
    # 启动简单的 HTTP 服务器
    if command -v python3 &> /dev/null; then
        log "在端口 $DASHBOARD_PORT 启动监控面板服务器..."
        cd "${PROJECT_ROOT}/logs"
        python3 -m http.server $DASHBOARD_PORT &
        local server_pid=$!
        
        log "监控面板已启动: http://localhost:$DASHBOARD_PORT/dashboard.html"
        log "按 Ctrl+C 停止服务器"
        
        trap "kill $server_pid 2>/dev/null || true" EXIT
        wait $server_pid
    else
        log "请安装 Python 3 以启动 Web 服务器，或直接打开文件: $dashboard_file"
    fi
}

# ====================
# 持续监控模式
# ====================

watch_mode() {
    log "启动持续监控模式 (间隔: ${CHECK_INTERVAL}秒)"
    log "按 Ctrl+C 停止监控"
    
    trap "log '监控已停止'; exit 0" INT TERM
    
    while true; do
        clear
        echo -e "${BLUE}=== 在线考试系统实时监控 ===${NC}"
        echo "时间: $(date)"
        echo "监控间隔: ${CHECK_INTERVAL}秒"
        echo
        
        # 服务状态
        check_service_status > /dev/null
        
        # 性能指标
        collect_metrics > /dev/null
        
        echo
        echo -e "${YELLOW}下次检查: $(date -d "+${CHECK_INTERVAL} seconds")${NC}"
        
        sleep $CHECK_INTERVAL
    done
}

# ====================
# 测试告警
# ====================

test_alert() {
    log "测试告警系统..."
    
    alert "这是一条测试告警信息"
    
    log "告警测试完成"
}

# ====================
# 主函数
# ====================

main() {
    # 创建日志目录
    mkdir -p "${PROJECT_ROOT}/logs"
    
    log "启动监控系统..."
    log "操作: $OPERATION"
    
    case $OPERATION in
        status)
            check_service_status
            ;;
        health)
            check_service_status
            collect_metrics
            ;;
        metrics)
            collect_metrics
            ;;
        logs)
            view_logs
            ;;
        dashboard)
            start_dashboard
            ;;
        watch)
            watch_mode
            ;;
        alert)
            test_alert
            ;;
        *)
            error "未知操作: $OPERATION"
            exit 1
            ;;
    esac
    
    log "监控操作完成"
}

# 解析参数并执行
parse_arguments "$@"
main