#!/bin/bash

# 在线考试系统回滚脚本
# 提供快速回滚、服务恢复和灾难恢复功能

set -e

# ====================
# 全局变量配置
# ====================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${PROJECT_ROOT}/logs/rollback-${TIMESTAMP}.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# 回滚配置
COMPOSE_FILE="docker-compose.yml"
BACKUP_DIR="${PROJECT_ROOT}/backups"
ROLLBACK_TIMEOUT=300
VERIFICATION_ENABLED=true
AUTO_BACKUP_BEFORE_ROLLBACK=true

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

critical() {
    echo -e "${PURPLE}[$(date +'%Y-%m-%d %H:%M:%S')] CRITICAL:${NC} $1" | tee -a "$LOG_FILE"
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
在线考试系统回滚工具

用法: $0 [选项] <操作> [目标]

操作:
    quick           快速回滚到上一个版本
    version         回滚到指定版本
    database        仅回滚数据库
    code            仅回滚代码
    full            完整系统回滚
    list-versions   列出可用版本
    create-checkpoint 创建回滚检查点
    emergency       紧急恢复模式

选项:
    -h, --help              显示帮助信息
    -v, --verbose           详细输出模式
    -f, --force             强制回滚，跳过确认
    --no-backup             回滚前不创建备份
    --no-verify             跳过回滚后验证
    --timeout <秒>          回滚超时时间 (默认: 300)
    --compose-file <文件>   指定 docker-compose 文件

示例:
    $0 quick                        # 快速回滚到上一版本
    $0 version v1.2.0              # 回滚到指定版本
    $0 database backup_20231201    # 仅回滚数据库
    $0 full --force                # 强制完整回滚
    $0 emergency                   # 紧急恢复模式

EOF
}

# ====================
# 参数解析
# ====================

parse_arguments() {
    OPERATION=""
    TARGET=""
    VERBOSE=false
    FORCE=false
    
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
            -f|--force)
                FORCE=true
                shift
                ;;
            --no-backup)
                AUTO_BACKUP_BEFORE_ROLLBACK=false
                shift
                ;;
            --no-verify)
                VERIFICATION_ENABLED=false
                shift
                ;;
            --timeout)
                ROLLBACK_TIMEOUT="$2"
                shift 2
                ;;
            --compose-file)
                COMPOSE_FILE="$2"
                shift 2
                ;;
            quick|version|database|code|full|list-versions|create-checkpoint|emergency)
                OPERATION="$1"
                if [ -n "$2" ] && [[ ! "$2" =~ ^- ]]; then
                    TARGET="$2"
                    shift
                fi
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
# 环境检查
# ====================

check_prerequisites() {
    log "检查回滚环境..."
    
    # 检查必要命令
    local required_commands=("docker" "docker-compose" "jq" "curl")
    
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
    
    # 检查备份目录
    if [ ! -d "$BACKUP_DIR" ]; then
        error "备份目录不存在: $BACKUP_DIR"
        exit 1
    fi
    
    # 创建日志目录
    mkdir -p "${PROJECT_ROOT}/logs"
    
    log "环境检查完成"
}

# ====================
# 获取当前版本信息
# ====================

get_current_version() {
    # 尝试从多个来源获取版本信息
    local version=""
    
    # 从 Git 获取
    if git -C "$PROJECT_ROOT" rev-parse HEAD &> /dev/null; then
        version=$(git -C "$PROJECT_ROOT" describe --tags --abbrev=7 2>/dev/null || git -C "$PROJECT_ROOT" rev-parse --short HEAD)
    fi
    
    # 从容器镜像标签获取
    if [ -z "$version" ]; then
        version=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" images --format "table {{.Tag}}" | grep -v "TAG" | head -n 1 || echo "unknown")
    fi
    
    echo "$version"
}

# ====================
# 列出可用版本
# ====================

list_versions() {
    log "可用的回滚版本:"
    echo
    
    printf "%-20s %-15s %-20s %-15s\n" "版本/备份" "类型" "创建时间" "状态"
    echo "======================================================================="
    
    # 列出备份版本
    if [ -d "$BACKUP_DIR" ]; then
        for backup in $(ls -1 "$BACKUP_DIR" | grep -E "(full_backup_|incremental_backup_)" | sort -r | head -10); do
            local backup_path="${BACKUP_DIR}/${backup}"
            local backup_type="备份"
            local backup_time="未知"
            local status="可用"
            
            if [ -f "${backup_path}/backup_info.json" ]; then
                backup_time=$(jq -r '.started_at // "unknown"' "${backup_path}/backup_info.json" | cut -dT -f1)
                local success=$(jq -r '.success // false' "${backup_path}/backup_info.json")
                if [ "$success" != "true" ]; then
                    status="损坏"
                fi
            fi
            
            printf "%-20s %-15s %-20s %-15s\n" "$backup" "$backup_type" "$backup_time" "$status"
        done
    fi
    
    # 列出 Git 版本
    if git -C "$PROJECT_ROOT" tag &> /dev/null; then
        echo
        echo "Git 标签版本:"
        git -C "$PROJECT_ROOT" tag --sort=-version:refname | head -5 | while read tag; do
            local tag_date=$(git -C "$PROJECT_ROOT" log -1 --format=%ad --date=short "$tag" 2>/dev/null || echo "未知")
            printf "%-20s %-15s %-20s %-15s\n" "$tag" "Git标签" "$tag_date" "可用"
        done
    fi
    
    echo
    local current_version=$(get_current_version)
    log "当前版本: $current_version"
}

# ====================
# 创建回滚前备份
# ====================

create_pre_rollback_backup() {
    if [ "$AUTO_BACKUP_BEFORE_ROLLBACK" = false ]; then
        return 0
    fi
    
    log "创建回滚前备份..."
    
    local backup_name="pre_rollback_${TIMESTAMP}"
    local backup_path="${BACKUP_DIR}/${backup_name}"
    
    mkdir -p "$backup_path"
    
    # 记录回滚前的状态
    cat > "${backup_path}/pre_rollback_info.json" << EOF
{
  "backup_id": "$backup_name",
  "backup_type": "pre_rollback",
  "current_version": "$(get_current_version)",
  "rollback_target": "$TARGET",
  "rollback_operation": "$OPERATION",
  "created_at": "$(date -Iseconds)",
  "hostname": "$(hostname)",
  "user": "${USER:-unknown}"
}
EOF
    
    # 备份关键配置和数据
    log "备份当前配置..."
    
    # 导出当前数据库状态
    if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q mysql-primary &> /dev/null; then
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
            mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
            --single-transaction \
            --routines \
            --triggers \
            --all-databases \
            > "${backup_path}/pre_rollback_mysql.sql" 2>/dev/null || warn "MySQL 备份失败"
    fi
    
    # 备份 Redis 数据
    if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q redis-master &> /dev/null; then
        local container_id=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q redis-master)
        docker cp "$container_id:/data/dump.rdb" "${backup_path}/pre_rollback_redis.rdb" 2>/dev/null || warn "Redis 备份失败"
    fi
    
    # 备份配置文件
    cp "${PROJECT_ROOT}/.env" "${backup_path}/" 2>/dev/null || true
    cp -r "${PROJECT_ROOT}/config" "${backup_path}/" 2>/dev/null || true
    
    log "✓ 回滚前备份完成: $backup_name"
    echo "$backup_name" > "${PROJECT_ROOT}/.last_pre_rollback_backup"
}

# ====================
# 快速回滚
# ====================

quick_rollback() {
    log "执行快速回滚..."
    
    # 查找最近的备份
    local latest_backup=$(find "$BACKUP_DIR" -name "full_backup_*" -type d | sort | tail -n 1)
    
    if [ -z "$latest_backup" ]; then
        error "未找到可用的备份"
        return 1
    fi
    
    local backup_name=$(basename "$latest_backup")
    log "回滚到最近备份: $backup_name"
    
    # 确认操作
    if [ "$FORCE" != true ]; then
        echo -e "${YELLOW}警告: 即将回滚到 $backup_name${NC}"
        echo "这将覆盖当前的数据和配置！"
        read -p "确认继续? (y/N): " -n 1 -r
        echo
        
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log "用户取消回滚操作"
            return 0
        fi
    fi
    
    # 执行回滚
    rollback_to_backup "$backup_name"
}

# ====================
# 版本回滚
# ====================

version_rollback() {
    local target_version="$TARGET"
    
    if [ -z "$target_version" ]; then
        list_versions
        echo
        read -p "请输入要回滚的版本: " target_version
    fi
    
    log "回滚到版本: $target_version"
    
    # 检查目标版本是否存在
    local backup_path="${BACKUP_DIR}/${target_version}"
    local is_git_tag=false
    
    if [ ! -d "$backup_path" ] && [ ! -f "${backup_path}.tar.gz" ]; then
        # 检查是否为 Git 标签
        if git -C "$PROJECT_ROOT" rev-parse "$target_version" &> /dev/null; then
            is_git_tag=true
        else
            error "版本不存在: $target_version"
            return 1
        fi
    fi
    
    # 确认操作
    if [ "$FORCE" != true ]; then
        echo -e "${YELLOW}警告: 即将回滚到版本 $target_version${NC}"
        read -p "确认继续? (y/N): " -n 1 -r
        echo
        
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log "用户取消回滚操作"
            return 0
        fi
    fi
    
    if [ "$is_git_tag" = true ]; then
        rollback_to_git_version "$target_version"
    else
        rollback_to_backup "$target_version"
    fi
}

# ====================
# 回滚到备份
# ====================

rollback_to_backup() {
    local backup_name="$1"
    local backup_path="${BACKUP_DIR}/${backup_name}"
    
    log "从备份恢复: $backup_name"
    
    # 创建回滚前备份
    create_pre_rollback_backup
    
    # 解压备份（如果需要）
    if [ -f "${backup_path}.tar.gz" ]; then
        log "解压备份文件..."
        cd "$BACKUP_DIR"
        tar -xzf "${backup_name}.tar.gz"
    fi
    
    # 停止服务
    log "停止所有服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" down --timeout 60
    
    # 等待服务完全停止
    sleep 10
    
    # 恢复数据库
    if [ -f "${backup_path}/mysql_backup.sql" ]; then
        log "恢复 MySQL 数据库..."
        
        # 启动 MySQL 服务
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d mysql-primary
        
        # 等待 MySQL 启动
        local mysql_ready=false
        local wait_time=0
        
        while [ $wait_time -lt 120 ]; do
            if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary mysqladmin ping -h localhost &> /dev/null; then
                mysql_ready=true
                break
            fi
            sleep 5
            wait_time=$((wait_time + 5))
            debug "等待 MySQL 启动... (${wait_time}s)"
        done
        
        if [ "$mysql_ready" = true ]; then
            docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
                mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" < "${backup_path}/mysql_backup.sql"
            log "✓ MySQL 数据库恢复完成"
        else
            error "MySQL 启动超时"
            return 1
        fi
    fi
    
    # 恢复 Redis
    if [ -f "${backup_path}/redis_dump.rdb" ]; then
        log "恢复 Redis 数据..."
        
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d redis-master
        sleep 10
        
        local container_id=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q redis-master)
        if [ -n "$container_id" ]; then
            docker stop "$container_id"
            docker cp "${backup_path}/redis_dump.rdb" "$container_id:/data/dump.rdb"
            docker start "$container_id"
            log "✓ Redis 数据恢复完成"
        fi
    fi
    
    # 恢复配置文件
    if [ -f "${backup_path}/.env" ]; then
        log "恢复配置文件..."
        cp "${backup_path}/.env" "${PROJECT_ROOT}/"
        log "✓ 配置文件恢复完成"
    fi
    
    # 恢复其他文件
    if [ -f "${backup_path}/filesystem.tar" ]; then
        log "恢复文件系统..."
        tar -xf "${backup_path}/filesystem.tar" -C "$PROJECT_ROOT"
        log "✓ 文件系统恢复完成"
    fi
    
    # 重启所有服务
    log "启动所有服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d
    
    # 等待服务启动并验证
    if [ "$VERIFICATION_ENABLED" = true ]; then
        verify_rollback
    fi
    
    log "🎉 回滚完成"
}

# ====================
# 回滚到 Git 版本
# ====================

rollback_to_git_version() {
    local git_version="$1"
    
    log "回滚到 Git 版本: $git_version"
    
    # 创建回滚前备份
    create_pre_rollback_backup
    
    # 停止服务
    log "停止所有服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" down --timeout 60
    
    # 切换到目标版本
    log "切换代码版本..."
    cd "$PROJECT_ROOT"
    
    if ! git checkout "$git_version"; then
        error "Git 版本切换失败"
        return 1
    fi
    
    log "✓ 代码版本切换完成"
    
    # 重新构建镜像
    log "重新构建 Docker 镜像..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" build --no-cache
    
    # 启动服务
    log "启动所有服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d
    
    # 验证回滚
    if [ "$VERIFICATION_ENABLED" = true ]; then
        verify_rollback
    fi
    
    log "🎉 Git 版本回滚完成"
}

# ====================
# 数据库回滚
# ====================

database_rollback() {
    local backup_name="$TARGET"
    
    if [ -z "$backup_name" ]; then
        list_versions
        echo
        read -p "请输入数据库备份名称: " backup_name
    fi
    
    local backup_path="${BACKUP_DIR}/${backup_name}"
    
    if [ ! -f "${backup_path}/mysql_backup.sql" ]; then
        error "数据库备份不存在: ${backup_path}/mysql_backup.sql"
        return 1
    fi
    
    log "仅回滚数据库: $backup_name"
    
    # 确认操作
    if [ "$FORCE" != true ]; then
        echo -e "${YELLOW}警告: 即将回滚数据库到 $backup_name${NC}"
        echo "这将覆盖当前的数据库数据！"
        read -p "确认继续? (y/N): " -n 1 -r
        echo
        
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log "用户取消数据库回滚操作"
            return 0
        fi
    fi
    
    # 创建当前数据库备份
    local current_backup="${BACKUP_DIR}/db_pre_rollback_${TIMESTAMP}.sql"
    log "备份当前数据库..."
    
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
        mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
        --single-transaction \
        --routines \
        --triggers \
        --all-databases \
        > "$current_backup"
    
    log "✓ 当前数据库已备份到: $(basename "$current_backup")"
    
    # 执行数据库回滚
    log "恢复数据库..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" < "${backup_path}/mysql_backup.sql"
    
    log "✓ 数据库回滚完成"
}

# ====================
# 创建检查点
# ====================

create_checkpoint() {
    log "创建回滚检查点..."
    
    local checkpoint_name="checkpoint_${TIMESTAMP}"
    local checkpoint_path="${BACKUP_DIR}/${checkpoint_name}"
    
    mkdir -p "$checkpoint_path"
    
    # 记录检查点信息
    cat > "${checkpoint_path}/checkpoint_info.json" << EOF
{
  "checkpoint_id": "$checkpoint_name",
  "checkpoint_type": "manual",
  "current_version": "$(get_current_version)",
  "created_at": "$(date -Iseconds)",
  "hostname": "$(hostname)",
  "user": "${USER:-unknown}",
  "services_status": $(get_services_status)
}
EOF
    
    # 创建快照
    "${SCRIPT_DIR}/backup.sh" full -d "$checkpoint_path" --no-compress
    
    log "🎉 检查点创建完成: $checkpoint_name"
    log "可使用以下命令回滚到此检查点:"
    log "$0 version $checkpoint_name"
}

# ====================
# 获取服务状态
# ====================

get_services_status() {
    local services_status="{"
    local first=true
    
    local services=("mysql-primary" "redis-master" "user-service" "exam-service" "frontend")
    
    for service in "${services[@]}"; do
        if [ "$first" = false ]; then
            services_status+=","
        fi
        first=false
        
        local status="stopped"
        if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q "$service" &> /dev/null; then
            local container_id=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q "$service")
            if [ -n "$container_id" ]; then
                local container_status=$(docker inspect --format='{{.State.Status}}' "$container_id")
                if [ "$container_status" = "running" ]; then
                    status="running"
                fi
            fi
        fi
        
        services_status+="\"$service\":\"$status\""
    done
    
    services_status+="}"
    echo "$services_status"
}

# ====================
# 验证回滚
# ====================

verify_rollback() {
    log "验证回滚结果..."
    
    local verification_failed=false
    
    # 等待服务启动
    log "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    local services=("mysql-primary" "redis-master" "user-service" "exam-service" "frontend" "api-gateway")
    
    for service in "${services[@]}"; do
        local max_attempts=12
        local attempt=0
        local service_ready=false
        
        while [ $attempt -lt $max_attempts ]; do
            if docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q "$service" &> /dev/null; then
                local container_id=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q "$service")
                if [ -n "$container_id" ]; then
                    local container_status=$(docker inspect --format='{{.State.Status}}' "$container_id")
                    if [ "$container_status" = "running" ]; then
                        service_ready=true
                        break
                    fi
                fi
            fi
            
            sleep 10
            attempt=$((attempt + 1))
            debug "等待 $service 启动... ($attempt/$max_attempts)"
        done
        
        if [ "$service_ready" = true ]; then
            log "✓ $service 服务正常"
        else
            error "✗ $service 服务启动失败"
            verification_failed=true
        fi
    done
    
    # 健康检查
    local health_endpoints=(
        "http://localhost/health:API网关"
        "http://localhost:8080/actuator/health:用户服务"
        "http://localhost:3000/api/health:前端应用"
    )
    
    for endpoint_info in "${health_endpoints[@]}"; do
        IFS=':' read -r url name <<< "$endpoint_info"
        
        local max_attempts=6
        local attempt=0
        local health_ok=false
        
        while [ $attempt -lt $max_attempts ]; do
            if curl -f -s --max-time 10 "$url" &> /dev/null; then
                health_ok=true
                break
            fi
            
            sleep 10
            attempt=$((attempt + 1))
            debug "检查 $name 健康状态... ($attempt/$max_attempts)"
        done
        
        if [ "$health_ok" = true ]; then
            log "✓ $name 健康检查通过"
        else
            error "✗ $name 健康检查失败"
            verification_failed=true
        fi
    done
    
    if [ "$verification_failed" = true ]; then
        error "回滚验证失败"
        return 1
    else
        log "✅ 回滚验证通过"
        return 0
    fi
}

# ====================
# 紧急恢复模式
# ====================

emergency_recovery() {
    critical "进入紧急恢复模式"
    
    # 强制停止所有服务
    log "强制停止所有服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" down --timeout 10 || true
    docker stop $(docker ps -q) 2>/dev/null || true
    
    # 查找最近的可用备份
    local emergency_backup=$(find "$BACKUP_DIR" -name "full_backup_*" -type d | sort | tail -n 1)
    
    if [ -z "$emergency_backup" ]; then
        critical "未找到可用的紧急恢复备份"
        
        # 尝试从检查点恢复
        local checkpoint=$(find "$BACKUP_DIR" -name "checkpoint_*" -type d | sort | tail -n 1)
        if [ -n "$checkpoint" ]; then
            emergency_backup="$checkpoint"
            log "使用检查点进行紧急恢复: $(basename "$checkpoint")"
        else
            critical "紧急恢复失败：无可用备份"
            return 1
        fi
    fi
    
    log "使用备份进行紧急恢复: $(basename "$emergency_backup")"
    
    # 跳过确认，强制恢复
    FORCE=true
    AUTO_BACKUP_BEFORE_ROLLBACK=false
    
    rollback_to_backup "$(basename "$emergency_backup")"
    
    critical "紧急恢复完成"
}

# ====================
# 主函数
# ====================

main() {
    # 创建日志目录
    mkdir -p "${PROJECT_ROOT}/logs"
    
    log "启动回滚系统..."
    log "操作: $OPERATION"
    
    # 检查环境
    check_prerequisites
    
    # 加载环境变量
    if [ -f "${PROJECT_ROOT}/.env" ]; then
        source "${PROJECT_ROOT}/.env"
    fi
    
    # 记录回滚开始
    cat > "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json" << EOF
{
  "session_id": "rollback_${TIMESTAMP}",
  "operation": "$OPERATION",
  "target": "$TARGET",
  "started_at": "$(date -Iseconds)",
  "current_version": "$(get_current_version)",
  "user": "${USER:-unknown}",
  "hostname": "$(hostname)"
}
EOF
    
    case $OPERATION in
        quick)
            quick_rollback
            ;;
        version)
            version_rollback
            ;;
        database)
            database_rollback
            ;;
        code)
            # 代码回滚 (Git 版本切换)
            if [ -n "$TARGET" ]; then
                rollback_to_git_version "$TARGET"
            else
                error "请指定目标版本"
                exit 1
            fi
            ;;
        full)
            if [ -n "$TARGET" ]; then
                rollback_to_backup "$TARGET"
            else
                quick_rollback
            fi
            ;;
        list-versions)
            list_versions
            ;;
        create-checkpoint)
            create_checkpoint
            ;;
        emergency)
            emergency_recovery
            ;;
        *)
            error "未知操作: $OPERATION"
            exit 1
            ;;
    esac
    
    # 更新回滚会话记录
    if [ -f "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json" ]; then
        jq --arg end_time "$(date -Iseconds)" \
           --arg final_version "$(get_current_version)" \
           '. + {completed_at: $end_time, final_version: $final_version, success: true}' \
           "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json" > \
           "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json.tmp" && \
           mv "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json.tmp" \
           "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json"
    fi
    
    log "回滚操作完成"
}

# 错误处理
handle_error() {
    local exit_code=$?
    critical "回滚过程中发生错误 (退出码: $exit_code)"
    
    # 更新会话记录
    if [ -f "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json" ]; then
        jq --arg end_time "$(date -Iseconds)" \
           '. + {completed_at: $end_time, success: false, error_code: '${exit_code}'}' \
           "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json" > \
           "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json.tmp" && \
           mv "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json.tmp" \
           "${PROJECT_ROOT}/logs/rollback_session_${TIMESTAMP}.json"
    fi
    
    echo
    echo -e "${RED}🚨 回滚失败！${NC}"
    echo -e "查看日志: ${BLUE}$LOG_FILE${NC}"
    echo -e "如需紧急恢复，请运行: ${BLUE}$0 emergency${NC}"
    echo
    
    exit $exit_code
}

# 设置错误处理
trap handle_error ERR

# 解析参数并执行
parse_arguments "$@"
main