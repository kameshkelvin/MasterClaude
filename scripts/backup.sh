#!/bin/bash

# 在线考试系统数据备份脚本
# 提供全量备份、增量备份和自动化备份管理

set -e

# ====================
# 全局变量配置
# ====================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${PROJECT_ROOT}/logs/backup-${TIMESTAMP}.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 备份配置
COMPOSE_FILE="docker-compose.yml"
BACKUP_DIR="${PROJECT_ROOT}/backups"
RETENTION_DAYS=30
COMPRESS=true
ENCRYPT=false
ENCRYPTION_KEY=""

# 数据库配置
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="exam_system"
DB_USER="root"
DB_PASSWORD=""

# 远程备份配置
REMOTE_BACKUP=false
REMOTE_HOST=""
REMOTE_USER=""
REMOTE_PATH=""
S3_BUCKET=""
S3_REGION=""

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
在线考试系统备份工具

用法: $0 [选项] <操作>

操作:
    full        全量备份
    incremental 增量备份
    restore     恢复备份
    list        列出备份文件
    cleanup     清理过期备份
    verify      验证备份完整性
    schedule    设置自动备份

选项:
    -h, --help              显示帮助信息
    -v, --verbose           详细输出模式
    -d, --backup-dir <目录>  备份目录 (默认: ./backups)
    -r, --retention <天数>   保留天数 (默认: 30)
    --no-compress           不压缩备份文件
    --encrypt               加密备份文件
    --encryption-key <密钥>  加密密钥
    --remote                启用远程备份
    --s3-bucket <bucket>    S3 存储桶名称

示例:
    $0 full                     # 执行全量备份
    $0 incremental             # 执行增量备份
    $0 restore backup_20231201 # 恢复指定备份
    $0 cleanup                 # 清理过期备份
    $0 schedule --cron         # 设置定时备份

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
            -d|--backup-dir)
                BACKUP_DIR="$2"
                shift 2
                ;;
            -r|--retention)
                RETENTION_DAYS="$2"
                shift 2
                ;;
            --no-compress)
                COMPRESS=false
                shift
                ;;
            --encrypt)
                ENCRYPT=true
                shift
                ;;
            --encryption-key)
                ENCRYPTION_KEY="$2"
                shift 2
                ;;
            --remote)
                REMOTE_BACKUP=true
                shift
                ;;
            --s3-bucket)
                S3_BUCKET="$2"
                shift 2
                ;;
            full|incremental|restore|list|cleanup|verify|schedule)
                OPERATION="$1"
                if [ "$1" = "restore" ] && [ -n "$2" ] && [[ ! "$2" =~ ^- ]]; then
                    RESTORE_TARGET="$2"
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
    log "检查备份环境..."
    
    # 检查必要命令
    local required_commands=("docker" "docker-compose" "tar")
    
    if [ "$COMPRESS" = true ]; then
        required_commands+=("gzip")
    fi
    
    if [ "$ENCRYPT" = true ]; then
        required_commands+=("openssl")
    fi
    
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
    
    # 创建备份目录
    mkdir -p "$BACKUP_DIR"
    
    # 检查磁盘空间
    local available_space=$(df "$BACKUP_DIR" | awk 'NR==2 {print $4}')
    local required_space=10485760  # 10GB in KB
    
    if [ "$available_space" -lt "$required_space" ]; then
        warn "备份目录磁盘空间不足，建议至少保留 10GB 空间"
    fi
    
    log "环境检查完成"
}

# ====================
# MySQL 数据库备份
# ====================

backup_mysql() {
    local backup_type="$1"
    local backup_file="$2"
    
    log "备份 MySQL 数据库..."
    
    # 检查数据库连接
    if ! docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary mysqladmin ping -h localhost &> /dev/null; then
        error "无法连接到 MySQL 数据库"
        return 1
    fi
    
    local mysql_backup_file="${backup_file}/mysql_backup.sql"
    
    if [ "$backup_type" = "full" ]; then
        # 全量备份
        debug "执行 MySQL 全量备份"
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
            mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
            --single-transaction \
            --routines \
            --triggers \
            --events \
            --all-databases \
            --master-data=2 \
            --flush-logs \
            > "$mysql_backup_file"
    else
        # 增量备份 (基于二进制日志)
        debug "执行 MySQL 增量备份"
        
        # 获取最后一个二进制日志文件
        local last_binlog=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
            mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW MASTER STATUS\G" | \
            grep File | awk '{print $2}')
        
        # 备份二进制日志
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
            mysqlbinlog --read-from-remote-server \
            --host=localhost \
            --user=root \
            --password="${MYSQL_ROOT_PASSWORD}" \
            "$last_binlog" > "${mysql_backup_file}.binlog"
    fi
    
    if [ $? -eq 0 ]; then
        log "✓ MySQL 数据库备份完成"
        
        # 记录备份信息
        cat > "${backup_file}/mysql_info.json" << EOF
{
  "backup_type": "$backup_type",
  "database_name": "$DB_NAME",
  "backup_time": "$(date -Iseconds)",
  "file_size": $(stat -f%z "$mysql_backup_file" 2>/dev/null || stat -c%s "$mysql_backup_file" 2>/dev/null || echo 0),
  "checksum": "$(md5sum "$mysql_backup_file" | awk '{print $1}')"
}
EOF
        return 0
    else
        error "MySQL 数据库备份失败"
        return 1
    fi
}

# ====================
# Redis 数据备份
# ====================

backup_redis() {
    local backup_file="$1"
    
    log "备份 Redis 数据..."
    
    # 检查 Redis 连接
    if ! docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T redis-master redis-cli ping &> /dev/null; then
        error "无法连接到 Redis"
        return 1
    fi
    
    # 执行 BGSAVE
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T redis-master redis-cli BGSAVE
    
    # 等待备份完成
    while true; do
        local save_in_progress=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T redis-master \
            redis-cli LASTSAVE)
        
        sleep 2
        
        local current_save=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T redis-master \
            redis-cli LASTSAVE)
        
        if [ "$save_in_progress" != "$current_save" ]; then
            break
        fi
    done
    
    # 复制 RDB 文件
    local container_id=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q redis-master)
    docker cp "$container_id:/data/dump.rdb" "${backup_file}/redis_dump.rdb"
    
    if [ $? -eq 0 ]; then
        log "✓ Redis 数据备份完成"
        
        # 记录备份信息
        cat > "${backup_file}/redis_info.json" << EOF
{
  "backup_time": "$(date -Iseconds)",
  "file_size": $(stat -f%z "${backup_file}/redis_dump.rdb" 2>/dev/null || stat -c%s "${backup_file}/redis_dump.rdb" 2>/dev/null || echo 0),
  "checksum": "$(md5sum "${backup_file}/redis_dump.rdb" | awk '{print $1}')"
}
EOF
        return 0
    else
        error "Redis 数据备份失败"
        return 1
    fi
}

# ====================
# 文件系统备份
# ====================

backup_filesystem() {
    local backup_file="$1"
    
    log "备份文件系统数据..."
    
    # 需要备份的目录
    local backup_paths=(
        "${PROJECT_ROOT}/ssl"
        "${PROJECT_ROOT}/config"
        "${PROJECT_ROOT}/uploads"
        "${PROJECT_ROOT}/.env"
    )
    
    local filesystem_backup="${backup_file}/filesystem.tar"
    
    # 创建文件系统备份
    tar -cf "$filesystem_backup" -C "$PROJECT_ROOT" \
        $(for path in "${backup_paths[@]}"; do
            if [ -e "$path" ]; then
                echo "$(basename "$path")"
            fi
        done) 2>/dev/null || true
    
    if [ -f "$filesystem_backup" ]; then
        log "✓ 文件系统备份完成"
        
        # 记录备份信息
        cat > "${backup_file}/filesystem_info.json" << EOF
{
  "backup_time": "$(date -Iseconds)",
  "file_size": $(stat -f%z "$filesystem_backup" 2>/dev/null || stat -c%s "$filesystem_backup" 2>/dev/null || echo 0),
  "checksum": "$(md5sum "$filesystem_backup" | awk '{print $1}')"
}
EOF
        return 0
    else
        warn "文件系统备份为空或失败"
        return 1
    fi
}

# ====================
# 全量备份
# ====================

full_backup() {
    log "开始执行全量备份..."
    
    local backup_name="full_backup_${TIMESTAMP}"
    local backup_path="${BACKUP_DIR}/${backup_name}"
    
    mkdir -p "$backup_path"
    
    # 记录备份开始信息
    cat > "${backup_path}/backup_info.json" << EOF
{
  "backup_id": "$backup_name",
  "backup_type": "full",
  "started_at": "$(date -Iseconds)",
  "hostname": "$(hostname)",
  "user": "${USER:-unknown}",
  "project_root": "$PROJECT_ROOT"
}
EOF
    
    local backup_success=true
    
    # 备份 MySQL
    if ! backup_mysql "full" "$backup_path"; then
        backup_success=false
    fi
    
    # 备份 Redis
    if ! backup_redis "$backup_path"; then
        backup_success=false
    fi
    
    # 备份文件系统
    if ! backup_filesystem "$backup_path"; then
        backup_success=false
    fi
    
    # 更新备份信息
    local end_time=$(date -Iseconds)
    local backup_size=$(du -sh "$backup_path" | awk '{print $1}')
    
    jq --arg end_time "$end_time" \
       --arg backup_size "$backup_size" \
       --argjson success "$backup_success" \
       '. + {completed_at: $end_time, backup_size: $backup_size, success: $success}' \
       "${backup_path}/backup_info.json" > "${backup_path}/backup_info.json.tmp" && \
       mv "${backup_path}/backup_info.json.tmp" "${backup_path}/backup_info.json"
    
    # 压缩备份
    if [ "$COMPRESS" = true ]; then
        compress_backup "$backup_path"
    fi
    
    # 加密备份
    if [ "$ENCRYPT" = true ]; then
        encrypt_backup "$backup_path"
    fi
    
    # 上传到远程
    if [ "$REMOTE_BACKUP" = true ]; then
        upload_backup "$backup_path"
    fi
    
    if [ "$backup_success" = true ]; then
        log "🎉 全量备份完成: $backup_name"
        log "备份大小: $backup_size"
        log "备份路径: $backup_path"
    else
        error "全量备份过程中出现错误"
        return 1
    fi
}

# ====================
# 增量备份
# ====================

incremental_backup() {
    log "开始执行增量备份..."
    
    # 查找最近的全量备份
    local last_full_backup=$(find "$BACKUP_DIR" -name "full_backup_*" -type d | sort | tail -n 1)
    
    if [ -z "$last_full_backup" ]; then
        warn "未找到全量备份，执行全量备份"
        full_backup
        return
    fi
    
    local backup_name="incremental_backup_${TIMESTAMP}"
    local backup_path="${BACKUP_DIR}/${backup_name}"
    
    mkdir -p "$backup_path"
    
    # 记录备份信息
    cat > "${backup_path}/backup_info.json" << EOF
{
  "backup_id": "$backup_name",
  "backup_type": "incremental",
  "base_backup": "$(basename "$last_full_backup")",
  "started_at": "$(date -Iseconds)",
  "hostname": "$(hostname)",
  "user": "${USER:-unknown}"
}
EOF
    
    # 执行增量备份（这里简化为备份最近修改的文件）
    log "执行增量数据备份..."
    
    # MySQL 增量备份
    backup_mysql "incremental" "$backup_path"
    
    # 查找自上次备份以来修改的文件
    local last_backup_time=$(stat -f%m "$last_full_backup" 2>/dev/null || stat -c%Y "$last_full_backup" 2>/dev/null)
    find "$PROJECT_ROOT" -newer "$last_full_backup" -type f \
        -not -path "*/logs/*" \
        -not -path "*/backups/*" \
        -not -path "*/.git/*" \
        -exec cp --parents {} "$backup_path/" \; 2>/dev/null || true
    
    local end_time=$(date -Iseconds)
    local backup_size=$(du -sh "$backup_path" | awk '{print $1}')
    
    jq --arg end_time "$end_time" \
       --arg backup_size "$backup_size" \
       '. + {completed_at: $end_time, backup_size: $backup_size, success: true}' \
       "${backup_path}/backup_info.json" > "${backup_path}/backup_info.json.tmp" && \
       mv "${backup_path}/backup_info.json.tmp" "${backup_path}/backup_info.json"
    
    log "✓ 增量备份完成: $backup_name"
    log "备份大小: $backup_size"
}

# ====================
# 压缩备份
# ====================

compress_backup() {
    local backup_path="$1"
    local backup_name=$(basename "$backup_path")
    
    log "压缩备份文件..."
    
    cd "$BACKUP_DIR"
    tar -czf "${backup_name}.tar.gz" "$backup_name"
    
    if [ $? -eq 0 ]; then
        local original_size=$(du -sh "$backup_name" | awk '{print $1}')
        local compressed_size=$(du -sh "${backup_name}.tar.gz" | awk '{print $1}')
        
        log "✓ 备份压缩完成"
        log "原始大小: $original_size"
        log "压缩后大小: $compressed_size"
        
        # 删除原始目录
        rm -rf "$backup_name"
    else
        error "备份压缩失败"
        return 1
    fi
}

# ====================
# 加密备份
# ====================

encrypt_backup() {
    local backup_path="$1"
    local backup_file="${backup_path}.tar.gz"
    
    if [ -z "$ENCRYPTION_KEY" ]; then
        error "未提供加密密钥"
        return 1
    fi
    
    log "加密备份文件..."
    
    openssl aes-256-cbc -salt -in "$backup_file" -out "${backup_file}.enc" -k "$ENCRYPTION_KEY"
    
    if [ $? -eq 0 ]; then
        log "✓ 备份加密完成"
        rm "$backup_file"
    else
        error "备份加密失败"
        return 1
    fi
}

# ====================
# 远程备份上传
# ====================

upload_backup() {
    local backup_path="$1"
    
    log "上传备份到远程存储..."
    
    if [ -n "$S3_BUCKET" ]; then
        upload_to_s3 "$backup_path"
    elif [ -n "$REMOTE_HOST" ]; then
        upload_to_remote "$backup_path"
    else
        warn "未配置远程存储"
    fi
}

upload_to_s3() {
    local backup_path="$1"
    local backup_file=$(find "$BACKUP_DIR" -name "$(basename "$backup_path")*" -type f | head -n 1)
    
    if command -v aws &> /dev/null; then
        aws s3 cp "$backup_file" "s3://${S3_BUCKET}/backups/"
        log "✓ 备份已上传到 S3"
    else
        error "AWS CLI 未安装"
    fi
}

upload_to_remote() {
    local backup_path="$1"
    local backup_file=$(find "$BACKUP_DIR" -name "$(basename "$backup_path")*" -type f | head -n 1)
    
    if command -v scp &> /dev/null; then
        scp "$backup_file" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/"
        log "✓ 备份已上传到远程服务器"
    else
        error "SCP 命令未找到"
    fi
}

# ====================
# 备份恢复
# ====================

restore_backup() {
    local backup_target="$1"
    
    if [ -z "$backup_target" ]; then
        list_backups
        echo
        read -p "请输入要恢复的备份名称: " backup_target
    fi
    
    local backup_path="${BACKUP_DIR}/${backup_target}"
    
    if [ ! -d "$backup_path" ] && [ ! -f "${backup_path}.tar.gz" ]; then
        error "备份不存在: $backup_target"
        return 1
    fi
    
    log "开始恢复备份: $backup_target"
    
    # 解压备份（如果需要）
    if [ -f "${backup_path}.tar.gz" ]; then
        log "解压备份文件..."
        cd "$BACKUP_DIR"
        tar -xzf "${backup_target}.tar.gz"
    fi
    
    # 确认恢复操作
    echo -e "${YELLOW}警告: 恢复操作将覆盖现有数据！${NC}"
    read -p "确认继续恢复? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log "用户取消恢复操作"
        return 0
    fi
    
    # 停止服务
    log "停止服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" down
    
    # 恢复 MySQL
    if [ -f "${backup_path}/mysql_backup.sql" ]; then
        log "恢复 MySQL 数据..."
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d mysql-primary
        sleep 30
        
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" exec -T mysql-primary \
            mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" < "${backup_path}/mysql_backup.sql"
        
        log "✓ MySQL 数据恢复完成"
    fi
    
    # 恢复 Redis
    if [ -f "${backup_path}/redis_dump.rdb" ]; then
        log "恢复 Redis 数据..."
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d redis-master
        sleep 10
        
        local container_id=$(docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" ps -q redis-master)
        docker cp "${backup_path}/redis_dump.rdb" "$container_id:/data/dump.rdb"
        docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" restart redis-master
        
        log "✓ Redis 数据恢复完成"
    fi
    
    # 恢复文件系统
    if [ -f "${backup_path}/filesystem.tar" ]; then
        log "恢复文件系统..."
        tar -xf "${backup_path}/filesystem.tar" -C "$PROJECT_ROOT"
        log "✓ 文件系统恢复完成"
    fi
    
    # 重启所有服务
    log "重启所有服务..."
    docker-compose -f "${PROJECT_ROOT}/${COMPOSE_FILE}" up -d
    
    log "🎉 备份恢复完成"
}

# ====================
# 列出备份
# ====================

list_backups() {
    log "备份文件列表:"
    echo
    
    printf "%-30s %-12s %-15s %-10s\n" "备份名称" "类型" "创建时间" "大小"
    echo "=================================================================="
    
    for backup in $(ls -1 "$BACKUP_DIR" | grep -E "(full_backup_|incremental_backup_)" | sort -r); do
        local backup_path="${BACKUP_DIR}/${backup}"
        local backup_type="未知"
        local backup_time="未知"
        local backup_size="未知"
        
        if [ -f "${backup_path}/backup_info.json" ]; then
            backup_type=$(jq -r '.backup_type // "unknown"' "${backup_path}/backup_info.json")
            backup_time=$(jq -r '.started_at // "unknown"' "${backup_path}/backup_info.json" | cut -dT -f1)
        fi
        
        if [ -d "$backup_path" ]; then
            backup_size=$(du -sh "$backup_path" | awk '{print $1}')
        elif [ -f "${backup_path}.tar.gz" ]; then
            backup_size=$(du -sh "${backup_path}.tar.gz" | awk '{print $1}')
        fi
        
        printf "%-30s %-12s %-15s %-10s\n" "$backup" "$backup_type" "$backup_time" "$backup_size"
    done
}

# ====================
# 清理过期备份
# ====================

cleanup_backups() {
    log "清理过期备份 (保留 $RETENTION_DAYS 天)..."
    
    local deleted_count=0
    local total_size=0
    
    find "$BACKUP_DIR" -name "*backup_*" -type d -mtime +$RETENTION_DAYS -print0 | \
    while IFS= read -r -d '' backup_path; do
        local size=$(du -s "$backup_path" | awk '{print $1}')
        total_size=$((total_size + size))
        
        log "删除过期备份: $(basename "$backup_path")"
        rm -rf "$backup_path"
        deleted_count=$((deleted_count + 1))
    done
    
    # 清理压缩文件
    find "$BACKUP_DIR" -name "*backup_*.tar.gz" -type f -mtime +$RETENTION_DAYS -delete
    
    if [ $deleted_count -gt 0 ]; then
        log "✓ 清理完成，删除了 $deleted_count 个过期备份"
        log "释放空间: $(echo "$total_size * 1024" | bc | numfmt --to=iec-i --suffix=B)"
    else
        log "没有需要清理的过期备份"
    fi
}

# ====================
# 验证备份完整性
# ====================

verify_backup() {
    local backup_target="$1"
    
    if [ -z "$backup_target" ]; then
        list_backups
        echo
        read -p "请输入要验证的备份名称: " backup_target
    fi
    
    local backup_path="${BACKUP_DIR}/${backup_target}"
    
    if [ ! -d "$backup_path" ] && [ ! -f "${backup_path}.tar.gz" ]; then
        error "备份不存在: $backup_target"
        return 1
    fi
    
    log "验证备份完整性: $backup_target"
    
    local verification_passed=true
    
    # 验证备份信息文件
    if [ -f "${backup_path}/backup_info.json" ]; then
        if jq empty "${backup_path}/backup_info.json" 2>/dev/null; then
            log "✓ 备份信息文件格式正确"
        else
            error "✗ 备份信息文件格式错误"
            verification_passed=false
        fi
    else
        warn "备份信息文件不存在"
    fi
    
    # 验证 MySQL 备份
    if [ -f "${backup_path}/mysql_backup.sql" ]; then
        if [ -f "${backup_path}/mysql_info.json" ]; then
            local stored_checksum=$(jq -r '.checksum' "${backup_path}/mysql_info.json")
            local actual_checksum=$(md5sum "${backup_path}/mysql_backup.sql" | awk '{print $1}')
            
            if [ "$stored_checksum" = "$actual_checksum" ]; then
                log "✓ MySQL 备份文件完整性验证通过"
            else
                error "✗ MySQL 备份文件完整性验证失败"
                verification_passed=false
            fi
        else
            warn "MySQL 备份信息文件不存在"
        fi
    fi
    
    # 验证 Redis 备份
    if [ -f "${backup_path}/redis_dump.rdb" ]; then
        if [ -f "${backup_path}/redis_info.json" ]; then
            local stored_checksum=$(jq -r '.checksum' "${backup_path}/redis_info.json")
            local actual_checksum=$(md5sum "${backup_path}/redis_dump.rdb" | awk '{print $1}')
            
            if [ "$stored_checksum" = "$actual_checksum" ]; then
                log "✓ Redis 备份文件完整性验证通过"
            else
                error "✗ Redis 备份文件完整性验证失败"
                verification_passed=false
            fi
        else
            warn "Redis 备份信息文件不存在"
        fi
    fi
    
    if [ "$verification_passed" = true ]; then
        log "🎉 备份完整性验证通过"
    else
        error "备份完整性验证失败"
        return 1
    fi
}

# ====================
# 设置自动备份
# ====================

schedule_backup() {
    log "设置自动备份任务..."
    
    local cron_entry="0 2 * * * $SCRIPT_DIR/backup.sh full >> $PROJECT_ROOT/logs/backup-cron.log 2>&1"
    
    echo "建议的 crontab 条目:"
    echo "$cron_entry"
    echo
    
    read -p "是否要自动添加到 crontab? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        (crontab -l 2>/dev/null; echo "$cron_entry") | crontab -
        log "✓ 自动备份任务已添加到 crontab"
        log "每天凌晨 2 点执行全量备份"
    else
        log "请手动添加 crontab 条目以启用自动备份"
    fi
}

# ====================
# 主函数
# ====================

main() {
    # 创建日志目录
    mkdir -p "${PROJECT_ROOT}/logs"
    
    log "启动备份系统..."
    log "操作: $OPERATION"
    
    # 检查环境
    check_prerequisites
    
    # 加载环境变量
    if [ -f "${PROJECT_ROOT}/.env" ]; then
        source "${PROJECT_ROOT}/.env"
    fi
    
    case $OPERATION in
        full)
            full_backup
            ;;
        incremental)
            incremental_backup
            ;;
        restore)
            restore_backup "$RESTORE_TARGET"
            ;;
        list)
            list_backups
            ;;
        cleanup)
            cleanup_backups
            ;;
        verify)
            verify_backup "$RESTORE_TARGET"
            ;;
        schedule)
            schedule_backup
            ;;
        *)
            error "未知操作: $OPERATION"
            exit 1
            ;;
    esac
    
    log "备份操作完成"
}

# 解析参数并执行
parse_arguments "$@"
main