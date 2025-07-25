-- 在线考试系统数据库初始化脚本
-- 基于系统设计文档创建数据库结构

SET NAMES utf8mb4;
SET time_zone = '+08:00';

-- ====================
-- 创建数据库和用户
-- ====================

CREATE DATABASE IF NOT EXISTS exam_system 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE exam_system;

-- 创建应用用户
CREATE USER IF NOT EXISTS 'exam_user'@'%' IDENTIFIED BY 'exam_password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX ON exam_system.* TO 'exam_user'@'%';

-- 创建只读用户（用于报表和分析）
CREATE USER IF NOT EXISTS 'exam_readonly'@'%' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON exam_system.* TO 'exam_readonly'@'%';

FLUSH PRIVILEGES;

-- ====================
-- 用户体系表
-- ====================

-- 用户基本信息表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    first_name VARCHAR(50) COMMENT '姓',
    last_name VARCHAR(50) COMMENT '名',
    phone VARCHAR(20) COMMENT '手机号',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    profile JSON COMMENT '用户资料',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    is_verified BOOLEAN DEFAULT FALSE COMMENT '是否验证',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login TIMESTAMP NULL COMMENT '最后登录时间',
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
) COMMENT '用户基本信息表';

-- 用户角色表
CREATE TABLE user_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '角色(student/teacher/admin/super_admin)',
    organization_id BIGINT COMMENT '所属机构ID',
    permissions JSON COMMENT '权限配置',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_role (role),
    INDEX idx_organization (organization_id)
) COMMENT '用户角色表';

-- 用户会话表
CREATE TABLE user_sessions (
    session_id VARCHAR(128) PRIMARY KEY COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    device_type VARCHAR(50) COMMENT '设备类型',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    metadata JSON COMMENT '会话元数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动时间',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_last_activity (last_activity)
) COMMENT '用户会话表';

-- ====================
-- 组织机构表
-- ====================

-- 组织机构表
CREATE TABLE organizations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '机构名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '机构代码',
    type VARCHAR(50) NOT NULL COMMENT '机构类型',
    parent_id BIGINT COMMENT '父机构ID',
    settings JSON COMMENT '机构配置',
    branding JSON COMMENT '品牌设置',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (parent_id) REFERENCES organizations(id),
    INDEX idx_code (code),
    INDEX idx_type (type),
    INDEX idx_parent_id (parent_id),
    INDEX idx_is_active (is_active)
) COMMENT '组织机构表';

-- ====================
-- 课程体系表
-- ====================

-- 课程分类表
CREATE TABLE course_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类代码',
    parent_id BIGINT COMMENT '父分类ID',
    description TEXT COMMENT '分类描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    
    FOREIGN KEY (parent_id) REFERENCES course_categories(id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order)
) COMMENT '课程分类表';

-- 课程表
CREATE TABLE courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '课程标题',
    description TEXT COMMENT '课程描述',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '课程代码',
    instructor_id BIGINT NOT NULL COMMENT '讲师ID',
    organization_id BIGINT NOT NULL COMMENT '机构ID',
    category_id BIGINT COMMENT '分类ID',
    status ENUM('draft', 'active', 'archived') DEFAULT 'draft' COMMENT '状态',
    metadata JSON COMMENT '课程元数据',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (instructor_id) REFERENCES users(id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (category_id) REFERENCES course_categories(id),
    INDEX idx_instructor_id (instructor_id),
    INDEX idx_organization_id (organization_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_start_date (start_date)
) COMMENT '课程表';

-- 课程注册表
CREATE TABLE enrollments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    status ENUM('enrolled', 'completed', 'dropped') DEFAULT 'enrolled' COMMENT '状态',
    progress DECIMAL(5,2) DEFAULT 0.00 COMMENT '学习进度',
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_course (user_id, course_id),
    INDEX idx_user_id (user_id),
    INDEX idx_course_id (course_id),
    INDEX idx_status (status)
) COMMENT '课程注册表';

-- ====================
-- 题库体系表
-- ====================

-- 题目分类表
CREATE TABLE question_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类代码',
    parent_id BIGINT COMMENT '父分类ID',
    description TEXT COMMENT '分类描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    
    FOREIGN KEY (parent_id) REFERENCES question_categories(id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order)
) COMMENT '题目分类表';

-- 题目表
CREATE TABLE questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL COMMENT '题目标题',
    content TEXT NOT NULL COMMENT '题目内容',
    type ENUM('single_choice', 'multiple_choice', 'true_false', 'fill_blank', 'essay', 'coding') NOT NULL COMMENT '题型',
    difficulty ENUM('easy', 'medium', 'hard') DEFAULT 'medium' COMMENT '难度',
    default_points DECIMAL(6,2) DEFAULT 1.00 COMMENT '默认分值',
    options JSON COMMENT '选项配置',
    correct_answer JSON COMMENT '正确答案',
    explanation TEXT COMMENT '解析',
    metadata JSON COMMENT '题目元数据',
    media_type VARCHAR(20) COMMENT '媒体类型',
    media_url VARCHAR(500) COMMENT '媒体URL',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    category_id BIGINT COMMENT '分类ID',
    status ENUM('draft', 'active', 'archived') DEFAULT 'draft' COMMENT '状态',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    avg_score DECIMAL(5,2) COMMENT '平均得分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES question_categories(id),
    INDEX idx_created_by (created_by),
    INDEX idx_category_id (category_id),
    INDEX idx_type (type),
    INDEX idx_difficulty (difficulty),
    INDEX idx_status (status),
    INDEX idx_usage_count (usage_count),
    FULLTEXT idx_content_search (title, content, explanation)
) COMMENT '题目表';

-- 题目标签表
CREATE TABLE question_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL COMMENT '题目ID',
    tag VARCHAR(50) NOT NULL COMMENT '标签名',
    tag_type VARCHAR(20) DEFAULT 'general' COMMENT '标签类型',
    
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    INDEX idx_question_id (question_id),
    INDEX idx_tag (tag),
    INDEX idx_tag_type (tag_type)
) COMMENT '题目标签表';

-- ====================
-- 考试体系表
-- ====================

-- 考试表
CREATE TABLE exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '考试标题',
    description TEXT COMMENT '考试描述',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '考试代码',
    course_id BIGINT COMMENT '课程ID',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    type ENUM('quiz', 'midterm', 'final', 'certification') DEFAULT 'quiz' COMMENT '类型',
    status ENUM('draft', 'published', 'archived') DEFAULT 'draft' COMMENT '状态',
    duration_minutes INT NOT NULL COMMENT '时长(分钟)',
    max_attempts INT DEFAULT 1 COMMENT '最大尝试次数',
    passing_score DECIMAL(5,2) DEFAULT 60.00 COMMENT '及格分数',
    start_time TIMESTAMP NULL COMMENT '开始时间',
    end_time TIMESTAMP NULL COMMENT '结束时间',
    settings JSON COMMENT '考试设置',
    proctoring_config JSON COMMENT '监考配置',
    shuffle_questions BOOLEAN DEFAULT FALSE COMMENT '随机排序题目',
    shuffle_options BOOLEAN DEFAULT FALSE COMMENT '随机排序选项',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    published_at TIMESTAMP NULL COMMENT '发布时间',
    
    FOREIGN KEY (course_id) REFERENCES courses(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_course_id (course_id),
    INDEX idx_created_by (created_by),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) COMMENT '考试表';

-- 考试规则表
CREATE TABLE exam_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型',
    rule_config JSON NOT NULL COMMENT '规则配置',
    priority INT DEFAULT 0 COMMENT '优先级',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    INDEX idx_exam_id (exam_id),
    INDEX idx_rule_type (rule_type),
    INDEX idx_priority (priority)
) COMMENT '考试规则表';

-- 考试题目关联表
CREATE TABLE exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    order_number INT NOT NULL COMMENT '题目顺序',
    points DECIMAL(6,2) NOT NULL COMMENT '分值',
    custom_config JSON COMMENT '自定义配置',
    is_required BOOLEAN DEFAULT TRUE COMMENT '是否必答',
    
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE RESTRICT,
    UNIQUE KEY uk_exam_question_order (exam_id, order_number),
    INDEX idx_exam_id (exam_id),
    INDEX idx_question_id (question_id)
) COMMENT '考试题目关联表';

-- ====================
-- 考试记录表
-- ====================

-- 考试记录表
CREATE TABLE exam_attempts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attempt_code VARCHAR(64) NOT NULL UNIQUE COMMENT '考试记录代码',
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status ENUM('in_progress', 'submitted', 'graded', 'cancelled') DEFAULT 'in_progress' COMMENT '状态',
    score DECIMAL(6,2) COMMENT '得分',
    percentage DECIMAL(5,2) COMMENT '得分率',
    grade VARCHAR(10) COMMENT '等级',
    answers_summary JSON COMMENT '答案摘要',
    time_tracking JSON COMMENT '时间统计',
    browser_info JSON COMMENT '浏览器信息',
    ip_address INET COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    submitted_at TIMESTAMP NULL COMMENT '提交时间',
    graded_at TIMESTAMP NULL COMMENT '评分时间',
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动',
    remaining_time INT COMMENT '剩余时间(秒)',
    security_log JSON COMMENT '安全日志',
    
    FOREIGN KEY (exam_id) REFERENCES exams(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at),
    INDEX idx_submitted_at (submitted_at),
    INDEX idx_score (score)
) COMMENT '考试记录表';

-- 答案表
CREATE TABLE answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL COMMENT '考试记录ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    user_answer JSON COMMENT '用户答案',
    correct_answer JSON COMMENT '正确答案',
    points_earned DECIMAL(6,2) DEFAULT 0.00 COMMENT '获得分数',
    points_possible DECIMAL(6,2) NOT NULL COMMENT '可能分数',
    status ENUM('correct', 'incorrect', 'partial', 'ungraded') DEFAULT 'ungraded' COMMENT '状态',
    feedback TEXT COMMENT '反馈',
    time_spent INT DEFAULT 0 COMMENT '用时(秒)',
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '回答时间',
    graded_at TIMESTAMP NULL COMMENT '评分时间',
    metadata JSON COMMENT '答案元数据',
    
    FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id),
    UNIQUE KEY uk_attempt_question (attempt_id, question_id),
    INDEX idx_attempt_id (attempt_id),
    INDEX idx_question_id (question_id),
    INDEX idx_status (status),
    INDEX idx_points_earned (points_earned)
) COMMENT '答案表';

-- ====================
-- 监考记录表
-- ====================

-- 监考日志表
CREATE TABLE proctor_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL COMMENT '考试记录ID',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
    severity ENUM('info', 'warning', 'error', 'critical') DEFAULT 'info' COMMENT '严重程度',
    event_data JSON COMMENT '事件数据',
    media_url VARCHAR(500) COMMENT '媒体URL',
    ai_analysis JSON COMMENT 'AI分析结果',
    confidence_score DECIMAL(5,4) COMMENT '置信度',
    requires_review BOOLEAN DEFAULT FALSE COMMENT '需要人工审核',
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    reviewer_id BIGINT COMMENT '审核人ID',
    reviewer_notes TEXT COMMENT '审核备注',
    
    FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    INDEX idx_attempt_id (attempt_id),
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_occurred_at (occurred_at),
    INDEX idx_requires_review (requires_review)
) COMMENT '监考日志表';

-- ====================
-- 统计分析表
-- ====================

-- 考试统计表
CREATE TABLE exam_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    stat_type VARCHAR(50) NOT NULL COMMENT '统计类型',
    stat_data JSON NOT NULL COMMENT '统计数据',
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    stat_date DATE NOT NULL COMMENT '统计日期',
    
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    UNIQUE KEY uk_exam_stat_date (exam_id, stat_type, stat_date),
    INDEX idx_exam_id (exam_id),
    INDEX idx_stat_type (stat_type),
    INDEX idx_stat_date (stat_date)
) COMMENT '考试统计表';

-- 用户表现表
CREATE TABLE user_performance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    exam_id BIGINT COMMENT '考试ID',
    course_id BIGINT COMMENT '课程ID',
    performance_data JSON COMMENT '表现数据',
    overall_score DECIMAL(6,2) COMMENT '总体得分',
    rank_position INT COMMENT '排名',
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_course_id (course_id),
    INDEX idx_overall_score (overall_score)
) COMMENT '用户表现表';

-- ====================
-- 系统配置表
-- ====================

-- 系统配置表
CREATE TABLE system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'string' COMMENT '配置类型',
    description TEXT COMMENT '配置描述',
    is_encrypted BOOLEAN DEFAULT FALSE COMMENT '是否加密',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_config_key (config_key)
) COMMENT '系统配置表';

-- ====================
-- 初始化数据
-- ====================

-- 插入默认系统配置
INSERT INTO system_configs (config_key, config_value, config_type, description) VALUES
('system.name', '在线考试系统', 'string', '系统名称'),
('system.version', '1.0.0', 'string', '系统版本'),
('exam.default_duration', '120', 'integer', '默认考试时长(分钟)'),
('exam.max_attempts', '3', 'integer', '默认最大尝试次数'),
('exam.passing_score', '60.00', 'decimal', '默认及格分数'),
('security.jwt_expires_in', '3600', 'integer', 'JWT过期时间(秒)'),
('security.max_login_attempts', '5', 'integer', '最大登录尝试次数'),
('upload.max_file_size', '104857600', 'integer', '最大文件上传大小(字节)'),
('notification.email_enabled', 'true', 'boolean', '是否启用邮件通知'),
('monitoring.metrics_enabled', 'true', 'boolean', '是否启用监控指标');

-- 插入默认题目分类
INSERT INTO question_categories (name, code, description, sort_order) VALUES
('数学', 'MATH', '数学相关题目', 1),
('语文', 'CHINESE', '语文相关题目', 2),
('英语', 'ENGLISH', '英语相关题目', 3),
('计算机', 'COMPUTER', '计算机相关题目', 4),
('综合', 'GENERAL', '综合类题目', 5);

-- 插入默认课程分类
INSERT INTO course_categories (name, code, description, sort_order) VALUES
('基础教育', 'BASIC', '基础教育课程', 1),
('职业教育', 'VOCATIONAL', '职业教育课程', 2),
('高等教育', 'HIGHER', '高等教育课程', 3),
('在线培训', 'TRAINING', '在线培训课程', 4);

-- 创建索引优化
-- 为分区做准备的索引
ALTER TABLE exam_attempts ADD INDEX idx_created_at_month (DATE_FORMAT(started_at, '%Y-%m'));
ALTER TABLE proctor_logs ADD INDEX idx_occurred_at_month (DATE_FORMAT(occurred_at, '%Y-%m'));

-- 复合索引优化
CREATE INDEX idx_users_active_created ON users(is_active, created_at);
CREATE INDEX idx_exams_status_start ON exams(status, start_time);
CREATE INDEX idx_attempts_user_exam_status ON exam_attempts(user_id, exam_id, status);

COMMIT;