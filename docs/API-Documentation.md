# 在线考试系统 API 文档

## 文档概述

本文档提供在线考试系统完整的 API 接口说明，包含所有微服务的端点定义、请求响应格式、认证方式和错误处理规范。

### API 版本
- **当前版本**: v1
- **基础URL**: `https://api.exam-system.com/api/v1`
- **协议**: HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8

### 认证方式
```http
Authorization: Bearer {access_token}
```

### 通用响应格式
```json
{
  "success": true,
  "data": {...},
  "message": "操作成功",
  "code": 200,
  "timestamp": "2024-01-15T10:30:00Z",
  "request_id": "req_123456789"
}
```

### 错误响应格式
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "请求参数验证失败",
    "details": [
      {
        "field": "email",
        "message": "邮箱格式不正确"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "request_id": "req_123456789"
}
```

---

## 1. 认证服务 API (Auth Service)

**Base URL**: `/api/v1/auth`

### 1.1 用户登录

**Endpoint**: `POST /login`

**描述**: 用户登录验证，返回访问令牌

**请求头**:
```http
Content-Type: application/json
```

**请求体**:
```json
{
  "username": "student001",
  "password": "securePassword123",
  "captcha_token": "captcha_abc123",
  "device_info": {
    "device_type": "desktop",
    "browser": "Chrome 120.0",
    "os": "Windows 11",
    "screen_resolution": "1920x1080"
  },
  "remember_me": false
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "rt_abc123def456...",
    "expires_in": 3600,
    "token_type": "Bearer",
    "user_profile": {
      "user_id": "usr_123456",
      "username": "student001",
      "email": "student@example.com",
      "first_name": "张",
      "last_name": "三",
      "avatar_url": "https://cdn.exam-system.com/avatars/usr_123456.jpg"
    },
    "roles": ["student"],
    "permissions": [
      "exam:take",
      "result:view",
      "profile:edit"
    ],
    "organization": {
      "id": "org_001",
      "name": "示例大学",
      "type": "university"
    }
  },
  "message": "登录成功"
}
```

**错误响应**:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "用户名或密码错误",
    "details": {
      "attempts_remaining": 2,
      "lockout_time": null
    }
  }
}
```

### 1.2 刷新令牌

**Endpoint**: `POST /refresh`

**请求体**:
```json
{
  "refresh_token": "rt_abc123def456..."
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 3600,
    "token_type": "Bearer"
  }
}
```

### 1.3 用户登出

**Endpoint**: `POST /logout`

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "message": "登出成功"
}
```

### 1.4 多因子认证验证

**Endpoint**: `POST /verify-mfa`

**请求体**:
```json
{
  "user_id": "usr_123456",
  "mfa_code": "123456",
  "mfa_type": "sms"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "verified": true,
    "session_token": "mfa_session_abc123"
  }
}
```

### 1.5 获取用户资料

**Endpoint**: `GET /profile`

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "user_id": "usr_123456",
    "username": "student001",
    "email": "student@example.com",
    "first_name": "张",
    "last_name": "三",
    "phone": "138****1234",
    "avatar_url": "https://cdn.exam-system.com/avatars/usr_123456.jpg",
    "profile": {
      "student_id": "2024001001",
      "major": "计算机科学与技术",
      "grade": "大三",
      "preferred_language": "zh-CN"
    },
    "roles": ["student"],
    "permissions": ["exam:take", "result:view"],
    "created_at": "2023-09-01T08:00:00Z",
    "last_login": "2024-01-15T10:30:00Z"
  }
}
```

---

## 2. 考试服务 API (Exam Service)

**Base URL**: `/api/v1/exams`

### 2.1 获取考试列表

**Endpoint**: `GET /`

**查询参数**:
```
course_id: string (可选) - 课程ID
status: string (可选) - 考试状态 (draft|published|archived)
type: string (可选) - 考试类型 (quiz|midterm|final|certification)
page: number (可选, 默认: 1) - 页码
limit: number (可选, 默认: 20) - 每页数量
search: string (可选) - 搜索关键词
```

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "exams": [
      {
        "id": "exam_001",
        "title": "数据结构期中考试",
        "description": "涵盖栈、队列、树等基础数据结构",
        "code": "DS2024_MIDTERM",
        "type": "midterm",
        "status": "published",
        "duration_minutes": 120,
        "max_attempts": 1,
        "passing_score": 60.0,
        "start_time": "2024-01-20T09:00:00Z",
        "end_time": "2024-01-20T11:00:00Z",
        "questions_count": 50,
        "total_points": 100.0,
        "course": {
          "id": "course_001",
          "title": "数据结构与算法",
          "code": "CS201"
        },
        "instructor": {
          "id": "usr_teacher001",
          "name": "李教授"
        },
        "attempt_status": {
          "attempted": false,
          "attempts_used": 0,
          "best_score": null,
          "can_attempt": true
        },
        "created_at": "2024-01-10T14:30:00Z",
        "published_at": "2024-01-15T09:00:00Z"
      }
    ],
    "pagination": {
      "current_page": 1,
      "per_page": 20,
      "total_items": 45,
      "total_pages": 3,
      "has_next": true,
      "has_previous": false
    },
    "filters": {
      "available_courses": [
        {"id": "course_001", "title": "数据结构与算法"},
        {"id": "course_002", "title": "操作系统原理"}
      ],
      "available_types": ["quiz", "midterm", "final"],
      "available_statuses": ["published", "archived"]
    }
  }
}
```

### 2.2 创建考试

**Endpoint**: `POST /`

**权限要求**: `exam:create`

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "title": "算法分析期末考试",
  "description": "包含时间复杂度分析、动态规划等内容",
  "course_id": "course_001",
  "type": "final",
  "duration_minutes": 180,
  "max_attempts": 1,
  "passing_score": 70.0,
  "start_time": "2024-01-25T14:00:00Z",
  "end_time": "2024-01-25T17:00:00Z",
  "settings": {
    "shuffle_questions": true,
    "shuffle_options": true,
    "show_results_immediately": false,
    "allow_review": true,
    "proctoring_enabled": true,
    "time_limit_warning": [30, 10, 5]
  },
  "proctoring_config": {
    "camera_required": true,
    "screen_sharing_required": true,
    "face_detection": true,
    "multiple_person_detection": true,
    "tab_switching_detection": true,
    "recording_enabled": true
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "exam_id": "exam_002",
    "code": "ALG2024_FINAL_001",
    "status": "draft",
    "created_at": "2024-01-15T11:00:00Z"
  },
  "message": "考试创建成功"
}
```

### 2.3 获取考试详情

**Endpoint**: `GET /{exam_id}`

**路径参数**:
- `exam_id`: 考试ID

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "exam": {
      "id": "exam_001",
      "title": "数据结构期中考试",
      "description": "涵盖栈、队列、树等基础数据结构",
      "code": "DS2024_MIDTERM",
      "type": "midterm",
      "status": "published",
      "duration_minutes": 120,
      "max_attempts": 1,
      "passing_score": 60.0,
      "start_time": "2024-01-20T09:00:00Z",
      "end_time": "2024-01-20T11:00:00Z",
      "settings": {
        "shuffle_questions": true,
        "shuffle_options": true,
        "show_results_immediately": false,
        "allow_review": true,
        "proctoring_enabled": true
      },
      "proctoring_config": {
        "camera_required": true,
        "screen_sharing_required": true,
        "face_detection": true
      },
      "course": {
        "id": "course_001",
        "title": "数据结构与算法",
        "code": "CS201"
      },
      "instructor": {
        "id": "usr_teacher001",
        "name": "李教授",
        "email": "li.prof@university.edu"
      }
    },
    "questions_count": 50,
    "total_points": 100.0,
    "attempt_history": [
      {
        "attempt_id": "attempt_001",
        "started_at": "2024-01-20T09:05:00Z",
        "submitted_at": "2024-01-20T10:45:00Z",
        "status": "graded",
        "score": 85.5,
        "percentage": 85.5,
        "grade": "B+",
        "time_spent_minutes": 100
      }
    ],
    "statistics": {
      "total_attempts": 156,
      "average_score": 78.2,
      "pass_rate": 89.1,
      "completion_rate": 94.2
    },
    "user_status": {
      "can_attempt": false,
      "attempts_used": 1,
      "remaining_attempts": 0,
      "last_attempt": {
        "attempt_id": "attempt_001",
        "score": 85.5,
        "status": "graded"
      }
    }
  }
}
```

### 2.4 开始考试

**Endpoint**: `POST /{exam_id}/start`

**路径参数**:
- `exam_id`: 考试ID

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "browser_info": {
    "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "viewport": "1920x1080",
    "timezone": "Asia/Shanghai",
    "language": "zh-CN"
  },
  "device_info": {
    "platform": "Windows",
    "screen_resolution": "1920x1080",
    "available_memory": 8192
  },
  "proctoring_consent": true
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "attempt_id": "attempt_002",
    "attempt_code": "ATT_DS2024_002_20240120",
    "session_token": "session_abc123def456",
    "remaining_time": 7200,
    "start_time": "2024-01-20T09:05:00Z",
    "end_time": "2024-01-20T11:05:00Z",
    "questions": [
      {
        "id": "q_001",
        "order_number": 1,
        "type": "single_choice",
        "title": "以下哪种数据结构遵循LIFO原则？",
        "content": "选择正确的数据结构类型",
        "points": 2.0,
        "options": [
          {"id": "opt_a", "text": "队列", "order": 1},
          {"id": "opt_b", "text": "栈", "order": 2},
          {"id": "opt_c", "text": "链表", "order": 3},
          {"id": "opt_d", "text": "数组", "order": 4}
        ],
        "media_url": null,
        "is_required": true
      }
    ],
    "proctoring_config": {
      "session_id": "proctor_session_001",
      "webrtc_config": {
        "ice_servers": [
          {"urls": "stun:stun.exam-system.com:3478"},
          {"urls": "turn:turn.exam-system.com:3478", "username": "exam", "credential": "secret"}
        ]
      },
      "monitoring_rules": [
        {"type": "face_detection", "enabled": true, "threshold": 0.8},
        {"type": "multiple_person", "enabled": true, "max_persons": 1},
        {"type": "tab_switching", "enabled": true, "max_switches": 3}
      ]
    }
  },
  "message": "考试已开始"
}
```

### 2.5 提交答案

**Endpoint**: `POST /{exam_id}/questions/{question_id}/answer`

**路径参数**:
- `exam_id`: 考试ID
- `question_id`: 题目ID

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "attempt_id": "attempt_002",
  "session_token": "session_abc123def456",
  "answer": {
    "selected_options": ["opt_b"],
    "text_answer": null,
    "confidence_level": 4
  },
  "time_spent": 45,
  "answer_timestamp": "2024-01-20T09:08:30Z"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "saved": true,
    "answer_id": "ans_001",
    "auto_saved": true,
    "next_question_id": "q_002",
    "progress": {
      "answered": 1,
      "total": 50,
      "percentage": 2.0
    },
    "remaining_time": 7155
  },
  "message": "答案已保存"
}
```

### 2.6 提交考试

**Endpoint**: `POST /{exam_id}/submit`

**路径参数**:
- `exam_id`: 考试ID

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "attempt_id": "attempt_002",
  "session_token": "session_abc123def456",
  "submit_type": "manual",
  "confirmation": true
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "submitted": true,
    "submission_time": "2024-01-20T10:45:00Z",
    "time_spent_minutes": 100,
    "answers_submitted": 50,
    "auto_grading_status": "completed",
    "score": 87.0,
    "percentage": 87.0,
    "grade": "B+",
    "passed": true,
    "certificate_eligible": true,
    "certificate_url": "https://certificates.exam-system.com/cert_attempt_002.pdf",
    "result_available_at": "2024-01-20T10:45:00Z"
  },
  "message": "考试提交成功"
}
```

### 2.7 获取考试结果

**Endpoint**: `GET /{exam_id}/results`

**路径参数**:
- `exam_id`: 考试ID

**查询参数**:
```
attempt_id: string (可选) - 特定考试记录ID
include_answers: boolean (可选, 默认: false) - 是否包含详细答案
```

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "attempt": {
      "id": "attempt_002",
      "code": "ATT_DS2024_002_20240120",
      "started_at": "2024-01-20T09:05:00Z",
      "submitted_at": "2024-01-20T10:45:00Z",
      "status": "graded",
      "time_spent_minutes": 100,
      "score": 87.0,
      "percentage": 87.0,
      "grade": "B+",
      "passed": true,
      "rank": 15,
      "total_participants": 156
    },
    "exam": {
      "id": "exam_001",
      "title": "数据结构期中考试",
      "total_points": 100.0,
      "passing_score": 60.0
    },
    "score_breakdown": {
      "by_question_type": {
        "single_choice": {"score": 35.0, "max_score": 40.0, "count": 20},
        "multiple_choice": {"score": 28.0, "max_score": 30.0, "count": 15},
        "true_false": {"score": 18.0, "max_score": 20.0, "count": 10},
        "fill_blank": {"score": 6.0, "max_score": 10.0, "count": 5}
      },
      "by_difficulty": {
        "easy": {"score": 18.0, "max_score": 20.0},
        "medium": {"score": 42.0, "max_score": 50.0},
        "hard": {"score": 27.0, "max_score": 30.0}
      }
    },
    "answers": [
      {
        "question_id": "q_001",
        "question_title": "以下哪种数据结构遵循LIFO原则？",
        "question_type": "single_choice",
        "points_possible": 2.0,
        "points_earned": 2.0,
        "status": "correct",
        "user_answer": ["opt_b"],
        "correct_answer": ["opt_b"],
        "feedback": "正确！栈（Stack）是遵循后进先出（LIFO）原则的数据结构。",
        "time_spent": 45,
        "answered_at": "2024-01-20T09:08:30Z"
      }
    ],
    "feedback": [
      {
        "category": "strengths",
        "message": "在基础数据结构概念方面表现优秀"
      },
      {
        "category": "improvements",
        "message": "建议加强树结构的遍历算法练习"
      }
    ],
    "certificate": {
      "available": true,
      "url": "https://certificates.exam-system.com/cert_attempt_002.pdf",
      "issued_at": "2024-01-20T10:45:00Z"
    },
    "statistics": {
      "class_average": 78.2,
      "your_rank": 15,
      "percentile": 85.2
    }
  }
}
```

---

## 3. 题库服务 API (Question Service)

**Base URL**: `/api/v1/questions`

### 3.1 获取题目列表

**Endpoint**: `GET /`

**查询参数**:
```
category_id: string (可选) - 题目分类ID
type: string (可选) - 题目类型
difficulty: string (可选) - 难度级别
tags: string (可选) - 标签，逗号分隔
search: string (可选) - 搜索关键词
status: string (可选) - 题目状态
page: number (可选, 默认: 1) - 页码
limit: number (可选, 默认: 20) - 每页数量
sort: string (可选) - 排序方式 (created_at|usage_count|avg_score)
order: string (可选) - 排序顺序 (asc|desc)
```

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "questions": [
      {
        "id": "q_001",
        "title": "栈的基本操作",
        "type": "single_choice",
        "difficulty": "medium",
        "default_points": 2.0,
        "usage_count": 45,
        "avg_score": 78.5,
        "status": "active",
        "category": {
          "id": "cat_001",
          "name": "数据结构",
          "code": "DS"
        },
        "tags": ["栈", "LIFO", "基础概念"],
        "created_by": {
          "id": "usr_teacher001",
          "name": "李教授"
        },
        "created_at": "2023-10-15T14:30:00Z",
        "updated_at": "2024-01-10T09:20:00Z"
      }
    ],
    "pagination": {
      "current_page": 1,
      "per_page": 20,
      "total_items": 1250,
      "total_pages": 63,
      "has_next": true,
      "has_previous": false
    },
    "filters": {
      "available_categories": [
        {"id": "cat_001", "name": "数据结构", "count": 345},
        {"id": "cat_002", "name": "算法分析", "count": 289}
      ],
      "available_types": [
        {"value": "single_choice", "label": "单选题", "count": 567},
        {"value": "multiple_choice", "label": "多选题", "count": 234}
      ],
      "available_difficulties": [
        {"value": "easy", "label": "简单", "count": 412},
        {"value": "medium", "label": "中等", "count": 624}
      ]
    }
  }
}
```

### 3.2 创建题目

**Endpoint**: `POST /`

**权限要求**: `question:create`

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "title": "二叉树遍历算法",
  "content": "给定一个二叉树，实现前序遍历算法",
  "type": "coding",
  "difficulty": "hard",
  "points": 10.0,
  "category_id": "cat_002",
  "tags": ["二叉树", "遍历", "递归"],
  "options": {
    "language_options": ["python", "java", "cpp"],
    "time_limit": 1000,
    "memory_limit": 256,
    "test_cases": [
      {
        "input": "[3,9,20,null,null,15,7]",
        "expected_output": "[3,9,20,15,7]",
        "is_sample": true
      }
    ]
  },
  "correct_answer": {
    "type": "code_template",
    "templates": {
      "python": "def preorder_traversal(root):\n    # Your implementation here\n    pass",
      "java": "public List<Integer> preorderTraversal(TreeNode root) {\n    // Your implementation here\n    return new ArrayList<>();\n}"
    }
  },
  "explanation": "前序遍历的顺序是：根节点 -> 左子树 -> 右子树",
  "media_url": "https://cdn.exam-system.com/images/binary_tree_example.png"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "question_id": "q_1251",
    "status": "draft",
    "version": 1,
    "created_at": "2024-01-15T11:30:00Z"
  },
  "message": "题目创建成功"
}
```

### 3.3 获取题目详情

**Endpoint**: `GET /{question_id}`

**路径参数**:
- `question_id`: 题目ID

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "question": {
      "id": "q_001",
      "title": "栈的基本操作",
      "content": "以下哪种数据结构遵循LIFO（后进先出）原则？",
      "type": "single_choice",
      "difficulty": "medium",
      "default_points": 2.0,
      "options": [
        {"id": "opt_a", "text": "队列", "order": 1},
        {"id": "opt_b", "text": "栈", "order": 2},
        {"id": "opt_c", "text": "链表", "order": 3},
        {"id": "opt_d", "text": "数组", "order": 4}
      ],
      "correct_answer": ["opt_b"],
      "explanation": "栈（Stack）是一种遵循后进先出（LIFO）原则的线性数据结构。",
      "media_type": "image",
      "media_url": "https://cdn.exam-system.com/images/stack_lifo.png",
      "status": "active",
      "category": {
        "id": "cat_001",
        "name": "数据结构",
        "code": "DS",
        "path": "计算机科学 > 数据结构"
      },
      "tags": [
        {"name": "栈", "type": "concept"},
        {"name": "LIFO", "type": "keyword"},
        {"name": "基础", "type": "difficulty"}
      ],
      "metadata": {
        "learning_objective": "理解栈的基本概念和特性",
        "cognitive_level": "comprehension",
        "estimated_time": 60
      },
      "created_by": {
        "id": "usr_teacher001",
        "name": "李教授",
        "email": "li.prof@university.edu"
      },
      "created_at": "2023-10-15T14:30:00Z",
      "updated_at": "2024-01-10T09:20:00Z",
      "version": 3
    },
    "statistics": {
      "usage_count": 45,
      "total_attempts": 1245,
      "correct_attempts": 978,
      "accuracy_rate": 78.5,
      "avg_time_spent": 45,
      "difficulty_rating": 2.3,
      "discrimination_index": 0.65
    },
    "usage_history": [
      {
        "exam_id": "exam_001",
        "exam_title": "数据结构期中考试",
        "usage_date": "2024-01-20T09:00:00Z",
        "attempts": 156,
        "accuracy": 82.1
      }
    ],
    "related_questions": [
      {
        "id": "q_002",
        "title": "队列的基本操作",
        "similarity": 0.85
      }
    ]
  }
}
```

### 3.4 更新题目

**Endpoint**: `PUT /{question_id}`

**权限要求**: `question:edit`

**路径参数**:
- `question_id`: 题目ID

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "title": "栈的基本操作（更新版）",
  "content": "以下哪种数据结构遵循LIFO（后进先出）原则？请选择正确答案。",
  "difficulty": "easy",
  "points": 2.0,
  "explanation": "栈（Stack）是一种遵循后进先出（LIFO）原则的线性数据结构。常见操作包括push（入栈）和pop（出栈）。",
  "tags": ["栈", "LIFO", "基础概念", "数据结构"],
  "metadata": {
    "learning_objective": "理解栈的基本概念、特性和操作",
    "cognitive_level": "comprehension",
    "estimated_time": 45
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "updated": true,
    "version": 4,
    "updated_at": "2024-01-15T12:00:00Z",
    "changes": [
      {"field": "title", "old": "栈的基本操作", "new": "栈的基本操作（更新版）"},
      {"field": "difficulty", "old": "medium", "new": "easy"},
      {"field": "explanation", "old": "...", "new": "..."}
    ]
  },
  "message": "题目更新成功"
}
```

### 3.5 批量导入题目

**Endpoint**: `POST /bulk-import`

**权限要求**: `question:bulk_import`

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "format": "json",
  "file_url": "https://uploads.exam-system.com/questions_batch_20240115.json",
  "category_id": "cat_001",
  "options": {
    "skip_duplicates": true,
    "validate_before_import": true,
    "default_status": "draft",
    "assign_to_user": "usr_teacher001"
  },
  "mapping": {
    "title_field": "question_text",
    "type_field": "question_type",
    "options_field": "choices",
    "answer_field": "correct_answer"
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "job_id": "import_job_20240115_001",
    "estimated_time": 300,
    "status": "processing",
    "progress_url": "/api/v1/questions/import-jobs/import_job_20240115_001",
    "total_questions": 150,
    "estimated_completion": "2024-01-15T12:35:00Z"
  },
  "message": "批量导入任务已启动"
}
```

### 3.6 获取题目分类

**Endpoint**: `GET /categories`

**查询参数**:
```
parent_id: string (可选) - 父分类ID
include_count: boolean (可选, 默认: false) - 是否包含题目数量
level: number (可选) - 分类层级
```

**响应**:
```json
{
  "success": true,
  "data": {
    "categories": [
      {
        "id": "cat_001",
        "name": "数据结构",
        "code": "DS",
        "parent_id": null,
        "description": "包含栈、队列、树、图等数据结构题目",
        "sort_order": 1,
        "level": 1,
        "path": "数据结构",
        "question_count": 345,
        "children": [
          {
            "id": "cat_001_01",
            "name": "线性结构",
            "code": "DS_LINEAR",
            "parent_id": "cat_001",
            "level": 2,
            "path": "数据结构 > 线性结构",
            "question_count": 125,
            "children": []
          }
        ],
        "is_active": true,
        "created_at": "2023-09-01T10:00:00Z"
      }
    ],
    "total_categories": 25,
    "max_level": 3
  }
}
```

### 3.7 题目内容验证

**Endpoint**: `POST /validate`

**权限要求**: `question:validate`

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "questions": [
    {
      "title": "栈的基本操作",
      "content": "以下哪种数据结构遵循LIFO原则？",
      "type": "single_choice",
      "options": [
        {"text": "队列"},
        {"text": "栈"},
        {"text": "链表"}
      ],
      "correct_answer": ["栈"]
    }
  ],
  "validation_rules": {
    "check_duplicates": true,
    "validate_answers": true,
    "check_grammar": true,
    "validate_media": true
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "validation_results": [
      {
        "question_index": 0,
        "is_valid": false,
        "errors": [
          {
            "field": "correct_answer",
            "code": "INVALID_ANSWER_FORMAT",
            "message": "正确答案格式不匹配题目类型",
            "suggestion": "单选题的正确答案应该是选项ID数组"
          }
        ],
        "warnings": [
          {
            "field": "content",
            "code": "GRAMMAR_SUGGESTION",
            "message": "建议在问句末尾添加问号"
          }
        ]
      }
    ],
    "summary": {
      "total_questions": 1,
      "valid_questions": 0,
      "questions_with_errors": 1,
      "questions_with_warnings": 1,
      "duplicate_questions": 0
    },
    "validation_time": "2024-01-15T12:15:00Z"
  }
}
```

---

## 4. 监考服务 API (Proctoring Service)

**Base URL**: `/api/v1/proctoring`

### 4.1 启动监考会话

**Endpoint**: `POST /session/start`

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "attempt_id": "attempt_002",
  "proctoring_level": "strict",
  "device_permissions": {
    "camera": true,
    "microphone": true,
    "screen_sharing": true
  },
  "environment_check": {
    "lighting_adequate": true,
    "background_clear": true,
    "noise_level_acceptable": true
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "session_id": "proctor_session_20240120_001",
    "webrtc_config": {
      "ice_servers": [
        {
          "urls": "stun:stun.exam-system.com:3478"
        },
        {
          "urls": "turn:turn.exam-system.com:3478",
          "username": "exam_user",
          "credential": "turn_secret_2024"
        }
      ],
      "ice_transport_policy": "all",
      "bundle_policy": "max-bundle"
    },
    "monitoring_rules": [
      {
        "type": "face_detection",
        "enabled": true,
        "threshold": 0.8,
        "check_interval": 5,
        "max_violations": 3
      },
      {
        "type": "multiple_person_detection",
        "enabled": true,
        "max_persons": 1,
        "check_interval": 10,
        "confidence_threshold": 0.7
      },
      {
        "type": "gaze_tracking",
        "enabled": true,
        "screen_boundary_tolerance": 0.1,
        "warning_threshold": 5
      },
      {
        "type": "audio_analysis",
        "enabled": true,
        "voice_detection": true,
        "background_noise_threshold": 0.3
      },
      {
        "type": "tab_switching_detection",
        "enabled": true,
        "max_switches": 3,
        "warning_before_violation": true
      }
    ],
    "recording_config": {
      "video_enabled": true,
      "audio_enabled": true,
      "screen_recording": true,
      "quality": "high",
      "compression": "h264"
    },
    "ai_analysis_config": {
      "real_time_analysis": true,
      "behavior_modeling": true,
      "anomaly_detection": true,
      "confidence_scoring": true
    }
  },
  "message": "监考会话已启动"
}
```

### 4.2 上报监考事件

**Endpoint**: `POST /session/{session_id}/events`

**路径参数**:
- `session_id`: 监考会话ID

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

**请求体** (multipart/form-data):
```
event_type: face_not_detected
timestamp: 2024-01-20T09:15:30.123Z
severity: warning
data: {
  "detection_confidence": 0.95,
  "duration_seconds": 3.2,
  "screen_coordinates": {"x": 960, "y": 540},
  "additional_context": "用户头部偏离摄像头视野"
}
media_file: [二进制文件数据 - 截图或视频片段]
```

**响应**:
```json
{
  "success": true,
  "data": {
    "event_id": "event_20240120_001",
    "recorded": true,
    "processing_status": "analyzing",
    "ai_analysis_pending": true,
    "action_required": "warning_issued",
    "violation_count": 1,
    "remaining_warnings": 2,
    "risk_score": 0.3,
    "recommendations": [
      {
        "type": "user_notification",
        "message": "请保持面部正对摄像头",
        "display_duration": 5000
      }
    ]
  },
  "message": "监考事件已记录"
}
```

### 4.3 会话心跳

**Endpoint**: `POST /session/{session_id}/heartbeat`

**路径参数**:
- `session_id`: 监考会话ID

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "status": "active",
  "metrics": {
    "connection_quality": "excellent",
    "video_fps": 30,
    "audio_quality": 0.95,
    "bandwidth_usage": "2.5Mbps",
    "cpu_usage": 0.45,
    "memory_usage": 0.32,
    "battery_level": 0.78
  },
  "current_activity": {
    "question_id": "q_005",
    "time_on_question": 120,
    "mouse_activity": true,
    "keyboard_activity": true,
    "tab_focused": true
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "continue": true,
    "session_valid": true,
    "next_heartbeat_in": 30,
    "warnings": [],
    "system_status": {
      "server_load": "normal",
      "ai_analysis_queue": 2,
      "recording_storage": "available"
    },
    "recommendations": [
      {
        "type": "optimization",
        "message": "网络状况良好，监考质量稳定"
      }
    ]
  }
}
```

### 4.4 结束监考会话

**Endpoint**: `POST /session/{session_id}/end`

**路径参数**:
- `session_id`: 监考会话ID

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "end_reason": "exam_completed",
  "final_status": "completed",
  "session_feedback": {
    "technical_issues": false,
    "user_cooperation": "excellent",
    "environment_quality": "good"
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "session_ended": true,
    "end_time": "2024-01-20T10:45:30Z",
    "session_duration": 6030,
    "total_events": 25,
    "violations": 2,
    "final_risk_score": 0.15,
    "recording_info": {
      "video_file_url": "https://recordings.exam-system.com/session_20240120_001_video.mp4",
      "audio_file_url": "https://recordings.exam-system.com/session_20240120_001_audio.m4a",
      "screen_recording_url": "https://recordings.exam-system.com/session_20240120_001_screen.mp4",
      "total_size_mb": 245.7,
      "retention_days": 90
    },
    "report_url": "https://reports.exam-system.com/proctoring/session_20240120_001.pdf",
    "ai_analysis_status": "completed",
    "manual_review_required": false
  },
  "message": "监考会话已结束"
}
```

### 4.5 获取监考报告

**Endpoint**: `GET /reports/{attempt_id}`

**路径参数**:
- `attempt_id`: 考试记录ID

**查询参数**:
```
include_media: boolean (可选, 默认: false) - 是否包含媒体文件链接
detail_level: string (可选, 默认: standard) - 报告详细程度 (summary|standard|detailed)
```

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "report": {
      "attempt_id": "attempt_002",
      "session_id": "proctor_session_20240120_001",
      "student": {
        "id": "usr_123456",
        "name": "张三",
        "student_id": "2024001001"
      },
      "exam": {
        "id": "exam_001",
        "title": "数据结构期中考试",
        "duration_minutes": 120
      },
      "session_summary": {
        "start_time": "2024-01-20T09:05:00Z",
        "end_time": "2024-01-20T10:45:30Z",
        "duration_minutes": 100.5,
        "completion_status": "completed"
      },
      "integrity_score": 0.92,
      "risk_level": "low",
      "overall_assessment": "通过"
    },
    "violations": [
      {
        "id": "violation_001",
        "type": "face_not_detected",
        "severity": "warning",
        "occurred_at": "2024-01-20T09:15:30Z",
        "duration_seconds": 3.2,
        "description": "考生面部短暂离开摄像头视野",
        "evidence_url": "https://evidence.exam-system.com/violation_001_screenshot.jpg",
        "ai_confidence": 0.95,
        "manual_review": {
          "required": false,
          "reviewed": true,
          "reviewer": "AI System",
          "decision": "minor_violation",
          "notes": "短暂离开，属于正常行为范围"
        }
      },
      {
        "id": "violation_002",
        "type": "tab_switching",
        "severity": "medium",
        "occurred_at": "2024-01-20T10:20:15Z",
        "duration_seconds": 5.8,
        "description": "检测到浏览器标签页切换",
        "evidence_url": "https://evidence.exam-system.com/violation_002_screen.mp4",
        "ai_confidence": 0.98,
        "manual_review": {
          "required": true,
          "reviewed": true,
          "reviewer": "张监考员",
          "decision": "acceptable",
          "notes": "学生误操作，立即返回考试页面"
        }
      }
    ],
    "behavior_analysis": {
      "attention_focus": {
        "average_score": 0.88,
        "focused_time_percentage": 95.2,
        "distraction_events": 3
      },
      "body_language": {
        "stress_indicators": "normal",
        "posture_stability": "good",
        "eye_movement_pattern": "normal"
      },
      "interaction_patterns": {
        "typing_rhythm": "consistent",
        "mouse_movement": "natural",
        "answer_timing": "reasonable"
      }
    },
    "technical_metrics": {
      "video_quality": "excellent",
      "audio_quality": "good",
      "connection_stability": "stable",
      "interruptions": 0,
      "data_completeness": 0.99
    },
    "evidence_files": [
      {
        "type": "session_video",
        "url": "https://recordings.exam-system.com/session_20240120_001_video.mp4",
        "size_mb": 145.3,
        "duration_minutes": 100.5
      },
      {
        "type": "screen_recording",
        "url": "https://recordings.exam-system.com/session_20240120_001_screen.mp4",
        "size_mb": 89.2,
        "duration_minutes": 100.5
      },
      {
        "type": "violation_screenshots",
        "count": 8,
        "total_size_mb": 2.1
      }
    ],
    "ai_analysis": {
      "model_version": "proctoring-ai-v2.1.0",
      "analysis_completion": "2024-01-20T11:15:00Z",
      "confidence_score": 0.94,
      "anomaly_detection": {
        "suspicious_behaviors": 0,
        "unusual_patterns": 1,
        "cheating_probability": 0.08
      },
      "behavioral_consistency": {
        "typing_pattern_match": 0.96,
        "timing_pattern_normal": true,
        "stress_level_assessment": "low"
      }
    },
    "recommendations": [
      {
        "category": "exam_validity",
        "recommendation": "考试结果有效，建议正常评分"
      },
      {
        "category": "future_improvements",
        "recommendation": "建议加强考前环境检查说明"
      }
    ]
  }
}
```

### 4.6 人工审核违规

**Endpoint**: `POST /violations/{violation_id}/review`

**权限要求**: `proctoring:review`

**路径参数**:
- `violation_id`: 违规记录ID

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "reviewer_decision": "acceptable",
  "severity_adjustment": "downgrade",
  "notes": "经人工审核，该行为属于正常范围内的误操作，不构成作弊行为。学生在发现错误后立即返回考试页面，未发现不当内容访问。",
  "evidence_review": {
    "video_reviewed": true,
    "screenshot_reviewed": true,
    "context_considered": true
  },
  "follow_up_actions": [
    "none"
  ]
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "review_completed": true,
    "review_id": "review_20240120_001",
    "reviewer": {
      "id": "usr_proctor001",
      "name": "张监考员"
    },
    "reviewed_at": "2024-01-20T14:30:00Z",
    "final_decision": "acceptable",
    "decision_impact": {
      "violation_status": "reviewed_acceptable",
      "integrity_score_change": +0.05,
      "exam_validity": "maintained"
    },
    "audit_trail": {
      "original_ai_decision": "medium_violation",
      "human_override": true,
      "override_reason": "context_analysis",
      "approval_required": false
    }
  },
  "message": "人工审核完成"
}
```

---

## 5. 用户服务 API (User Service)

**Base URL**: `/api/v1/users`

### 5.1 获取用户列表

**Endpoint**: `GET /`

**权限要求**: `user:list`

**查询参数**:
```
role: string (可选) - 用户角色筛选
organization_id: string (可选) - 组织机构筛选
status: string (可选) - 用户状态筛选
search: string (可选) - 搜索关键词
page: number (可选, 默认: 1) - 页码
limit: number (可选, 默认: 20) - 每页数量
sort: string (可选) - 排序字段
order: string (可选) - 排序顺序
```

**请求头**:
```http
Authorization: Bearer {access_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "users": [
      {
        "id": "usr_123456",
        "username": "student001",
        "email": "student@example.com",
        "first_name": "张",
        "last_name": "三",
        "phone": "138****1234",
        "avatar_url": "https://cdn.exam-system.com/avatars/usr_123456.jpg",
        "roles": ["student"],
        "organization": {
          "id": "org_001",
          "name": "示例大学"
        },
        "status": "active",
        "is_verified": true,
        "created_at": "2023-09-01T08:00:00Z",
        "last_login": "2024-01-15T10:30:00Z"
      }
    ],
    "pagination": {
      "current_page": 1,
      "per_page": 20,
      "total_items": 2456,
      "total_pages": 123,
      "has_next": true,
      "has_previous": false
    }
  }
}
```

### 5.2 创建用户

**Endpoint**: `POST /`

**权限要求**: `user:create`

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "username": "newstudent001",
  "email": "newstudent@example.com",
  "password": "SecurePassword123!",
  "first_name": "李",
  "last_name": "四",
  "phone": "139****5678",
  "roles": ["student"],
  "organization_id": "org_001",
  "profile": {
    "student_id": "2024001002",
    "major": "软件工程",
    "grade": "大一",
    "enrollment_year": 2024
  },
  "send_welcome_email": true
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "user_id": "usr_789012",
    "username": "newstudent001",
    "email": "newstudent@example.com",
    "activation_required": true,
    "activation_token": "act_token_abc123",
    "welcome_email_sent": true,
    "created_at": "2024-01-15T13:00:00Z"
  },
  "message": "用户创建成功"
}
```

---

## 6. 通知服务 API (Notification Service)

**Base URL**: `/api/v1/notifications`

### 6.1 发送通知

**Endpoint**: `POST /send`

**权限要求**: `notification:send`

**请求头**:
```http
Authorization: Bearer {access_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "recipients": [
    {
      "user_id": "usr_123456",
      "channels": ["email", "sms", "push"]
    }
  ],
  "template": "exam_reminder",
  "data": {
    "exam_title": "数据结构期中考试",
    "exam_time": "2024-01-20T09:00:00Z",
    "duration_minutes": 120,
    "exam_url": "https://app.exam-system.com/exam/exam_001"
  },
  "scheduling": {
    "send_at": "2024-01-19T20:00:00Z",
    "timezone": "Asia/Shanghai"
  },
  "priority": "high"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "notification_id": "notif_20240115_001",
    "recipients_count": 1,
    "scheduled_at": "2024-01-19T20:00:00Z",
    "estimated_delivery": "2024-01-19T20:01:00Z",
    "channels_used": ["email", "sms", "push"],
    "status": "scheduled"
  },
  "message": "通知已发送"
}
```

---

## 7. 错误代码参考

### 7.1 HTTP 状态码

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功 |
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | 未认证或认证失败 |
| 403 | Forbidden | 权限不足 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突 |
| 422 | Unprocessable Entity | 请求格式正确但语义错误 |
| 429 | Too Many Requests | 请求频率超限 |
| 500 | Internal Server Error | 服务器内部错误 |
| 503 | Service Unavailable | 服务暂时不可用 |

### 7.2 业务错误代码

#### 认证相关错误
```json
{
  "INVALID_CREDENTIALS": "用户名或密码错误",
  "ACCOUNT_LOCKED": "账户已被锁定",
  "TOKEN_EXPIRED": "访问令牌已过期",
  "TOKEN_INVALID": "访问令牌无效",
  "MFA_REQUIRED": "需要多因子认证",
  "MFA_INVALID": "多因子认证码无效"
}
```

#### 考试相关错误
```json
{
  "EXAM_NOT_FOUND": "考试不存在",
  "EXAM_NOT_STARTED": "考试尚未开始",
  "EXAM_ENDED": "考试已结束",
  "EXAM_ALREADY_TAKEN": "已达到最大考试次数",
  "INSUFFICIENT_PERMISSIONS": "权限不足",
  "INVALID_EXAM_STATE": "考试状态无效",
  "ANSWER_SUBMISSION_FAILED": "答案提交失败",
  "SESSION_EXPIRED": "考试会话已过期"
}
```

#### 题目相关错误
```json
{
  "QUESTION_NOT_FOUND": "题目不存在",
  "INVALID_QUESTION_TYPE": "题目类型无效",
  "DUPLICATE_QUESTION": "题目重复",
  "VALIDATION_FAILED": "题目验证失败",
  "IMPORT_FAILED": "批量导入失败"
}
```

#### 监考相关错误
```json
{
  "PROCTORING_SESSION_NOT_FOUND": "监考会话不存在",
  "CAMERA_ACCESS_DENIED": "摄像头访问被拒绝",
  "MICROPHONE_ACCESS_DENIED": "麦克风访问被拒绝",
  "SCREEN_SHARING_FAILED": "屏幕共享失败",
  "RECORDING_FAILED": "录制失败",
  "AI_ANALYSIS_FAILED": "AI分析失败"
}
```

---

## 8. 最佳实践

### 8.1 认证和安全

1. **令牌管理**:
   - 访问令牌有效期为15分钟
   - 使用刷新令牌自动续期
   - 安全存储令牌，避免XSS攻击

2. **请求签名**:
   - 敏感操作使用请求签名
   - 包含时间戳防止重放攻击

3. **权限检查**:
   - 每个请求都进行权限验证
   - 使用最小权限原则

### 8.2 错误处理

1. **统一错误格式**:
   - 使用标准错误响应格式
   - 提供详细的错误信息和建议

2. **错误日志**:
   - 记录所有错误到日志系统
   - 包含请求ID便于追踪

### 8.3 性能优化

1. **缓存策略**:
   - 合理使用HTTP缓存头
   - 缓存频繁访问的数据

2. **分页处理**:
   - 大数据集使用分页
   - 提供合理的默认分页大小

3. **并发控制**:
   - 关键操作使用乐观锁
   - 避免长时间阻塞

### 8.4 API版本管理

1. **版本策略**:
   - URL路径包含版本号
   - 保持向后兼容性

2. **废弃处理**:
   - 提前通知API变更
   - 提供迁移指南

---

## 9. SDK 和工具

### 9.1 官方SDK

- **JavaScript SDK**: `npm install @exam-system/js-sdk`
- **Python SDK**: `pip install exam-system-sdk`
- **Java SDK**: Maven依赖配置

### 9.2 开发工具

- **API测试**: Postman Collection
- **文档生成**: OpenAPI 3.0规范
- **Mock服务**: 本地开发Mock服务器

### 9.3 监控和调试

- **API监控**: 实时性能指标
- **错误追踪**: 分布式链路追踪
- **调试工具**: 请求/响应日志

---

本文档将持续更新，请关注版本变更通知。如有疑问，请联系技术支持团队。