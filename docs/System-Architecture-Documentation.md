# 在线考试系统架构文档

## 概述

本文档详细描述了在线考试系统的系统架构设计，包括系统整体架构、微服务架构、技术栈选择、部署架构、安全架构以及性能和可扩展性设计。该文档旨在为团队后续维护和交付提供完整的架构指南。

## 目录

1. [系统整体架构](#系统整体架构)
2. [微服务架构设计](#微服务架构设计)
3. [技术栈与框架](#技术栈与框架)
4. [数据架构](#数据架构)
5. [安全架构](#安全架构)
6. [性能架构](#性能架构)
7. [部署架构](#部署架构)
8. [监控与运维架构](#监控与运维架构)
9. [扩展性设计](#扩展性设计)
10. [架构决策记录](#架构决策记录)

---

## 系统整体架构

### 架构风格
- **微服务架构**: 采用分布式微服务架构，实现服务的独立部署和扩展
- **事件驱动架构**: 通过消息队列实现服务间异步通信
- **领域驱动设计**: 按业务领域划分微服务边界

### 架构层次

```
┌─────────────────────────────────────────────────────────────┐
│                        Web层                                │
├─────────────────────────────────────────────────────────────┤
│ React前端应用    │    移动端应用    │    管理后台            │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                      API网关层                               │
├─────────────────────────────────────────────────────────────┤
│  Nginx + API Gateway (路由、负载均衡、限流、认证)            │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                      业务服务层                             │
├─────────────────────────────────────────────────────────────┤
│ 用户服务 │ 考试服务 │ 题库服务 │ 监考服务 │ 通知服务        │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                      数据存储层                             │
├─────────────────────────────────────────────────────────────┤
│ PostgreSQL │   Redis   │  文件存储  │   消息队列            │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件说明

#### 1. Web层
- **前端应用**: React + TypeScript 构建的SPA应用
- **移动端**: 响应式设计支持移动设备
- **管理后台**: 基于相同技术栈的管理界面

#### 2. API网关层
- **Nginx**: 作为反向代理和负载均衡器
- **API网关**: 统一路由、认证、限流、监控
- **SSL终止**: 统一处理HTTPS证书

#### 3. 业务服务层
- **微服务集群**: 5个核心业务服务
- **服务发现**: 基于Docker Compose的服务发现
- **负载均衡**: 通过Nginx实现服务间负载均衡

#### 4. 数据存储层
- **关系型数据库**: PostgreSQL主从复制
- **缓存系统**: Redis主从复制
- **文件存储**: 本地文件系统或云存储
- **消息队列**: Redis Pub/Sub

---

## 微服务架构设计

### 服务划分原则
1. **单一职责**: 每个服务专注于特定的业务领域
2. **高内聚低耦合**: 服务内部高度相关，服务间松散耦合
3. **数据自治**: 每个服务管理自己的数据存储
4. **无状态设计**: 服务实例无状态，支持水平扩展

### 核心微服务

#### 1. 用户服务 (User Service)
**端口**: 8080

**职责**:
- 用户注册、登录、认证
- 用户信息管理
- 权限控制和角色管理
- JWT令牌管理

**技术栈**:
- Spring Boot 2.7+
- Spring Security
- JWT
- PostgreSQL

**核心API**:
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `GET /api/users/profile` - 获取用户信息
- `PUT /api/users/profile` - 更新用户信息

#### 2. 考试服务 (Exam Service)
**端口**: 8081

**职责**:
- 考试管理和配置
- 考试会话管理
- 成绩计算和统计
- 考试状态监控

**技术栈**:
- Spring Boot 2.7+
- Redis (会话管理)
- PostgreSQL

**核心API**:
- `GET /api/exams` - 获取可用考试
- `POST /api/exams/{id}/start` - 开始考试
- `POST /api/exams/{id}/submit` - 提交答案
- `GET /api/exams/{id}/results` - 获取考试结果

#### 3. 题库服务 (Question Service)
**端口**: 8082

**职责**:
- 题目管理和分类
- 题库维护
- 题目随机选择
- 答案验证

**技术栈**:
- Spring Boot 2.7+
- PostgreSQL
- Redis (缓存)

**核心API**:
- `GET /api/questions/categories` - 获取题目分类
- `GET /api/questions/random` - 随机获取题目
- `POST /api/questions` - 创建题目
- `PUT /api/questions/{id}` - 更新题目

#### 4. 监考服务 (Proctoring Service)
**端口**: 8083

**职责**:
- 实时监考功能
- 异常行为检测
- 视频/音频处理
- 监考报告生成

**技术栈**:
- Node.js + Express
- WebRTC
- Socket.io
- AI/ML库

**核心API**:
- `POST /api/proctoring/sessions` - 创建监考会话
- `GET /api/proctoring/sessions/{id}` - 获取监考状态
- `POST /api/proctoring/alerts` - 提交异常报告

#### 5. 通知服务 (Notification Service)
**端口**: 8084

**职责**:
- 邮件通知
- 短信通知
- 系统内通知
- 通知模板管理

**技术栈**:
- Spring Boot 2.7+
- Redis (消息队列)
- SMTP/短信网关

**核心API**:
- `POST /api/notifications/email` - 发送邮件
- `POST /api/notifications/sms` - 发送短信
- `GET /api/notifications` - 获取通知列表

### 服务间通信

#### 同步通信
- **HTTP/REST**: 服务间直接API调用
- **负载均衡**: 通过Nginx实现请求分发
- **超时控制**: 设置合理的超时时间

#### 异步通信
- **消息队列**: 使用Redis Pub/Sub
- **事件驱动**: 基于事件的松耦合通信
- **重试机制**: 消息处理失败重试

### 服务治理

#### 服务发现
```yaml
# docker-compose服务发现配置
version: '3.8'
services:
  user-service:
    image: exam-system/user-service
    networks:
      - exam-network
    
  exam-service:
    image: exam-system/exam-service
    networks:
      - exam-network
    depends_on:
      - user-service
```

#### 健康检查
```java
// Spring Boot健康检查端点
@RestController
public class HealthController {
    
    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(status);
    }
}
```

---

## 技术栈与框架

### 后端技术栈

#### Java微服务 (用户、考试、题库、通知服务)
- **框架**: Spring Boot 2.7+
- **安全**: Spring Security + JWT
- **数据访问**: Spring Data JPA + Hibernate
- **缓存**: Spring Cache + Redis
- **监控**: Spring Boot Actuator
- **文档**: Swagger/OpenAPI 3

#### Node.js服务 (监考服务)
- **框架**: Express.js
- **实时通信**: Socket.io
- **WebRTC**: simple-peer
- **AI/ML**: TensorFlow.js
- **图像处理**: Sharp

### 前端技术栈
- **框架**: React 18+
- **语言**: TypeScript
- **状态管理**: Redux Toolkit
- **路由**: React Router
- **UI组件**: Ant Design
- **实时通信**: Socket.io-client
- **视频处理**: WebRTC API

### 数据库技术栈
- **关系型数据库**: PostgreSQL 14+
- **缓存**: Redis 7+
- **连接池**: HikariCP
- **数据迁移**: Flyway

### 运维技术栈
- **容器化**: Docker + Docker Compose
- **反向代理**: Nginx
- **SSL证书**: Let's Encrypt
- **监控**: Prometheus + Grafana
- **日志**: ELK Stack (可选)

---

## 数据架构

### 数据存储策略

#### 1. 关系型数据存储 (PostgreSQL)
**主要数据**:
- 用户信息和认证数据
- 考试配置和结果
- 题库和题目内容
- 系统配置信息

**特性**:
- ACID事务保证
- 复杂查询支持
- 数据一致性
- 主从复制

#### 2. 缓存存储 (Redis)
**缓存数据**:
- 用户会话信息
- 考试实时状态
- 热点题目数据
- 临时计算结果

**特性**:
- 高性能读写
- 数据过期机制
- 发布订阅功能
- 主从复制

#### 3. 文件存储
**存储内容**:
- 题目附件 (图片、音频、视频)
- 用户头像
- 考试录像
- 系统日志文件

**存储策略**:
- 本地文件系统
- 支持云存储扩展
- CDN加速

### 数据分片策略

#### 水平分片
```sql
-- 按时间分片考试记录表
CREATE TABLE exam_attempts_2024_01 (
    LIKE exam_attempts INCLUDING ALL
) PARTITION OF exam_attempts 
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- 按组织分片用户表
CREATE TABLE users_org_1 (
    LIKE users INCLUDING ALL
) PARTITION OF users 
FOR VALUES WITH (modulus 4, remainder 0);
```

#### 读写分离
```yaml
# 数据库配置
database:
  primary:
    host: mysql-primary
    port: 3306
    username: root
    password: ${MYSQL_ROOT_PASSWORD}
    
  secondary:
    host: mysql-secondary
    port: 3306
    username: readonly
    password: ${MYSQL_READONLY_PASSWORD}
```

### 数据一致性保证

#### 1. 分布式事务
```java
@Transactional
@GlobalTransactional // Seata分布式事务
public class ExamService {
    
    public void submitExam(Long examId, Long userId, List<Answer> answers) {
        // 更新考试状态
        examRepository.updateStatus(examId, COMPLETED);
        
        // 计算成绩
        Score score = calculateScore(answers);
        scoreService.saveScore(score); // 跨服务调用
        
        // 发送通知
        notificationService.sendResult(userId, score); // 跨服务调用
    }
}
```

#### 2. 最终一致性
```java
// 基于事件的最终一致性
@EventListener
public class ExamEventHandler {
    
    @Async
    @EventListener(ExamCompletedEvent.class)
    public void handleExamCompleted(ExamCompletedEvent event) {
        // 异步更新统计数据
        statisticsService.updateExamStats(event.getExamId());
        
        // 异步发送通知
        notificationService.notifyResult(event.getUserId());
    }
}
```

---

## 安全架构

### 安全策略层次

#### 1. 网络安全
- **HTTPS**: 全站HTTPS加密传输
- **防火墙**: 限制不必要的端口访问
- **VPN**: 管理员访问需要VPN
- **DDoS防护**: 通过CDN和限流实现

#### 2. 应用安全
- **JWT认证**: 无状态token认证
- **角色权限**: RBAC权限控制
- **API限流**: 防止API滥用
- **输入验证**: 防止注入攻击

#### 3. 数据安全
- **数据加密**: 敏感数据加密存储
- **访问控制**: 行级安全策略
- **审计日志**: 完整的操作审计
- **备份加密**: 备份数据加密

### 认证与授权架构

#### JWT认证流程
```
┌─────────┐    1.登录请求    ┌──────────────┐
│  客户端  │ ──────────────→ │   用户服务    │
│         │                 │             │
│         │ ←────────────── │             │
└─────────┘   2.返回JWT      └──────────────┘
     │                            
     │ 3.携带JWT访问
     ▼
┌─────────┐    4.验证JWT     ┌──────────────┐
│API网关  │ ──────────────→ │   业务服务    │
│         │                 │             │
│         │ ←────────────── │             │
└─────────┘   5.返回数据      └──────────────┘
```

#### 权限控制模型
```java
// RBAC权限模型
@Entity
public class User {
    private Set<Role> roles;
}

@Entity  
public class Role {
    private Set<Permission> permissions;
}

@Entity
public class Permission {
    private String resource;
    private String action;
}

// 权限检查
@PreAuthorize("hasPermission('exam', 'read')")
public List<Exam> getAvailableExams() {
    return examRepository.findAvailable();
}
```

### 监考安全机制

#### 1. 防作弊技术
- **摄像头监控**: 实时视频监控
- **屏幕监控**: 屏幕共享检测
- **键盘鼠标监控**: 异常操作检测
- **AI行为分析**: 机器学习异常检测

#### 2. 数据完整性
```javascript
// 答案提交完整性校验
function submitAnswer(questionId, answer) {
    const timestamp = Date.now();
    const checksum = crypto
        .createHash('sha256')
        .update(`${questionId}-${answer}-${timestamp}-${sessionKey}`)
        .digest('hex');
    
    return {
        questionId,
        answer,
        timestamp,
        checksum
    };
}
```

### 安全配置示例

#### Nginx安全配置
```nginx
# SSL配置
ssl_certificate /etc/ssl/certs/exam-system.crt;
ssl_certificate_key /etc/ssl/private/exam-system.key;
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;

# 安全头
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
add_header X-XSS-Protection "1; mode=block";
add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";

# 限流配置
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req zone=api burst=20 nodelay;
```

---

## 性能架构

### 性能目标
- **响应时间**: API响应 < 200ms，页面加载 < 3s
- **并发用户**: 支持1000+并发考试
- **可用性**: 99.9%系统可用性
- **吞吐量**: 10000+ API请求/分钟

### 缓存策略

#### 1. 多级缓存架构
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  浏览器缓存  │    │   CDN缓存   │    │  应用缓存   │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                          │
               ┌─────────────┐
               │  数据库缓存  │
               └─────────────┘
```

#### 2. Redis缓存配置
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
                
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

#### 3. 缓存使用示例
```java
@Service
public class QuestionService {
    
    @Cacheable(value = "questions", key = "#categoryId")
    public List<Question> getQuestionsByCategory(Long categoryId) {
        return questionRepository.findByCategory(categoryId);
    }
    
    @CacheEvict(value = "questions", key = "#question.categoryId")
    public Question updateQuestion(Question question) {
        return questionRepository.save(question);
    }
}
```

### 数据库性能优化

#### 1. 索引策略
```sql
-- 复合索引优化查询
CREATE INDEX idx_exam_attempts_user_exam ON exam_attempts (user_id, exam_id, started_at);

-- 部分索引减少索引大小
CREATE INDEX idx_active_exams ON exams (created_at) WHERE status = 'ACTIVE';

-- JSON字段索引
CREATE INDEX idx_question_metadata ON questions USING GIN (metadata);
```

#### 2. 查询优化
```java
// 批量操作减少数据库交互
@Modifying
@Query("UPDATE exam_attempts SET status = :status WHERE exam_id = :examId")
void updateExamStatus(@Param("examId") Long examId, @Param("status") String status);

// 分页查询避免全表扫描
public Page<ExamResult> getExamResults(Long examId, Pageable pageable) {
    return examResultRepository.findByExamIdOrderByScoreDesc(examId, pageable);
}
```

### 负载均衡与扩展

#### 1. 水平扩展配置
```yaml
# Docker Compose扩展配置
version: '3.8'
services:
  user-service:
    image: exam-system/user-service
    deploy:
      replicas: 3
    networks:
      - exam-network
```

#### 2. Nginx负载均衡
```nginx
upstream user_service {
    least_conn;
    server user-service-1:8080 weight=1;
    server user-service-2:8080 weight=1;
    server user-service-3:8080 weight=1;
}

location /api/users/ {
    proxy_pass http://user_service;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 部署架构

### 容器化部署

#### 1. Docker镜像构建
```dockerfile
# Java服务Dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app
COPY target/user-service.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. Docker Compose编排
```yaml
version: '3.8'

services:
  # 数据库服务
  mysql-primary:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: exam_system
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - exam-network

  # Redis缓存
  redis-master:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    networks:
      - exam-network

  # 业务服务
  user-service:
    build: ./user-service
    environment:
      SPRING_PROFILES_ACTIVE: production
      DB_HOST: mysql-primary
      REDIS_HOST: redis-master
    depends_on:
      - mysql-primary
      - redis-master
    networks:
      - exam-network

  # API网关
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/ssl
    depends_on:
      - user-service
      - exam-service
    networks:
      - exam-network

volumes:
  mysql_data:
  redis_data:

networks:
  exam-network:
    driver: bridge
```

### 环境配置

#### 1. 环境变量配置
```bash
# .env文件
# 数据库配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_USER=exam_user
MYSQL_PASSWORD=exam_password

# Redis配置
REDIS_PASSWORD=redis_password

# JWT配置
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=86400000

# 邮件配置
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
```

#### 2. 应用配置文件
```yaml
# application-production.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/exam_system
    username: ${DB_USER:root}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
  redis:
    host: ${REDIS_HOST:localhost}
    port: 6379
    password: ${REDIS_PASSWORD}
    
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    
logging:
  level:
    com.examSystem: INFO
  file:
    name: /app/logs/application.log
```

### 部署流程

#### 1. 自动化部署脚本
```bash
#!/bin/bash
# deploy.sh

set -e

echo "开始部署在线考试系统..."

# 1. 拉取最新代码
git pull origin main

# 2. 构建应用
./mvnw clean package -DskipTests

# 3. 构建Docker镜像
docker-compose build

# 4. 停止旧服务
docker-compose down

# 5. 启动新服务
docker-compose up -d

# 6. 健康检查
./scripts/health-check.sh

echo "部署完成！"
```

#### 2. 滚动更新
```bash
#!/bin/bash
# rolling-update.sh

SERVICE_NAME=$1
NEW_IMAGE=$2

echo "开始滚动更新 $SERVICE_NAME..."

# 逐个更新服务实例
for i in {1..3}; do
    echo "更新实例 $i..."
    docker-compose stop ${SERVICE_NAME}-${i}
    docker-compose rm -f ${SERVICE_NAME}-${i}
    docker-compose up -d ${SERVICE_NAME}-${i}
    
    # 等待健康检查通过
    sleep 30
    ./scripts/health-check.sh ${SERVICE_NAME}-${i}
done

echo "滚动更新完成！"
```

---

## 监控与运维架构

### 监控体系

#### 1. 应用监控
```java
// Spring Boot Actuator监控端点
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        boolean databaseConnected = checkDatabaseConnection();
        boolean redisConnected = checkRedisConnection();
        
        if (databaseConnected && redisConnected) {
            return Health.up()
                .withDetail("database", "connected")
                .withDetail("redis", "connected")
                .build();
        }
        
        return Health.down()
            .withDetail("database", databaseConnected ? "connected" : "disconnected")
            .withDetail("redis", redisConnected ? "connected" : "disconnected")
            .build();
    }
}
```

#### 2. 性能监控
```yaml
# Prometheus配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 3. 日志监控
```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/app/logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/app/logs/application.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 运维自动化

#### 1. 健康检查脚本
```bash
#!/bin/bash
# health-check.sh

check_service() {
    local service_name=$1
    local service_url=$2
    
    echo "检查服务 $service_name..."
    
    response=$(curl -s -o /dev/null -w "%{http_code}" $service_url)
    
    if [ $response -eq 200 ]; then
        echo "✓ $service_name 健康"
        return 0
    else
        echo "✗ $service_name 不健康 (HTTP $response)"
        return 1
    fi
}

# 检查所有服务
check_service "API网关" "http://localhost/health"
check_service "用户服务" "http://localhost:8080/actuator/health"
check_service "考试服务" "http://localhost:8081/actuator/health"
check_service "题库服务" "http://localhost:8082/actuator/health"
```

#### 2. 备份脚本
```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)

echo "开始系统备份..."

# 数据库备份
docker-compose exec -T mysql-primary mysqldump \
    -uroot -p${MYSQL_ROOT_PASSWORD} \
    --single-transaction \
    --routines \
    --triggers \
    exam_system > ${BACKUP_DIR}/mysql_${DATE}.sql

# Redis备份
docker-compose exec -T redis-master redis-cli BGSAVE
docker cp $(docker-compose ps -q redis-master):/data/dump.rdb \
    ${BACKUP_DIR}/redis_${DATE}.rdb

# 配置文件备份
tar -czf ${BACKUP_DIR}/config_${DATE}.tar.gz \
    docker-compose.yml .env nginx.conf

echo "备份完成: ${BACKUP_DIR}"
```

---

## 扩展性设计

### 水平扩展策略

#### 1. 微服务扩展
```yaml
# 服务扩展配置
version: '3.8'
services:
  user-service:
    image: exam-system/user-service
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
      restart_policy:
        condition: on-failure
```

#### 2. 数据库扩展
```sql
-- 读写分离配置
-- 主库配置
server-id = 1
log-bin = mysql-bin
binlog-format = ROW

-- 从库配置  
server-id = 2
relay-log = mysql-relay-bin
read-only = 1
```

#### 3. 缓存扩展
```yaml
# Redis集群配置
version: '3.8'
services:
  redis-master:
    image: redis:7-alpine
    command: redis-server --port 6379 --cluster-enabled yes
    
  redis-slave-1:
    image: redis:7-alpine
    command: redis-server --port 6379 --slaveof redis-master 6379
    
  redis-slave-2:
    image: redis:7-alpine
    command: redis-server --port 6379 --slaveof redis-master 6379
```

### 垂直扩展策略

#### 1. 资源优化
```java
// JVM调优参数
-Xms512m -Xmx1024m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
```

#### 2. 连接池优化
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

### 云原生扩展

#### 1. Kubernetes部署
```yaml
# k8s-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: exam-system/user-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_HOST
          value: "mysql-service"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
```

#### 2. 自动扩展
```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 架构决策记录

### ADR-001: 微服务架构选择

**状态**: 已接受

**背景**: 系统需要支持高并发、高可用，团队需要独立开发部署

**决策**: 采用微服务架构

**后果**: 
- 优点: 独立扩展、技术栈多样性、故障隔离
- 缺点: 复杂性增加、分布式事务、网络延迟

### ADR-002: 数据库选择

**状态**: 已接受

**背景**: 需要事务支持、复杂查询、数据一致性

**决策**: PostgreSQL作为主数据库，Redis作为缓存

**后果**:
- 优点: ACID事务、丰富特性、高性能缓存
- 缺点: 运维复杂性、内存消耗

### ADR-003: 容器化部署

**状态**: 已接受

**背景**: 环境一致性、快速部署、资源利用率

**决策**: 使用Docker + Docker Compose

**后果**:
- 优点: 环境一致、快速部署、资源隔离
- 缺点: 学习成本、调试复杂

### ADR-004: 认证方案

**状态**: 已接受

**背景**: 无状态认证、跨服务访问、移动端支持

**决策**: JWT令牌认证

**后果**:
- 优点: 无状态、跨域支持、移动友好
- 缺点: 令牌撤销困难、载荷大小限制

---

## 总结

本系统架构设计遵循微服务架构原则，采用现代化技术栈，实现了高可用、高性能、可扩展的在线考试系统。架构设计考虑了以下关键因素：

1. **业务领域分离**: 按业务职责划分微服务
2. **技术栈统一**: 使用成熟稳定的技术栈
3. **数据一致性**: 保证分布式环境下的数据一致性
4. **安全性**: 多层次安全防护
5. **性能优化**: 多级缓存和数据库优化
6. **运维友好**: 容器化部署和监控体系
7. **扩展性**: 支持水平和垂直扩展

该架构为系统的长期发展和维护提供了坚实的技术基础，支持团队高效协作和系统持续演进。