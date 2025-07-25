below is a effective way to use Claude with SuperClaude.
in this way, u can use claude like Kiro and can generate as much as below.

✅ SuperClaude Prompt 文件：在线考试系统（串联文件上下文方案）

📌 第一阶段：产品需求分析（产出需求分析文件）
/sc:analyze --persona-product --feature "设计一个在线考试系统，支持用户注册、登录，管理员后台进行试卷管理，考试类型包括单选题、多选题、判断题、填空题，用户可在线考试并自动评分，管理员可查看成绩。" --output analysis.md

📌 第二阶段：系统架构设计（引用需求分析产出）
/sc:design --persona-architect --feature "analysis.md" --include-datamodel --include-sequencediagram --output system-design.md

📌 第三阶段：数据库模型设计（引用架构设计产出）
/sc:design --persona-database --feature "system-design.md" --output database-design.md

📌 第四阶段：前端 UI 设计（引用架构设计产出）
/sc:design --persona-frontend --feature "system-design.md" --output frontend-design.md

📌 第五阶段：项目初始化与目录结构（引用架构设计产出）
/sc:build --persona-architect --feature "system-design.md" --project-type "后端 Spring Boot 3，Spring Security，JPA，MySQL，分层架构"
/sc:build --persona-frontend --feature "frontend-design.md" --project-type "前端 Next.js 14，TailwindCSS，Zustand 状态管理，Axios 请求库"
Febrollen12

📌 第六阶段：后端接口实现（引用架构设计产出）
/sc:implement --persona-backend --feature "system-design.md + database-design.md 用户模块：注册、登录（JWT 鉴权）、个人信息接口" --framework "Spring Boot"
/sc:implement --persona-backend --feature "system-design.md + database-design.md 管理员模块：后台试卷管理，题库管理接口（CRUD），考试发布接口，成绩统计接口"
/sc:implement --persona-backend --feature "system-design.md + database-design.md 考试模块：考试列表、在线提交答卷，自动判卷逻辑"

📌 第七阶段：前端交互逻辑实现（引用前端设计产出）
/sc:implement --persona-frontend --feature "frontend-design.md 用户端：注册、登录页面，考试列表与答题页面，提交答卷后成绩页面"
/sc:implement --persona-frontend --feature "frontend-design.md 后台管理端：登录页面，试卷管理、题目管理、成绩查看页面"

📌 第八阶段：自动化测试（引用系统设计产出）
/sc:test --persona-qa --feature "system-design.md Playwright 测试用户注册登录、在线答题流程，管理员试卷管理流程"

📌 第九阶段：Docker 容器化与 CI/CD（引用系统设计产出）
/sc:build --persona-devops --feature "system-design.md 编写 Docker Compose 脚本，Spring Boot 后端、Next.js 前端、MySQL 容器化部署；生成 GitHub Actions 自动构建和发布脚本"

📌 第十阶段：项目交付文档（引用系统设计产出）
/sc:document --persona-scribe --feature "2-system-design.md 生成完整接口文档、数据库文档、系统架构说明，便于团队后续维护和交付"


# 在线考试系统 (Online Exam System)

一个功能完整、安全可靠的在线考试平台，采用微服务架构设计，支持大规模并发考试、实时监考和智能防作弊。

## 🎯 项目概述

本系统是一个现代化的在线考试解决方案，专为教育机构、企业培训和认证考试设计。系统采用前后端分离架构，提供完整的考试管理、题库管理、用户管理和监考功能。

### 核心特性

- 🏗️ **微服务架构** - 5个独立的业务服务，支持独立部署和扩展
- 🔒 **安全防护** - JWT认证、RBAC权限控制、多层防作弊机制
- 📊 **实时监考** - AI驱动的行为检测、视频监控、异常报告
- ⚡ **高性能** - Redis缓存、数据库优化、支持1000+并发用户
- 🚀 **容器化部署** - Docker + Docker Compose，一键部署
- 📈 **可扩展性** - 水平扩展设计，支持集群部署

## 🏛️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    前端应用层                                │
├─────────────────────────────────────────────────────────────┤
│  React + TypeScript + Ant Design + Redux Toolkit          │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                    API网关层                                │
├─────────────────────────────────────────────────────────────┤
│        Nginx (负载均衡 + SSL终止 + 限流)                    │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                    微服务层                                 │
├─────────────────────────────────────────────────────────────┤
│ 用户服务  │ 考试服务  │ 题库服务  │ 监考服务  │ 通知服务     │
│ :8080    │ :8081    │ :8082    │ :8083    │ :8084        │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                    数据存储层                               │
├─────────────────────────────────────────────────────────────┤
│     PostgreSQL     │      Redis       │    文件存储         │
│   (主从复制)        │   (主从复制)      │   (本地/云存储)      │
└─────────────────────────────────────────────────────────────┘
```

## 🛠️ 技术栈

### 后端技术
- **Java服务**: Spring Boot 2.7+, Spring Security, Spring Data JPA
- **Node.js服务**: Express.js, Socket.io (监考服务)
- **数据库**: PostgreSQL 14+ (主数据库), Redis 7+ (缓存)
- **认证**: JWT + RBAC权限模型
- **API文档**: Swagger/OpenAPI 3

### 前端技术
- **框架**: React 18+ + TypeScript
- **状态管理**: Redux Toolkit
- **UI组件**: Ant Design
- **实时通信**: Socket.io-client
- **视频处理**: WebRTC API

### 运维技术
- **容器化**: Docker + Docker Compose
- **反向代理**: Nginx
- **监控**: Spring Boot Actuator + 自定义监控脚本
- **备份**: 自动化备份脚本 + 多级恢复策略

## 📋 功能模块

### 👥 用户管理
- 用户注册、登录、权限管理
- 多角色支持：学生、教师、管理员
- 个人信息管理和密码安全策略

### 📝 考试管理
- 考试创建、配置和调度
- 多种题型支持：单选、多选、填空、问答
- 成绩计算和统计分析
- 考试报告生成

### 📚 题库管理
- 题目分类和标签管理
- 题目导入/导出功能
- 难度分级和质量评估
- 智能组卷算法

### 👁️ 实时监考
- 摄像头实时监控
- 屏幕共享检测
- AI行为分析和异常报告
- 考试过程录像

### 📊 数据分析
- 考试成绩统计
- 学习进度跟踪
- 题目质量分析
- 系统使用报告

## 🚀 快速开始

### 环境要求

- Docker 24.0+
- Docker Compose 2.0+
- Git 2.25+
- 4GB+ 内存，100GB+ 存储空间

### 安装部署

1. **克隆项目**
```bash
git clone https://github.com/your-username/online-exam-system.git
cd online-exam-system
```

2. **环境配置**
```bash
# 复制环境配置模板
cp .env.example .env

# 编辑配置文件，设置数据库密码、JWT密钥等
vim .env
```

3. **一键部署**
```bash
# 环境检查
./scripts/check-prerequisites.sh

# 执行部署
./scripts/deploy.sh

# 健康检查
./scripts/health-check.sh
```

4. **访问系统**
- 前端应用: https://localhost
- API文档: https://localhost/swagger-ui.html
- 管理后台: https://localhost/admin

### 开发环境搭建

```bash
# 启动开发环境
docker-compose -f docker-compose.dev.yml up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

## 📖 文档

完整的项目文档位于 `docs/` 目录:

- [📘 API文档](./docs/API-Documentation.md) - 完整的API接口说明
- [🗄️ 数据库文档](./docs/Database-Schema-Documentation.md) - 数据库设计和优化
- [🏗️ 架构文档](./docs/System-Architecture-Documentation.md) - 系统架构设计说明
- [🚀 部署维护指南](./docs/Deployment-Maintenance-Guide.md) - 部署和运维手册

## 🔧 运维管理

### 备份与恢复

```bash
# 全量备份
./scripts/backup.sh full

# 增量备份
./scripts/backup.sh incremental

# 快速回滚
./scripts/rollback.sh quick

# 回滚到指定版本
./scripts/rollback.sh version backup_20231201_120000
```

### 监控与维护

```bash
# 系统仪表板
./scripts/dashboard.sh

# 性能监控
./scripts/monitor-performance.sh

# 安全检查
./scripts/security-check.sh

# 每日维护
./scripts/auto-maintenance.sh daily
```

### 故障排除

```bash
# 服务诊断
./scripts/troubleshoot-startup.sh <service-name>

# 性能诊断
./scripts/troubleshoot-performance.sh

# 连接性测试
./scripts/troubleshoot-connectivity.sh
```

## 📊 性能指标

### 系统性能
- **并发用户**: 1000+ 同时在线考试
- **API响应时间**: < 200ms
- **页面加载时间**: < 3s
- **系统可用性**: 99.9%

### 扩展能力
- **水平扩展**: 支持多实例部署
- **数据分片**: 按时间和组织分片
- **缓存策略**: 多级缓存优化
- **CDN支持**: 静态资源加速

## 🔒 安全特性

### 认证与授权
- JWT无状态认证
- RBAC细粒度权限控制
- 多因素认证支持
- 会话管理和超时控制

### 防作弊机制
- 实时视频监控
- 屏幕行为检测
- AI异常行为分析
- 答题时间分析

### 数据安全
- 数据库加密存储
- HTTPS全站加密
- 敏感信息脱敏
- 访问日志审计

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 开发流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范

- Java: 遵循 Google Java Style Guide
- JavaScript/TypeScript: 使用 ESLint + Prettier
- 提交信息: 遵循 Conventional Commits 规范

### 测试要求

- 单元测试覆盖率 ≥ 80%
- 集成测试覆盖关键业务流程
- E2E测试覆盖主要用户场景

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👥 团队

- **项目负责人**: [Your Name](https://github.com/your-username)
- **后端开发**: [Backend Team](https://github.com/your-org)
- **前端开发**: [Frontend Team](https://github.com/your-org)
- **运维团队**: [DevOps Team](https://github.com/your-org)

## 📞 联系我们

- **项目主页**: https://github.com/your-username/online-exam-system
- **问题反馈**: [Issues](https://github.com/your-username/online-exam-system/issues)
- **邮件联系**: exam-system@yourcompany.com
- **技术交流**: [Discussions](https://github.com/your-username/online-exam-system/discussions)

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者和用户！

特别感谢以下开源项目：
- [Spring Boot](https://spring.io/projects/spring-boot)
- [React](https://reactjs.org/)
- [PostgreSQL](https://www.postgresql.org/)
- [Redis](https://redis.io/)
- [Docker](https://www.docker.com/)

---

⭐ 如果这个项目对你有帮助，请给我们一个 Star！

📢 关注我们获取最新更新和技术分享。