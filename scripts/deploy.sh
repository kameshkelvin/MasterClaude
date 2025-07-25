#!/bin/bash

# 在线考试系统部署脚本
# 支持开发环境、测试环境和生产环境的一键部署

set -e

# ====================
# 全局变量配置
# ====================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${PROJECT_ROOT}/logs/deploy-${TIMESTAMP}.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认配置
ENVIRONMENT="development"
COMPOSE_FILE="docker-compose.yml"
BACKUP_ENABLED=true
HEALTH_CHECK_TIMEOUT=300
VERBOSE=false
DRY_RUN=false

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
在线考试系统部署脚本

用法: $0 [选项] <环境>

环境:
    dev         开发环境部署
    staging     测试环境部署  
    prod        生产环境部署

选项:
    -h, --help          显示帮助信息
    -v, --verbose       详细输出模式
    -n, --dry-run       预演模式，不执行实际部署
    --no-backup         跳过数据备份
    --force             强制部署，跳过确认
    --timeout <秒>      健康检查超时时间 (默认: 300)
    --compose-file <文件> 指定 docker-compose 文件

示例:
    $0 dev                      # 部署开发环境
    $0 staging --verbose        # 详细模式部署测试环境
    $0 prod --no-backup --force # 强制部署生产环境（不备份）

EOF
}

# ====================
# 参数解析
# ====================

parse_arguments() {
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
            -n|--dry-run)
                DRY_RUN=true
                shift
                ;;
            --no-backup)
                BACKUP_ENABLED=false
                shift
                ;;
            --force)
                FORCE_DEPLOY=true
                shift
                ;;
            --timeout)
                HEALTH_CHECK_TIMEOUT="$2"
                shift 2
                ;;
            --compose-file)
                COMPOSE_FILE="$2"
                shift 2
                ;;
            dev|development)
                ENVIRONMENT="development"
                shift
                ;;
            staging|test)
                ENVIRONMENT="staging"
                shift
                ;;
            prod|production)
                ENVIRONMENT="production"
                shift
                ;;
            *)
                error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# ====================
# 环境检查函数
# ====================

check_prerequisites() {
    log "检查部署环境前置条件..."
    
    # 检查必要的命令
    local required_commands=("docker" "docker-compose" "curl" "jq")
    
    for cmd in "${required_commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            error "缺少必要命令: $cmd"
            exit 1
        fi
        debug "✓ $cmd 命令可用"
    done
    
    # 检查 Docker 服务
    if ! docker info &> /dev/null; then
        error "Docker 服务未运行"
        exit 1
    fi
    debug "✓ Docker 服务正常"
    
    # 检查环境变量文件
    local env_file="${PROJECT_ROOT}/.env"
    if [ ! -f "$env_file" ]; then
        warn "环境变量文件不存在，从模板创建..."
        cp "${PROJECT_ROOT}/.env.example" "$env_file"
        warn "请编辑 .env 文件设置正确的环境变量"
    fi
    
    # 检查磁盘空间
    local available_space=$(df "${PROJECT_ROOT}" | awk 'NR==2 {print $4}')
    local required_space=5242880  # 5GB in KB
    
    if [ "$available_space" -lt "$required_space" ]; then
        error "磁盘空间不足，至少需要 5GB 可用空间"
        exit 1
    fi
    debug "✓ 磁盘空间充足 ($(($available_space / 1024 / 1024))GB 可用)"
    
    log "环境检查完成"
}

# ====================
# 数据备份函数
# ====================

backup_data() {
    if [ "$BACKUP_ENABLED" = false ]; then
        warn "跳过数据备份"
        return 0
    fi
    
    log "开始数据备份..."
    
    local backup_dir="${PROJECT_ROOT}/backups/${TIMESTAMP}"
    mkdir -p "$backup_dir"
    
    # 检查是否有运行的服务
    if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q mysql-primary &> /dev/null; then
        log "备份 MySQL 数据库..."
        
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
            mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" --all-databases --routines --triggers \
            > "${backup_dir}/mysql_backup.sql"
        
        if [ $? -eq 0 ]; then
            log "✓ MySQL 备份完成"
        else
            error "MySQL 备份失败"
            return 1
        fi
        
        # 备份 Redis 数据
        log "备份 Redis 数据..."
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T redis-master \
            redis-cli --rdb /tmp/redis_backup.rdb
        
        docker cp $(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q redis-master):/tmp/redis_backup.rdb \
            "${backup_dir}/redis_backup.rdb"
        
        if [ $? -eq 0 ]; then
            log "✓ Redis 备份完成"
        else
            warn "Redis 备份失败，继续部署"
        fi
    else
        log "没有运行的数据库服务，跳过备份"
    fi
    
    # 压缩备份文件
    log "压缩备份文件..."
    tar -czf "${PROJECT_ROOT}/backups/backup_${TIMESTAMP}.tar.gz" -C "${PROJECT_ROOT}/backups" "${TIMESTAMP}"
    rm -rf "$backup_dir"
    
    log "数据备份完成: backup_${TIMESTAMP}.tar.gz"
}

# ====================
# 部署确认函数
# ====================

confirm_deployment() {
    if [ "$FORCE_DEPLOY" = true ]; then
        return 0
    fi
    
    echo
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}         部署确认信息${NC}"
    echo -e "${YELLOW}========================================${NC}"
    echo -e "环境:        ${BLUE}$ENVIRONMENT${NC}"
    echo -e "Compose文件: ${BLUE}$COMPOSE_FILE${NC}"
    echo -e "备份启用:    ${BLUE}$BACKUP_ENABLED${NC}"
    echo -e "预演模式:    ${BLUE}$DRY_RUN${NC}"
    echo -e "${YELLOW}========================================${NC}"
    echo
    
    if [ "$ENVIRONMENT" = "production" ]; then
        echo -e "${RED}⚠️  警告: 您即将部署到生产环境！${NC}"
        echo
    fi
    
    read -p "确认继续部署? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log "用户取消部署"
        exit 0
    fi
}

# ====================
# Docker 镜像管理
# ====================

pull_images() {
    log "拉取最新镜像..."
    
    if [ "$DRY_RUN" = true ]; then
        log "[DRY RUN] 将拉取镜像"
        return 0
    fi
    
    # 根据环境选择不同的镜像标签
    case $ENVIRONMENT in
        development)
            export IMAGE_TAG="latest"
            ;;
        staging)
            export IMAGE_TAG="develop"
            ;;
        production)
            export IMAGE_TAG="${RELEASE_VERSION:-latest}"
            ;;
    esac
    
    debug "使用镜像标签: $IMAGE_TAG"
    
    # 拉取镜像
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" pull --quiet
    
    if [ $? -eq 0 ]; then
        log "✓ 镜像拉取完成"
    else
        error "镜像拉取失败"
        exit 1
    fi
}

# ====================
# 服务部署函数
# ====================

deploy_services() {
    log "开始部署服务..."
    
    if [ "$DRY_RUN" = true ]; then
        log "[DRY RUN] 将部署以下服务:"
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" config --services
        return 0
    fi
    
    # 设置环境变量
    export ENVIRONMENT
    export COMPOSE_PROJECT_NAME="exam-system-${ENVIRONMENT}"
    
    # 创建必要的目录
    mkdir -p "${PROJECT_ROOT}/logs"
    mkdir -p "${PROJECT_ROOT}/ssl"
    
    # 滚动更新部署
    log "启动基础设施服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d \
        mysql-primary redis-master rabbitmq elasticsearch minio
    
    # 等待基础服务就绪
    wait_for_service "mysql-primary" "3306" "MySQL"
    wait_for_service "redis-master" "6379" "Redis"
    wait_for_service "rabbitmq" "5672" "RabbitMQ"
    wait_for_service "elasticsearch" "9200" "Elasticsearch"
    wait_for_service "minio" "9000" "MinIO"
    
    log "启动应用服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d \
        user-service exam-service question-service proctoring-service ai-service
    
    # 等待应用服务就绪
    wait_for_service "user-service" "8080" "用户服务"
    wait_for_service "exam-service" "8080" "考试服务"
    wait_for_service "question-service" "8080" "题库服务"
    wait_for_service "proctoring-service" "8080" "监考服务"
    wait_for_service "ai-service" "5000" "AI服务"
    
    log "启动前端和网关服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d \
        frontend api-gateway
    
    # 等待前端服务就绪
    wait_for_service "frontend" "3000" "前端应用"
    wait_for_service "api-gateway" "80" "API网关"
    
    log "启动监控服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d \
        prometheus grafana logstash kibana filebeat
    
    log "✓ 所有服务部署完成"
}

# ====================
# 服务等待函数
# ====================

wait_for_service() {
    local service_name=$1
    local port=$2
    local display_name=$3
    local timeout=${HEALTH_CHECK_TIMEOUT:-300}
    local counter=0
    
    log "等待 $display_name 服务就绪..."
    
    while [ $counter -lt $timeout ]; do
        if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T "$service_name" \
           nc -z localhost "$port" &> /dev/null; then
            log "✓ $display_name 服务已就绪"
            return 0
        fi
        
        sleep 5
        counter=$((counter + 5))
        
        if [ $((counter % 30)) -eq 0 ]; then
            debug "$display_name 服务等待中... (${counter}s/${timeout}s)"
        fi
    done
    
    error "$display_name 服务启动超时"
    show_service_logs "$service_name"
    return 1
}

# ====================
# 健康检查函数
# ====================

health_check() {
    log "执行系统健康检查..."
    
    local endpoints=(
        "http://localhost/health:API网关"
        "http://localhost:8080/actuator/health:用户服务"
        "http://localhost:8081/actuator/health:考试服务"
        "http://localhost:8082/actuator/health:题库服务"
        "http://localhost:8083/actuator/health:监考服务"
        "http://localhost:3000/api/health:前端应用"
        "http://localhost:5000/health:AI服务"
    )
    
    local failed_checks=0
    
    for endpoint_info in "${endpoints[@]}"; do
        IFS=':' read -r url name <<< "$endpoint_info"
        
        debug "检查 $name ($url)"
        
        if curl -f -s --max-time 10 "$url" &> /dev/null; then
            log "✓ $name 健康检查通过"
        else
            error "✗ $name 健康检查失败"
            failed_checks=$((failed_checks + 1))
        fi
    done
    
    if [ $failed_checks -eq 0 ]; then
        log "🎉 所有服务健康检查通过！"
        return 0
    else
        error "有 $failed_checks 个服务健康检查失败"
        return 1
    fi
}

# ====================
# 服务日志显示
# ====================

show_service_logs() {
    local service_name=$1
    local lines=${2:-50}
    
    echo
    echo -e "${YELLOW}========== $service_name 服务日志 (最近 $lines 行) ==========${NC}"
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" logs --tail "$lines" "$service_name"
    echo -e "${YELLOW}================================================================${NC}"
    echo
}

# ====================
# 部署后处理
# ====================

post_deployment() {
    log "执行部署后处理..."
    
    # 显示部署信息
    show_deployment_info
    
    # 清理旧的镜像和容器
    if [ "$ENVIRONMENT" != "development" ]; then
        log "清理旧的 Docker 资源..."
        docker system prune -f --volumes
        log "✓ Docker 资源清理完成"
    fi
    
    # 创建部署记录
    create_deployment_record
    
    log "部署后处理完成"
}

# ====================
# 显示部署信息
# ====================

show_deployment_info() {
    local external_ip=$(curl -s http://checkip.amazonaws.com/ || echo "localhost")
    
    echo
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}         部署完成信息${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "环境:           ${BLUE}$ENVIRONMENT${NC}"
    echo -e "部署时间:       ${BLUE}$(date)${NC}"
    echo -e "项目版本:       ${BLUE}${IMAGE_TAG:-latest}${NC}"
    echo
    echo -e "${GREEN}🌐 访问地址:${NC}"
    echo -e "前端应用:       ${BLUE}http://$external_ip:3000${NC}"
    echo -e "API网关:        ${BLUE}http://$external_ip${NC}"
    echo -e "Grafana监控:    ${BLUE}http://$external_ip:3001${NC}"
    echo -e "Kibana日志:     ${BLUE}http://$external_ip:5601${NC}"
    echo -e "MinIO存储:      ${BLUE}http://$external_ip:9001${NC}"
    echo
    echo -e "${GREEN}📊 监控端点:${NC}"
    echo -e "Prometheus:     ${BLUE}http://$external_ip:9090${NC}"
    echo -e "RabbitMQ管理:   ${BLUE}http://$external_ip:15672${NC}"
    echo
    echo -e "${GREEN}🔧 管理命令:${NC}"
    echo -e "查看服务状态:   ${BLUE}docker-compose ps${NC}"
    echo -e "查看服务日志:   ${BLUE}docker-compose logs -f [service]${NC}"
    echo -e "停止所有服务:   ${BLUE}docker-compose down${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo
}

# ====================
# 创建部署记录
# ====================

create_deployment_record() {
    local record_file="${PROJECT_ROOT}/deployments.log"
    
    cat >> "$record_file" << EOF
========================================
部署记录 - $(date)
========================================
环境: $ENVIRONMENT
版本: ${IMAGE_TAG:-latest}
操作者: ${USER:-unknown}
主机: $(hostname)
Git提交: $(git rev-parse HEAD 2>/dev/null || echo "unknown")
Git分支: $(git branch --show-current 2>/dev/null || echo "unknown")
备份文件: ${BACKUP_ENABLED:+backup_${TIMESTAMP}.tar.gz}
日志文件: deploy-${TIMESTAMP}.log
========================================

EOF
    
    debug "部署记录已保存到 $record_file"
}

# ====================
# 错误处理
# ====================

handle_error() {
    local exit_code=$?
    error "部署过程中发生错误 (退出码: $exit_code)"
    
    echo
    echo -e "${RED}🚨 部署失败！请检查以下信息:${NC}"
    echo -e "1. 查看部署日志: ${BLUE}$LOG_FILE${NC}"
    echo -e "2. 检查服务状态: ${BLUE}docker-compose ps${NC}"
    echo -e "3. 查看服务日志: ${BLUE}docker-compose logs${NC}"
    echo
    
    if [ "$BACKUP_ENABLED" = true ] && [ -f "${PROJECT_ROOT}/backups/backup_${TIMESTAMP}.tar.gz" ]; then
        echo -e "${YELLOW}💾 数据备份文件: backup_${TIMESTAMP}.tar.gz${NC}"
        echo -e "如需回滚，请使用备份文件恢复数据"
        echo
    fi
    
    exit $exit_code
}

# ====================
# 主函数
# ====================

main() {
    # 设置错误处理
    trap handle_error ERR
    
    # 创建日志目录
    mkdir -p "${PROJECT_ROOT}/logs"
    
    log "开始部署在线考试系统..."
    log "脚本版本: 1.0.0"
    log "执行用户: ${USER:-unknown}"
    log "执行主机: $(hostname)"
    
    # 解析命令行参数
    parse_arguments "$@"
    
    # 显示配置信息
    debug "部署配置:"
    debug "  环境: $ENVIRONMENT"
    debug "  Compose文件: $COMPOSE_FILE"
    debug "  备份启用: $BACKUP_ENABLED"
    debug "  预演模式: $DRY_RUN"
    debug "  详细输出: $VERBOSE"
    
    # 执行部署流程
    check_prerequisites
    confirm_deployment
    backup_data
    pull_images
    deploy_services
    
    if [ "$DRY_RUN" = false ]; then
        health_check
        post_deployment
        
        log "🎉 在线考试系统部署成功完成！"
        log "部署日志已保存到: $LOG_FILE"
    else
        log "✓ 预演模式完成，未执行实际部署"
    fi
}

# 执行主函数
main "$@"