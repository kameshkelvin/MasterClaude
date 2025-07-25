# 在线考试系统 - 系统设计文档

## 系统概述

基于产品需求分析，设计一个支持10K+并发用户、99.9%可用性的企业级在线考试系统。采用云原生微服务架构，具备高可扩展性、安全性和可维护性。

### 核心设计原则
- **可扩展性优先**: 水平扩展支持，弹性伸缩
- **安全性内建**: 多层防护，零信任架构
- **高可用性**: 故障隔离，自动恢复
- **性能优化**: 缓存策略，异步处理
- **监控可观测**: 全链路追踪，实时监控

---

## 1. 系统架构设计

### 1.1 整体架构

```mermaid
graph TB
    subgraph "用户层"
        WEB[Web前端<br/>React/Vue.js]
        MOBILE[移动端<br/>React Native/PWA]
        ADMIN[管理后台<br/>Admin Dashboard]
    end
    
    subgraph "接入层"
        CDN[CDN全球加速<br/>CloudFlare/AWS CloudFront]
        WAF[Web应用防火墙<br/>DDoS防护]
        LB[负载均衡器<br/>Nginx/HAProxy]
    end
    
    subgraph "网关层"
        API_GW[API网关<br/>Kong/AWS API Gateway]
        AUTH_MW[认证中间件<br/>JWT验证]
        RATE_LIMIT[限流熔断<br/>Redis Sentinel]
    end
    
    subgraph "业务服务层"
        USER_SVC[用户服务<br/>User Service]
        AUTH_SVC[认证服务<br/>Auth Service]
        EXAM_SVC[考试服务<br/>Exam Service]
        QUESTION_SVC[题库服务<br/>Question Service]
        PROCTORING_SVC[监考服务<br/>Proctoring Service]
        ANALYTICS_SVC[分析服务<br/>Analytics Service]
        NOTIFICATION_SVC[通知服务<br/>Notification Service]
    end
    
    subgraph "数据层"
        POSTGRES[(PostgreSQL<br/>主数据库)]
        REDIS[(Redis集群<br/>缓存/会话)]
        ES[(Elasticsearch<br/>搜索分析)]
        MONGO[(MongoDB<br/>非结构化数据)]
        S3[对象存储<br/>AWS S3/MinIO]
    end
    
    subgraph "基础设施层"
        MQ[消息队列<br/>RabbitMQ/Kafka]
        MONITOR[监控系统<br/>Prometheus/Grafana]
        LOG[日志系统<br/>ELK Stack]
        BACKUP[备份系统<br/>自动备份]
    end
    
    WEB --> CDN
    MOBILE --> CDN
    ADMIN --> CDN
    CDN --> WAF
    WAF --> LB
    LB --> API_GW
    API_GW --> AUTH_MW
    AUTH_MW --> RATE_LIMIT
    
    RATE_LIMIT --> USER_SVC
    RATE_LIMIT --> AUTH_SVC
    RATE_LIMIT --> EXAM_SVC
    RATE_LIMIT --> QUESTION_SVC
    RATE_LIMIT --> PROCTORING_SVC
    RATE_LIMIT --> ANALYTICS_SVC
    RATE_LIMIT --> NOTIFICATION_SVC
    
    USER_SVC --> POSTGRES
    AUTH_SVC --> POSTGRES
    EXAM_SVC --> POSTGRES
    QUESTION_SVC --> POSTGRES
    PROCTORING_SVC --> MONGO
    ANALYTICS_SVC --> ES
    
    USER_SVC --> REDIS
    AUTH_SVC --> REDIS
    EXAM_SVC --> REDIS
    
    QUESTION_SVC --> S3
    PROCTORING_SVC --> S3
    
    USER_SVC --> MQ
    EXAM_SVC --> MQ
    NOTIFICATION_SVC --> MQ
    
    API_GW --> MONITOR
    USER_SVC --> MONITOR
    EXAM_SVC --> MONITOR
    
    API_GW --> LOG
    USER_SVC --> LOG
    EXAM_SVC --> LOG
```

### 1.2 架构特点

#### 微服务拆分策略
- **领域驱动设计**: 按业务领域拆分服务边界
- **数据库分离**: 每个服务独立数据库，避免数据耦合
- **API版本管理**: 支持平滑升级和向后兼容
- **服务治理**: 服务注册发现、熔断降级

#### 高可用保障
- **多活部署**: 跨可用区部署，故障自动切换
- **服务冗余**: 关键服务多实例部署
- **数据备份**: 实时备份和异地容灾
- **监控告警**: 7x24小时监控和自动告警

---

## 2. 数据模型设计

### 2.1 核心实体关系图

```mermaid
erDiagram
    %% 用户体系
    User ||--o{ UserRole : "has"
    User ||--o{ ExamAttempt : "takes"
    User ||--o{ UserSession : "creates"
    
    User {
        bigint id PK "用户ID"
        varchar username UK "用户名"
        varchar email UK "邮箱"
        varchar password_hash "密码哈希"
        varchar first_name "姓"
        varchar last_name "名" 
        varchar phone "手机号"
        varchar avatar_url "头像URL"
        jsonb profile "用户资料"
        boolean is_active "是否激活"
        boolean is_verified "是否验证"
        timestamp created_at "创建时间"
        timestamp updated_at "更新时间"
        timestamp last_login "最后登录"
    }
    
    UserRole {
        bigint id PK
        bigint user_id FK
        varchar role "角色(student/teacher/admin)"
        varchar organization "所属机构"
        jsonb permissions "权限配置"
        timestamp created_at
        timestamp expires_at "过期时间"
    }
    
    UserSession {
        varchar session_id PK "会话ID"
        bigint user_id FK
        varchar device_type "设备类型"
        varchar ip_address "IP地址"
        varchar user_agent "用户代理"
        jsonb metadata "会话元数据"
        timestamp created_at
        timestamp expires_at
        timestamp last_activity "最后活动"
    }
    
    %% 组织机构
    Organization ||--o{ Course : "offers"
    Organization ||--o{ User : "employs"
    
    Organization {
        bigint id PK "机构ID"
        varchar name "机构名称"
        varchar code UK "机构代码"
        varchar type "机构类型"
        jsonb settings "机构配置"
        jsonb branding "品牌设置"
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    
    %% 课程体系
    Course ||--o{ Exam : "contains"
    Course ||--o{ Enrollment : "has"
    Course ||--o{ CourseCategory : "belongs_to"
    
    Course {
        bigint id PK "课程ID"
        varchar title "课程标题"
        text description "课程描述"
        varchar code UK "课程代码"
        bigint instructor_id FK "讲师ID"
        bigint organization_id FK "机构ID"
        varchar status "状态(draft/active/archived)"
        jsonb metadata "课程元数据"
        timestamp start_date "开始日期"
        timestamp end_date "结束日期"
        timestamp created_at
        timestamp updated_at
    }
    
    CourseCategory {
        bigint id PK "分类ID"
        varchar name "分类名称"
        varchar code UK "分类代码"
        bigint parent_id FK "父分类ID"
        int sort_order "排序"
        boolean is_active
    }
    
    Enrollment {
        bigint id PK "注册ID"
        bigint user_id FK
        bigint course_id FK
        varchar status "状态(enrolled/completed/dropped)"
        decimal progress "学习进度"
        timestamp enrolled_at "注册时间"
        timestamp completed_at "完成时间"
    }
    
    %% 考试体系
    Exam ||--o{ ExamQuestion : "contains"
    Exam ||--o{ ExamAttempt : "attempted_by"
    Exam ||--o{ ExamRule : "has_rules"
    
    Exam {
        bigint id PK "考试ID"
        varchar title "考试标题"
        text description "考试描述"
        varchar code UK "考试代码"
        bigint course_id FK
        bigint created_by FK "创建者ID"
        varchar type "类型(quiz/midterm/final/certification)"
        varchar status "状态(draft/published/archived)"
        int duration_minutes "时长(分钟)"
        int max_attempts "最大尝试次数"
        decimal passing_score "及格分数"
        timestamp start_time "开始时间"
        timestamp end_time "结束时间"
        jsonb settings "考试设置"
        jsonb proctoring_config "监考配置"
        boolean shuffle_questions "随机排序题目"
        boolean shuffle_options "随机排序选项"
        timestamp created_at
        timestamp updated_at
        timestamp published_at "发布时间"
    }
    
    ExamRule {
        bigint id PK "规则ID"
        bigint exam_id FK
        varchar rule_type "规则类型"
        jsonb rule_config "规则配置"
        int priority "优先级"
        boolean is_active
    }
    
    %% 题库体系
    Question ||--o{ ExamQuestion : "used_in"
    Question ||--o{ Answer : "has_answers"
    Question ||--o{ QuestionTag : "tagged_with"
    QuestionCategory ||--o{ Question : "contains"
    
    Question {
        bigint id PK "题目ID"
        varchar title "题目标题"
        text content "题目内容"
        varchar type "题型(single_choice/multiple_choice/true_false/fill_blank/essay/coding)"
        varchar difficulty "难度(easy/medium/hard)"
        decimal default_points "默认分值"
        jsonb options "选项配置"
        jsonb correct_answer "正确答案"
        text explanation "解析"
        jsonb metadata "题目元数据"
        varchar media_type "媒体类型"
        varchar media_url "媒体URL"
        bigint created_by FK "创建者ID"
        bigint category_id FK "分类ID"
        varchar status "状态(draft/active/archived)"
        int usage_count "使用次数"
        decimal avg_score "平均得分"
        timestamp created_at
        timestamp updated_at
    }
    
    QuestionCategory {
        bigint id PK "题目分类ID"
        varchar name "分类名称"
        varchar code UK "分类代码"
        bigint parent_id FK "父分类ID"
        text description "分类描述"
        int sort_order "排序"
        boolean is_active
    }
    
    QuestionTag {
        bigint id PK "标签ID"
        bigint question_id FK
        varchar tag "标签名"
        varchar tag_type "标签类型"
    }
    
    ExamQuestion {
        bigint id PK "考试题目关联ID"
        bigint exam_id FK
        bigint question_id FK
        int order_number "题目顺序"
        decimal points "分值"
        jsonb custom_config "自定义配置"
        boolean is_required "是否必答"
    }
    
    %% 考试记录
    ExamAttempt ||--o{ Answer : "contains"
    ExamAttempt ||--o{ ProctorLog : "monitored_by"
    
    ExamAttempt {
        bigint id PK "考试记录ID"
        varchar attempt_code UK "考试记录代码"
        bigint exam_id FK
        bigint user_id FK
        varchar status "状态(in_progress/submitted/graded/cancelled)"
        decimal score "得分"
        decimal percentage "得分率"
        varchar grade "等级"
        jsonb answers_summary "答案摘要"
        jsonb time_tracking "时间统计"
        jsonb browser_info "浏览器信息"
        inet ip_address "IP地址"
        varchar user_agent "用户代理"
        timestamp started_at "开始时间"
        timestamp submitted_at "提交时间"
        timestamp graded_at "评分时间"
        timestamp last_activity "最后活动"
        int remaining_time "剩余时间(秒)"
        jsonb security_log "安全日志"
    }
    
    Answer {
        bigint id PK "答案ID"
        bigint attempt_id FK
        bigint question_id FK
        jsonb user_answer "用户答案"
        jsonb correct_answer "正确答案"
        decimal points_earned "获得分数"
        decimal points_possible "可能分数"
        varchar status "状态(correct/incorrect/partial/ungraded)"
        text feedback "反馈"
        int time_spent "用时(秒)"
        timestamp answered_at "回答时间"
        timestamp graded_at "评分时间"
        jsonb metadata "答案元数据"
    }
    
    %% 监考记录
    ProctorLog {
        bigint id PK "监考日志ID"
        bigint attempt_id FK
        varchar event_type "事件类型"
        varchar severity "严重程度"
        jsonb event_data "事件数据"
        varchar media_url "媒体URL"
        varchar ai_analysis "AI分析结果"
        decimal confidence_score "置信度"
        boolean requires_review "需要人工审核"
        timestamp occurred_at "发生时间"
        timestamp reviewed_at "审核时间"
        varchar reviewer_notes "审核备注"
    }
    
    %% 分析统计
    ExamStatistics {
        bigint id PK "统计ID"
        bigint exam_id FK
        varchar stat_type "统计类型"
        jsonb stat_data "统计数据"
        timestamp calculated_at "计算时间"
        date stat_date "统计日期"
    }
    
    UserPerformance {
        bigint id PK "用户表现ID"
        bigint user_id FK
        bigint exam_id FK
        bigint course_id FK
        jsonb performance_data "表现数据"
        decimal overall_score "总体得分"
        int rank "排名"
        timestamp calculated_at
    }
```

### 2.2 数据库优化策略

#### 分库分表策略
```sql
-- 按机构ID分库
CREATE TABLE exam_attempts_org_1 PARTITION OF exam_attempts 
FOR VALUES WITH (MODULUS 4, REMAINDER 0);

-- 按时间分表
CREATE TABLE exam_attempts_y2024q1 PARTITION OF exam_attempts 
FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

-- 复合分区
CREATE TABLE proctor_logs_2024_org1 PARTITION OF proctor_logs
FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
```

#### 索引优化
```sql
-- 复合索引
CREATE INDEX CONCURRENTLY idx_exam_attempts_user_exam_status 
ON exam_attempts(user_id, exam_id, status) 
WHERE status IN ('in_progress', 'submitted');

-- 部分索引
CREATE INDEX CONCURRENTLY idx_questions_active_type 
ON questions(type, difficulty) 
WHERE status = 'active';

-- GIN索引用于JSON搜索
CREATE INDEX CONCURRENTLY idx_questions_metadata_gin 
ON questions USING GIN (metadata);

-- 全文搜索索引
CREATE INDEX CONCURRENTLY idx_questions_content_fts 
ON questions USING GIN (to_tsvector('english', content));
```

---

## 3. 系统时序图

### 3.1 用户认证流程

```mermaid
sequenceDiagram
    participant U as 用户
    participant FE as 前端应用
    participant GW as API网关
    participant AUTH as 认证服务
    participant REDIS as Redis
    participant DB as 数据库
    participant MFA as 多因子认证

    U->>FE: 输入用户名密码
    FE->>GW: POST /auth/login
    GW->>AUTH: 转发认证请求
    
    AUTH->>DB: 验证用户凭据
    DB-->>AUTH: 返回用户信息
    
    alt 启用MFA
        AUTH->>MFA: 发送验证码
        MFA-->>U: 短信/邮件验证码
        U->>FE: 输入验证码
        FE->>AUTH: 提交验证码
        AUTH->>MFA: 验证码校验
        MFA-->>AUTH: 验证结果
    end
    
    AUTH->>AUTH: 生成JWT Token
    AUTH->>REDIS: 存储Session信息
    AUTH-->>GW: 返回Token + 用户信息
    GW-->>FE: 返回认证结果
    FE-->>U: 登录成功，跳转主页
    
    Note over FE,REDIS: Token自动刷新机制
    loop 每15分钟
        FE->>GW: GET /auth/refresh
        GW->>AUTH: 验证刷新Token
        AUTH->>REDIS: 检查Session状态
        AUTH-->>FE: 返回新Token
    end
```

### 3.2 考试创建和发布流程

```mermaid
sequenceDiagram
    participant T as 教师
    participant FE as 前端应用
    participant GW as API网关
    participant EXAM as 考试服务
    participant QUESTION as 题库服务
    participant VALIDATE as 验证服务
    participant CACHE as Redis缓存
    participant DB as 数据库
    participant MQ as 消息队列

    T->>FE: 创建新考试
    FE->>GW: POST /exams/create
    GW->>EXAM: 创建考试请求
    
    EXAM->>DB: 保存考试基本信息
    DB-->>EXAM: 返回考试ID
    
    T->>FE: 添加题目到考试
    FE->>GW: POST /exams/{id}/questions
    GW->>EXAM: 添加题目请求
    
    EXAM->>QUESTION: 验证题目可用性
    QUESTION->>DB: 查询题目信息
    DB-->>QUESTION: 返回题目详情
    QUESTION-->>EXAM: 题目验证结果
    
    EXAM->>DB: 保存考试题目关联
    
    T->>FE: 配置考试参数
    FE->>GW: PUT /exams/{id}/settings
    GW->>EXAM: 更新考试配置
    
    EXAM->>VALIDATE: 验证考试配置
    VALIDATE-->>EXAM: 验证结果
    
    alt 配置验证通过
        EXAM->>DB: 更新考试状态为就绪
        EXAM->>CACHE: 缓存考试配置
        EXAM-->>GW: 配置保存成功
    else 配置验证失败
        EXAM-->>GW: 返回验证错误
    end
    
    T->>FE: 发布考试
    FE->>GW: POST /exams/{id}/publish
    GW->>EXAM: 发布考试请求
    
    EXAM->>VALIDATE: 最终发布前验证
    VALIDATE-->>EXAM: 验证通过
    
    EXAM->>DB: 更新考试状态为已发布
    EXAM->>MQ: 发送考试发布事件
    EXAM-->>GW: 发布成功
    GW-->>FE: 返回发布结果
    FE-->>T: 显示发布成功
    
    Note over MQ: 异步处理
    MQ->>CACHE: 预热考试数据
    MQ->>DB: 记录操作日志
    MQ->>NOTIFICATION: 发送通知给学生
```

### 3.3 在线考试流程

```mermaid
sequenceDiagram
    participant S as 学生
    participant FE as 前端应用
    participant GW as API网关
    participant EXAM as 考试服务
    participant PROCTOR as 监考服务
    participant CACHE as Redis缓存
    participant DB as 数据库
    participant STORAGE as 对象存储
    participant AI as AI分析服务

    S->>FE: 点击开始考试
    FE->>GW: POST /exams/{id}/start
    GW->>EXAM: 开始考试请求
    
    EXAM->>DB: 验证考试权限和状态
    DB-->>EXAM: 权限验证通过
    
    EXAM->>CACHE: 检查考试缓存
    alt 缓存命中
        CACHE-->>EXAM: 返回考试数据
    else 缓存未命中
        EXAM->>DB: 查询考试详情
        DB-->>EXAM: 返回考试数据
        EXAM->>CACHE: 更新缓存
    end
    
    EXAM->>DB: 创建考试记录
    DB-->>EXAM: 返回记录ID
    
    EXAM->>PROCTOR: 启动监考会话
    PROCTOR->>STORAGE: 准备录制存储
    PROCTOR-->>EXAM: 监考会话已就绪
    
    EXAM-->>GW: 返回考试数据和Session
    GW-->>FE: 考试开始成功
    FE-->>S: 显示考试界面
    
    Note over S,FE: 监考监控开始
    FE->>PROCTOR: 开启摄像头/屏幕共享
    PROCTOR->>AI: 启动行为分析
    
    loop 考试过程中
        S->>FE: 答题操作
        FE->>GW: POST /exams/{id}/answer
        GW->>EXAM: 保存答案
        EXAM->>CACHE: 缓存答案(防丢失)
        EXAM->>DB: 异步持久化答案
        
        par 监考监控
            FE->>PROCTOR: 发送视频流
            PROCTOR->>AI: 实时行为分析
            AI-->>PROCTOR: 异常行为检测结果
            
            alt 检测到异常
                PROCTOR->>STORAGE: 保存证据文件
                PROCTOR->>DB: 记录违规日志
                PROCTOR->>FE: 发送警告通知
                FE-->>S: 显示警告信息
            end
        end
        
        par 自动保存
            FE->>EXAM: 定时保存进度
            EXAM->>CACHE: 更新进度缓存
        end
    end
    
    S->>FE: 提交考试
    FE->>GW: POST /exams/{id}/submit
    GW->>EXAM: 提交考试请求
    
    EXAM->>CACHE: 获取所有答案
    EXAM->>DB: 最终保存答案
    EXAM->>DB: 更新考试状态为已提交
    
    EXAM->>PROCTOR: 结束监考会话
    PROCTOR->>STORAGE: 完成录制文件上传
    PROCTOR->>AI: 生成最终分析报告
    AI-->>PROCTOR: 返回分析报告
    PROCTOR->>DB: 保存监考报告
    
    EXAM->>EXAM: 自动评分(客观题)
    EXAM->>DB: 保存评分结果
    
    EXAM-->>GW: 提交成功
    GW-->>FE: 返回提交结果
    FE-->>S: 显示提交成功页面
    
    Note over EXAM,DB: 异步处理
    EXAM->>MQ: 发送评分任务(主观题)
    EXAM->>MQ: 发送结果通知任务
```

### 3.4 AI监考分析流程

```mermaid
sequenceDiagram
    participant CAM as 摄像头
    participant FE as 前端应用
    participant PROCTOR as 监考服务
    participant AI as AI分析服务
    participant ML as 机器学习模型
    participant STORAGE as 对象存储
    participant DB as 数据库
    participant ALERT as 告警服务

    Note over CAM,FE: 实时视频流采集
    CAM->>FE: 摄像头视频流
    FE->>PROCTOR: WebRTC视频流传输
    
    PROCTOR->>PROCTOR: 视频流预处理和帧提取
    
    par 多模型并行分析
        PROCTOR->>ML: 人脸识别模型
        ML-->>PROCTOR: 身份验证结果
        
        PROCTOR->>ML: 行为检测模型  
        ML-->>PROCTOR: 异常行为识别
        
        PROCTOR->>ML: 视线追踪模型
        ML-->>PROCTOR: 注意力分析
        
        PROCTOR->>ML: 物体检测模型
        ML-->>PROCTOR: 作弊工具检测
    end
    
    PROCTOR->>AI: 融合多模态分析结果
    AI->>AI: 风险评分计算
    
    alt 低风险(分数<0.3)
        AI-->>PROCTOR: 正常状态
        PROCTOR->>DB: 记录正常日志
    else 中风险(0.3<=分数<0.7)
        AI-->>PROCTOR: 疑似异常
        PROCTOR->>STORAGE: 保存关键帧
        PROCTOR->>DB: 记录疑似违规
        PROCTOR->>FE: 发送警告
        FE-->>CAM: 显示警告信息
    else 高风险(分数>=0.7)
        AI-->>PROCTOR: 严重违规
        PROCTOR->>STORAGE: 保存完整视频段
        PROCTOR->>DB: 记录严重违规
        PROCTOR->>ALERT: 触发实时告警
        ALERT->>FE: 发送管理员通知
        PROCTOR->>FE: 考试暂停/终止
    end
    
    Note over PROCTOR,DB: 持续学习优化
    loop 每小时
        DB->>AI: 获取标注数据
        AI->>ML: 模型增量训练
        ML-->>AI: 更新模型权重
        AI->>PROCTOR: 部署新模型版本
    end
    
    Note over PROCTOR,STORAGE: 考试结束后处理
    PROCTOR->>AI: 生成完整分析报告
    AI->>STORAGE: 保存分析结果
    AI->>DB: 更新用户信誉分数
```

---

## 4. API接口设计

### 4.1 认证服务API

```yaml
认证服务 (Auth Service):
  base_url: /api/v1/auth
  
  endpoints:
    - path: /login
      method: POST
      description: 用户登录
      request:
        username: string
        password: string
        captcha_token?: string
        device_info?: object
      response:
        access_token: string
        refresh_token: string
        expires_in: number
        user_profile: object
        permissions: array
      
    - path: /logout
      method: POST
      description: 用户登出
      headers:
        Authorization: Bearer {token}
      response:
        success: boolean
        
    - path: /refresh
      method: POST
      description: 刷新Token
      request:
        refresh_token: string
      response:
        access_token: string
        expires_in: number
        
    - path: /verify-mfa
      method: POST
      description: 多因子认证验证
      request:
        user_id: string
        mfa_code: string
        mfa_type: string
      response:
        verified: boolean
        session_token: string
        
    - path: /profile
      method: GET
      description: 获取用户资料
      headers:
        Authorization: Bearer {token}
      response:
        user_id: string
        username: string
        email: string
        profile: object
        roles: array
        permissions: array
```

### 4.2 考试服务API

```yaml
考试服务 (Exam Service):
  base_url: /api/v1/exams
  
  endpoints:
    - path: /
      method: GET
      description: 获取考试列表
      query_params:
        course_id?: string
        status?: string
        page?: number
        limit?: number
      response:
        exams: array
        pagination: object
        
    - path: /
      method: POST
      description: 创建考试
      headers:
        Authorization: Bearer {token}
      request:
        title: string
        description?: string
        course_id: string
        type: string
        duration_minutes: number
        max_attempts: number
        passing_score: number
        start_time: datetime
        end_time: datetime
        settings: object
      response:
        exam_id: string
        status: string
        
    - path: /{exam_id}
      method: GET
      description: 获取考试详情
      headers:
        Authorization: Bearer {token}
      response:
        exam: object
        questions_count: number
        attempt_history: array
        
    - path: /{exam_id}/start
      method: POST
      description: 开始考试
      headers:
        Authorization: Bearer {token}
      request:
        browser_info: object
        screen_resolution: string
      response:
        attempt_id: string
        questions: array
        session_token: string
        remaining_time: number
        proctoring_config: object
        
    - path: /{exam_id}/questions/{question_id}/answer
      method: POST
      description: 提交答案
      headers:
        Authorization: Bearer {token}
      request:
        attempt_id: string
        answer: object
        time_spent: number
        session_token: string
      response:
        saved: boolean
        next_question_id?: string
        
    - path: /{exam_id}/submit
      method: POST
      description: 提交考试
      headers:
        Authorization: Bearer {token}
      request:
        attempt_id: string
        session_token: string
      response:
        submitted: boolean
        score?: number
        grade?: string
        certificate_url?: string
        
    - path: /{exam_id}/results
      method: GET
      description: 获取考试结果
      headers:
        Authorization: Bearer {token}
      query_params:
        attempt_id?: string
      response:
        attempt: object
        score: number
        grade: string
        answers: array
        feedback: array
        certificate_url?: string
```

### 4.3 题库服务API

```yaml
题库服务 (Question Service):
  base_url: /api/v1/questions
  
  endpoints:
    - path: /
      method: GET
      description: 获取题目列表
      query_params:
        category_id?: string
        type?: string
        difficulty?: string
        tags?: string
        search?: string
        page?: number
        limit?: number
      response:
        questions: array
        filters: object
        pagination: object
        
    - path: /
      method: POST
      description: 创建题目
      headers:
        Authorization: Bearer {token}
      request:
        title: string
        content: string
        type: string
        difficulty: string
        points: number
        options?: object
        correct_answer: object
        explanation?: string
        category_id: string
        tags?: array
        media_url?: string
      response:
        question_id: string
        status: string
        
    - path: /{question_id}
      method: GET
      description: 获取题目详情
      headers:
        Authorization: Bearer {token}
      response:
        question: object
        statistics: object
        usage_history: array
        
    - path: /{question_id}
      method: PUT
      description: 更新题目
      headers:
        Authorization: Bearer {token}
      request:
        title?: string
        content?: string
        difficulty?: string
        points?: number
        options?: object
        correct_answer?: object
        explanation?: string
        tags?: array
      response:
        updated: boolean
        version: number
        
    - path: /bulk-import
      method: POST
      description: 批量导入题目
      headers:
        Authorization: Bearer {token}
      request:
        format: string
        file_url: string
        category_id: string
        options: object
      response:
        job_id: string
        estimated_time: number
        
    - path: /categories
      method: GET
      description: 获取题目分类
      response:
        categories: array
        
    - path: /validate
      method: POST
      description: 题目内容验证
      headers:
        Authorization: Bearer {token}
      request:
        questions: array
      response:
        validation_results: array
        summary: object
```

### 4.4 监考服务API

```yaml
监考服务 (Proctoring Service):
  base_url: /api/v1/proctoring
  
  endpoints:
    - path: /session/start
      method: POST
      description: 启动监考会话
      headers:
        Authorization: Bearer {token}
      request:
        attempt_id: string
        proctoring_level: string
        device_permissions: object
      response:
        session_id: string
        webrtc_config: object
        monitoring_rules: array
        
    - path: /session/{session_id}/events
      method: POST
      description: 上报监考事件
      headers:
        Authorization: Bearer {token}
      request:
        event_type: string
        timestamp: datetime
        data: object
        media_blob?: binary
      response:
        recorded: boolean
        action_required?: string
        
    - path: /session/{session_id}/heartbeat
      method: POST
      description: 会话心跳
      headers:
        Authorization: Bearer {token}
      request:
        status: string
        metrics: object
      response:
        continue: boolean
        warnings?: array
        
    - path: /session/{session_id}/end
      method: POST
      description: 结束监考会话
      headers:
        Authorization: Bearer {token}
      response:
        session_ended: boolean
        report_url: string
        
    - path: /reports/{attempt_id}
      method: GET
      description: 获取监考报告
      headers:
        Authorization: Bearer {token}
      response:
        report: object
        violations: array
        evidence_files: array
        ai_analysis: object
        risk_score: number
        
    - path: /violations/{violation_id}/review
      method: POST
      description: 人工审核违规
      headers:
        Authorization: Bearer {token}
      request:
        reviewer_decision: string
        notes: string
      response:
        review_completed: boolean
        final_decision: string
```

---

## 5. 部署架构设计

### 5.1 容器化部署

#### Docker容器配置
```dockerfile
# 多阶段构建 - 前端应用
FROM node:18-alpine AS frontend-build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine AS frontend
COPY --from=frontend-build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80

# 后端服务
FROM python:3.11-slim AS backend
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["gunicorn", "--bind", "0.0.0.0:8000", "--workers", "4", "wsgi:application"]

# 监考服务 (Node.js + WebRTC)
FROM node:18-slim AS proctoring
RUN apt-get update && apt-get install -y \
    ffmpeg \
    libopencv-dev \
    python3-pip
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
EXPOSE 3000
CMD ["node", "server.js"]
```

#### Docker Compose开发环境
```yaml
version: '3.8'
services:
  # 数据库
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: exam_system
      POSTGRES_USER: exam_user
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
      
  elasticsearch:
    image: elasticsearch:8.8.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    volumes:
      - es_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
      
  # 消息队列
  rabbitmq:
    image: rabbitmq:3-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: exam_user
      RABBITMQ_DEFAULT_PASS: secure_password
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
      
  # 应用服务
  api-gateway:
    image: kong:latest
    environment:
      KONG_DATABASE: "off"
      KONG_DECLARATIVE_CONFIG: /kong/declarative/kong.yml
      KONG_PROXY_ACCESS_LOG: /dev/stdout
      KONG_ADMIN_ACCESS_LOG: /dev/stdout
      KONG_PROXY_ERROR_LOG: /dev/stderr
      KONG_ADMIN_ERROR_LOG: /dev/stderr
      KONG_ADMIN_LISTEN: "0.0.0.0:8001"
    volumes:
      - ./kong.yml:/kong/declarative/kong.yml
    ports:
      - "8000:8000"
      - "8001:8001"
      
  auth-service:
    build: 
      context: ./services/auth
      dockerfile: Dockerfile
    environment:
      DATABASE_URL: postgresql://exam_user:secure_password@postgres:5432/exam_system
      REDIS_URL: redis://redis:6379/0
      JWT_SECRET: your-jwt-secret
    depends_on:
      - postgres
      - redis
    ports:
      - "8001:8000"
      
  exam-service:
    build:
      context: ./services/exam
      dockerfile: Dockerfile
    environment:
      DATABASE_URL: postgresql://exam_user:secure_password@postgres:5432/exam_system
      REDIS_URL: redis://redis:6379/1
      RABBITMQ_URL: amqp://exam_user:secure_password@rabbitmq:5672/
    depends_on:
      - postgres
      - redis
      - rabbitmq
    ports:
      - "8002:8000"
      
  proctoring-service:
    build:
      context: ./services/proctoring
      dockerfile: Dockerfile
    environment:
      DATABASE_URL: postgresql://exam_user:secure_password@postgres:5432/exam_system
      REDIS_URL: redis://redis:6379/2
      AWS_ACCESS_KEY_ID: your-access-key
      AWS_SECRET_ACCESS_KEY: your-secret-key
      AWS_S3_BUCKET: exam-recordings
    depends_on:
      - postgres
      - redis
    ports:
      - "3000:3000"
      
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "80:80"
    depends_on:
      - api-gateway

volumes:
  postgres_data:
  redis_data: 
  es_data:
  rabbitmq_data:
```

### 5.2 Kubernetes生产部署

#### 命名空间和配置
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: exam-system
  labels:
    name: exam-system

---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: exam-config
  namespace: exam-system
data:
  DATABASE_HOST: postgres-service
  REDIS_HOST: redis-service
  ELASTICSEARCH_HOST: elasticsearch-service
  LOG_LEVEL: INFO
  MAX_UPLOAD_SIZE: "100MB"
  EXAM_TIMEOUT_MINUTES: "180"

---
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: exam-secrets
  namespace: exam-system
type: Opaque
data:
  DATABASE_PASSWORD: c2VjdXJlX3Bhc3N3b3Jk  # base64 encoded
  JWT_SECRET: eW91ci1qd3Qtc2VjcmV0
  REDIS_PASSWORD: cmVkaXNfcGFzc3dvcmQ=
  AWS_ACCESS_KEY_ID: eW91ci1hd3MtYWNjZXNzLWtleQ==
  AWS_SECRET_ACCESS_KEY: eW91ci1hd3Mtc2VjcmV0LWtleQ==
```

#### 数据库部署
```yaml
# postgres-deployment.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: exam-system
spec:
  serviceName: postgres-service
  replicas: 3
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: exam_system
        - name: POSTGRES_USER
          value: exam_user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: DATABASE_PASSWORD
        - name: POSTGRES_REPLICATION_MODE
          value: master
        - name: POSTGRES_REPLICATION_USER
          value: replicator
        - name: POSTGRES_REPLICATION_PASSWORD
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: DATABASE_PASSWORD
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 100Gi
      storageClassName: fast-ssd

---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: exam-system
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
```

#### 应用服务部署
```yaml
# exam-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: exam-service
  namespace: exam-system
spec:
  replicas: 5
  selector:
    matchLabels:
      app: exam-service
  template:
    metadata:
      labels:
        app: exam-service
    spec:
      containers:
      - name: exam-service
        image: exam-system/exam-service:latest
        ports:
        - containerPort: 8000
        env:
        - name: DATABASE_URL
          value: postgresql://exam_user:$(DATABASE_PASSWORD)@postgres-service:5432/exam_system
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: DATABASE_PASSWORD
        - name: REDIS_URL
          value: redis://redis-service:6379/1
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: exam-secrets
              key: JWT_SECRET
        envFrom:
        - configMapRef:
            name: exam-config
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8000
          initialDelaySeconds: 5
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: exam-service
  namespace: exam-system
spec:
  selector:
    app: exam-service
  ports:
  - port: 80
    targetPort: 8000
  type: ClusterIP

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: exam-service-hpa
  namespace: exam-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: exam-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### Ingress网关配置
```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: exam-system-ingress
  namespace: exam-system
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"
spec:
  tls:
  - hosts:
    - api.exam-system.com
    - app.exam-system.com
    secretName: exam-system-tls
  rules:
  - host: api.exam-system.com
    http:
      paths:
      - path: /api/v1/auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 80
      - path: /api/v1/exams
        pathType: Prefix
        backend:
          service:
            name: exam-service
            port:
              number: 80
      - path: /api/v1/questions
        pathType: Prefix
        backend:
          service:
            name: question-service
            port:
              number: 80
      - path: /api/v1/proctoring
        pathType: Prefix
        backend:
          service:
            name: proctoring-service
            port:
              number: 80
  - host: app.exam-system.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
```

### 5.3 监控和日志

#### Prometheus监控配置
```yaml
# monitoring.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: exam-system
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    rule_files:
      - "/etc/prometheus/rules/*.yml"
    
    scrape_configs:
      - job_name: 'kubernetes-apiservers'
        kubernetes_sd_configs:
        - role: endpoints
        scheme: https
        tls_config:
          ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
        relabel_configs:
        - source_labels: [__meta_kubernetes_namespace, __meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
          action: keep
          regex: default;kubernetes;https
      
      - job_name: 'exam-services'
        kubernetes_sd_configs:
        - role: endpoints
          namespaces:
            names:
            - exam-system
        relabel_configs:
        - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
    
    alerting:
      alertmanagers:
      - static_configs:
        - targets:
          - alertmanager:9093

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: alerting-rules
  namespace: exam-system
data:
  exam-system.yml: |
    groups:
    - name: exam-system-alerts
      rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is above 10% for 5 minutes"
          
      - alert: DatabaseConnections
        expr: pg_stat_activity_count > 80
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High database connections"
          description: "Database connections are above 80"
          
      - alert: ExamServiceDown
        expr: up{job="exam-services",service="exam-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Exam service is down"
          description: "Exam service has been down for more than 1 minute"
```

#### ELK日志配置
```yaml
# logging.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: filebeat-config
  namespace: exam-system
data:
  filebeat.yml: |
    filebeat.inputs:
    - type: container
      paths:
        - /var/log/containers/*exam-system*.log
      processors:
        - add_kubernetes_metadata:
            host: ${NODE_NAME}
            matchers:
            - logs_path:
                logs_path: "/var/log/containers/"
    
    output.elasticsearch:
      hosts: ['elasticsearch:9200']
      index: "exam-system-logs-%{+yyyy.MM.dd}"
    
    setup.template.name: "exam-system"
    setup.template.pattern: "exam-system-*"
    
    logging.level: info
    logging.to_files: true
    logging.files:
      path: /var/log/filebeat
      name: filebeat
      keepfiles: 7
      permissions: 0644

---
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: filebeat
  namespace: exam-system
spec:
  selector:
    matchLabels:
      app: filebeat
  template:
    metadata:
      labels:
        app: filebeat
    spec:
      serviceAccountName: filebeat
      terminationGracePeriodSeconds: 30
      containers:
      - name: filebeat
        image: elastic/filebeat:8.8.0
        args: [
          "-c", "/etc/filebeat.yml",
          "-e",
        ]
        env:
        - name: NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        resources:
          limits:
            memory: 200Mi
            cpu: 100m
          requests:
            memory: 100Mi
            cpu: 100m
        volumeMounts:
        - name: config
          mountPath: /etc/filebeat.yml
          readOnly: true
          subPath: filebeat.yml
        - name: data
          mountPath: /usr/share/filebeat/data
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
        - name: varlog
          mountPath: /var/log
          readOnly: true
      volumes:
      - name: config
        configMap:
          defaultMode: 0640
          name: filebeat-config
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
      - name: varlog
        hostPath:
          path: /var/log
      - name: data
        hostPath:
          path: /var/lib/filebeat-data
          type: DirectoryOrCreate
```

---

## 6. 安全架构设计

### 6.1 安全防护体系

#### 多层安全架构
```mermaid
graph TB
    subgraph "边界安全层"
        WAF[Web应用防火墙]
        DDoS[DDoS防护]
        CDN_SEC[CDN安全]
    end
    
    subgraph "网络安全层"
        VPC[私有网络VPC]
        SG[安全组规则]
        NACL[网络访问控制]
    end
    
    subgraph "应用安全层"
        API_LIMIT[API限流]
        AUTH_GATE[认证网关]
        RBAC[角色权限控制]
        OAUTH[OAuth2.0/JWT]
    end
    
    subgraph "数据安全层"
        ENCRYPT[数据加密]
        BACKUP[安全备份]
        AUDIT[审计日志]
        MASK[数据脱敏]
    end
    
    subgraph "监控安全层"
        SIEM[安全信息与事件管理]
        ANOMALY[异常检测]
        INCIDENT[事件响应]
        FORENSICS[数字取证]
    end
```

#### 零信任架构实现
```yaml
零信任安全策略:
  身份验证:
    - 多因子认证(MFA)必须启用
    - 设备指纹识别和验证
    - 行为基线建模和异常检测
    - Session过期时间动态调整
    
  权限控制:
    - 最小权限原则
    - 动态权限授权
    - 基于上下文的访问控制
    - 实时权限撤销机制
    
  网络隔离:
    - 微分段网络架构
    - 东西向流量加密
    - API网关统一入口
    - 服务间mTLS通信
    
  数据保护:
    - 端到端加密传输
    - 静态数据AES-256加密
    - 敏感数据字段级加密
    - 数据分类分级保护
```

### 6.2 认证和授权

#### JWT Token设计
```yaml
JWT配置:
  算法: RS256 (非对称加密)
  访问令牌生存期: 15分钟
  刷新令牌生存期: 7天
  密钥轮换周期: 30天
  
令牌结构:
  Header:
    alg: RS256
    typ: JWT
    kid: key-id-2024-01
    
  Payload:
    iss: exam-system-auth
    sub: user-id
    aud: exam-system-api
    exp: expiration-time
    iat: issued-at
    jti: jwt-id
    roles: [student, teacher, admin]
    permissions: [exam:read, exam:write]
    org_id: organization-id
    device_id: device-fingerprint
    
  Signature:
    RS256(base64UrlEncode(header) + "." + base64UrlEncode(payload), private-key)
```

#### RBAC权限模型
```yaml
角色权限模型:
  角色定义:
    student:
      permissions:
        - exam:take
        - result:view
        - profile:edit
      scopes:
        - org:{org_id}
        - course:{enrolled_courses}
        
    teacher:
      permissions:
        - exam:create
        - exam:edit
        - question:create
        - question:edit
        - result:view_all
        - analytics:view
      scopes:
        - org:{org_id}
        - course:{teaching_courses}
        
    admin:
      permissions:
        - "*:*"  # 全部权限
      scopes:
        - org:{org_id}
        
    super_admin:
      permissions:
        - "*:*"
      scopes:
        - "*"  # 全局权限
        
  权限检查中间件:
    def check_permission(required_permission, scope=None):
        user_roles = get_user_roles(current_user.id)
        user_permissions = get_permissions_by_roles(user_roles)
        
        if required_permission in user_permissions:
            if scope and not check_scope(scope, user_roles):
                raise PermissionDenied("Insufficient scope")
            return True
        raise PermissionDenied("Permission denied")
```

### 6.3 数据安全

#### 加密策略
```python
# 数据加密配置
ENCRYPTION_CONFIG = {
    # 传输加密
    'TRANSPORT': {
        'TLS_VERSION': '1.3',
        'CIPHER_SUITES': [
            'TLS_AES_256_GCM_SHA384',
            'TLS_CHACHA20_POLY1305_SHA256',
            'TLS_AES_128_GCM_SHA256'
        ],
        'HSTS_MAX_AGE': 31536000,
        'CERTIFICATE_PINNING': True
    },
    
    # 静态数据加密
    'DATA_AT_REST': {
        'ALGORITHM': 'AES-256-GCM',
        'KEY_DERIVATION': 'PBKDF2',
        'ITERATIONS': 100000,
        'KEY_ROTATION_DAYS': 90
    },
    
    # 字段级加密
    'FIELD_ENCRYPTION': {
        'SENSITIVE_FIELDS': [
            'users.password_hash',
            'users.id_card_number',
            'users.phone',
            'exam_attempts.browser_fingerprint',
            'proctor_logs.face_recognition_data'
        ],
        'ENCRYPTION_KEY_ID': 'field-encryption-2024-01'
    },
    
    # 密钥管理
    'KEY_MANAGEMENT': {
        'KMS_PROVIDER': 'AWS_KMS',  # or 'HashiCorp_Vault'
        'KEY_ROTATION_ENABLED': True,
        'BACKUP_ENCRYPTION_ENABLED': True,
        'AUDIT_LOGGING_ENABLED': True
    }
}

# 敏感数据处理类
class SecureDataHandler:
    def __init__(self):
        self.kms_client = boto3.client('kms')
        self.data_key_cache = {}
        
    def encrypt_sensitive_data(self, data: str, context: dict) -> str:
        """加密敏感数据"""
        try:
            # 获取数据密钥
            data_key = self._get_data_key(context)
            
            # AES-GCM加密
            cipher = AES.new(data_key, AES.MODE_GCM)
            ciphertext, tag = cipher.encrypt_and_digest(data.encode())
            
            # 组合加密结果
            encrypted_data = {
                'ciphertext': base64.b64encode(ciphertext).decode(),
                'tag': base64.b64encode(tag).decode(),
                'nonce': base64.b64encode(cipher.nonce).decode(),
                'context': context
            }
            
            return json.dumps(encrypted_data)
            
        except Exception as e:
            logger.error(f"Encryption failed: {e}")
            raise EncryptionException("Failed to encrypt sensitive data")
    
    def decrypt_sensitive_data(self, encrypted_data: str) -> str:
        """解密敏感数据"""
        try:
            data = json.loads(encrypted_data)
            
            # 获取数据密钥
            data_key = self._get_data_key(data['context'])
            
            # AES-GCM解密
            cipher = AES.new(data_key, AES.MODE_GCM, 
                           nonce=base64.b64decode(data['nonce']))
            decrypted = cipher.decrypt_and_verify(
                base64.b64decode(data['ciphertext']),
                base64.b64decode(data['tag'])
            )
            
            return decrypted.decode()
            
        except Exception as e:
            logger.error(f"Decryption failed: {e}")
            raise DecryptionException("Failed to decrypt sensitive data")
```

#### 数据脱敏和匿名化
```python
# 数据脱敏配置
MASKING_RULES = {
    'phone': {
        'pattern': r'(\d{3})\d{4}(\d{4})',
        'replacement': r'\1****\2'
    },
    'email': {
        'pattern': r'([^@]{1,3})[^@]*(@.*)',
        'replacement': r'\1***\2'
    },
    'id_card': {
        'pattern': r'(\d{6})\d{8}(\d{4})',
        'replacement': r'\1********\2'
    },
    'ip_address': {
        'pattern': r'(\d+\.\d+)\.\d+\.\d+',
        'replacement': r'\1.xxx.xxx'
    }
}

class DataMaskingService:
    @staticmethod
    def mask_user_data(user_data: dict, mask_level: str = 'standard') -> dict:
        """用户数据脱敏"""
        masked_data = user_data.copy()
        
        if mask_level == 'standard':
            # 标准脱敏：保留部分信息用于识别
            if 'phone' in masked_data:
                masked_data['phone'] = re.sub(
                    MASKING_RULES['phone']['pattern'],
                    MASKING_RULES['phone']['replacement'],
                    masked_data['phone']
                )
            
            if 'email' in masked_data:
                masked_data['email'] = re.sub(
                    MASKING_RULES['email']['pattern'],
                    MASKING_RULES['email']['replacement'],
                    masked_data['email']
                )
                
        elif mask_level == 'full':
            # 完全匿名化
            sensitive_fields = ['phone', 'email', 'id_card', 'real_name']
            for field in sensitive_fields:
                if field in masked_data:
                    masked_data[field] = f"[MASKED_{field.upper()}]"
                    
        return masked_data
    
    @staticmethod
    def anonymize_exam_data(exam_data: dict) -> dict:
        """考试数据匿名化（用于分析）"""
        anonymized = exam_data.copy()
        
        # 移除直接标识符
        remove_fields = ['user_id', 'username', 'email', 'real_name', 'ip_address']
        for field in remove_fields:
            anonymized.pop(field, None)
        
        # 生成匿名ID
        if 'user_id' in exam_data:
            anonymized['anonymous_id'] = hashlib.sha256(
                f"{exam_data['user_id']}-{ANONYMIZATION_SALT}".encode()
            ).hexdigest()[:16]
        
        # 时间泛化（精确到小时）
        if 'submitted_at' in anonymized:
            dt = datetime.fromisoformat(anonymized['submitted_at'])
            anonymized['submitted_hour'] = dt.replace(minute=0, second=0, microsecond=0)
            del anonymized['submitted_at']
            
        return anonymized
```

---

## 7. 性能优化策略

### 7.1 缓存架构

#### 多层缓存设计
```mermaid
graph TB
    subgraph "浏览器缓存层"
        BROWSER[浏览器缓存<br/>Static Assets]
        LOCAL_STORAGE[本地存储<br/>User Preferences]
    end
    
    subgraph "CDN缓存层"
        CDN_CACHE[CDN边缘缓存<br/>Static Resources]
        CDN_API[CDN API缓存<br/>Public API Response]
    end
    
    subgraph "应用缓存层"
        NGINX_CACHE[Nginx缓存<br/>Reverse Proxy]
        APP_CACHE[应用内存缓存<br/>Frequently Used Data]
    end
    
    subgraph "分布式缓存层"
        REDIS_CLUSTER[Redis集群<br/>Session & Hot Data]
        REDIS_CACHE[Redis缓存<br/>Query Results]
    end
    
    subgraph "数据库缓存层"
        DB_BUFFER[数据库缓冲池<br/>Query Cache]
        DB_INDEX[索引缓存<br/>Index Buffer]
    end
    
    BROWSER --> CDN_CACHE
    CDN_CACHE --> NGINX_CACHE
    NGINX_CACHE --> APP_CACHE
    APP_CACHE --> REDIS_CLUSTER
    REDIS_CLUSTER --> DB_BUFFER
```

#### Redis缓存策略
```python
# Redis缓存配置
REDIS_CONFIG = {
    'CLUSTER_NODES': [
        {'host': 'redis-node-1', 'port': 6379},
        {'host': 'redis-node-2', 'port': 6379},
        {'host': 'redis-node-3', 'port': 6379}
    ],
    'MAX_CONNECTIONS': 50,
    'DECODE_RESPONSES': True,
    'HEALTH_CHECK_INTERVAL': 30,
    'SOCKET_KEEPALIVE': True,
    'SOCKET_KEEPALIVE_OPTIONS': {
        'TCP_KEEPIDLE': 1,
        'TCP_KEEPINTVL': 3,
        'TCP_KEEPCNT': 5
    }
}

# 缓存策略类
class CacheStrategy:
    def __init__(self):
        self.redis = redis.RedisCluster(**REDIS_CONFIG)
        
    def cache_exam_data(self, exam_id: str, data: dict, ttl: int = 3600):
        """缓存考试数据"""
        key = f"exam:{exam_id}"
        pipeline = self.redis.pipeline()
        
        # 缓存考试基本信息
        pipeline.hset(f"{key}:info", mapping={
            'title': data['title'],
            'duration': data['duration_minutes'],
            'status': data['status'],
            'start_time': data['start_time'],
            'end_time': data['end_time']
        })
        pipeline.expire(f"{key}:info", ttl)
        
        # 缓存考试题目（使用压缩）
        questions_json = json.dumps(data['questions'])
        compressed_questions = zlib.compress(questions_json.encode())
        pipeline.set(f"{key}:questions", compressed_questions, ex=ttl)
        
        # 缓存考试配置
        pipeline.hset(f"{key}:config", mapping=data['settings'])
        pipeline.expire(f"{key}:config", ttl)
        
        pipeline.execute()
    
    def get_cached_exam(self, exam_id: str) -> dict:
        """获取缓存的考试数据"""
        key = f"exam:{exam_id}"
        pipeline = self.redis.pipeline()
        
        pipeline.hgetall(f"{key}:info")
        pipeline.get(f"{key}:questions")
        pipeline.hgetall(f"{key}:config")
        
        results = pipeline.execute()
        
        if not any(results):
            return None
            
        # 解压题目数据
        compressed_questions = results[1]
        if compressed_questions:
            questions_json = zlib.decompress(compressed_questions).decode()
            questions = json.loads(questions_json)
        else:
            questions = []
            
        return {
            'info': results[0],
            'questions': questions,
            'config': results[2]
        }
    
    def cache_user_session(self, user_id: str, session_data: dict, ttl: int = 1800):
        """缓存用户会话数据"""
        key = f"session:{user_id}"
        
        # 使用Hash存储会话数据
        self.redis.hset(key, mapping={
            'user_id': session_data['user_id'],
            'username': session_data['username'],
            'roles': json.dumps(session_data['roles']),
            'permissions': json.dumps(session_data['permissions']),
            'last_activity': datetime.now().isoformat()
        })
        self.redis.expire(key, ttl)
        
        # 维护活跃用户集合（用于统计）
        self.redis.sadd('active_users', user_id)
        self.redis.expire('active_users', ttl)
    
    def invalidate_cache_pattern(self, pattern: str):
        """批量失效缓存"""
        keys = self.redis.keys(pattern)
        if keys:
            self.redis.delete(*keys)
    
    def warm_up_cache(self):
        """缓存预热"""
        # 预热热门考试数据
        popular_exams = self._get_popular_exams()
        for exam in popular_exams:
            self.cache_exam_data(exam['id'], exam, ttl=7200)
        
        # 预热题库数据
        frequent_questions = self._get_frequent_questions()
        for question in frequent_questions:
            key = f"question:{question['id']}"
            self.redis.setex(key, 3600, json.dumps(question))
```

### 7.2 数据库优化

#### 查询优化策略
```sql
-- 1. 复合索引优化
-- 考试记录查询优化
CREATE INDEX CONCURRENTLY idx_exam_attempts_composite 
ON exam_attempts(user_id, exam_id, status, created_at DESC)
WHERE status IN ('completed', 'submitted');

-- 题目搜索优化
CREATE INDEX CONCURRENTLY idx_questions_search 
ON questions(status, type, difficulty, created_at DESC)
WHERE status = 'active'
INCLUDE (title, content);

-- 2. 分区表优化
-- 按时间分区考试记录表
CREATE TABLE exam_attempts_y2024q1 PARTITION OF exam_attempts 
FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE exam_attempts_y2024q2 PARTITION OF exam_attempts 
FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

-- 3. 物化视图优化
-- 考试统计物化视图
CREATE MATERIALIZED VIEW exam_statistics AS
SELECT 
    e.id as exam_id,
    e.title,
    COUNT(ea.id) as total_attempts,
    AVG(ea.score) as avg_score,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY ea.score) as median_score,
    COUNT(CASE WHEN ea.status = 'completed' THEN 1 END) as completed_count,
    MAX(ea.created_at) as last_attempt_time
FROM exams e
LEFT JOIN exam_attempts ea ON e.id = ea.exam_id
WHERE e.status = 'active'
GROUP BY e.id, e.title;

-- 创建唯一索引以支持并发刷新
CREATE UNIQUE INDEX ON exam_statistics (exam_id);

-- 4. 查询重写示例
-- 优化前：N+1查询问题
-- SELECT * FROM exams WHERE course_id = 1;
-- 然后对每个考试查询: SELECT COUNT(*) FROM exam_attempts WHERE exam_id = ?

-- 优化后：使用JOIN和子查询
SELECT 
    e.*,
    COALESCE(stats.attempt_count, 0) as attempt_count,
    COALESCE(stats.avg_score, 0) as avg_score
FROM exams e
LEFT JOIN (
    SELECT 
        exam_id,
        COUNT(*) as attempt_count,
        AVG(score) as avg_score
    FROM exam_attempts 
    WHERE status = 'completed'
    GROUP BY exam_id
) stats ON e.id = stats.exam_id
WHERE e.course_id = $1 AND e.status = 'active'
ORDER BY e.created_at DESC;

-- 5. 批量操作优化
-- 批量插入答案记录
INSERT INTO answers (attempt_id, question_id, user_answer, points_earned, answered_at)
SELECT 
    $1::bigint as attempt_id,
    question_data.question_id,
    question_data.user_answer,
    question_data.points_earned,
    NOW()
FROM (
    SELECT unnest($2::bigint[]) as question_id,
           unnest($3::jsonb[]) as user_answer,
           unnest($4::decimal[]) as points_earned
) as question_data;
```

#### 连接池和事务优化
```python
# 数据库连接池配置
DATABASE_CONFIG = {
    'ENGINE': 'django.db.backends.postgresql',
    'NAME': 'exam_system',
    'USER': 'exam_user',
    'PASSWORD': 'secure_password',
    'HOST': 'postgres-cluster.local',
    'PORT': '5432',
    'OPTIONS': {
        'sslmode': 'require',
        'application_name': 'exam_system',
        'connect_timeout': 10,
        'options': '-c default_transaction_isolation=read_committed'
    },
    'CONN_MAX_AGE': 300,  # 5分钟连接复用
    'CONN_HEALTH_CHECKS': True,
    'ATOMIC_REQUESTS': False,  # 手动控制事务
}

# 连接池配置
DATABASES = {
    'default': {
        **DATABASE_CONFIG,
        'ENGINE': 'django_db_geventpool.backends.postgresql_psycopg2',
        'OPTIONS': {
            **DATABASE_CONFIG['OPTIONS'],
            'MAX_CONNS': 50,
            'MIN_CONNS': 5,
            'REUSE_CONNS': True
        }
    },
    'read_replica': {
        **DATABASE_CONFIG,
        'HOST': 'postgres-read-replica.local',
        'OPTIONS': {
            **DATABASE_CONFIG['OPTIONS'],
            'MAX_CONNS': 20,
            'MIN_CONNS': 2
        }
    }
}

# 数据库路由器
class DatabaseRouter:
    """
    数据库读写分离路由器
    """
    read_db_models = [
        'ExamStatistics', 'UserPerformance', 
        'QuestionAnalytics', 'SystemMetrics'
    ]
    
    def db_for_read(self, model, **hints):
        """读操作路由"""
        if model._meta.app_label in ['analytics', 'reporting']:
            return 'read_replica'
        if model.__name__ in self.read_db_models:
            return 'read_replica'
        return 'default'
    
    def db_for_write(self, model, **hints):
        """写操作路由"""
        return 'default'
    
    def allow_migrate(self, db, app_label, model_name=None, **hints):
        """迁移控制"""
        if db == 'read_replica':
            return False
        return True

# 高性能事务管理
class OptimizedTransactionManager:
    def __init__(self):
        self.connection = connections['default']
        
    @contextmanager
    def bulk_transaction(self, batch_size: int = 1000):
        """批量事务处理"""
        with transaction.atomic():
            try:
                # 设置事务参数优化批量操作
                with self.connection.cursor() as cursor:
                    cursor.execute("SET synchronous_commit = OFF")
                    cursor.execute("SET wal_buffers = '16MB'")
                    cursor.execute("SET checkpoint_segments = 32")
                yield
            finally:
                # 恢复默认设置
                with self.connection.cursor() as cursor:
                    cursor.execute("SET synchronous_commit = ON")
                    
    def bulk_create_answers(self, answers_data: List[dict]):
        """批量创建答案记录"""
        with self.bulk_transaction():
            Answer.objects.bulk_create([
                Answer(**data) for data in answers_data
            ], batch_size=500, ignore_conflicts=True)
            
    def bulk_update_scores(self, attempts_data: List[dict]):
        """批量更新分数"""
        with self.bulk_transaction():
            attempts = [ExamAttempt(id=data['id'], score=data['score']) 
                       for data in attempts_data]
            ExamAttempt.objects.bulk_update(attempts, ['score'], batch_size=200)
```

### 7.3 前端性能优化

#### 资源优化策略
```javascript
// Webpack生产环境配置
const webpack = require('webpack');
const TerserPlugin = require('terser-webpack-plugin');
const CompressionPlugin = require('compression-webpack-plugin');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

module.exports = {
  mode: 'production',
  
  // 代码分割配置
  optimization: {
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          chunks: 'all',
        },
        common: {
          name: 'common',
          minChunks: 2,
          chunks: 'all',
          enforce: true
        }
      }
    },
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          compress: {
            drop_console: true,
            drop_debugger: true,
            pure_funcs: ['console.log']
          }
        }
      })
    ]
  },
  
  plugins: [
    // Gzip压缩
    new CompressionPlugin({
      algorithm: 'gzip',
      test: /\.(js|css|html|svg)$/,
      threshold: 8192,
      minRatio: 0.8
    }),
    
    // Bundle分析
    new BundleAnalyzerPlugin({
      analyzerMode: 'static',
      openAnalyzer: false,
      reportFilename: 'bundle-report.html'
    }),
    
    // 环境变量
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('production'),
      'process.env.API_BASE_URL': JSON.stringify('https://api.exam-system.com')
    })
  ]
};

// React性能优化组件
import React, { memo, useMemo, useCallback, lazy, Suspense } from 'react';
import { debounce } from 'lodash';

// 懒加载组件
const ExamComponent = lazy(() => import('./components/ExamComponent'));
const AnalyticsChart = lazy(() => import('./components/AnalyticsChart'));

// 考试界面优化组件
const ExamInterface = memo(({ exam, questions, onAnswerSubmit }) => {
  // 缓存计算结果
  const totalQuestions = useMemo(() => questions.length, [questions]);
  
  // 防抖提交答案
  const debouncedSubmit = useCallback(
    debounce((questionId, answer) => {
      onAnswerSubmit(questionId, answer);
    }, 500),
    [onAnswerSubmit]
  );
  
  // 虚拟滚动实现（大量题目时）
  const VirtualizedQuestionList = useMemo(() => {
    if (questions.length > 100) {
      return lazy(() => import('./components/VirtualizedList'));
    }
    return null;
  }, [questions.length]);
  
  return (
    <div className="exam-interface">
      <ExamHeader 
        title={exam.title} 
        totalQuestions={totalQuestions}
        timeRemaining={exam.timeRemaining}
      />
      
      <Suspense fallback={<div>Loading questions...</div>}>
        {VirtualizedQuestionList ? (
          <VirtualizedQuestionList 
            items={questions}
            onAnswerChange={debouncedSubmit}
          />
        ) : (
          <QuestionList 
            questions={questions}
            onAnswerChange={debouncedSubmit}
          />
        )}
      </Suspense>
    </div>
  );
});

// Service Worker缓存策略
const CACHE_NAME = 'exam-system-v1.2.0';
const STATIC_CACHE_URLS = [
  '/',
  '/static/css/main.css',
  '/static/js/main.js',
  '/static/fonts/roboto.woff2'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(STATIC_CACHE_URLS))
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  
  // API请求缓存策略
  if (request.url.includes('/api/')) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          // 只缓存成功的GET请求
          if (request.method === 'GET' && response.status === 200) {
            const responseClone = response.clone();
            caches.open(CACHE_NAME)
              .then((cache) => cache.put(request, responseClone));
          }
          return response;
        })
        .catch(() => {
          // 网络失败时使用缓存
          return caches.match(request);
        })
    );
  } else {
    // 静态资源缓存优先策略
    event.respondWith(
      caches.match(request)
        .then((response) => {
          return response || fetch(request);
        })
    );
  }
});

// 前端监控和性能追踪
class PerformanceMonitor {
  constructor() {
    this.metrics = {};
    this.observer = null;
    this.initPerformanceObserver();
  }
  
  initPerformanceObserver() {
    if ('PerformanceObserver' in window) {
      this.observer = new PerformanceObserver((list) => {
        list.getEntries().forEach((entry) => {
          this.collectMetric(entry);
        });
      });
      
      this.observer.observe({ entryTypes: ['measure', 'navigation', 'paint'] });
    }
  }
  
  collectMetric(entry) {
    const { name, entryType, startTime, duration } = entry;
    
    if (entryType === 'navigation') {
      this.metrics.pageLoad = {
        domContentLoaded: entry.domContentLoadedEventEnd - entry.domContentLoadedEventStart,
        loadComplete: entry.loadEventEnd - entry.loadEventStart,
        firstByte: entry.responseStart - entry.requestStart
      };
    }
    
    if (entryType === 'paint') {
      this.metrics[name] = duration;
    }
    
    // 发送关键指标
    if (this.shouldReportMetric(name, duration)) {
      this.reportMetric({
        metric: name,
        value: duration,
        timestamp: Date.now(),
        url: window.location.pathname
      });
    }
  }
  
  shouldReportMetric(name, value) {
    // 只上报关键性能指标
    const criticalMetrics = [
      'first-contentful-paint',
      'largest-contentful-paint',
      'exam-load-time',
      'question-render-time'
    ];
    
    return criticalMetrics.includes(name) || value > 1000; // 超过1秒的操作
  }
  
  async reportMetric(metric) {
    try {
      await fetch('/api/v1/metrics/performance', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(metric)
      });
    } catch (error) {
      console.warn('Failed to report performance metric:', error);
    }
  }
  
  // 手动测量关键操作
  measureExamLoad() {
    performance.mark('exam-load-start');
    return () => {
      performance.mark('exam-load-end');
      performance.measure('exam-load-time', 'exam-load-start', 'exam-load-end');
    };
  }
}

// 初始化性能监控
const performanceMonitor = new PerformanceMonitor();
window.performanceMonitor = performanceMonitor;
```

---

## 总结

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"id": "1", "content": "Analyze product requirements from analysis.md", "status": "completed", "priority": "high"}, {"id": "2", "content": "Design scalable system architecture", "status": "completed", "priority": "high"}, {"id": "3", "content": "Create comprehensive data model", "status": "completed", "priority": "high"}, {"id": "4", "content": "Design sequence diagrams for critical workflows", "status": "completed", "priority": "high"}, {"id": "5", "content": "Define API contracts and service interfaces", "status": "completed", "priority": "medium"}, {"id": "6", "content": "Create deployment and infrastructure design", "status": "completed", "priority": "medium"}, {"id": "7", "content": "Output complete system design to system-design.md", "status": "completed", "priority": "high"}]