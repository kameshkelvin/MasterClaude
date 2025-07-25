import { Page } from '@playwright/test';
import { faker } from 'faker';

export class TestDataSeeder {
  constructor(private page: Page) {}

  async seedTestUsers() {
    console.log('Creating test users...');
    
    const users = [
      // Student users
      {
        username: 'student001',
        email: 'student001@test.com',
        password: 'Test123!',
        firstName: '张',
        lastName: '三',
        role: 'STUDENT',
        isActive: true,
        isVerified: true
      },
      {
        username: 'student002', 
        email: 'student002@test.com',
        password: 'Test123!',
        firstName: '李',
        lastName: '四',
        role: 'STUDENT',
        isActive: true,
        isVerified: true
      },
      // Teacher user
      {
        username: 'teacher001',
        email: 'teacher001@test.com',
        password: 'Test123!',
        firstName: '王',
        lastName: '老师',
        role: 'TEACHER',
        isActive: true,
        isVerified: true
      },
      // Admin user
      {
        username: 'admin001',
        email: 'admin001@test.com',
        password: 'Admin123!',
        firstName: '管理员',
        lastName: '一号',
        role: 'ADMIN',
        isActive: true,
        isVerified: true
      },
      // Super admin user
      {
        username: 'superadmin',
        email: 'superadmin@test.com',
        password: 'SuperAdmin123!',
        firstName: '超级',
        lastName: '管理员',
        role: 'SUPER_ADMIN',
        isActive: true,
        isVerified: true
      }
    ];

    for (const user of users) {
      try {
        const response = await this.page.request.post('/api/v1/test/users', {
          data: user
        });
        
        if (response.ok()) {
          console.log(`✅ Created user: ${user.username}`);
        } else {
          console.log(`⚠️ User may already exist: ${user.username}`);
        }
      } catch (error) {
        console.error(`❌ Failed to create user ${user.username}:`, error);
      }
    }
  }

  async seedTestExams() {
    console.log('Creating test exams...');
    
    const exams = [
      {
        title: '计算机基础知识测试',
        description: '测试计算机基础知识掌握情况',
        category: '计算机科学',
        duration: 60,
        maxAttempts: 3,
        passingScore: 60,
        status: 'DRAFT',
        settings: {
          allowReview: true,
          showResults: true,
          timeLimit: true,
          randomizeQuestions: false,
          passScore: 60
        }
      },
      {
        title: '数学基础能力测试',
        description: '评估数学基础运算能力',
        category: '数学',
        duration: 90,
        maxAttempts: 2,
        passingScore: 70,
        status: 'ACTIVE',
        startDate: new Date().toISOString(),
        endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
        settings: {
          allowReview: false,
          showResults: true,
          timeLimit: true,
          randomizeQuestions: true,
          passScore: 70
        }
      },
      {
        title: '英语水平测试',
        description: '综合评估英语听说读写能力',
        category: '英语',
        duration: 120,
        maxAttempts: 1,
        passingScore: 80,
        status: 'ACTIVE',
        startDate: new Date().toISOString(),
        endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        settings: {
          allowReview: true,
          showResults: false,
          timeLimit: true,
          randomizeQuestions: true,
          passScore: 80
        }
      }
    ];

    for (const exam of exams) {
      try {
        const response = await this.page.request.post('/api/v1/test/exams', {
          data: exam,
          headers: {
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok()) {
          console.log(`✅ Created exam: ${exam.title}`);
        } else {
          console.log(`⚠️ Exam may already exist: ${exam.title}`);
        }
      } catch (error) {
        console.error(`❌ Failed to create exam ${exam.title}:`, error);
      }
    }
  }

  async seedTestQuestions() {
    console.log('Creating test questions...');
    
    const questions = [
      // Single choice questions
      {
        title: '计算机的中央处理器是什么？',
        content: '计算机的核心组件，负责执行指令和控制计算机的运行',
        type: 'SINGLE_CHOICE',
        category: '计算机科学',
        difficulty: 'EASY',
        points: 5,
        options: [
          { content: 'CPU', isCorrect: true },
          { content: 'GPU', isCorrect: false },
          { content: 'RAM', isCorrect: false },
          { content: 'ROM', isCorrect: false }
        ],
        explanation: 'CPU（Central Processing Unit）是计算机的中央处理器，是计算机的核心组件。'
      },
      {
        title: '以下哪个是编程语言？',
        content: '选择一种编程语言',
        type: 'SINGLE_CHOICE',
        category: '计算机科学',
        difficulty: 'EASY',
        points: 5,
        options: [
          { content: 'HTML', isCorrect: false },
          { content: 'CSS', isCorrect: false },
          { content: 'JavaScript', isCorrect: true },
          { content: 'HTTP', isCorrect: false }
        ],
        explanation: 'JavaScript是一种编程语言，而HTML和CSS是标记语言，HTTP是协议。'
      },
      // Multiple choice questions
      {
        title: '以下哪些是前端开发技术？',
        content: '选择所有适用的前端开发技术',
        type: 'MULTIPLE_CHOICE',
        category: '计算机科学',
        difficulty: 'MEDIUM',
        points: 10,
        options: [
          { content: 'React', isCorrect: true },
          { content: 'Vue.js', isCorrect: true },
          { content: 'Node.js', isCorrect: false },
          { content: 'Angular', isCorrect: true },
          { content: 'Express.js', isCorrect: false }
        ],
        explanation: 'React、Vue.js和Angular是前端框架，Node.js和Express.js是后端技术。'
      },
      // True/False questions
      {
        title: 'HTTP是无状态协议',
        content: 'HTTP协议本身不维护客户端和服务端之间的状态信息',
        type: 'TRUE_FALSE',
        category: '计算机科学',
        difficulty: 'MEDIUM',
        points: 5,
        correctAnswer: 'true',
        explanation: 'HTTP是无状态协议，每次请求都是独立的，服务器不会保存客户端的状态信息。'
      },
      // Fill in the blank questions
      {
        title: 'HTML元素结构',
        content: 'HTML元素由______标签和______标签组成，中间包含内容。',
        type: 'FILL_BLANK',
        category: '计算机科学',
        difficulty: 'EASY',
        points: 8,
        correctAnswer: '开始|结束|开放|闭合',
        explanation: 'HTML元素由开始标签和结束标签组成，例如<p>内容</p>。'
      },
      // Essay questions
      {
        title: '什么是响应式网页设计？',
        content: '请简述响应式网页设计的概念和主要实现方法。',
        type: 'ESSAY',
        category: '计算机科学',
        difficulty: 'HARD',
        points: 20,
        correctAnswer: '参考答案：响应式网页设计是一种网页设计方法，使网页能够在不同设备和屏幕尺寸上提供良好的用户体验。主要实现方法包括：1. 流体网格布局 2. 灵活的图片和媒体 3. CSS媒体查询 4. 移动优先设计原则',
        explanation: '响应式设计确保网站在各种设备上都能正常显示和使用。'
      },
      // Math questions
      {
        title: '二次方程求解',
        content: '解方程：x² - 5x + 6 = 0',
        type: 'SINGLE_CHOICE',
        category: '数学',
        difficulty: 'MEDIUM',
        points: 10,
        options: [
          { content: 'x = 2 或 x = 3', isCorrect: true },
          { content: 'x = 1 或 x = 6', isCorrect: false },
          { content: 'x = -2 或 x = -3', isCorrect: false },
          { content: 'x = 0 或 x = 5', isCorrect: false }
        ],
        explanation: '使用因式分解：(x-2)(x-3) = 0，所以 x = 2 或 x = 3。'
      },
      // English questions
      {
        title: '英语语法选择',
        content: 'Choose the correct form: "I ____ to the store yesterday."',
        type: 'SINGLE_CHOICE',
        category: '英语',
        difficulty: 'EASY',
        points: 5,
        options: [
          { content: 'go', isCorrect: false },
          { content: 'goes', isCorrect: false },
          { content: 'went', isCorrect: true },
          { content: 'going', isCorrect: false }
        ],
        explanation: '"Yesterday"表示过去时间，应该使用过去式"went"。'
      }
    ];

    for (const question of questions) {
      try {
        const response = await this.page.request.post('/api/v1/test/questions', {
          data: question,
          headers: {
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok()) {
          console.log(`✅ Created question: ${question.title}`);
        } else {
          console.log(`⚠️ Question may already exist: ${question.title}`);
        }
      } catch (error) {
        console.error(`❌ Failed to create question ${question.title}:`, error);
      }
    }
  }

  async seedExamQuestions() {
    console.log('Linking questions to exams...');
    
    // This would typically involve making API calls to associate
    // questions with specific exams based on their IDs
    try {
      const response = await this.page.request.post('/api/v1/test/exam-questions/link', {
        data: {
          examTitle: '计算机基础知识测试',
          questionTitles: [
            '计算机的中央处理器是什么？',
            '以下哪个是编程语言？',
            '以下哪些是前端开发技术？',
            'HTTP是无状态协议',
            'HTML元素结构'
          ]
        }
      });
      
      if (response.ok()) {
        console.log('✅ Linked questions to exam: 计算机基础知识测试');
      }
    } catch (error) {
      console.error('❌ Failed to link questions to exams:', error);
    }
  }
}