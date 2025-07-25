# 在线考试系统数据库架构文档

## 文档概述

本文档详细描述在线考试系统的数据库架构设计，包含所有表结构、索引策略、分区方案、性能优化和维护指南。

### 数据库技术栈
- **主数据库**: PostgreSQL 15+
- **缓存数据库**: Redis 7.0+
- **搜索引擎**: Elasticsearch 8.8+
- **文档存储**: MongoDB 6.0+
- **对象存储**: AWS S3 / MinIO

---

## 1. 数据库架构总览

### 1.1 架构设计原则

- **数据一致性**: 严格的ACID事务保证
- **高可用性**: 主从复制 + 故障自动切换
- **性能优化**: 分区表 + 索引优化
- **数据安全**: 字段级加密 + 访问控制
- **扩展性**: 水平分片 + 读写分离

### 1.2 数据库连接信息

```yaml
主数据库集群:
  primary:
    host: postgres-primary.exam-system.local
    port: 5432
    database: exam_system
    max_connections: 200
    
  read_replicas:
    - host: postgres-replica-1.exam-system.local
      port: 5432
      weight: 100
    - host: postgres-replica-2.exam-system.local
      port: 5432  
      weight: 80

Redis集群:
  nodes:
    - host: redis-node-1.exam-system.local:6379
    - host: redis-node-2.exam-system.local:6379
    - host: redis-node-3.exam-system.local:6379
  cluster_enabled: true
  sentinel_enabled: true
```

---

## 2. 核心表结构设计

### 2.1 用户体系表

#### 2.1.1 用户基本信息表 (users)

**表描述**: 存储系统所有用户的基本信息

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    profile JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP WITH TIME ZONE,
    phone_verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP WITH TIME ZONE,
    login_count INTEGER DEFAULT 0,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- 索引定义
    CONSTRAINT users_username_length CHECK (char_length(username) >= 3),
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_phone_format CHECK (phone IS NULL OR phone ~* '^\+?[1-9]\d{1,14}$')
);

-- 创建索引
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);
CREATE INDEX CONCURRENTLY idx_users_phone ON users(phone) WHERE phone IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_users_active_created ON users(is_active, created_at);
CREATE INDEX CONCURRENTLY idx_users_last_login ON users(last_login) WHERE last_login IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_users_profile_gin ON users USING GIN (profile);

-- 创建触发器自动更新 updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language plpgsql;

CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE users IS '用户基本信息表';
COMMENT ON COLUMN users.id IS '用户唯一标识符';
COMMENT ON COLUMN users.username IS '用户名，全局唯一';
COMMENT ON COLUMN users.profile IS 'JSON格式的扩展用户资料';
COMMENT ON COLUMN users.failed_login_attempts IS '连续失败登录次数';
COMMENT ON COLUMN users.locked_until IS '账户锁定到期时间';
```

**字段说明**:

| 字段名 | 类型 | 是否必填 | 说明 | 示例值 |
|--------|------|----------|------|--------|
| id | BIGINT | 是 | 用户唯一标识符 | 1001 |
| username | VARCHAR(50) | 是 | 用户名，全局唯一 | student001 |
| email | VARCHAR(100) | 是 | 邮箱地址，全局唯一 | student@example.com |
| password_hash | VARCHAR(255) | 是 | 密码哈希值 | $2b$12$... |
| profile | JSONB | 否 | 扩展用户信息 | {"student_id": "2024001", "major": "CS"} |
| is_active | BOOLEAN | 否 | 账户是否激活 | true |
| failed_login_attempts | INTEGER | 否 | 连续登录失败次数 | 0 |

#### 2.1.2 用户角色表 (user_roles)

**表描述**: 用户角色关联表，支持多角色

```sql
CREATE TABLE user_roles (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('student', 'teacher', 'admin', 'super_admin', 'proctor')),
    organization_id BIGINT REFERENCES organizations(id),
    permissions JSONB DEFAULT '[]',
    scope_restrictions JSONB DEFAULT '{}',
    granted_by BIGINT REFERENCES users(id),
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- 确保用户在同一组织内的角色唯一性
    UNIQUE(user_id, role, organization_id)
);

CREATE INDEX CONCURRENTLY idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX CONCURRENTLY idx_user_roles_role ON user_roles(role);
CREATE INDEX CONCURRENTLY idx_user_roles_organization ON user_roles(organization_id);
CREATE INDEX CONCURRENTLY idx_user_roles_active_expires ON user_roles(is_active, expires_at);

COMMENT ON TABLE user_roles IS '用户角色关联表';
COMMENT ON COLUMN user_roles.permissions IS 'JSON数组格式的特殊权限';
COMMENT ON COLUMN user_roles.scope_restrictions IS 'JSON格式的权限范围限制';
```

#### 2.1.3 用户会话表 (user_sessions)

**表描述**: 用户登录会话管理

```sql
CREATE TABLE user_sessions (
    session_id VARCHAR(128) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_type VARCHAR(50),
    device_fingerprint VARCHAR(128),
    ip_address INET,
    user_agent TEXT,
    geolocation JSONB,
    metadata JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    logout_at TIMESTAMP WITH TIME ZONE,
    logout_reason VARCHAR(50)
);

CREATE INDEX CONCURRENTLY idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX CONCURRENTLY idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX CONCURRENTLY idx_user_sessions_active ON user_sessions(is_active, last_activity);
CREATE INDEX CONCURRENTLY idx_user_sessions_ip ON user_sessions(ip_address);

-- 自动清理过期会话
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_sessions 
    WHERE expires_at < CURRENT_TIMESTAMP 
       OR (logout_at IS NOT NULL AND logout_at < CURRENT_TIMESTAMP - INTERVAL '7 days');
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE user_sessions IS '用户会话管理表';
```

### 2.2 组织机构表

#### 2.2.1 组织机构表 (organizations)

**表描述**: 学校、学院、部门等组织架构

```sql
CREATE TABLE organizations (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('university', 'college', 'department', 'training_center', 'enterprise')),
    parent_id BIGINT REFERENCES organizations(id),
    level INTEGER DEFAULT 1,
    path VARCHAR(500), -- 层级路径，如 "1.2.5"
    settings JSONB DEFAULT '{}',
    branding JSONB DEFAULT '{}',
    contact_info JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX CONCURRENTLY idx_organizations_code ON organizations(code);
CREATE INDEX CONCURRENTLY idx_organizations_type ON organizations(type);
CREATE INDEX CONCURRENTLY idx_organizations_parent_id ON organizations(parent_id);
CREATE INDEX CONCURRENTLY idx_organizations_path ON organizations USING GIN (path gin_trgm_ops);
CREATE INDEX CONCURRENTLY idx_organizations_active ON organizations(is_active);

-- 更新层级路径的触发器
CREATE OR REPLACE FUNCTION update_organization_path()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_id IS NULL THEN
        NEW.path = NEW.id::VARCHAR;
        NEW.level = 1;
    ELSE
        SELECT path || '.' || NEW.id::VARCHAR, level + 1
        INTO NEW.path, NEW.level
        FROM organizations 
        WHERE id = NEW.parent_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_organization_path
    BEFORE INSERT OR UPDATE ON organizations
    FOR EACH ROW
    EXECUTE FUNCTION update_organization_path();

COMMENT ON TABLE organizations IS '组织机构表';
COMMENT ON COLUMN organizations.path IS '层级路径，便于查询子机构';
COMMENT ON COLUMN organizations.settings IS '机构配置信息';
COMMENT ON COLUMN organizations.branding IS '品牌设置（logo、颜色等）';
```

### 2.3 课程体系表

#### 2.3.1 课程分类表 (course_categories)

```sql
CREATE TABLE course_categories (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES course_categories(id),
    description TEXT,
    icon VARCHAR(100),
    color VARCHAR(7), -- 十六进制颜色代码
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX CONCURRENTLY idx_course_categories_parent_id ON course_categories(parent_id);
CREATE INDEX CONCURRENTLY idx_course_categories_sort_order ON course_categories(sort_order);

COMMENT ON TABLE course_categories IS '课程分类表';
```

#### 2.3.2 课程表 (courses)

```sql
CREATE TABLE courses (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    code VARCHAR(50) NOT NULL UNIQUE,
    instructor_id BIGINT NOT NULL REFERENCES users(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    category_id BIGINT REFERENCES course_categories(id),
    status VARCHAR(20) DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'archived', 'suspended')),
    difficulty_level VARCHAR(20) DEFAULT 'beginner' CHECK (difficulty_level IN ('beginner', 'intermediate', 'advanced')),
    language VARCHAR(10) DEFAULT 'zh-CN',
    metadata JSONB DEFAULT '{}',
    syllabus JSONB DEFAULT '[]', -- 课程大纲
    learning_objectives JSONB DEFAULT '[]', -- 学习目标
    prerequisites JSONB DEFAULT '[]', -- 先修课程
    credits DECIMAL(3,1) DEFAULT 0.0,
    duration_hours INTEGER DEFAULT 0,
    max_students INTEGER,
    enrollment_start_date DATE,
    enrollment_end_date DATE,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX CONCURRENTLY idx_courses_instructor_id ON courses(instructor_id);
CREATE INDEX CONCURRENTLY idx_courses_organization_id ON courses(organization_id);
CREATE INDEX CONCURRENTLY idx_courses_category_id ON courses(category_id);
CREATE INDEX CONCURRENTLY idx_courses_status ON courses(status);
CREATE INDEX CONCURRENTLY idx_courses_start_date ON courses(start_date);
CREATE INDEX CONCURRENTLY idx_courses_metadata_gin ON courses USING GIN (metadata);

-- 全文搜索索引
CREATE INDEX CONCURRENTLY idx_courses_search ON courses USING GIN (
    to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(code, ''))
);

COMMENT ON TABLE courses IS '课程信息表';
COMMENT ON COLUMN courses.metadata IS '课程元数据（标签、难度、领域等）';
COMMENT ON COLUMN courses.syllabus IS '课程大纲，JSON数组格式';
```

#### 2.3.3 课程注册表 (enrollments)

```sql
CREATE TABLE enrollments (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'enrolled' CHECK (status IN ('enrolled', 'active', 'completed', 'dropped', 'suspended')),
    progress DECIMAL(5,2) DEFAULT 0.00 CHECK (progress >= 0 AND progress <= 100),
    grade VARCHAR(5), -- A+, A, B+, B, C+, C, D, F
    final_score DECIMAL(5,2),
    enrollment_source VARCHAR(50) DEFAULT 'manual', -- manual, bulk_import, api, self_enrollment
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    dropped_at TIMESTAMP WITH TIME ZONE,
    last_activity_at TIMESTAMP WITH TIME ZONE,
    
    UNIQUE(user_id, course_id)
);

CREATE INDEX CONCURRENTLY idx_enrollments_user_id ON enrollments(user_id);
CREATE INDEX CONCURRENTLY idx_enrollments_course_id ON enrollments(course_id);
CREATE INDEX CONCURRENTLY idx_enrollments_status ON enrollments(status);
CREATE INDEX CONCURRENTLY idx_enrollments_progress ON enrollments(progress);
CREATE INDEX CONCURRENTLY idx_enrollments_enrolled_at ON enrollments(enrolled_at);

COMMENT ON TABLE enrollments IS '课程注册表';
COMMENT ON COLUMN enrollments.progress IS '学习进度百分比';
```

### 2.4 题库体系表

#### 2.4.1 题目分类表 (question_categories)

```sql
CREATE TABLE question_categories (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES question_categories(id),
    description TEXT,
    icon VARCHAR(100),
    color VARCHAR(7),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- 防止循环引用
    CONSTRAINT no_self_reference CHECK (id != parent_id)
);

CREATE INDEX CONCURRENTLY idx_question_categories_parent_id ON question_categories(parent_id);
CREATE INDEX CONCURRENTLY idx_question_categories_sort_order ON question_categories(sort_order);

COMMENT ON TABLE question_categories IS '题目分类表，支持多级分类';
```

#### 2.4.2 题目表 (questions)

**表描述**: 核心题目信息存储

```sql
-- 为大表启用分区（按创建时间分区）
CREATE TABLE questions (
    id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN (
        'single_choice', 'multiple_choice', 'true_false', 
        'fill_blank', 'essay', 'coding', 'matching', 
        'ordering', 'short_answer'
    )),
    difficulty VARCHAR(10) DEFAULT 'medium' CHECK (difficulty IN ('easy', 'medium', 'hard')),
    default_points DECIMAL(6,2) DEFAULT 1.00 CHECK (default_points > 0),
    options JSONB, -- 选择题选项、编程题测试用例等
    correct_answer JSONB NOT NULL, -- 正确答案
    explanation TEXT, -- 答案解析
    hints JSONB DEFAULT '[]', -- 提示信息
    metadata JSONB DEFAULT '{}', -- 扩展元数据
    media_type VARCHAR(20), -- image, video, audio, document
    media_url VARCHAR(500),
    created_by BIGINT NOT NULL REFERENCES users(id),
    category_id BIGINT REFERENCES question_categories(id),
    status VARCHAR(20) DEFAULT 'draft' CHECK (status IN ('draft', 'review', 'active', 'archived', 'deleted')),
    version INTEGER DEFAULT 1,
    parent_question_id BIGINT, -- 用于题目版本管理
    language VARCHAR(10) DEFAULT 'zh-CN',
    estimated_time INTEGER DEFAULT 60, -- 预估答题时间（秒）
    cognitive_level VARCHAR(20) DEFAULT 'knowledge', -- bloom分类法
    learning_objectives JSONB DEFAULT '[]',
    usage_count INTEGER DEFAULT 0,
    avg_score DECIMAL(5,2),
    avg_time_spent INTEGER, -- 平均答题时间
    difficulty_rating DECIMAL(3,2), -- 实际难度评级
    discrimination_index DECIMAL(4,3), -- 区分度
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 创建分区表
CREATE TABLE questions_y2024 PARTITION OF questions
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
    
CREATE TABLE questions_y2025 PARTITION OF questions
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

-- 创建索引（需要在每个分区上创建）
CREATE INDEX CONCURRENTLY idx_questions_y2024_created_by ON questions_y2024(created_by);
CREATE INDEX CONCURRENTLY idx_questions_y2024_category_id ON questions_y2024(category_id);
CREATE INDEX CONCURRENTLY idx_questions_y2024_type ON questions_y2024(type);
CREATE INDEX CONCURRENTLY idx_questions_y2024_difficulty ON questions_y2024(difficulty);
CREATE INDEX CONCURRENTLY idx_questions_y2024_status ON questions_y2024(status);
CREATE INDEX CONCURRENTLY idx_questions_y2024_usage_count ON questions_y2024(usage_count);

-- 复合索引
CREATE INDEX CONCURRENTLY idx_questions_y2024_active_type_difficulty 
ON questions_y2024(status, type, difficulty) WHERE status = 'active';

-- JSON索引
CREATE INDEX CONCURRENTLY idx_questions_y2024_metadata_gin ON questions_y2024 USING GIN (metadata);
CREATE INDEX CONCURRENTLY idx_questions_y2024_options_gin ON questions_y2024 USING GIN (options);

-- 全文搜索索引
CREATE INDEX CONCURRENTLY idx_questions_y2024_content_search ON questions_y2024 USING GIN (
    to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, '') || ' ' || coalesce(explanation, ''))
);

COMMENT ON TABLE questions IS '题目信息表，按年份分区';
COMMENT ON COLUMN questions.options IS '题目选项配置，JSON格式';
COMMENT ON COLUMN questions.cognitive_level IS 'Bloom认知分类：knowledge, comprehension, application, analysis, synthesis, evaluation';
COMMENT ON COLUMN questions.discrimination_index IS '题目区分度，范围-1到1，值越大区分度越好';
```

#### 2.4.3 题目标签表 (question_tags)

```sql
CREATE TABLE question_tags (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    question_id BIGINT NOT NULL,
    tag VARCHAR(50) NOT NULL,
    tag_type VARCHAR(20) DEFAULT 'general' CHECK (tag_type IN ('general', 'skill', 'topic', 'keyword', 'difficulty')),
    weight DECIMAL(3,2) DEFAULT 1.00, -- 标签权重
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(question_id, tag, tag_type)
);

CREATE INDEX CONCURRENTLY idx_question_tags_question_id ON question_tags(question_id);
CREATE INDEX CONCURRENTLY idx_question_tags_tag ON question_tags(tag);
CREATE INDEX CONCURRENTLY idx_question_tags_type ON question_tags(tag_type);

-- 创建标签统计视图
CREATE MATERIALIZED VIEW question_tag_stats AS
SELECT 
    tag,
    tag_type,
    COUNT(*) as usage_count,
    AVG(weight) as avg_weight
FROM question_tags qt
JOIN questions q ON qt.question_id = q.id
WHERE q.status = 'active'
GROUP BY tag, tag_type;

CREATE UNIQUE INDEX ON question_tag_stats (tag, tag_type);

COMMENT ON TABLE question_tags IS '题目标签关联表';
COMMENT ON MATERIALIZED VIEW question_tag_stats IS '标签使用统计，每日刷新';
```

### 2.5 考试体系表

#### 2.5.1 考试表 (exams)

```sql
CREATE TABLE exams (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    code VARCHAR(50) NOT NULL UNIQUE,
    course_id BIGINT REFERENCES courses(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(30) DEFAULT 'quiz' CHECK (type IN ('quiz', 'assignment', 'midterm', 'final', 'certification', 'practice')),
    status VARCHAR(20) DEFAULT 'draft' CHECK (status IN ('draft', 'review', 'published', 'active', 'completed', 'archived', 'cancelled')),
    difficulty_level VARCHAR(20) DEFAULT 'medium',
    language VARCHAR(10) DEFAULT 'zh-CN',
    
    -- 时间设置
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    time_limit_type VARCHAR(20) DEFAULT 'fixed' CHECK (time_limit_type IN ('fixed', 'per_question', 'flexible')),
    extra_time_minutes INTEGER DEFAULT 0,
    
    -- 尝试设置
    max_attempts INTEGER DEFAULT 1 CHECK (max_attempts > 0),
    attempts_interval_hours INTEGER DEFAULT 0,
    
    -- 评分设置
    total_points DECIMAL(8,2) DEFAULT 0.00,
    passing_score DECIMAL(5,2) DEFAULT 60.00,
    grade_scale JSONB DEFAULT '{}', -- 等级评分标准
    auto_grading BOOLEAN DEFAULT TRUE,
    manual_grading_required BOOLEAN DEFAULT FALSE,
    
    -- 时间窗口
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    registration_start TIMESTAMP WITH TIME ZONE,
    registration_end TIMESTAMP WITH TIME ZONE,
    result_release_time TIMESTAMP WITH TIME ZONE,
    
    -- 考试设置
    settings JSONB DEFAULT '{}',
    instructions TEXT,
    rules JSONB DEFAULT '[]',
    
    -- 监考设置
    proctoring_enabled BOOLEAN DEFAULT FALSE,
    proctoring_config JSONB DEFAULT '{}',
    
    -- 题目设置
    shuffle_questions BOOLEAN DEFAULT FALSE,
    shuffle_options BOOLEAN DEFAULT FALSE,
    show_results_immediately BOOLEAN DEFAULT FALSE,
    allow_review BOOLEAN DEFAULT TRUE,
    allow_backtrack BOOLEAN DEFAULT TRUE,
    
    -- 统计信息
    question_count INTEGER DEFAULT 0,
    participant_count INTEGER DEFAULT 0,
    completion_rate DECIMAL(5,2) DEFAULT 0.00,
    average_score DECIMAL(5,2),
    
    -- 时间戳
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    
    -- 约束
    CONSTRAINT exam_time_window CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time),
    CONSTRAINT exam_registration_window CHECK (registration_start IS NULL OR registration_end IS NULL OR registration_start < registration_end)
);

-- 创建索引
CREATE INDEX CONCURRENTLY idx_exams_course_id ON exams(course_id);
CREATE INDEX CONCURRENTLY idx_exams_created_by ON exams(created_by);
CREATE INDEX CONCURRENTLY idx_exams_type ON exams(type);
CREATE INDEX CONCURRENTLY idx_exams_status ON exams(status);
CREATE INDEX CONCURRENTLY idx_exams_start_time ON exams(start_time);
CREATE INDEX CONCURRENTLY idx_exams_end_time ON exams(end_time);
CREATE INDEX CONCURRENTLY idx_exams_status_start ON exams(status, start_time);

-- JSON索引
CREATE INDEX CONCURRENTLY idx_exams_settings_gin ON exams USING GIN (settings);
CREATE INDEX CONCURRENTLY idx_exams_proctoring_config_gin ON exams USING GIN (proctoring_config);

-- 全文搜索
CREATE INDEX CONCURRENTLY idx_exams_search ON exams USING GIN (
    to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(code, ''))
);

COMMENT ON TABLE exams IS '考试信息表';
COMMENT ON COLUMN exams.settings IS '考试配置：显示设置、导航设置、提交设置等';
COMMENT ON COLUMN exams.proctoring_config IS '监考配置：摄像头、屏幕录制、AI检测等';
COMMENT ON COLUMN exams.grade_scale IS '评分等级配置：{90: "A", 80: "B", 70: "C"}';
```

#### 2.5.2 考试规则表 (exam_rules)

```sql
CREATE TABLE exam_rules (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL CHECK (rule_type IN (
        'time_limit', 'attempt_limit', 'ip_restriction', 
        'browser_requirement', 'device_restriction', 
        'proctoring_requirement', 'plagiarism_check'
    )),
    rule_config JSONB NOT NULL,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    violation_action VARCHAR(30) DEFAULT 'warning' CHECK (violation_action IN ('warning', 'terminate', 'flag', 'ignore')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(exam_id, rule_type, priority)
);

CREATE INDEX CONCURRENTLY idx_exam_rules_exam_id ON exam_rules(exam_id);
CREATE INDEX CONCURRENTLY idx_exam_rules_type ON exam_rules(rule_type);
CREATE INDEX CONCURRENTLY idx_exam_rules_priority ON exam_rules(priority);

COMMENT ON TABLE exam_rules IS '考试规则配置表';
COMMENT ON COLUMN exam_rules.rule_config IS '规则配置JSON：条件、参数、阈值等';
```

#### 2.5.3 考试题目关联表 (exam_questions)

```sql
CREATE TABLE exam_questions (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL,
    section_id BIGINT, -- 考试分组/部分
    order_number INTEGER NOT NULL,
    points DECIMAL(6,2) NOT NULL CHECK (points > 0),
    time_limit_seconds INTEGER, -- 单题时间限制
    custom_config JSONB DEFAULT '{}', -- 题目特定配置
    is_required BOOLEAN DEFAULT TRUE,
    is_bonus BOOLEAN DEFAULT FALSE, -- 是否为加分题
    weight DECIMAL(4,2) DEFAULT 1.00, -- 题目权重
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(exam_id, order_number),
    UNIQUE(exam_id, question_id) -- 防止同一题目在考试中重复
);

CREATE INDEX CONCURRENTLY idx_exam_questions_exam_id ON exam_questions(exam_id);
CREATE INDEX CONCURRENTLY idx_exam_questions_question_id ON exam_questions(question_id);
CREATE INDEX CONCURRENTLY idx_exam_questions_section_id ON exam_questions(section_id);
CREATE INDEX CONCURRENTLY idx_exam_questions_order ON exam_questions(exam_id, order_number);

COMMENT ON TABLE exam_questions IS '考试题目关联表';
COMMENT ON COLUMN exam_questions.custom_config IS '题目自定义配置：显示方式、特殊要求等';
```

### 2.6 考试记录表

#### 2.6.1 考试记录表 (exam_attempts)

**表描述**: 用户考试记录，高频访问表，需要分区优化

```sql
-- 按时间和组织分区的考试记录表
CREATE TABLE exam_attempts (
    id BIGINT NOT NULL,
    attempt_code VARCHAR(64) NOT NULL UNIQUE,
    exam_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    
    -- 状态信息
    status VARCHAR(20) DEFAULT 'in_progress' CHECK (status IN (
        'in_progress', 'paused', 'submitted', 'auto_submitted', 
        'graded', 'under_review', 'cancelled', 'flagged'
    )),
    attempt_number INTEGER DEFAULT 1,
    
    -- 评分信息
    score DECIMAL(6,2),
    percentage DECIMAL(5,2),
    grade VARCHAR(5),
    points_earned DECIMAL(8,2) DEFAULT 0.00,
    points_possible DECIMAL(8,2) DEFAULT 0.00,
    bonus_points DECIMAL(6,2) DEFAULT 0.00,
    
    -- 统计信息
    questions_answered INTEGER DEFAULT 0,
    questions_correct INTEGER DEFAULT 0,
    questions_partial INTEGER DEFAULT 0,
    questions_skipped INTEGER DEFAULT 0,
    
    -- 时间信息
    time_spent_seconds INTEGER DEFAULT 0,
    remaining_time_seconds INTEGER,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITH TIME ZONE,
    graded_at TIMESTAMP WITH TIME ZONE,
    last_activity TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- 环境信息
    browser_info JSONB,
    device_info JSONB,
    ip_address INET,
    geolocation JSONB,
    user_agent TEXT,
    
    -- 监考信息
    proctoring_session_id VARCHAR(128),
    integrity_score DECIMAL(4,3), -- 诚信度评分 0-1
    violations_count INTEGER DEFAULT 0,
    flags JSONB DEFAULT '[]',
    
    -- 答案摘要
    answers_summary JSONB,
    time_tracking JSONB,
    navigation_log JSONB DEFAULT '[]',
    
    -- 安全日志
    security_events JSONB DEFAULT '[]',
    suspicious_activities JSONB DEFAULT '[]',
    
    -- 评分详情
    auto_grading_completed BOOLEAN DEFAULT FALSE,
    manual_grading_required BOOLEAN DEFAULT FALSE,
    manual_grading_completed BOOLEAN DEFAULT FALSE,
    graded_by BIGINT,
    
    -- 元数据
    metadata JSONB DEFAULT '{}',
    
    PRIMARY KEY (id, started_at, organization_id)
) PARTITION BY RANGE (started_at);

-- 创建分区表（按月分区以优化查询性能）
CREATE TABLE exam_attempts_2024_01 PARTITION OF exam_attempts
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
    
CREATE TABLE exam_attempts_2024_02 PARTITION OF exam_attempts
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- 继续创建其他月份分区...

-- 为每个分区创建索引
CREATE INDEX CONCURRENTLY idx_exam_attempts_2024_01_exam_id ON exam_attempts_2024_01(exam_id);
CREATE INDEX CONCURRENTLY idx_exam_attempts_2024_01_user_id ON exam_attempts_2024_01(user_id);
CREATE INDEX CONCURRENTLY idx_exam_attempts_2024_01_status ON exam_attempts_2024_01(status);
CREATE INDEX CONCURRENTLY idx_exam_attempts_2024_01_user_exam_status ON exam_attempts_2024_01(user_id, exam_id, status);
CREATE INDEX CONCURRENTLY idx_exam_attempts_2024_01_score ON exam_attempts_2024_01(score) WHERE score IS NOT NULL;
CREATE INDEX CONCURRENTLY idx_exam_attempts_2024_01_submitted_at ON exam_attempts_2024_01(submitted_at) WHERE submitted_at IS NOT NULL;

-- JSON索引
CREATE INDEX CONCURRENTLY idx_exam_attempts_2024_01_browser_info_gin ON exam_attempts_2024_01 USING GIN (browser_info);
CREATE INDEX CONCURRENTLY idx_exam_attempts_2024_01_flags_gin ON exam_attempts_2024_01 USING GIN (flags);

COMMENT ON TABLE exam_attempts IS '考试记录表，按时间分区存储';
COMMENT ON COLUMN exam_attempts.attempt_code IS '考试记录唯一编码，便于查询和跟踪';
COMMENT ON COLUMN exam_attempts.integrity_score IS '考试诚信度评分，基于AI监考分析';
```

#### 2.6.2 答案表 (answers)

```sql
-- 答案表也按时间分区
CREATE TABLE answers (
    id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    
    -- 答案内容
    user_answer JSONB, -- 用户答案
    original_answer JSONB, -- 原始答案（用于审计）
    correct_answer JSONB, -- 正确答案
    
    -- 评分信息
    points_earned DECIMAL(6,2) DEFAULT 0.00,
    points_possible DECIMAL(6,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'ungraded' CHECK (status IN (
        'correct', 'incorrect', 'partial', 'ungraded', 
        'under_review', 'disputed', 'bonus'
    )),
    
    -- 评分详情
    auto_score DECIMAL(6,2), -- 自动评分结果
    manual_score DECIMAL(6,2), -- 人工评分结果
    final_score DECIMAL(6,2), -- 最终得分
    grading_method VARCHAR(20) DEFAULT 'auto', -- auto, manual, hybrid
    
    -- 反馈信息
    feedback TEXT,
    grader_notes TEXT,
    rubric_scores JSONB, -- 评分细则得分
    
    -- 时间信息
    time_spent_seconds INTEGER DEFAULT 0,
    first_answered_at TIMESTAMP WITH TIME ZONE,
    last_modified_at TIMESTAMP WITH TIME ZONE,
    answered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    graded_at TIMESTAMP WITH TIME ZONE,
    
    -- 行为分析
    revision_count INTEGER DEFAULT 0,
    keystroke_patterns JSONB,
    mouse_patterns JSONB,
    confidence_level INTEGER, -- 1-5 学生自评信心度
    
    -- 元数据
    metadata JSONB DEFAULT '{}',
    version INTEGER DEFAULT 1,
    
    -- 审计字段
    graded_by BIGINT,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    
    PRIMARY KEY (id, answered_at)
) PARTITION BY RANGE (answered_at);

-- 创建分区
CREATE TABLE answers_2024_01 PARTITION OF answers
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- 创建索引
CREATE INDEX CONCURRENTLY idx_answers_2024_01_attempt_id ON answers_2024_01(attempt_id);
CREATE INDEX CONCURRENTLY idx_answers_2024_01_question_id ON answers_2024_01(question_id);
CREATE INDEX CONCURRENTLY idx_answers_2024_01_status ON answers_2024_01(status);
CREATE INDEX CONCURRENTLY idx_answers_2024_01_attempt_question ON answers_2024_01(attempt_id, question_id);
CREATE INDEX CONCURRENTLY idx_answers_2024_01_points_earned ON answers_2024_01(points_earned);

-- JSON索引
CREATE INDEX CONCURRENTLY idx_answers_2024_01_user_answer_gin ON answers_2024_01 USING GIN (user_answer);

COMMENT ON TABLE answers IS '考试答案表，按时间分区';
COMMENT ON COLUMN answers.rubric_scores IS '评分细则得分详情，JSON格式';
COMMENT ON COLUMN answers.keystroke_patterns IS '键盘输入模式分析，用于检测异常';
```

### 2.7 监考系统表

#### 2.7.1 监考日志表 (proctor_logs)

```sql
-- 监考日志表，数据量大，需要分区
CREATE TABLE proctor_logs (
    id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    
    -- 事件信息
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN (
        'session_start', 'session_end', 'face_detection', 'face_not_detected',
        'multiple_person', 'gaze_tracking', 'tab_switching', 'window_switching',
        'screen_sharing_stopped', 'camera_blocked', 'microphone_blocked',
        'suspicious_behavior', 'network_disconnection', 'device_change',
        'copy_paste', 'right_click', 'keyboard_shortcut', 'mobile_device_detected'
    )),
    severity VARCHAR(10) DEFAULT 'info' CHECK (severity IN ('info', 'warning', 'error', 'critical')),
    
    -- 事件数据
    event_data JSONB NOT NULL,
    raw_data JSONB, -- 原始传感器数据
    processed_data JSONB, -- 处理后的数据
    
    -- 媒体证据
    media_type VARCHAR(20), -- screenshot, video_clip, audio_clip
    media_url VARCHAR(500),
    media_metadata JSONB,
    
    -- AI分析结果
    ai_analysis JSONB,
    ai_model_version VARCHAR(20),
    confidence_score DECIMAL(5,4) CHECK (confidence_score >= 0 AND confidence_score <= 1),
    risk_score DECIMAL(5,4) CHECK (risk_score >= 0 AND risk_score <= 1),
    
    -- 人工审核
    requires_review BOOLEAN DEFAULT FALSE,
    reviewed BOOLEAN DEFAULT FALSE,
    reviewer_id BIGINT,
    reviewer_decision VARCHAR(20), -- approved, rejected, needs_investigation
    reviewer_notes TEXT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    
    -- 时间信息
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- 位置信息
    screen_coordinates JSONB, -- {x: 123, y: 456}
    camera_region JSONB, -- 摄像头检测区域
    
    -- 上下文信息
    question_id BIGINT,
    user_activity JSONB, -- 用户当时的操作
    
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

-- 创建分区（按周分区，监考数据时效性要求高）
CREATE TABLE proctor_logs_2024_w01 PARTITION OF proctor_logs
    FOR VALUES FROM ('2024-01-01') TO ('2024-01-08');

-- 创建索引
CREATE INDEX CONCURRENTLY idx_proctor_logs_2024_w01_attempt_id ON proctor_logs_2024_w01(attempt_id);
CREATE INDEX CONCURRENTLY idx_proctor_logs_2024_w01_session_id ON proctor_logs_2024_w01(session_id);
CREATE INDEX CONCURRENTLY idx_proctor_logs_2024_w01_event_type ON proctor_logs_2024_w01(event_type);
CREATE INDEX CONCURRENTLY idx_proctor_logs_2024_w01_severity ON proctor_logs_2024_w01(severity);
CREATE INDEX CONCURRENTLY idx_proctor_logs_2024_w01_requires_review ON proctor_logs_2024_w01(requires_review) WHERE requires_review = TRUE;
CREATE INDEX CONCURRENTLY idx_proctor_logs_2024_w01_confidence_score ON proctor_logs_2024_w01(confidence_score);

-- JSON索引
CREATE INDEX CONCURRENTLY idx_proctor_logs_2024_w01_event_data_gin ON proctor_logs_2024_w01 USING GIN (event_data);
CREATE INDEX CONCURRENTLY idx_proctor_logs_2024_w01_ai_analysis_gin ON proctor_logs_2024_w01 USING GIN (ai_analysis);

COMMENT ON TABLE proctor_logs IS '监考日志表，记录所有监考事件';
COMMENT ON COLUMN proctor_logs.confidence_score IS 'AI检测的置信度分数';
COMMENT ON COLUMN proctor_logs.risk_score IS '风险评分，用于优先级排序';
```

#### 2.7.2 监考会话表 (proctor_sessions)

```sql
CREATE TABLE proctor_sessions (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    session_id VARCHAR(128) NOT NULL UNIQUE,
    attempt_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    exam_id BIGINT NOT NULL,
    
    -- 会话状态
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'paused', 'completed', 'terminated', 'error')),
    
    -- 技术信息
    webrtc_config JSONB,
    connection_quality VARCHAR(20) DEFAULT 'unknown',
    bandwidth_info JSONB,
    device_capabilities JSONB,
    
    -- 监控配置
    monitoring_rules JSONB NOT NULL,
    recording_config JSONB,
    ai_analysis_config JSONB,
    
    -- 统计信息
    total_events INTEGER DEFAULT 0,
    warning_events INTEGER DEFAULT 0,
    error_events INTEGER DEFAULT 0,
    critical_events INTEGER DEFAULT 0,
    
    -- 质量指标
    video_quality_score DECIMAL(3,2),
    audio_quality_score DECIMAL(3,2),
    overall_quality_score DECIMAL(3,2),
    uptime_percentage DECIMAL(5,2),
    
    -- 录制信息
    recording_urls JSONB,
    recording_duration_seconds INTEGER DEFAULT 0,
    recording_size_mb DECIMAL(10,2),
    
    -- 时间信息
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    last_heartbeat TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- 最终评估
    final_integrity_score DECIMAL(4,3),
    recommendation VARCHAR(20), -- pass, flag, investigate, reject
    manual_review_required BOOLEAN DEFAULT FALSE,
    
    -- 审计信息
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX CONCURRENTLY idx_proctor_sessions_attempt_id ON proctor_sessions(attempt_id);
CREATE INDEX CONCURRENTLY idx_proctor_sessions_user_id ON proctor_sessions(user_id);
CREATE INDEX CONCURRENTLY idx_proctor_sessions_exam_id ON proctor_sessions(exam_id);
CREATE INDEX CONCURRENTLY idx_proctor_sessions_status ON proctor_sessions(status);
CREATE INDEX CONCURRENTLY idx_proctor_sessions_started_at ON proctor_sessions(started_at);
CREATE INDEX CONCURRENTLY idx_proctor_sessions_integrity_score ON proctor_sessions(final_integrity_score) WHERE final_integrity_score IS NOT NULL;

COMMENT ON TABLE proctor_sessions IS '监考会话管理表';
COMMENT ON COLUMN proctor_sessions.final_integrity_score IS '最终诚信度评分，综合所有监考数据';
```

### 2.8 统计分析表

#### 2.8.1 考试统计表 (exam_statistics)

```sql
-- 物化视图形式的统计表，定期刷新
CREATE MATERIALIZED VIEW exam_statistics AS
SELECT 
    e.id as exam_id,
    e.title,
    e.course_id,
    e.created_by,
    
    -- 参与统计
    COUNT(ea.id) as total_attempts,
    COUNT(DISTINCT ea.user_id) as unique_participants,
    COUNT(CASE WHEN ea.status = 'completed' THEN 1 END) as completed_attempts,
    COUNT(CASE WHEN ea.status = 'in_progress' THEN 1 END) as in_progress_attempts,
    
    -- 分数统计
    AVG(ea.score) as average_score,
    STDDEV(ea.score) as score_std_dev,
    MIN(ea.score) as min_score,
    MAX(ea.score) as max_score,
    PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY ea.score) as score_q1,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY ea.score) as median_score,
    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY ea.score) as score_q3,
    
    -- 时间统计
    AVG(ea.time_spent_seconds) as avg_time_spent,
    MIN(ea.time_spent_seconds) as min_time_spent,
    MAX(ea.time_spent_seconds) as max_time_spent,
    
    -- 通过率统计
    COUNT(CASE WHEN ea.score >= e.passing_score THEN 1 END)::DECIMAL / NULLIF(COUNT(ea.id), 0) * 100 as pass_rate,
    
    -- 完成率统计
    COUNT(CASE WHEN ea.status IN ('completed', 'graded') THEN 1 END)::DECIMAL / NULLIF(COUNT(ea.id), 0) * 100 as completion_rate,
    
    -- 诚信度统计
    AVG(ea.integrity_score) as avg_integrity_score,
    COUNT(CASE WHEN ea.violations_count > 0 THEN 1 END) as attempts_with_violations,
    
    -- 时间窗口
    DATE_TRUNC('day', MIN(ea.started_at)) as first_attempt_date,
    DATE_TRUNC('day', MAX(ea.started_at)) as last_attempt_date,
    
    -- 更新时间
    CURRENT_TIMESTAMP as calculated_at
    
FROM exams e
LEFT JOIN exam_attempts ea ON e.id = ea.exam_id
WHERE e.status IN ('published', 'active', 'completed')
GROUP BY e.id, e.title, e.course_id, e.created_by, e.passing_score;

-- 创建唯一索引以支持并发刷新
CREATE UNIQUE INDEX ON exam_statistics (exam_id);
CREATE INDEX ON exam_statistics (course_id);
CREATE INDEX ON exam_statistics (average_score);
CREATE INDEX ON exam_statistics (pass_rate);
CREATE INDEX ON exam_statistics (calculated_at);

COMMENT ON MATERIALIZED VIEW exam_statistics IS '考试统计数据，每小时刷新一次';
```

#### 2.8.2 用户表现表 (user_performance)

```sql
CREATE TABLE user_performance (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id BIGINT REFERENCES exams(id),
    course_id BIGINT REFERENCES courses(id),
    organization_id BIGINT REFERENCES organizations(id),
    
    -- 性能指标
    performance_data JSONB NOT NULL,
    
    -- 综合评分
    overall_score DECIMAL(6,2),
    percentile_rank DECIMAL(5,2), -- 百分位排名
    z_score DECIMAL(6,3), -- 标准分数
    
    -- 排名信息
    rank_position INTEGER,
    total_participants INTEGER,
    
    -- 能力评估
    skill_levels JSONB, -- 各技能点评分
    knowledge_gaps JSONB, -- 知识薄弱点
    strengths JSONB, -- 优势领域
    
    -- 学习建议
    recommendations JSONB DEFAULT '[]',
    next_steps JSONB DEFAULT '[]',
    
    -- 时间信息
    performance_period_start DATE,
    performance_period_end DATE,
    calculated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- 元数据
    calculation_method VARCHAR(50) DEFAULT 'irt_model', -- IRT, classical, adaptive
    confidence_interval JSONB, -- 置信区间
    
    UNIQUE(user_id, exam_id, calculated_at) -- 防重复计算
);

CREATE INDEX CONCURRENTLY idx_user_performance_user_id ON user_performance(user_id);
CREATE INDEX CONCURRENTLY idx_user_performance_exam_id ON user_performance(exam_id);
CREATE INDEX CONCURRENTLY idx_user_performance_course_id ON user_performance(course_id);
CREATE INDEX CONCURRENTLY idx_user_performance_overall_score ON user_performance(overall_score);
CREATE INDEX CONCURRENTLY idx_user_performance_rank ON user_performance(rank_position);
CREATE INDEX CONCURRENTLY idx_user_performance_calculated_at ON user_performance(calculated_at);

-- JSON索引
CREATE INDEX CONCURRENTLY idx_user_performance_data_gin ON user_performance USING GIN (performance_data);
CREATE INDEX CONCURRENTLY idx_user_performance_skills_gin ON user_performance USING GIN (skill_levels);

COMMENT ON TABLE user_performance IS '用户学习表现分析表';
COMMENT ON COLUMN user_performance.z_score IS '标准分数，用于跨考试比较';
COMMENT ON COLUMN user_performance.skill_levels IS '各知识点/技能掌握程度评分';
```

### 2.9 系统配置表

#### 2.9.1 系统配置表 (system_configs)

```sql
CREATE TABLE system_configs (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    config_type VARCHAR(20) DEFAULT 'string' CHECK (config_type IN ('string', 'number', 'boolean', 'json', 'encrypted')),
    category VARCHAR(50) DEFAULT 'general',
    description TEXT,
    default_value TEXT,
    validation_rules JSONB,
    is_encrypted BOOLEAN DEFAULT FALSE,
    is_sensitive BOOLEAN DEFAULT FALSE,
    requires_restart BOOLEAN DEFAULT FALSE,
    environment VARCHAR(20) DEFAULT 'all' CHECK (environment IN ('all', 'development', 'staging', 'production')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id)
);

CREATE INDEX CONCURRENTLY idx_system_configs_category ON system_configs(category);
CREATE INDEX CONCURRENTLY idx_system_configs_environment ON system_configs(environment);
CREATE INDEX CONCURRENTLY idx_system_configs_sensitive ON system_configs(is_sensitive);

COMMENT ON TABLE system_configs IS '系统配置参数表';
COMMENT ON COLUMN system_configs.validation_rules IS '配置值验证规则，JSON格式';
```

---

## 3. 索引优化策略

### 3.1 主要索引类型

#### 3.1.1 B-Tree索引（默认）
```sql
-- 单列索引
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);

-- 复合索引（注意字段顺序）
CREATE INDEX CONCURRENTLY idx_exam_attempts_user_exam_status 
ON exam_attempts(user_id, exam_id, status);

-- 部分索引（提高效率）
CREATE INDEX CONCURRENTLY idx_exams_active_published 
ON exams(start_time, end_time) 
WHERE status = 'published';

-- 包含索引（减少回表）
CREATE INDEX CONCURRENTLY idx_questions_type_difficulty 
ON questions(type, difficulty) 
INCLUDE (title, default_points);
```

#### 3.1.2 GIN索引（JSON和全文搜索）
```sql
-- JSON字段索引
CREATE INDEX CONCURRENTLY idx_questions_metadata_gin 
ON questions USING GIN (metadata);

-- 全文搜索索引
CREATE INDEX CONCURRENTLY idx_questions_fulltext_search 
ON questions USING GIN (
    to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))
);

-- 多字段GIN索引
CREATE INDEX CONCURRENTLY idx_exam_attempts_flags_gin 
ON exam_attempts USING GIN (flags, security_events);
```

#### 3.1.3 Hash索引（等值查询）
```sql
-- 适用于大表的等值查询
CREATE INDEX CONCURRENTLY idx_user_sessions_session_id_hash 
ON user_sessions USING HASH (session_id);
```

### 3.2 索引监控和维护

#### 3.2.1 索引使用情况监控
```sql
-- 监控索引使用情况
CREATE OR REPLACE VIEW index_usage_stats AS
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- 查找未使用的索引
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0 
  AND schemaname = 'public'
  AND indexname NOT LIKE '%_pkey';
```

#### 3.2.2 索引维护脚本
```sql
-- 重建索引统计信息
CREATE OR REPLACE FUNCTION refresh_table_statistics()
RETURNS TEXT AS $$
DECLARE
    table_record RECORD;
    result_text TEXT := '';
BEGIN
    FOR table_record IN 
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
    LOOP
        EXECUTE 'ANALYZE ' || table_record.tablename;
        result_text := result_text || table_record.tablename || ', ';
    END LOOP;
    
    RETURN 'Analyzed tables: ' || result_text;
END;
$$ LANGUAGE plpgsql;

-- 定期清理和重建索引
CREATE OR REPLACE FUNCTION maintenance_reindex_tables()
RETURNS TEXT AS $$
DECLARE
    table_record RECORD;
    result_text TEXT := '';
BEGIN
    FOR table_record IN 
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public'
          AND tablename LIKE '%_202%' -- 只处理分区表
    LOOP
        EXECUTE 'REINDEX TABLE ' || table_record.tablename;
        result_text := result_text || table_record.tablename || ', ';
    END LOOP;
    
    RETURN 'Reindexed tables: ' || result_text;
END;
$$ LANGUAGE plpgsql;
```

---

## 4. 分区策略

### 4.1 时间分区

#### 4.1.1 考试记录表分区
```sql
-- 按月分区的考试记录表
CREATE TABLE exam_attempts_y2024m01 PARTITION OF exam_attempts
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE exam_attempts_y2024m02 PARTITION OF exam_attempts
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- 自动创建分区的函数
CREATE OR REPLACE FUNCTION create_monthly_partition(
    table_name TEXT, 
    start_date DATE
) RETURNS TEXT AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := table_name || '_y' || EXTRACT(YEAR FROM start_date) || 'm' || LPAD(EXTRACT(MONTH FROM start_date)::TEXT, 2, '0');
    end_date := start_date + INTERVAL '1 month';
    
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
        partition_name, table_name, start_date, end_date
    );
    
    -- 创建索引
    EXECUTE format('CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_%s_created_at ON %I (created_at)', partition_name, partition_name);
    
    RETURN partition_name || ' created successfully';
END;
$$ LANGUAGE plpgsql;
```

#### 4.1.2 自动分区管理
```sql
-- 定时任务创建未来分区
CREATE OR REPLACE FUNCTION auto_create_partitions()
RETURNS TEXT AS $$
DECLARE
    current_month DATE;
    future_month DATE;
    result_text TEXT := '';
BEGIN
    current_month := DATE_TRUNC('month', CURRENT_DATE);
    
    -- 创建未来3个月的分区
    FOR i IN 0..2 LOOP
        future_month := current_month + (i || ' month')::INTERVAL;
        
        -- 为主要表创建分区
        result_text := result_text || create_monthly_partition('exam_attempts', future_month) || '; ';
        result_text := result_text || create_monthly_partition('answers', future_month) || '; ';
        result_text := result_text || create_monthly_partition('proctor_logs', future_month) || '; ';
    END LOOP;
    
    RETURN result_text;
END;
$$ LANGUAGE plpgsql;

-- 清理旧分区
CREATE OR REPLACE FUNCTION cleanup_old_partitions(
    table_name TEXT,
    keep_months INTEGER DEFAULT 12
) RETURNS TEXT AS $$
DECLARE
    partition_record RECORD;
    cutoff_date DATE;
    result_text TEXT := '';
BEGIN
    cutoff_date := DATE_TRUNC('month', CURRENT_DATE) - (keep_months || ' month')::INTERVAL;
    
    FOR partition_record IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE tablename LIKE table_name || '_y%'
          AND schemaname = 'public'
    LOOP
        -- 解析分区日期并判断是否需要删除
        -- 这里需要根据分区命名规则来解析日期
        -- 简化处理：如果表名包含较早的年份则删除
        IF partition_record.tablename ~ (EXTRACT(YEAR FROM cutoff_date) - 1)::TEXT THEN
            EXECUTE 'DROP TABLE IF EXISTS ' || partition_record.tablename;
            result_text := result_text || partition_record.tablename || ' dropped; ';
        END IF;
    END LOOP;
    
    RETURN result_text;
END;
$$ LANGUAGE plpgsql;
```

### 4.2 范围分区

#### 4.2.1 按组织分区
```sql
-- 按组织ID范围分区（适用于多租户）
CREATE TABLE user_activities_org_1_100 PARTITION OF user_activities
    FOR VALUES FROM (1) TO (101);
    
CREATE TABLE user_activities_org_101_200 PARTITION OF user_activities
    FOR VALUES FROM (101) TO (201);
```

---

## 5. 性能优化

### 5.1 查询优化

#### 5.1.1 常见查询模式优化

```sql
-- 1. 用户考试历史查询优化
-- 优化前：可能产生全表扫描
SELECT * FROM exam_attempts 
WHERE user_id = 12345 
ORDER BY started_at DESC;

-- 优化后：使用复合索引
CREATE INDEX CONCURRENTLY idx_exam_attempts_user_started 
ON exam_attempts(user_id, started_at DESC);

-- 2. 考试统计查询优化
-- 优化前：实时计算统计信息
SELECT 
    COUNT(*) as total_attempts,
    AVG(score) as avg_score,
    MAX(score) as max_score
FROM exam_attempts 
WHERE exam_id = 456 AND status = 'completed';

-- 优化后：使用物化视图
REFRESH MATERIALIZED VIEW CONCURRENTLY exam_statistics;

SELECT 
    total_attempts,
    average_score,
    max_score
FROM exam_statistics 
WHERE exam_id = 456;

-- 3. 题目搜索优化
-- 优化前：LIKE查询性能差
SELECT * FROM questions 
WHERE title LIKE '%数据结构%' OR content LIKE '%数据结构%';

-- 优化后：使用全文搜索
SELECT *, ts_rank(search_vector, query) as rank
FROM questions, plainto_tsquery('simple', '数据结构') query
WHERE search_vector @@ query
ORDER BY rank DESC;
```

#### 5.1.2 复杂查询优化

```sql
-- 考试排行榜查询优化
WITH ranked_attempts AS (
    SELECT 
        user_id,
        exam_id,
        score,
        ROW_NUMBER() OVER (PARTITION BY exam_id ORDER BY score DESC, submitted_at ASC) as rank
    FROM exam_attempts
    WHERE status = 'completed'
      AND exam_id = $1
),
user_info AS (
    SELECT 
        u.id,
        u.username,
        u.first_name,
        u.last_name,
        u.avatar_url
    FROM users u
    WHERE u.id IN (SELECT user_id FROM ranked_attempts WHERE rank <= 100)
)
SELECT 
    ra.rank,
    ra.score,
    ui.username,
    ui.first_name,
    ui.last_name,
    ui.avatar_url
FROM ranked_attempts ra
JOIN user_info ui ON ra.user_id = ui.id
WHERE ra.rank <= 100
ORDER BY ra.rank;
```

### 5.2 连接池配置

```sql
-- 连接池监控视图
CREATE OR REPLACE VIEW connection_stats AS
SELECT 
    datname,
    numbackends,
    xact_commit,
    xact_rollback,
    blks_read,
    blks_hit,
    temp_files,
    temp_bytes,
    deadlocks,
    stats_reset
FROM pg_stat_database 
WHERE datname = current_database();

-- 活跃连接监控
CREATE OR REPLACE VIEW active_connections AS
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    query_start,
    state_change,
    EXTRACT(EPOCH FROM (now() - query_start)) as query_duration_seconds,
    query
FROM pg_stat_activity 
WHERE state != 'idle'
ORDER BY query_start;
```

### 5.3 缓存策略

#### 5.3.1 应用级缓存配置

```sql
-- 缓存配置表
CREATE TABLE cache_configs (
    cache_key VARCHAR(100) PRIMARY KEY,
    ttl_seconds INTEGER DEFAULT 3600,
    cache_type VARCHAR(20) DEFAULT 'redis' CHECK (cache_type IN ('redis', 'memory', 'database')),
    invalidation_rules JSONB DEFAULT '[]',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 插入缓存配置
INSERT INTO cache_configs (cache_key, ttl_seconds, cache_type, invalidation_rules) VALUES
('exam_details', 1800, 'redis', '["exam_updated", "questions_changed"]'),
('user_profile', 3600, 'redis', '["user_updated", "role_changed"]'),
('question_list', 7200, 'redis', '["question_created", "question_updated"]'),
('exam_statistics', 3600, 'database', '["exam_completed", "hourly_refresh"]');
```

#### 5.3.2 数据库级缓存

```sql
-- 配置数据库缓存参数
ALTER SYSTEM SET shared_buffers = '2GB';
ALTER SYSTEM SET effective_cache_size = '6GB';
ALTER SYSTEM SET work_mem = '16MB';
ALTER SYSTEM SET maintenance_work_mem = '512MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '64MB';
ALTER SYSTEM SET default_statistics_target = 500;

-- 重新加载配置
SELECT pg_reload_conf();
```

---

## 6. 数据安全与备份

### 6.1 数据加密

#### 6.1.1 字段级加密

```sql
-- 启用加密扩展
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 敏感字段加密函数
CREATE OR REPLACE FUNCTION encrypt_sensitive_data(data TEXT, key_id TEXT DEFAULT 'default')
RETURNS TEXT AS $$
BEGIN
    RETURN encode(
        encrypt(
            data::bytea, 
            decode(get_config_value('encryption_key_' || key_id), 'base64'),
            'aes'
        ), 
        'base64'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 解密函数
CREATE OR REPLACE FUNCTION decrypt_sensitive_data(encrypted_data TEXT, key_id TEXT DEFAULT 'default')
RETURNS TEXT AS $$
BEGIN
    RETURN convert_from(
        decrypt(
            decode(encrypted_data, 'base64'),
            decode(get_config_value('encryption_key_' || key_id), 'base64'),
            'aes'
        ),
        'UTF8'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 获取配置值函数
CREATE OR REPLACE FUNCTION get_config_value(key_name TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN (SELECT config_value FROM system_configs WHERE config_key = key_name);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

#### 6.1.2 行级安全策略

```sql
-- 启用行级安全
ALTER TABLE exam_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_performance ENABLE ROW LEVEL SECURITY;

-- 用户只能访问自己的考试记录
CREATE POLICY user_own_attempts ON exam_attempts
    FOR ALL TO application_role
    USING (user_id = current_setting('app.current_user_id')::BIGINT);

-- 教师可以访问自己课程的考试记录
CREATE POLICY teacher_course_attempts ON exam_attempts
    FOR SELECT TO teacher_role
    USING (
        exam_id IN (
            SELECT e.id 
            FROM exams e 
            JOIN courses c ON e.course_id = c.id 
            WHERE c.instructor_id = current_setting('app.current_user_id')::BIGINT
        )
    );

-- 管理员可以访问本组织的所有记录
CREATE POLICY admin_org_attempts ON exam_attempts
    FOR ALL TO admin_role
    USING (
        organization_id = current_setting('app.current_org_id')::BIGINT
    );
```

### 6.2 备份策略

#### 6.2.1 自动备份脚本

```bash
#!/bin/bash
# database_backup.sh

# 配置变量
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="exam_system"
DB_USER="backup_user"
BACKUP_DIR="/var/backups/postgresql"
RETENTION_DAYS=30
S3_BUCKET="exam-system-backups"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 生成备份文件名
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="exam_system_backup_${TIMESTAMP}.sql"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILE}"

# 执行备份
echo "开始备份数据库: $(date)"
pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
    --format=custom \
    --compress=9 \
    --verbose \
    --file=$BACKUP_PATH

if [ $? -eq 0 ]; then
    echo "数据库备份完成: $BACKUP_PATH"
    
    # 压缩备份文件
    gzip $BACKUP_PATH
    COMPRESSED_FILE="${BACKUP_PATH}.gz"
    
    # 上传到S3
    aws s3 cp $COMPRESSED_FILE s3://$S3_BUCKET/daily/
    
    # 清理本地旧备份
    find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
    
    echo "备份流程完成: $(date)"
else
    echo "备份失败: $(date)"
    exit 1
fi
```

#### 6.2.2 数据恢复脚本

```bash
#!/bin/bash
# database_restore.sh

# 配置变量
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="exam_system_restore"
DB_USER="restore_user"
BACKUP_FILE="$1"

if [ -z "$BACKUP_FILE" ]; then
    echo "使用方法: $0 <backup_file>"
    exit 1
fi

# 检查备份文件
if [ ! -f "$BACKUP_FILE" ]; then
    echo "备份文件不存在: $BACKUP_FILE"
    exit 1
fi

# 创建恢复数据库
echo "创建恢复数据库: $DB_NAME"
createdb -h $DB_HOST -p $DB_PORT -U $DB_USER $DB_NAME

# 恢复数据
echo "开始恢复数据: $(date)"
if [[ $BACKUP_FILE == *.gz ]]; then
    gunzip -c $BACKUP_FILE | pg_restore -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME --verbose
else
    pg_restore -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME --verbose $BACKUP_FILE
fi

if [ $? -eq 0 ]; then
    echo "数据恢复完成: $(date)"
    echo "恢复的数据库: $DB_NAME"
else
    echo "数据恢复失败: $(date)"
    exit 1
fi
```

---

## 7. 监控与维护

### 7.1 性能监控

#### 7.1.1 数据库性能监控视图

```sql
-- 慢查询监控
CREATE OR REPLACE VIEW slow_queries AS
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time,
    stddev_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements 
WHERE mean_time > 1000 -- 超过1秒的查询
ORDER BY total_time DESC;

-- 表空间使用情况
CREATE OR REPLACE VIEW table_sizes AS
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    pg_total_relation_size(schemaname||'.'||tablename) AS size_bytes
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 索引效率分析
CREATE OR REPLACE VIEW index_efficiency AS
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
    CASE 
        WHEN idx_scan = 0 THEN 'Unused'
        WHEN idx_scan < 50 THEN 'Low Usage'
        WHEN idx_scan < 500 THEN 'Medium Usage'
        ELSE 'High Usage'
    END AS usage_category
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

#### 7.1.2 应用层监控

```sql
-- 考试系统关键指标监控表
CREATE TABLE system_metrics (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,4) NOT NULL,
    metric_unit VARCHAR(20),
    tags JSONB DEFAULT '{}',
    collected_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_system_metrics_name_time (metric_name, collected_at),
    INDEX idx_system_metrics_tags_gin USING GIN (tags)
);

-- 关键业务指标收集函数
CREATE OR REPLACE FUNCTION collect_business_metrics()
RETURNS TABLE(metric_name TEXT, metric_value DECIMAL, metric_unit TEXT) AS $$
BEGIN
    RETURN QUERY
    WITH metrics AS (
        -- 活跃用户数
        SELECT 'active_users_24h'::TEXT, COUNT(DISTINCT user_id)::DECIMAL, 'count'::TEXT
        FROM user_sessions 
        WHERE last_activity > CURRENT_TIMESTAMP - INTERVAL '24 hours'
        
        UNION ALL
        
        -- 进行中的考试数量
        SELECT 'active_exams', COUNT(*)::DECIMAL, 'count'
        FROM exam_attempts 
        WHERE status = 'in_progress'
        
        UNION ALL
        
        -- 系统平均响应时间（从应用日志中获取）
        SELECT 'avg_response_time_ms', AVG(response_time)::DECIMAL, 'milliseconds'
        FROM (
            SELECT EXTRACT(EPOCH FROM (updated_at - created_at)) * 1000 as response_time
            FROM exam_attempts 
            WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour'
            LIMIT 1000
        ) recent_responses
        
        UNION ALL
        
        -- 数据库连接数
        SELECT 'database_connections', COUNT(*)::DECIMAL, 'count'
        FROM pg_stat_activity
        WHERE state = 'active'
    )
    SELECT * FROM metrics;
END;
$$ LANGUAGE plpgsql;
```

### 7.2 定期维护任务

#### 7.2.1 数据清理任务

```sql
-- 清理过期会话
CREATE OR REPLACE FUNCTION cleanup_expired_data()
RETURNS TEXT AS $$
DECLARE
    deleted_sessions INTEGER;
    deleted_logs INTEGER;
    result_text TEXT;
BEGIN
    -- 清理过期用户会话
    DELETE FROM user_sessions 
    WHERE expires_at < CURRENT_TIMESTAMP 
       OR (logout_at IS NOT NULL AND logout_at < CURRENT_TIMESTAMP - INTERVAL '7 days');
    GET DIAGNOSTICS deleted_sessions = ROW_COUNT;
    
    -- 清理旧的监考日志（保留3个月）
    DELETE FROM proctor_logs 
    WHERE occurred_at < CURRENT_TIMESTAMP - INTERVAL '3 months';
    GET DIAGNOSTICS deleted_logs = ROW_COUNT;
    
    -- 更新统计信息
    ANALYZE user_sessions;
    ANALYZE proctor_logs;
    
    result_text := format('Cleanup completed: %s sessions, %s proctor logs deleted', 
                         deleted_sessions, deleted_logs);
    
    -- 记录清理日志
    INSERT INTO system_metrics (metric_name, metric_value, metric_unit, tags)
    VALUES 
        ('cleanup_sessions_deleted', deleted_sessions, 'count', '{"task": "cleanup"}'),
        ('cleanup_logs_deleted', deleted_logs, 'count', '{"task": "cleanup"}');
    
    RETURN result_text;
END;
$$ LANGUAGE plpgsql;
```

#### 7.2.2 统计信息更新

```sql
-- 自动更新物化视图
CREATE OR REPLACE FUNCTION refresh_materialized_views()
RETURNS TEXT AS $$
DECLARE
    view_record RECORD;
    result_text TEXT := '';
BEGIN
    -- 刷新所有物化视图
    FOR view_record IN 
        SELECT schemaname, matviewname 
        FROM pg_matviews 
        WHERE schemaname = 'public'
    LOOP
        EXECUTE 'REFRESH MATERIALIZED VIEW CONCURRENTLY ' || view_record.schemaname || '.' || view_record.matviewname;
        result_text := result_text || view_record.matviewname || ', ';
    END LOOP;
    
    RETURN 'Refreshed materialized views: ' || result_text;
END;
$$ LANGUAGE plpgsql;

-- 定时任务配置（使用pg_cron扩展）
-- 每小时刷新统计视图
SELECT cron.schedule('refresh-stats', '0 * * * *', 'SELECT refresh_materialized_views();');

-- 每天凌晨2点执行数据清理
SELECT cron.schedule('cleanup-data', '0 2 * * *', 'SELECT cleanup_expired_data();');

-- 每天凌晨3点执行表统计信息更新
SELECT cron.schedule('analyze-tables', '0 3 * * *', 'SELECT refresh_table_statistics();');
```

---

## 8. 数据库连接配置

### 8.1 连接池配置示例

```yaml
# PostgreSQL连接池配置（使用PgBouncer）
[databases]
exam_system = host=postgres-primary.local port=5432 dbname=exam_system
exam_system_read = host=postgres-replica.local port=5432 dbname=exam_system

[pgbouncer]
pool_mode = transaction
max_client_conn = 200
default_pool_size = 25
max_db_connections = 50
max_user_connections = 30

# 应用配置
server_reset_query = DISCARD ALL
server_check_query = SELECT 1
server_check_delay = 30
server_fast_close = 1

# 日志配置
log_connections = 1
log_disconnections = 1
log_pooler_errors = 1
```

### 8.2 应用层连接配置

```python
# Django数据库配置示例
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'exam_system',
        'USER': 'exam_user',
        'PASSWORD': os.environ.get('DATABASE_PASSWORD'),
        'HOST': 'postgres-primary.local',
        'PORT': '5432',
        'OPTIONS': {
            'sslmode': 'require',
            'application_name': 'exam_system_app',
            'connect_timeout': 10,
            'options': '-c default_transaction_isolation=read_committed'
        },
        'CONN_MAX_AGE': 300,  # 5分钟连接复用
        'CONN_HEALTH_CHECKS': True,
    },
    'read_replica': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'exam_system',
        'USER': 'exam_readonly',
        'PASSWORD': os.environ.get('DATABASE_READONLY_PASSWORD'),
        'HOST': 'postgres-replica.local',
        'PORT': '5432',
        'OPTIONS': {
            'sslmode': 'require',
            'application_name': 'exam_system_readonly',
        },
    }
}

# 数据库路由配置
DATABASE_ROUTERS = ['exam_system.routers.DatabaseRouter']
```

---

## 9. 故障排查指南

### 9.1 常见性能问题

#### 9.1.1 慢查询诊断

```sql
-- 查看当前执行的慢查询
SELECT 
    pid,
    now() - pg_stat_activity.query_start AS duration,
    query,
    state,
    wait_event_type,
    wait_event
FROM pg_stat_activity 
WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes'
  AND state = 'active';

-- 查看最消耗资源的查询
SELECT 
    query,
    calls,
    total_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent,
    mean_time
FROM pg_stat_statements 
ORDER BY total_time DESC 
LIMIT 20;
```

#### 9.1.2 锁等待分析

```sql
-- 查看锁等待情况
SELECT DISTINCT
    pl.pid,
    psa.usename,
    psa.query_start,
    psa.state,
    psa.query,
    pl.locktype,
    pl.mode,
    pl.granted
FROM pg_stat_activity psa
JOIN pg_locks pl ON pl.pid = psa.pid
WHERE NOT pl.granted
ORDER BY psa.query_start;

-- 查看阻塞关系
WITH RECURSIVE blocking_tree AS (
    SELECT 
        blocked_locks.pid AS blocked_pid,
        blocked_activity.usename AS blocked_user,
        blocking_locks.pid AS blocking_pid,
        blocking_activity.usename AS blocking_user,
        blocked_activity.query AS blocked_statement,
        blocking_activity.query AS blocking_statement,
        1 AS level
    FROM pg_catalog.pg_locks blocked_locks
    JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
    JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
        AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
        AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
    WHERE NOT blocked_locks.granted AND blocking_locks.granted
    
    UNION ALL
    
    SELECT 
        bt.blocked_pid,
        bt.blocked_user,
        blocking_locks.pid,
        blocking_activity.usename,
        bt.blocked_statement,
        blocking_activity.query,
        bt.level + 1
    FROM blocking_tree bt
    JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.pid = bt.blocking_pid
    JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
    WHERE bt.level < 5
)
SELECT * FROM blocking_tree ORDER BY level, blocked_pid;
```

### 9.2 磁盘空间监控

```sql
-- 监控表空间大小
CREATE OR REPLACE FUNCTION check_disk_usage()
RETURNS TABLE(
    database_name TEXT,
    table_schema TEXT,
    table_name TEXT,
    size_pretty TEXT,
    size_bytes BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        current_database()::TEXT,
        schemaname::TEXT,
        tablename::TEXT,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)),
        pg_total_relation_size(schemaname||'.'||tablename)
    FROM pg_tables 
    WHERE schemaname = 'public'
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
END;
$$ LANGUAGE plpgsql;

-- 监控数据库整体大小
SELECT 
    datname,
    pg_size_pretty(pg_database_size(datname)) as size,
    pg_database_size(datname) as size_bytes
FROM pg_database 
WHERE datname = current_database();
```

---

## 10. 总结

本数据库架构文档详细描述了在线考试系统的完整数据结构设计，包括：

### 10.1 设计亮点

1. **高性能架构**: 采用分区表、索引优化、读写分离等策略
2. **数据安全**: 字段级加密、行级安全、审计日志
3. **高可用性**: 主从复制、自动故障切换、备份恢复
4. **扩展性**: 分区策略、连接池、缓存设计
5. **监控完善**: 性能监控、业务指标、故障诊断

### 10.2 维护建议

1. **定期维护**: 执行统计信息更新、索引重建、数据清理
2. **监控告警**: 设置关键指标阈值，及时发现问题
3. **备份验证**: 定期验证备份可用性和恢复流程
4. **性能调优**: 根据业务增长调整配置参数
5. **安全审计**: 定期检查权限配置和访问日志

### 10.3 扩展方向

1. **分片扩展**: 当单机无法满足需求时的水平分片方案
2. **多租户**: 支持SaaS模式的多租户架构优化
3. **实时分析**: 基于流处理的实时数据分析
4. **AI集成**: 机器学习模型的数据存储和训练支持

本架构设计能够支撑10K+并发用户的在线考试系统，并为未来业务发展提供良好的扩展基础。

---

**文档版本**: v1.0.0  
**最后更新**: 2024-01-15  
**维护团队**: 数据库架构组

如有疑问，请联系技术支持团队。