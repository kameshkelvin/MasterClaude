# 考试系统用户服务 (User Service)

基于Spring Boot的微服务架构用户管理系统，提供用户注册、登录、认证授权等核心功能。

## 🚀 功能特性

### 用户认证
- ✅ 用户注册（支持用户名、邮箱、手机号）
- ✅ 用户登录/登出
- ✅ JWT Token认证
- ✅ Token刷新机制
- ✅ 密码加密存储（BCrypt）

### 用户管理  
- ✅ 用户资料管理
- ✅ 密码修改
- ✅ 用户状态管理（激活/禁用/锁定）
- ✅ 角色权限管理
- ✅ 用户查询（分页、搜索）

### 安全特性
- ✅ Spring Security集成
- ✅ CORS跨域配置
- ✅ 全局异常处理
- ✅ 输入参数验证
- ✅ SQL注入防护

### 系统特性
- ✅ RESTful API设计
- ✅ Swagger API文档
- ✅ 多环境配置支持
- ✅ 数据库审计
- ✅ 单元测试覆盖

## 🛠 技术栈

- **框架**: Spring Boot 3.2.0
- **语言**: Java 17
- **数据库**: PostgreSQL
- **缓存**: Redis
- **认证**: JWT + Spring Security
- **文档**: SpringDoc OpenAPI 3
- **测试**: JUnit 5 + Mockito
- **构建**: Maven

## 📋 API接口

### 认证管理 (`/api/v1/auth`)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/register` | 用户注册 | ❌ |
| POST | `/login` | 用户登录 | ❌ |
| POST | `/refresh` | 刷新Token | ❌ |
| POST | `/logout` | 用户登出 | ✅ |
| GET | `/verify` | 验证Token | ❌ |

### 用户管理 (`/api/v1/users`)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/me` | 获取当前用户信息 | ✅ |
| PUT | `/me/profile` | 更新用户资料 | ✅ |
| PUT | `/me/password` | 修改密码 | ✅ |
| GET | `/me/stats` | 获取用户统计 | ✅ |
| GET | `/{userId}` | 获取指定用户信息 | ✅ (管理员) |
| GET | `/` | 分页查询用户列表 | ✅ (管理员) |
| PUT | `/{userId}/status` | 更新用户状态 | ✅ (管理员) |
| DELETE | `/{userId}` | 删除用户 | ✅ (管理员) |

## 🗄️ 数据库设计

### 核心表结构

- **users** - 用户基础信息表
- **user_roles** - 用户角色关联表  
- **user_sessions** - 用户会话管理表

详细的数据库设计请参考 `database-design.md` 文档。

## 🚀 快速开始

### 1. 环境准备

```bash
# Java 17
java -version

# PostgreSQL 13+
createdb exam_system

# Redis (可选)
redis-server
```

### 2. 克隆项目

```bash
git clone <repository-url>
cd user-service
```

### 3. 配置数据库

修改 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/exam_system
    username: your_username
    password: your_password
```

### 4. 运行应用

```bash
# 开发环境
mvn spring-boot:run

# 或者构建后运行
mvn clean package
java -jar target/user-service-1.0.0.jar
```

### 5. 访问API文档

打开浏览器访问: http://localhost:8081/swagger-ui.html

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=UserServiceTest

# 生成测试报告
mvn surefire-report:report
```

## 📝 配置说明

### 环境配置

- **开发环境**: `application-dev.yml`
- **测试环境**: `application-test.yml`  
- **生产环境**: `application-prod.yml`

### JWT配置

```yaml
jwt:
  secret: your-secret-key
  expiration: 3600000  # 1小时
  refresh-expiration: 604800000  # 7天
```

### 安全配置

```yaml
app:
  security:
    password-strength: 12  # BCrypt强度
    max-login-attempts: 5
    lockout-duration: 900  # 锁定时间(秒)
```

## 🔧 开发指南

### 代码结构

```
src/main/java/com/examSystem/userService/
├── config/          # 配置类
├── controller/      # 控制器
├── dto/            # 数据传输对象
├── entity/         # JPA实体
├── exception/      # 异常处理
├── repository/     # 数据访问层
├── security/       # 安全配置
└── service/        # 业务逻辑层
```

### 编码规范

- 遵循阿里巴巴Java开发手册
- 使用统一的代码格式化配置
- 方法和类需要完整的JavaDoc注释
- 异常处理必须记录日志

### 提交规范

```bash
# 功能开发
git commit -m "feat: 添加用户注册功能"

# 问题修复  
git commit -m "fix: 修复登录验证逻辑错误"

# 文档更新
git commit -m "docs: 更新API文档"
```

## 🚀 部署

### Docker部署

```bash
# 构建镜像
docker build -t user-service:1.0.0 .

# 运行容器
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=localhost \
  -e DB_USER=exam_user \
  -e DB_PASSWORD=exam_password \
  user-service:1.0.0
```

### 生产环境注意事项

1. **安全配置**
   - 使用强密码的JWT密钥
   - 配置HTTPS证书
   - 启用审计日志

2. **性能优化**
   - 配置数据库连接池
   - 启用Redis缓存
   - 调整JVM参数

3. **监控告警**
   - 集成Prometheus监控
   - 配置日志聚合
   - 设置健康检查

## 📞 支持

- **文档**: [系统设计文档](system-design.md)
- **API文档**: http://localhost:8081/swagger-ui.html
- **问题反馈**: 请创建GitHub Issue
- **邮箱支持**: dev@examSystem.com

## 📄 许可证

本项目采用 MIT 许可证，详情请参阅 [LICENSE](LICENSE) 文件。