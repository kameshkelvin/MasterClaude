# 在线考试系统部署与维护指南

## 概述

本文档提供在线考试系统的完整部署和维护指南，包括环境准备、系统部署、日常运维、备份恢复、监控告警以及故障排除等内容。本指南基于系统的自动化脚本，为运维团队提供标准化的操作流程。

## 目录

1. [环境准备](#环境准备)
2. [系统部署](#系统部署)
3. [配置管理](#配置管理)
4. [备份策略](#备份策略)
5. [故障恢复](#故障恢复)
6. [监控运维](#监控运维)
7. [性能调优](#性能调优)
8. [安全维护](#安全维护)
9. [故障排除](#故障排除)
10. [运维工具](#运维工具)

---

## 环境准备

### 系统要求

#### 硬件要求
- **CPU**: 4核心以上 (推荐8核心)
- **内存**: 8GB以上 (推荐16GB)
- **存储**: 100GB以上SSD (推荐500GB)
- **网络**: 1Gbps带宽

#### 软件要求
- **操作系统**: Ubuntu 20.04 LTS / CentOS 8+
- **Docker**: 24.0+
- **Docker Compose**: 2.0+
- **Git**: 2.25+

### 环境准备脚本

#### 1. 系统初始化
```bash
#!/bin/bash
# setup-environment.sh

set -e

echo "开始环境准备..."

# 更新系统包
sudo apt update && sudo apt upgrade -y

# 安装必要软件包
sudo apt install -y \
    curl \
    wget \
    git \
    jq \
    htop \
    net-tools \
    unzip

# 安装Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
sudo usermod -aG docker $USER

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.21.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 配置防火墙
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
sudo ufw --force enable

echo "环境准备完成！请重新登录以应用Docker组权限。"
```

#### 2. 目录结构创建
```bash
#!/bin/bash
# create-directories.sh

PROJECT_ROOT="/opt/exam-system"

# 创建项目目录结构
sudo mkdir -p $PROJECT_ROOT/{
    backups,
    logs,
    ssl,
    config,
    uploads,
    data/mysql,
    data/redis
}

# 设置权限
sudo chown -R $USER:$USER $PROJECT_ROOT
chmod 755 $PROJECT_ROOT
chmod 700 $PROJECT_ROOT/backups
chmod 700 $PROJECT_ROOT/ssl

echo "目录结构创建完成"
ls -la $PROJECT_ROOT
```

### 依赖服务检查

#### 环境检查脚本
```bash
#!/bin/bash
# check-prerequisites.sh

echo "开始环境检查..."

# 检查必要命令
REQUIRED_COMMANDS=("docker" "docker-compose" "git" "curl" "jq")

for cmd in "${REQUIRED_COMMANDS[@]}"; do
    if command -v "$cmd" &> /dev/null; then
        echo "✓ $cmd 已安装"
    else
        echo "✗ $cmd 未安装"
        exit 1
    fi
done

# 检查Docker服务
if systemctl is-active --quiet docker; then
    echo "✓ Docker 服务运行中"
else
    echo "✗ Docker 服务未运行"
    exit 1
fi

# 检查端口占用
PORTS=(80 443 3306 6379 8080 8081 8082 8083 8084)
for port in "${PORTS[@]}"; do
    if netstat -tuln | grep -q ":$port "; then
        echo "⚠ 端口 $port 已被占用"
    else
        echo "✓ 端口 $port 可用"
    fi
done

# 检查磁盘空间
AVAILABLE_SPACE=$(df /opt | awk 'NR==2 {print $4}')
REQUIRED_SPACE=10485760  # 10GB in KB

if [ "$AVAILABLE_SPACE" -gt "$REQUIRED_SPACE" ]; then
    echo "✓ 磁盘空间充足 ($(echo "$AVAILABLE_SPACE/1024/1024" | bc)GB 可用)"
else
    echo "⚠ 磁盘空间不足，建议至少保留10GB空间"
fi

echo "环境检查完成"
```

---

## 系统部署

### 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                        负载均衡                              │
├─────────────────────────────────────────────────────────────┤
│                    Nginx (80/443)                          │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                      微服务层                               │
├─────────────────────────────────────────────────────────────┤
│ 用户服务:8080 │ 考试服务:8081 │ 题库服务:8082 │ 监考服务:8083 │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                      数据存储层                             │
├─────────────────────────────────────────────────────────────┤
│         MySQL:3306          │         Redis:6379           │
└─────────────────────────────────────────────────────────────┘
```

### 核心部署脚本

#### 1. 主部署脚本 (deploy.sh)
```bash
#!/bin/bash
# deploy.sh - 主部署脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${PROJECT_ROOT}/logs/deploy-${TIMESTAMP}.log"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARN:${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1" | tee -a "$LOG_FILE"
}

deploy_system() {
    log "开始部署在线考试系统..."
    
    # 1. 环境检查
    log "执行环境检查..."
    if ! ./scripts/check-prerequisites.sh; then
        error "环境检查失败"
        exit 1
    fi
    
    # 2. 拉取最新代码
    log "拉取最新代码..."
    git pull origin main
    
    # 3. 构建应用
    log "构建Java应用..."
    ./mvnw clean package -DskipTests
    
    # 4. 构建Docker镜像
    log "构建Docker镜像..."
    docker-compose build --no-cache
    
    # 5. 停止旧服务 (如果存在)
    log "停止现有服务..."
    docker-compose down --timeout 60 || true
    
    # 6. 创建部署前备份
    log "创建部署前备份..."
    ./scripts/backup.sh full
    
    # 7. 启动数据库服务
    log "启动数据库服务..."
    docker-compose up -d mysql-primary redis-master
    
    # 等待数据库就绪
    wait_for_database
    
    # 8. 数据库迁移
    log "执行数据库迁移..."
    run_database_migration
    
    # 9. 启动所有服务
    log "启动所有服务..."
    docker-compose up -d
    
    # 10. 健康检查
    log "执行健康检查..."
    if ./scripts/health-check.sh; then
        log "🎉 部署成功完成！"
    else
        error "健康检查失败，开始回滚..."
        rollback_deployment
        exit 1
    fi
}

wait_for_database() {
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if docker-compose exec -T mysql-primary mysqladmin ping -h localhost &> /dev/null; then
            log "✓ MySQL 数据库就绪"
            return 0
        fi
        
        sleep 10
        attempt=$((attempt + 1))
        log "等待 MySQL 启动... ($attempt/$max_attempts)"
    done
    
    error "MySQL 启动超时"
    return 1
}

run_database_migration() {
    # 使用Flyway进行数据库迁移
    docker-compose exec -T mysql-primary mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" exam_system < ./db/migrations/init.sql
}

rollback_deployment() {
    log "开始回滚部署..."
    ./scripts/rollback.sh quick --force
}

# 错误处理
handle_error() {
    local exit_code=$?
    error "部署过程中发生错误 (退出码: $exit_code)"
    rollback_deployment
    exit $exit_code
}

trap handle_error ERR

# 主函数
main() {
    # 创建日志目录
    mkdir -p "${PROJECT_ROOT}/logs"
    
    # 加载环境变量
    if [ -f "${PROJECT_ROOT}/.env" ]; then
        source "${PROJECT_ROOT}/.env"
    else
        error "环境配置文件 .env 不存在"
        exit 1
    fi
    
    deploy_system
}

main "$@"
```

#### 2. 健康检查脚本 (health-check.sh)
```bash
#!/bin/bash
# health-check.sh

set -e

PROJECT_ROOT="$(dirname "$(dirname "${BASH_SOURCE[0]}")")"
TIMEOUT=30

# 服务健康检查配置
declare -A SERVICES=(
    ["API网关"]="http://localhost/health"
    ["用户服务"]="http://localhost:8080/actuator/health"
    ["考试服务"]="http://localhost:8081/actuator/health"
    ["题库服务"]="http://localhost:8082/actuator/health"
    ["监考服务"]="http://localhost:8083/health"
    ["通知服务"]="http://localhost:8084/actuator/health"
)

check_service() {
    local service_name=$1
    local service_url=$2
    local max_attempts=10
    local attempt=0
    
    echo "检查服务: $service_name"
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -f -s --max-time $TIMEOUT "$service_url" > /dev/null 2>&1; then
            echo "✓ $service_name 健康"
            return 0
        fi
        
        sleep 5
        attempt=$((attempt + 1))
        echo "等待 $service_name 就绪... ($attempt/$max_attempts)"
    done
    
    echo "✗ $service_name 健康检查失败"
    return 1
}

check_database_connection() {
    echo "检查数据库连接..."
    
    if docker-compose -f "${PROJECT_ROOT}/docker-compose.yml" exec -T mysql-primary \
       mysqladmin ping -h localhost &> /dev/null; then
        echo "✓ MySQL 数据库连接正常"
    else
        echo "✗ MySQL 数据库连接失败"
        return 1
    fi
    
    if docker-compose -f "${PROJECT_ROOT}/docker-compose.yml" exec -T redis-master \
       redis-cli ping &> /dev/null; then
        echo "✓ Redis 连接正常"
    else
        echo "✗ Redis 连接失败"
        return 1
    fi
}

main() {
    echo "开始系统健康检查..."
    
    local failed_services=0
    
    # 检查数据库连接
    if ! check_database_connection; then
        failed_services=$((failed_services + 1))
    fi
    
    # 检查各个服务
    for service_name in "${!SERVICES[@]}"; do
        if ! check_service "$service_name" "${SERVICES[$service_name]}"; then
            failed_services=$((failed_services + 1))
        fi
    done
    
    # 检查Docker容器状态
    echo "检查容器状态..."
    if docker-compose -f "${PROJECT_ROOT}/docker-compose.yml" ps --format "table {{.Name}}\\t{{.Status}}" | grep -q "Up"; then
        echo "✓ 容器运行状态正常"
    else
        echo "✗ 存在停止的容器"
        failed_services=$((failed_services + 1))
    fi
    
    # 结果总结
    if [ $failed_services -eq 0 ]; then
        echo "🎉 所有服务健康检查通过！"
        return 0
    else
        echo "❌ $failed_services 个服务健康检查失败"
        return 1
    fi
}

main "$@"
```

### 滚动更新部署

#### 滚动更新脚本
```bash
#!/bin/bash
# rolling-update.sh

set -e

SERVICE_NAME=$1
NEW_VERSION=${2:-latest}

if [ -z "$SERVICE_NAME" ]; then
    echo "用法: $0 <service-name> [version]"
    echo "可用服务: user-service, exam-service, question-service, proctoring-service, notification-service"
    exit 1
fi

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

rolling_update() {
    local service=$1
    local version=$2
    
    log "开始滚动更新 $service 到版本 $version"
    
    # 1. 拉取新镜像
    log "拉取新镜像..."
    docker pull "exam-system/${service}:${version}"
    
    # 2. 获取当前运行的实例数量
    local current_instances=$(docker-compose ps -q $service | wc -l)
    
    if [ $current_instances -eq 0 ]; then
        log "服务当前未运行，直接启动"
        docker-compose up -d $service
        return
    fi
    
    # 3. 滚动更新每个实例
    for i in $(seq 1 $current_instances); do
        log "更新实例 $i/$current_instances"
        
        # 停止一个实例
        local container_id=$(docker-compose ps -q $service | head -n 1)
        docker stop $container_id
        docker rm $container_id
        
        # 启动新实例
        docker-compose up -d --no-deps $service
        
        # 等待新实例就绪
        sleep 30
        
        # 健康检查
        if ! ./scripts/health-check.sh; then
            log "新实例健康检查失败，回滚..."
            docker-compose down $service
            docker-compose up -d $service
            exit 1
        fi
        
        log "实例 $i 更新完成"
    done
    
    log "滚动更新完成"
}

rolling_update $SERVICE_NAME $NEW_VERSION
```

---

## 配置管理

### 环境配置

#### 1. 主配置文件 (.env)
```bash
# 系统配置
COMPOSE_PROJECT_NAME=exam-system
TZ=Asia/Shanghai

# 数据库配置
MYSQL_ROOT_PASSWORD=your_secure_mysql_root_password
MYSQL_DATABASE=exam_system
MYSQL_USER=exam_user
MYSQL_PASSWORD=your_secure_mysql_password

# Redis配置
REDIS_PASSWORD=your_secure_redis_password

# JWT配置
JWT_SECRET=your_very_long_jwt_secret_key_here
JWT_EXPIRATION=86400000

# 邮件配置
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password

# 文件上传配置
UPLOAD_MAX_SIZE=50MB
UPLOAD_ALLOWED_TYPES=jpg,jpeg,png,pdf,doc,docx

# SSL证书配置
SSL_CERT_PATH=/etc/ssl/certs/exam-system.crt
SSL_KEY_PATH=/etc/ssl/private/exam-system.key

# 监控配置
ENABLE_METRICS=true
METRICS_PORT=9090

# 备份配置
BACKUP_RETENTION_DAYS=30
AUTO_BACKUP_TIME=02:00
```

#### 2. Nginx配置
```nginx
# nginx.conf
upstream user_service {
    least_conn;
    server user-service:8080 max_fails=3 fail_timeout=30s;
}

upstream exam_service {
    least_conn;
    server exam-service:8081 max_fails=3 fail_timeout=30s;
}

upstream question_service {
    least_conn;
    server question-service:8082 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL配置
    ssl_certificate /etc/ssl/certs/exam-system.crt;
    ssl_certificate_key /etc/ssl/private/exam-system.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;
    
    # 安全头
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";
    
    # 限流配置
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=auth:10m rate=5r/s;
    
    # 静态文件
    location /static/ {
        alias /var/www/static/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # API路由
    location /api/auth/ {
        limit_req zone=auth burst=10 nodelay;
        proxy_pass http://user_service;
        include proxy_params;
    }
    
    location /api/users/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://user_service;
        include proxy_params;
    }
    
    location /api/exams/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://exam_service;
        include proxy_params;
    }
    
    location /api/questions/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://question_service;
        include proxy_params;
    }
    
    # 健康检查
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
    
    # 前端应用
    location / {
        try_files $uri $uri/ /index.html;
        root /var/www/html;
    }
}

# proxy_params文件内容
proxy_set_header Host $http_host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_connect_timeout 60s;
proxy_send_timeout 60s;
proxy_read_timeout 60s;
```

### 配置验证脚本

```bash
#!/bin/bash
# validate-config.sh

set -e

PROJECT_ROOT="$(dirname "$(dirname "${BASH_SOURCE[0]}")")"

validate_env_file() {
    echo "验证环境配置文件..."
    
    if [ ! -f "${PROJECT_ROOT}/.env" ]; then
        echo "✗ .env 文件不存在"
        return 1
    fi
    
    # 检查必需的环境变量
    required_vars=(
        "MYSQL_ROOT_PASSWORD"
        "MYSQL_PASSWORD"  
        "REDIS_PASSWORD"
        "JWT_SECRET"
        "SMTP_USERNAME"
        "SMTP_PASSWORD"
    )
    
    source "${PROJECT_ROOT}/.env"
    
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            echo "✗ 环境变量 $var 未设置"
            return 1
        else
            echo "✓ $var 已设置"
        fi
    done
}

validate_docker_compose() {
    echo "验证Docker Compose配置..."
    
    if docker-compose -f "${PROJECT_ROOT}/docker-compose.yml" config -q; then
        echo "✓ docker-compose.yml 配置有效"
    else
        echo "✗ docker-compose.yml 配置无效"
        return 1
    fi
}

validate_nginx_config() {
    echo "验证Nginx配置..."
    
    if [ -f "${PROJECT_ROOT}/nginx.conf" ]; then
        # 测试Nginx配置
        docker run --rm -v "${PROJECT_ROOT}/nginx.conf:/etc/nginx/nginx.conf" nginx:alpine nginx -t
        echo "✓ Nginx配置有效"
    else
        echo "⚠ Nginx配置文件不存在"
    fi
}

validate_ssl_certificates() {
    echo "验证SSL证书..."
    
    if [ -f "${PROJECT_ROOT}/ssl/exam-system.crt" ] && [ -f "${PROJECT_ROOT}/ssl/exam-system.key" ]; then
        # 检查证书有效期
        expiry_date=$(openssl x509 -in "${PROJECT_ROOT}/ssl/exam-system.crt" -noout -enddate | cut -d= -f2)
        expiry_timestamp=$(date -d "$expiry_date" +%s)
        current_timestamp=$(date +%s)
        days_until_expiry=$(( (expiry_timestamp - current_timestamp) / 86400 ))
        
        if [ $days_until_expiry -gt 30 ]; then
            echo "✓ SSL证书有效 ($days_until_expiry 天后到期)"
        else
            echo "⚠ SSL证书即将到期 ($days_until_expiry 天后到期)"
        fi
    else
        echo "⚠ SSL证书文件不存在"
    fi
}

main() {
    echo "开始配置验证..."
    
    local failed_checks=0
    
    if ! validate_env_file; then
        failed_checks=$((failed_checks + 1))
    fi
    
    if ! validate_docker_compose; then
        failed_checks=$((failed_checks + 1))
    fi
    
    validate_nginx_config
    validate_ssl_certificates
    
    if [ $failed_checks -eq 0 ]; then
        echo "🎉 所有配置验证通过！"
        return 0
    else
        echo "❌ $failed_checks 个配置验证失败"
        return 1
    fi
}

main "$@"
```

---

## 备份策略

### 备份类型与策略

#### 1. 全量备份
- **频率**: 每日凌晨2点自动执行
- **保留期**: 30天
- **包含内容**: 
  - MySQL数据库完整备份
  - Redis数据快照
  - 应用配置文件
  - 上传文件

#### 2. 增量备份
- **频率**: 每4小时执行一次
- **保留期**: 7天
- **包含内容**:
  - MySQL二进制日志
  - 变更的配置文件
  - 新增的上传文件

#### 3. 配置备份
- **频率**: 配置变更时触发
- **保留期**: 永久保留
- **包含内容**:
  - 环境配置文件
  - Nginx配置
  - Docker Compose配置

### 备份脚本使用

基于系统提供的 `backup.sh` 脚本:

#### 基本用法
```bash
# 执行全量备份
./scripts/backup.sh full

# 执行增量备份
./scripts/backup.sh incremental

# 列出所有备份
./scripts/backup.sh list

# 验证备份完整性
./scripts/backup.sh verify backup_20231201_120000

# 清理过期备份
./scripts/backup.sh cleanup

# 设置自动备份
./scripts/backup.sh schedule
```

#### 高级选项
```bash
# 指定备份目录
./scripts/backup.sh full -d /custom/backup/path

# 设置保留天数
./scripts/backup.sh cleanup -r 7

# 不压缩备份
./scripts/backup.sh full --no-compress

# 加密备份
./scripts/backup.sh full --encrypt --encryption-key your_key

# 启用远程备份
./scripts/backup.sh full --remote --s3-bucket your-bucket
```

### 自动化备份配置

#### Crontab配置
```bash
# 编辑crontab
crontab -e

# 添加以下任务
# 每日2点执行全量备份
0 2 * * * /opt/exam-system/scripts/backup.sh full >> /opt/exam-system/logs/backup-cron.log 2>&1

# 每4小时执行增量备份
0 */4 * * * /opt/exam-system/scripts/backup.sh incremental >> /opt/exam-system/logs/backup-cron.log 2>&1

# 每周日清理过期备份
0 3 * * 0 /opt/exam-system/scripts/backup.sh cleanup >> /opt/exam-system/logs/backup-cron.log 2>&1
```

#### 备份监控脚本
```bash
#!/bin/bash
# backup-monitor.sh

BACKUP_DIR="/opt/exam-system/backups"
ALERT_EMAIL="admin@yourcompany.com"

check_backup_status() {
    local today=$(date +%Y%m%d)
    local latest_backup=$(find "$BACKUP_DIR" -name "full_backup_${today}_*" -type d | sort | tail -1)
    
    if [ -z "$latest_backup" ]; then
        echo "警告: 今日未发现全量备份"
        send_alert "备份警告" "今日未发现全量备份，请检查备份系统"
        return 1
    fi
    
    # 检查备份完整性
    if [ -f "$latest_backup/backup_info.json" ]; then
        local success=$(jq -r '.success' "$latest_backup/backup_info.json")
        if [ "$success" != "true" ]; then
            echo "警告: 最新备份可能不完整"
            send_alert "备份警告" "最新备份可能不完整，请检查备份日志"
            return 1
        fi
    fi
    
    echo "✓ 备份状态正常"
    return 0
}

send_alert() {
    local subject=$1
    local message=$2
    
    echo "$message" | mail -s "$subject" "$ALERT_EMAIL"
}

check_backup_status
```

---

## 故障恢复

### 恢复策略

基于系统提供的 `rollback.sh` 脚本，提供多种恢复选项:

#### 1. 快速回滚
```bash
# 快速回滚到最新备份
./scripts/rollback.sh quick

# 强制回滚（跳过确认）
./scripts/rollback.sh quick --force

# 详细输出
./scripts/rollback.sh quick --verbose
```

#### 2. 版本回滚
```bash
# 回滚到指定版本
./scripts/rollback.sh version backup_20231201_120000

# 回滚到Git标签版本
./scripts/rollback.sh version v1.2.0

# 列出可用版本
./scripts/rollback.sh list-versions
```

#### 3. 部分回滚
```bash
# 仅回滚数据库
./scripts/rollback.sh database backup_20231201_120000

# 仅回滚代码
./scripts/rollback.sh code v1.2.0

# 完整系统回滚
./scripts/rollback.sh full backup_20231201_120000
```

#### 4. 紧急恢复
```bash
# 紧急恢复模式（自动选择最近备份）
./scripts/rollback.sh emergency
```

### 常见故障场景

#### 1. 数据库故障恢复
```bash
#!/bin/bash
# recover-database.sh

set -e

PROJECT_ROOT="/opt/exam-system"

recover_mysql() {
    echo "开始MySQL数据库恢复..."
    
    # 停止MySQL服务
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" stop mysql-primary
    
    # 备份当前损坏的数据
    mv "$PROJECT_ROOT/data/mysql" "$PROJECT_ROOT/data/mysql.corrupted.$(date +%Y%m%d_%H%M%S)"
    
    # 重新创建数据目录
    mkdir -p "$PROJECT_ROOT/data/mysql"
    
    # 启动MySQL服务
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" up -d mysql-primary
    
    # 等待MySQL启动
    sleep 30
    
    # 从最新备份恢复
    local latest_backup=$(find "$PROJECT_ROOT/backups" -name "full_backup_*" -type d | sort | tail -1)
    if [ -n "$latest_backup" ] && [ -f "$latest_backup/mysql_backup.sql" ]; then
        echo "从备份恢复: $(basename $latest_backup)"
        docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary \
            mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" < "$latest_backup/mysql_backup.sql"
        echo "✓ MySQL数据库恢复完成"
    else
        echo "✗ 未找到可用的数据库备份"
        exit 1
    fi
}

recover_mysql
```

#### 2. 服务异常恢复
```bash
#!/bin/bash
# recover-service.sh

SERVICE_NAME=$1

if [ -z "$SERVICE_NAME" ]; then
    echo "用法: $0 <service-name>"
    exit 1
fi

PROJECT_ROOT="/opt/exam-system"

recover_service() {
    local service=$1
    
    echo "恢复服务: $service"
    
    # 1. 检查服务状态
    local container_id=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" ps -q $service)
    
    if [ -n "$container_id" ]; then
        local status=$(docker inspect --format='{{.State.Status}}' $container_id)
        echo "当前状态: $status"
        
        if [ "$status" != "running" ]; then
            # 2. 尝试重启服务
            echo "尝试重启服务..."
            docker-compose -f "$PROJECT_ROOT/docker-compose.yml" restart $service
            sleep 10
            
            # 3. 检查重启后状态
            if docker-compose -f "$PROJECT_ROOT/docker-compose.yml" ps $service | grep -q "Up"; then
                echo "✓ 服务重启成功"
                return 0
            fi
        fi
    fi
    
    # 4. 强制重新创建服务
    echo "强制重新创建服务..."
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" stop $service
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" rm -f $service
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" up -d $service
    
    # 5. 等待服务就绪
    sleep 30
    
    # 6. 健康检查
    if "$PROJECT_ROOT/scripts/health-check.sh"; then
        echo "✓ 服务恢复成功"
    else
        echo "✗ 服务恢复失败"
        return 1
    fi
}

recover_service $SERVICE_NAME
```

### 灾难恢复计划

#### RTO/RPO目标
- **RTO (恢复时间目标)**: 30分钟内恢复服务
- **RPO (恢复点目标)**: 最多丢失4小时数据

#### 恢复流程
1. **故障确认** (5分钟)
2. **启动恢复程序** (5分钟)
3. **数据恢复** (15分钟)
4. **服务验证** (5分钟)

#### 恢复验证清单
```bash
#!/bin/bash
# disaster-recovery-checklist.sh

echo "灾难恢复验证清单"
echo "=================="

# 1. 数据库连接测试
echo "1. 检查数据库连接..."
if docker-compose exec -T mysql-primary mysqladmin ping -h localhost &> /dev/null; then
    echo "   ✓ MySQL连接正常"
else
    echo "   ✗ MySQL连接失败"
fi

# 2. 缓存服务测试
echo "2. 检查缓存服务..."
if docker-compose exec -T redis-master redis-cli ping &> /dev/null; then
    echo "   ✓ Redis连接正常"
else
    echo "   ✗ Redis连接失败"
fi

# 3. 应用服务测试
echo "3. 检查应用服务..."
./scripts/health-check.sh

# 4. 数据完整性验证
echo "4. 验证数据完整性..."
# 检查关键数据表记录数
local user_count=$(docker-compose exec -T mysql-primary mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SELECT COUNT(*) FROM exam_system.users;" | tail -1)
echo "   用户数量: $user_count"

# 5. 功能测试
echo "5. 功能测试..."
# 测试登录API
if curl -f -s -X POST http://localhost/api/auth/login -H "Content-Type: application/json" -d '{"username":"test","password":"test"}' > /dev/null; then
    echo "   ✓ 登录功能正常"
else
    echo "   ⚠ 登录功能测试失败（可能是测试数据问题）"
fi

echo "=================="
echo "恢复验证完成"
```

---

## 监控运维

### 系统监控

#### 1. 服务监控
```bash
#!/bin/bash
# monitor-services.sh

PROJECT_ROOT="/opt/exam-system"
ALERT_THRESHOLD=3

monitor_services() {
    local services=("mysql-primary" "redis-master" "user-service" "exam-service" "question-service" "proctoring-service" "notification-service" "nginx")
    local failed_services=()
    
    for service in "${services[@]}"; do
        local container_id=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" ps -q $service 2>/dev/null)
        
        if [ -z "$container_id" ]; then
            echo "⚠ 服务 $service 未运行"
            failed_services+=($service)
            continue
        fi
        
        local status=$(docker inspect --format='{{.State.Status}}' $container_id 2>/dev/null)
        
        if [ "$status" != "running" ]; then
            echo "⚠ 服务 $service 状态异常: $status"
            failed_services+=($service)
        else
            echo "✓ 服务 $service 运行正常"
        fi
    done
    
    if [ ${#failed_services[@]} -gt 0 ]; then
        echo "发现 ${#failed_services[@]} 个异常服务: ${failed_services[*]}"
        
        if [ ${#failed_services[@]} -ge $ALERT_THRESHOLD ]; then
            send_alert "系统告警" "发现多个服务异常: ${failed_services[*]}"
        fi
        
        return 1
    fi
    
    return 0
}

send_alert() {
    local subject=$1
    local message=$2
    
    # 发送邮件告警
    echo "$message" | mail -s "$subject" admin@yourcompany.com
    
    # 发送企业微信告警（可选）
    # curl -X POST "https://qyapi.weixin.qq.com/cgi-bin/webhook/send" \
    #      -H "Content-Type: application/json" \
    #      -d '{"msgtype": "text", "text": {"content": "'$subject': '$message'"}}'
}

monitor_services
```

#### 2. 资源监控
```bash
#!/bin/bash
# monitor-resources.sh

# CPU使用率监控
check_cpu_usage() {
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    echo "CPU使用率: ${cpu_usage}%"
    
    if (( $(echo "$cpu_usage > 80" | bc -l) )); then
        echo "⚠ CPU使用率过高: ${cpu_usage}%"
        return 1
    fi
    return 0
}

# 内存使用率监控
check_memory_usage() {
    local mem_info=$(free | grep Mem)
    local total=$(echo $mem_info | awk '{print $2}')
    local used=$(echo $mem_info | awk '{print $3}')
    local usage=$(echo "scale=2; $used/$total*100" | bc)
    
    echo "内存使用率: ${usage}%"
    
    if (( $(echo "$usage > 85" | bc -l) )); then
        echo "⚠ 内存使用率过高: ${usage}%"
        return 1
    fi
    return 0
}

# 磁盘使用率监控
check_disk_usage() {
    local disk_usage=$(df /opt | awk 'NR==2 {print $5}' | cut -d'%' -f1)
    echo "磁盘使用率: ${disk_usage}%"
    
    if [ $disk_usage -gt 85 ]; then
        echo "⚠ 磁盘使用率过高: ${disk_usage}%"
        return 1
    fi
    return 0
}

# Docker资源监控
check_docker_resources() {
    echo "Docker容器资源使用情况:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
}

main() {
    echo "系统资源监控 - $(date)"
    echo "========================"
    
    local alerts=0
    
    if ! check_cpu_usage; then
        alerts=$((alerts + 1))
    fi
    
    if ! check_memory_usage; then
        alerts=$((alerts + 1))
    fi
    
    if ! check_disk_usage; then
        alerts=$((alerts + 1))
    fi
    
    check_docker_resources
    
    if [ $alerts -gt 0 ]; then
        echo "发现 $alerts 个资源告警"
        return 1
    fi
    
    return 0
}

main
```

#### 3. 性能监控
```bash
#!/bin/bash
# monitor-performance.sh

PROJECT_ROOT="/opt/exam-system"

# API响应时间监控
check_api_response_time() {
    local endpoints=(
        "http://localhost/api/auth/login"
        "http://localhost/api/users/profile"
        "http://localhost/api/exams"
        "http://localhost/api/questions/categories"
    )
    
    echo "API响应时间监控:"
    
    for endpoint in "${endpoints[@]}"; do
        local response_time=$(curl -w "%{time_total}" -s -o /dev/null "$endpoint" 2>/dev/null || echo "timeout")
        
        if [ "$response_time" = "timeout" ]; then
            echo "  ✗ $endpoint: 超时"
        else
            local time_ms=$(echo "$response_time * 1000" | bc | cut -d. -f1)
            echo "  → $endpoint: ${time_ms}ms"
            
            if [ $time_ms -gt 2000 ]; then
                echo "  ⚠ 响应时间过长: ${time_ms}ms"
            fi
        fi
    done
}

# 数据库性能监控
check_database_performance() {
    echo "数据库性能监控:"
    
    # 检查MySQL连接数
    local connections=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW STATUS LIKE 'Threads_connected';" | tail -1 | awk '{print $2}')
    echo "  MySQL连接数: $connections"
    
    # 检查慢查询
    local slow_queries=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW STATUS LIKE 'Slow_queries';" | tail -1 | awk '{print $2}')
    echo "  慢查询数量: $slow_queries"
    
    # 检查Redis内存使用
    local redis_memory=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T redis-master \
        redis-cli info memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')
    echo "  Redis内存使用: $redis_memory"
}

main() {
    echo "性能监控报告 - $(date)"
    echo "========================"
    
    check_api_response_time
    echo
    check_database_performance
}

main
```

### 日志管理

#### 日志收集配置
```yaml
# docker-compose.yml 日志配置
version: '3.8'

services:
  user-service:
    image: exam-system/user-service
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "3"
    volumes:
      - ./logs:/app/logs

  nginx:
    image: nginx:alpine
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "5"
    volumes:
      - ./logs/nginx:/var/log/nginx
```

#### 日志分析脚本
```bash
#!/bin/bash
# analyze-logs.sh

PROJECT_ROOT="/opt/exam-system"
LOG_DIR="$PROJECT_ROOT/logs"

# 分析错误日志
analyze_errors() {
    echo "错误日志分析 (最近24小时):"
    
    # 统计各类错误
    find "$LOG_DIR" -name "*.log" -mtime -1 -exec grep -i "error" {} \; | \
        awk '{print $4}' | sort | uniq -c | sort -nr | head -10
    
    echo
    echo "最新错误日志:"
    find "$LOG_DIR" -name "*.log" -mtime -1 -exec grep -i "error" {} \; | tail -5
}

# 分析访问日志
analyze_access() {
    echo "访问日志分析:"
    
    if [ -f "$LOG_DIR/nginx/access.log" ]; then
        # 统计访问量最多的IP
        echo "访问量最多的IP (Top 10):"
        awk '{print $1}' "$LOG_DIR/nginx/access.log" | sort | uniq -c | sort -nr | head -10
        
        echo
        # 统计HTTP状态码
        echo "HTTP状态码分布:"
        awk '{print $9}' "$LOG_DIR/nginx/access.log" | sort | uniq -c | sort -nr
        
        echo
        # 统计访问最多的API
        echo "访问最多的API (Top 10):"
        awk '{print $7}' "$LOG_DIR/nginx/access.log" | grep -E "^/api" | sort | uniq -c | sort -nr | head -10
    fi
}

# 日志清理
cleanup_logs() {
    echo "清理过期日志 (保留30天)..."
    
    find "$LOG_DIR" -name "*.log" -mtime +30 -delete
    find "$LOG_DIR" -name "*.log.*" -mtime +30 -delete
    
    echo "日志清理完成"
}

case "$1" in
    "errors")
        analyze_errors
        ;;
    "access")
        analyze_access
        ;;
    "cleanup")
        cleanup_logs
        ;;
    "")
        analyze_errors
        echo
        analyze_access
        ;;
    *)
        echo "用法: $0 [errors|access|cleanup]"
        exit 1
        ;;
esac
```

---

## 性能调优

### 应用层优化

#### JVM参数调优
```bash
# 用户服务JVM配置
JAVA_OPTS="-Xms512m -Xmx1024m
           -XX:+UseG1GC
           -XX:MaxGCPauseMillis=200
           -XX:+HeapDumpOnOutOfMemoryError
           -XX:HeapDumpPath=/app/logs/
           -XX:+UseStringDeduplication
           -XX:+OptimizeStringConcat"
```

#### 数据库连接池优化
```yaml
# application.yml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
```

### 数据库优化

#### MySQL性能调优脚本
```bash
#!/bin/bash
# optimize-mysql.sh

PROJECT_ROOT="/opt/exam-system"

optimize_mysql_config() {
    echo "优化MySQL配置..."
    
    cat > "$PROJECT_ROOT/config/mysql.cnf" << EOF
[mysqld]
# 基本配置
default-storage-engine = InnoDB
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 性能配置
innodb_buffer_pool_size = 1G
innodb_buffer_pool_instances = 4
innodb_log_file_size = 256M
innodb_log_buffer_size = 16M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT

# 连接配置
max_connections = 200
max_connect_errors = 100000
thread_cache_size = 16

# 查询缓存
query_cache_type = 1
query_cache_size = 256M
query_cache_limit = 2M

# 临时表
tmp_table_size = 256M
max_heap_table_size = 256M

# 二进制日志
log-bin = mysql-bin
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 100M

# 慢查询日志
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
log_queries_not_using_indexes = 1
EOF

    echo "MySQL配置优化完成"
}

create_indexes() {
    echo "创建性能索引..."
    
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" exam_system << EOF

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

-- 考试表索引
CREATE INDEX IF NOT EXISTS idx_exams_status_start_time ON exams(status, start_time);
CREATE INDEX IF NOT EXISTS idx_exams_organization_id ON exams(organization_id);

-- 考试记录索引
CREATE INDEX IF NOT EXISTS idx_exam_attempts_user_exam ON exam_attempts(user_id, exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_started_at ON exam_attempts(started_at);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_status ON exam_attempts(status);

-- 题目表索引
CREATE INDEX IF NOT EXISTS idx_questions_category_id ON questions(category_id);
CREATE INDEX IF NOT EXISTS idx_questions_difficulty ON questions(difficulty);
CREATE INDEX IF NOT EXISTS idx_questions_type ON questions(type);

-- 答案表索引
CREATE INDEX IF NOT EXISTS idx_user_answers_attempt_id ON user_answers(attempt_id);
CREATE INDEX IF NOT EXISTS idx_user_answers_question_id ON user_answers(question_id);

-- 复合索引
CREATE INDEX IF NOT EXISTS idx_exam_attempts_composite ON exam_attempts(user_id, exam_id, started_at);

EOF

    echo "索引创建完成"
}

analyze_slow_queries() {
    echo "分析慢查询..."
    
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec mysql-primary \
        mysqldumpslow /var/log/mysql/slow.log | head -20
}

optimize_mysql_config
create_indexes
analyze_slow_queries
```

#### Redis优化配置
```bash
#!/bin/bash
# optimize-redis.sh

PROJECT_ROOT="/opt/exam-system"

optimize_redis_config() {
    echo "优化Redis配置..."
    
    cat > "$PROJECT_ROOT/config/redis.conf" << EOF
# 内存配置
maxmemory 1gb
maxmemory-policy allkeys-lru

# 持久化配置
save 900 1
save 300 10
save 60 10000
rdbcompression yes
rdbchecksum yes

# AOF配置
appendonly yes
appendfsync everysec
no-appendfsync-on-rewrite no
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# 网络配置
tcp-keepalive 300
timeout 300

# 客户端配置
maxclients 10000

# 其他优化
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
EOF

    echo "Redis配置优化完成"
}

optimize_redis_config
```

### 缓存策略优化

#### 缓存配置脚本
```bash
#!/bin/bash
# optimize-cache.sh

echo "缓存策略优化..."

# 1. 应用级缓存配置
cat > cache-config.yml << EOF
spring:
  cache:
    type: redis
    redis:
      time-to-live: 1800000  # 30分钟
      cache-null-values: false
  redis:
    timeout: 2s
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5

# 自定义缓存配置
cache:
  user-info:
    ttl: 3600    # 1小时
  question-cache:
    ttl: 1800    # 30分钟
  exam-config:
    ttl: 7200    # 2小时
EOF

echo "缓存配置完成"
```

---

## 安全维护

### 安全检查脚本

```bash
#!/bin/bash
# security-check.sh

PROJECT_ROOT="/opt/exam-system"

check_ssl_certificates() {
    echo "检查SSL证书..."
    
    if [ -f "$PROJECT_ROOT/ssl/exam-system.crt" ]; then
        local expiry_date=$(openssl x509 -in "$PROJECT_ROOT/ssl/exam-system.crt" -noout -enddate | cut -d= -f2)
        local expiry_timestamp=$(date -d "$expiry_date" +%s)
        local current_timestamp=$(date +%s)
        local days_until_expiry=$(( (expiry_timestamp - current_timestamp) / 86400 ))
        
        if [ $days_until_expiry -lt 30 ]; then
            echo "⚠ SSL证书将在 $days_until_expiry 天后到期"
            return 1
        else
            echo "✓ SSL证书有效期: $days_until_expiry 天"
        fi
    else
        echo "✗ SSL证书文件不存在"
        return 1
    fi
}

check_docker_security() {
    echo "检查Docker安全配置..."
    
    # 检查容器是否以root用户运行
    local root_containers=$(docker ps --format "{{.Names}}" | xargs -I {} docker exec {} whoami 2>/dev/null | grep -c root || true)
    
    if [ $root_containers -gt 0 ]; then
        echo "⚠ 发现 $root_containers 个容器以root用户运行"
    else
        echo "✓ 容器用户权限配置正常"
    fi
    
    # 检查容器网络配置
    local bridge_containers=$(docker network ls | grep -c bridge)
    echo "  网桥网络数量: $bridge_containers"
}

check_firewall_rules() {
    echo "检查防火墙规则..."
    
    if command -v ufw &> /dev/null; then
        local ufw_status=$(ufw status | head -1)
        echo "  UFW状态: $ufw_status"
        
        if [[ $ufw_status == *"active"* ]]; then
            echo "✓ 防火墙已启用"
        else
            echo "⚠ 防火墙未启用"
            return 1
        fi
    else
        echo "⚠ UFW防火墙未安装"
    fi
}

check_password_security() {
    echo "检查密码安全性..."
    
    if [ -f "$PROJECT_ROOT/.env" ]; then
        source "$PROJECT_ROOT/.env"
        
        # 检查密码长度
        local weak_passwords=0
        
        if [ ${#MYSQL_ROOT_PASSWORD} -lt 12 ]; then
            echo "⚠ MySQL root密码长度不足"
            weak_passwords=$((weak_passwords + 1))
        fi
        
        if [ ${#REDIS_PASSWORD} -lt 12 ]; then
            echo "⚠ Redis密码长度不足"
            weak_passwords=$((weak_passwords + 1))
        fi
        
        if [ ${#JWT_SECRET} -lt 32 ]; then
            echo "⚠ JWT密钥长度不足"
            weak_passwords=$((weak_passwords + 1))
        fi
        
        if [ $weak_passwords -eq 0 ]; then
            echo "✓ 密码强度检查通过"
        else
            echo "⚠ 发现 $weak_passwords 个弱密码"
            return 1
        fi
    fi
}

scan_vulnerabilities() {
    echo "扫描容器漏洞..."
    
    # 使用Docker自带的安全扫描（如果可用）
    if docker version --format '{{.Server.Version}}' | grep -q "20.10"; then
        echo "执行Docker镜像安全扫描..."
        
        local images=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" config | grep "image:" | awk '{print $2}' | sort -u)
        
        for image in $images; do
            echo "扫描镜像: $image"
            # docker scan "$image" 2>/dev/null || echo "  扫描工具不可用"
        done
    fi
}

main() {
    echo "安全检查报告 - $(date)"
    echo "========================"
    
    local security_issues=0
    
    if ! check_ssl_certificates; then
        security_issues=$((security_issues + 1))
    fi
    
    check_docker_security
    
    if ! check_firewall_rules; then
        security_issues=$((security_issues + 1))
    fi
    
    if ! check_password_security; then
        security_issues=$((security_issues + 1))
    fi
    
    scan_vulnerabilities
    
    echo "========================"
    if [ $security_issues -eq 0 ]; then
        echo "✓ 安全检查通过"
    else
        echo "⚠ 发现 $security_issues 个安全问题"
    fi
    
    return $security_issues
}

main
```

### 安全加固脚本

```bash
#!/bin/bash
# security-hardening.sh

PROJECT_ROOT="/opt/exam-system"

harden_docker() {
    echo "Docker安全加固..."
    
    # 配置Docker daemon安全选项
    cat > /etc/docker/daemon.json << EOF
{
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "100m",
        "max-file": "3"
    },
    "userland-proxy": false,
    "no-new-privileges": true,
    "seccomp-profile": "/etc/docker/seccomp.json"
}
EOF

    systemctl restart docker
    echo "✓ Docker安全配置完成"
}

configure_fail2ban() {
    echo "配置Fail2ban..."
    
    # 安装fail2ban
    apt update && apt install -y fail2ban
    
    # 配置Nginx防护
    cat > /etc/fail2ban/jail.d/nginx-http-auth.conf << EOF
[nginx-http-auth]
enabled = true
port = http,https
logpath = /opt/exam-system/logs/nginx/error.log
EOF

    systemctl enable fail2ban
    systemctl restart fail2ban
    echo "✓ Fail2ban配置完成"
}

update_ssl_certificates() {
    echo "更新SSL证书..."
    
    # 使用Let's Encrypt (需要根据实际域名配置)
    if command -v certbot &> /dev/null; then
        certbot renew --dry-run
        echo "✓ SSL证书检查完成"
    else
        echo "⚠ Certbot未安装，跳过SSL证书更新"
    fi
}

configure_security_headers() {
    echo "配置安全头..."
    
    # 更新Nginx配置中的安全头
    cat >> "$PROJECT_ROOT/nginx.conf" << EOF

# 安全头配置
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
add_header X-XSS-Protection "1; mode=block";
add_header Referrer-Policy "strict-origin-when-cross-origin";
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';";
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()";

EOF

    echo "✓ 安全头配置完成"
}

main() {
    echo "开始安全加固..."
    
    harden_docker
    configure_fail2ban
    update_ssl_certificates
    configure_security_headers
    
    echo "安全加固完成"
}

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
    echo "请以root用户运行此脚本"
    exit 1
fi

main
```

---

## 故障排除

### 常见问题诊断

#### 1. 服务启动失败
```bash
#!/bin/bash
# troubleshoot-startup.sh

SERVICE_NAME=$1

if [ -z "$SERVICE_NAME" ]; then
    echo "用法: $0 <service-name>"
    exit 1
fi

PROJECT_ROOT="/opt/exam-system"

diagnose_startup_failure() {
    local service=$1
    
    echo "诊断服务启动失败: $service"
    echo "================================"
    
    # 1. 检查容器日志
    echo "1. 检查容器日志:"
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" logs --tail=50 $service
    
    echo
    # 2. 检查容器状态
    echo "2. 检查容器状态:"
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" ps $service
    
    echo
    # 3. 检查端口占用
    echo "3. 检查端口占用:"
    local port=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" config | grep -A 5 "$service:" | grep -E "^\s*-\s*[0-9]+" | head -1 | cut -d: -f1 | tr -d ' -')
    if [ -n "$port" ]; then
        netstat -tuln | grep ":$port"
    fi
    
    echo
    # 4. 检查依赖服务
    echo "4. 检查依赖服务:"
    local dependencies=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" config | grep -A 10 "$service:" | grep "depends_on:" -A 5 | grep -E "^\s*-" | cut -d- -f2 | tr -d ' ')
    
    for dep in $dependencies; do
        local dep_status=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" ps $dep | tail -1 | awk '{print $4}')
        echo "  $dep: $dep_status"
    done
    
    echo
    # 5. 检查资源使用
    echo "5. 检查系统资源:"
    echo "  内存使用: $(free -h | grep Mem | awk '{print $3 "/" $2}')"
    echo "  磁盘使用: $(df -h /opt | tail -1 | awk '{print $3 "/" $2 " (" $5 ")"}')"
    
    echo
    # 6. 提供解决建议
    echo "6. 解决建议:"
    suggest_solutions $service
}

suggest_solutions() {
    local service=$1
    
    case $service in
        "mysql-primary")
            echo "  - 检查MySQL数据目录权限"
            echo "  - 检查内存是否足够 (建议4GB+)"
            echo "  - 查看MySQL错误日志: docker-compose logs mysql-primary"
            ;;
        "redis-master")
            echo "  - 检查Redis配置文件语法"
            echo "  - 检查内存使用情况"
            echo "  - 尝试: docker-compose restart redis-master"
            ;;
        "*-service")
            echo "  - 检查应用配置文件"
            echo "  - 确认数据库连接配置正确"
            echo "  - 检查JVM内存设置"
            echo "  - 尝试: docker-compose build --no-cache $service"
            ;;
        "nginx")
            echo "  - 检查Nginx配置语法: nginx -t"
            echo "  - 检查SSL证书文件是否存在"
            echo "  - 检查上游服务是否可用"
            ;;
    esac
}

diagnose_startup_failure $SERVICE_NAME
```

#### 2. 性能问题诊断
```bash
#!/bin/bash
# troubleshoot-performance.sh

PROJECT_ROOT="/opt/exam-system"

diagnose_performance() {
    echo "性能问题诊断"
    echo "============"
    
    # 1. 系统资源使用
    echo "1. 系统资源使用:"
    echo "CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)%"
    echo "内存: $(free | grep Mem | awk '{printf "%.1f%", $3/$2 * 100.0}')"
    echo "磁盘I/O: $(iostat -d 1 2 | tail -1 | awk '{print $4 " r/s, " $5 " w/s"}')"
    
    echo
    # 2. 容器资源使用
    echo "2. 容器资源使用:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}"
    
    echo
    # 3. 数据库性能
    echo "3. 数据库性能:"
    check_database_performance
    
    echo
    # 4. 网络连接
    echo "4. 网络连接统计:"
    netstat -an | awk '/^tcp/ {print $6}' | sort | uniq -c | sort -nr
    
    echo
    # 5. 慢查询分析
    echo "5. 最近的慢查询:"
    if [ -f "$PROJECT_ROOT/logs/mysql-slow.log" ]; then
        tail -20 "$PROJECT_ROOT/logs/mysql-slow.log"
    else
        echo "  慢查询日志不存在"
    fi
}

check_database_performance() {
    # MySQL性能指标
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "
            SELECT 
                'Threads_connected' as Metric,
                VARIABLE_VALUE as Value
            FROM performance_schema.global_status 
            WHERE VARIABLE_NAME = 'Threads_connected'
            UNION ALL
            SELECT 
                'Slow_queries' as Metric,
                VARIABLE_VALUE as Value
            FROM performance_schema.global_status 
            WHERE VARIABLE_NAME = 'Slow_queries'
            UNION ALL
            SELECT 
                'Questions' as Metric,
                VARIABLE_VALUE as Value
            FROM performance_schema.global_status 
            WHERE VARIABLE_NAME = 'Questions';
        " 2>/dev/null || echo "  无法获取MySQL性能数据"
}

provide_optimization_suggestions() {
    echo "优化建议:"
    echo "========"
    
    # CPU使用率检查
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    if (( $(echo "$cpu_usage > 80" | bc -l) )); then
        echo "- CPU使用率过高 (${cpu_usage}%):"
        echo "  * 考虑增加CPU核心数"
        echo "  * 优化应用程序代码"
        echo "  * 启用应用级缓存"
    fi
    
    # 内存使用率检查
    local mem_usage=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}')
    if [ $mem_usage -gt 85 ]; then
        echo "- 内存使用率过高 (${mem_usage}%):"
        echo "  * 增加系统内存"
        echo "  * 调整JVM堆内存设置"
        echo "  * 优化数据库缓存配置"
    fi
    
    # 磁盘使用率检查
    local disk_usage=$(df /opt | awk 'NR==2 {print $5}' | cut -d'%' -f1)
    if [ $disk_usage -gt 80 ]; then
        echo "- 磁盘使用率过高 (${disk_usage}%):"
        echo "  * 清理日志文件"
        echo "  * 清理过期备份"
        echo "  * 增加磁盘容量"
    fi
}

main() {
    diagnose_performance
    echo
    provide_optimization_suggestions
}

main
```

#### 3. 连接问题诊断
```bash
#!/bin/bash
# troubleshoot-connectivity.sh

PROJECT_ROOT="/opt/exam-system"

test_database_connectivity() {
    echo "测试数据库连接..."
    
    # 测试MySQL连接
    if docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary mysqladmin ping -h localhost &> /dev/null; then
        echo "✓ MySQL连接正常"
        
        # 测试具体数据库
        if docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary \
           mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "USE exam_system; SELECT 1;" &> /dev/null; then
            echo "✓ exam_system数据库访问正常"
        else
            echo "✗ exam_system数据库访问失败"
        fi
    else
        echo "✗ MySQL连接失败"
        echo "  尝试重启MySQL: docker-compose restart mysql-primary"
    fi
    
    # 测试Redis连接
    if docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T redis-master redis-cli ping &> /dev/null; then
        echo "✓ Redis连接正常"
    else
        echo "✗ Redis连接失败"
        echo "  尝试重启Redis: docker-compose restart redis-master"
    fi
}

test_service_connectivity() {
    echo "测试服务连接..."
    
    local services=(
        "user-service:8080:/actuator/health"
        "exam-service:8081:/actuator/health"
        "question-service:8082:/actuator/health"
        "proctoring-service:8083:/health"
        "notification-service:8084:/actuator/health"
    )
    
    for service_info in "${services[@]}"; do
        IFS=':' read -r service port path <<< "$service_info"
        
        local url="http://localhost:${port}${path}"
        
        if curl -f -s --max-time 5 "$url" &> /dev/null; then
            echo "✓ $service 服务正常"
        else
            echo "✗ $service 服务连接失败"
            echo "  检查服务状态: docker-compose ps $service"
            echo "  查看服务日志: docker-compose logs $service"
        fi
    done
}

test_external_connectivity() {
    echo "测试外部连接..."
    
    # 测试互联网连接
    if ping -c 1 8.8.8.8 &> /dev/null; then
        echo "✓ 互联网连接正常"
    else
        echo "✗ 互联网连接失败"
    fi
    
    # 测试DNS解析
    if nslookup google.com &> /dev/null; then
        echo "✓ DNS解析正常"
    else
        echo "✗ DNS解析失败"
    fi
    
    # 测试SMTP连接
    if [ -n "${SMTP_HOST}" ]; then
        if timeout 5 bash -c "</dev/tcp/${SMTP_HOST}/${SMTP_PORT:-587}" 2>/dev/null; then
            echo "✓ SMTP服务器连接正常"
        else
            echo "✗ SMTP服务器连接失败"
        fi
    fi
}

check_port_conflicts() {
    echo "检查端口冲突..."
    
    local required_ports=(80 443 3306 6379 8080 8081 8082 8083 8084)
    
    for port in "${required_ports[@]}"; do
        if netstat -tuln | grep -q ":$port "; then
            local process=$(netstat -tuln | grep ":$port " | head -1)
            echo "端口 $port: 已占用 - $process"
        else
            echo "端口 $port: 可用"
        fi
    done
}

main() {
    echo "连接问题诊断"
    echo "============"
    
    test_database_connectivity
    echo
    test_service_connectivity
    echo
    test_external_connectivity
    echo
    check_port_conflicts
}

# 加载环境变量
if [ -f "$PROJECT_ROOT/.env" ]; then
    source "$PROJECT_ROOT/.env"
fi

main
```

---

## 运维工具

### 运维仪表板脚本

```bash
#!/bin/bash
# dashboard.sh - 运维仪表板

PROJECT_ROOT="/opt/exam-system"

show_system_overview() {
    echo "=========================================="
    echo "        在线考试系统运维仪表板"
    echo "=========================================="
    echo "时间: $(date)"
    echo "主机: $(hostname)"
    echo "系统: $(uname -s) $(uname -r)"
    echo "----------------------------------------"
}

show_service_status() {
    echo "📊 服务状态:"
    
    local services=("mysql-primary" "redis-master" "user-service" "exam-service" "question-service" "proctoring-service" "notification-service" "nginx")
    
    for service in "${services[@]}"; do
        local status="❌ 停止"
        local container_id=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" ps -q $service 2>/dev/null)
        
        if [ -n "$container_id" ]; then
            local container_status=$(docker inspect --format='{{.State.Status}}' $container_id 2>/dev/null)
            if [ "$container_status" = "running" ]; then
                status="✅ 运行中"
            else
                status="⚠️  异常($container_status)"
            fi
        fi
        
        printf "  %-20s %s\n" "$service" "$status"
    done
}

show_resource_usage() {
    echo
    echo "💻 资源使用:"
    
    # CPU使用率
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    printf "  CPU使用率:          %s%%\n" "$cpu_usage"
    
    # 内存使用率
    local mem_info=$(free | grep Mem)
    local total=$(echo $mem_info | awk '{print $2}')
    local used=$(echo $mem_info | awk '{print $3}')
    local usage=$(echo "scale=1; $used/$total*100" | bc)
    printf "  内存使用率:         %s%% (%s/%s)\n" "$usage" "$(($used/1024/1024))GB" "$(($total/1024/1024))GB"
    
    # 磁盘使用率
    local disk_info=$(df /opt | tail -1)
    local disk_usage=$(echo $disk_info | awk '{print $5}' | cut -d'%' -f1)
    local disk_used=$(echo $disk_info | awk '{print $3}')
    local disk_total=$(echo $disk_info | awk '{print $2}')
    printf "  磁盘使用率:         %s%% (%sGB/%sGB)\n" "$disk_usage" "$((disk_used/1024/1024))" "$((disk_total/1024/1024))"
}

show_database_status() {
    echo
    echo "🗄️  数据库状态:"
    
    # MySQL状态
    if docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary mysqladmin ping -h localhost &> /dev/null; then
        local connections=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary \
            mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW STATUS LIKE 'Threads_connected';" 2>/dev/null | tail -1 | awk '{print $2}')
        printf "  MySQL:             ✅ 运行中 (%s连接)\n" "${connections:-0}"
    else
        printf "  MySQL:             ❌ 停止\n"
    fi
    
    # Redis状态
    if docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T redis-master redis-cli ping &> /dev/null; then
        local memory=$(docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T redis-master \
            redis-cli info memory 2>/dev/null | grep used_memory_human | cut -d: -f2 | tr -d '\r')
        printf "  Redis:             ✅ 运行中 (%s内存)\n" "${memory:-0B}"
    else
        printf "  Redis:             ❌ 停止\n"
    fi
}

show_recent_logs() {
    echo
    echo "📝 最近日志 (错误):"
    
    local log_files=$(find "$PROJECT_ROOT/logs" -name "*.log" -mtime -1 2>/dev/null)
    
    if [ -n "$log_files" ]; then
        echo "$log_files" | xargs grep -i "error" 2>/dev/null | tail -5 | while read line; do
            echo "  $line"
        done
    else
        echo "  无最近错误日志"
    fi
}

show_backup_status() {
    echo
    echo "💾 备份状态:"
    
    local latest_backup=$(find "$PROJECT_ROOT/backups" -name "full_backup_*" -type d 2>/dev/null | sort | tail -1)
    
    if [ -n "$latest_backup" ]; then
        local backup_name=$(basename "$latest_backup")
        local backup_date=$(echo "$backup_name" | grep -o '[0-9]\{8\}_[0-9]\{6\}')
        local formatted_date=$(echo "$backup_date" | sed 's/_/ /' | sed 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\1-\2-\3/')
        printf "  最新备份:          %s (%s)\n" "$formatted_date" "$backup_name"
        
        # 检查备份是否成功
        if [ -f "$latest_backup/backup_info.json" ]; then
            local success=$(jq -r '.success // false' "$latest_backup/backup_info.json" 2>/dev/null)
            if [ "$success" = "true" ]; then
                printf "  备份状态:          ✅ 成功\n"
            else
                printf "  备份状态:          ❌ 失败\n"
            fi
        fi
    else
        printf "  最新备份:          ❌ 无备份\n"
    fi
}

show_quick_actions() {
    echo
    echo "🔧 快速操作:"
    echo "  1) 重启所有服务:     docker-compose restart"
    echo "  2) 查看服务日志:     ./scripts/dashboard.sh logs <service>"
    echo "  3) 执行健康检查:     ./scripts/health-check.sh"
    echo "  4) 执行备份:         ./scripts/backup.sh full"
    echo "  5) 性能监控:         ./scripts/monitor-performance.sh"
    echo "  6) 安全检查:         ./scripts/security-check.sh"
    echo "=========================================="
}

show_service_logs() {
    local service=$1
    
    if [ -z "$service" ]; then
        echo "可用服务:"
        docker-compose -f "$PROJECT_ROOT/docker-compose.yml" config --services
        return
    fi
    
    echo "显示 $service 服务日志 (最近50行):"
    echo "=================================="
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" logs --tail=50 "$service"
}

main() {
    case "$1" in
        "logs")
            show_service_logs "$2"
            ;;
        "")
            show_system_overview
            show_service_status
            show_resource_usage
            show_database_status
            show_recent_logs
            show_backup_status
            show_quick_actions
            ;;
        *)
            echo "用法: $0 [logs <service-name>]"
            exit 1
            ;;
    esac
}

# 加载环境变量
if [ -f "$PROJECT_ROOT/.env" ]; then
    source "$PROJECT_ROOT/.env"
fi

main "$@"
```

### 自动化运维脚本

```bash
#!/bin/bash
# auto-maintenance.sh - 自动化运维任务

PROJECT_ROOT="/opt/exam-system"
MAINTENANCE_LOG="$PROJECT_ROOT/logs/maintenance-$(date +%Y%m%d).log"

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$MAINTENANCE_LOG"
}

daily_maintenance() {
    log "开始每日维护任务..."
    
    # 1. 清理日志文件
    log "清理过期日志文件..."
    find "$PROJECT_ROOT/logs" -name "*.log" -mtime +7 -delete
    
    # 2. 清理Docker资源
    log "清理Docker资源..."
    docker system prune -f
    
    # 3. 备份检查
    log "检查备份状态..."
    if ! "$PROJECT_ROOT/scripts/backup-monitor.sh"; then
        log "⚠ 备份状态异常"
    fi
    
    # 4. 健康检查
    log "执行健康检查..."
    if ! "$PROJECT_ROOT/scripts/health-check.sh"; then
        log "⚠ 健康检查失败"
        
        # 尝试自动修复
        log "尝试自动修复..."
        docker-compose -f "$PROJECT_ROOT/docker-compose.yml" restart
        sleep 30
        
        if "$PROJECT_ROOT/scripts/health-check.sh"; then
            log "✓ 自动修复成功"
        else
            log "✗ 自动修复失败，需要人工干预"
            send_alert "系统故障" "自动修复失败，需要人工干预"
        fi
    fi
    
    # 5. 性能监控
    log "收集性能数据..."
    "$PROJECT_ROOT/scripts/monitor-performance.sh" | tee -a "$MAINTENANCE_LOG"
    
    # 6. 安全检查
    log "执行安全检查..."
    if ! "$PROJECT_ROOT/scripts/security-check.sh"; then
        log "⚠ 发现安全问题"
        send_alert "安全警告" "发现安全问题，请查看维护日志"
    fi
    
    log "每日维护任务完成"
}

weekly_maintenance() {
    log "开始每周维护任务..."
    
    # 1. 深度备份
    log "执行深度备份..."
    "$PROJECT_ROOT/scripts/backup.sh" full --verify
    
    # 2. 数据库优化
    log "数据库优化..."
    docker-compose -f "$PROJECT_ROOT/docker-compose.yml" exec -T mysql-primary \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "
            OPTIMIZE TABLE exam_system.users;
            OPTIMIZE TABLE exam_system.exams;
            OPTIMIZE TABLE exam_system.exam_attempts;
            OPTIMIZE TABLE exam_system.questions;
        " 2>/dev/null || log "数据库优化失败"
    
    # 3. 清理过期备份
    log "清理过期备份..."
    "$PROJECT_ROOT/scripts/backup.sh" cleanup
    
    # 4. SSL证书检查
    log "SSL证书检查..."
    if [ -f "$PROJECT_ROOT/ssl/exam-system.crt" ]; then
        local expiry_date=$(openssl x509 -in "$PROJECT_ROOT/ssl/exam-system.crt" -noout -enddate | cut -d= -f2)
        local days_until_expiry=$(( ($(date -d "$expiry_date" +%s) - $(date +%s)) / 86400 ))
        
        if [ $days_until_expiry -lt 30 ]; then
            log "⚠ SSL证书将在 $days_until_expiry 天后到期"
            send_alert "SSL证书警告" "SSL证书将在 $days_until_expiry 天后到期"
        fi
    fi
    
    log "每周维护任务完成"
}

send_alert() {
    local subject=$1
    local message=$2
    
    # 发送邮件告警
    echo "$message" | mail -s "$subject" admin@yourcompany.com 2>/dev/null || log "邮件发送失败"
    
    # 记录到日志
    log "告警: $subject - $message"
}

main() {
    # 创建日志目录
    mkdir -p "$PROJECT_ROOT/logs"
    
    # 加载环境变量
    if [ -f "$PROJECT_ROOT/.env" ]; then
        source "$PROJECT_ROOT/.env"
    fi
    
    case "$1" in
        "daily")
            daily_maintenance
            ;;
        "weekly")
            weekly_maintenance
            ;;
        "")
            echo "用法: $0 [daily|weekly]"
            echo "  daily  - 执行每日维护任务"
            echo "  weekly - 执行每周维护任务"
            exit 1
            ;;
        *)
            echo "未知参数: $1"
            exit 1
            ;;
    esac
}

main "$@"
```

---

## 总结

本部署与维护指南提供了在线考试系统的完整运维方案，涵盖了从环境准备到故障排除的全流程。关键要点包括:

### 部署要点
1. **环境标准化**: 统一的环境配置和依赖管理
2. **自动化部署**: 脚本化的部署流程，减少人为错误
3. **健康检查**: 完善的服务健康监控机制
4. **回滚策略**: 快速可靠的故障恢复方案

### 运维要点
1. **监控体系**: 全方位的系统和应用监控
2. **备份策略**: 多层次的数据备份和恢复机制
3. **安全维护**: 持续的安全检查和加固措施
4. **性能优化**: 基于监控数据的性能调优

### 最佳实践
1. **文档化**: 所有操作都有详细的文档和脚本
2. **自动化**: 最大程度地实现自动化运维
3. **监控驱动**: 基于监控数据进行运维决策
4. **预防为主**: 主动发现和解决潜在问题

通过遵循本指南，运维团队可以确保在线考试系统的稳定运行和高效维护。