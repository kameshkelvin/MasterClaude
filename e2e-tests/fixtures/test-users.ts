export const TEST_USERS = {
  student: {
    username: 'student001',
    email: 'student001@test.com',
    password: 'Test123!',
    displayName: '张三',
    role: 'STUDENT'
  },
  
  student2: {
    username: 'student002',
    email: 'student002@test.com', 
    password: 'Test123!',
    displayName: '李四',
    role: 'STUDENT'
  },
  
  teacher: {
    username: 'teacher001',
    email: 'teacher001@test.com',
    password: 'Test123!',
    displayName: '王老师',
    role: 'TEACHER'
  },
  
  admin: {
    username: 'admin001',
    email: 'admin001@test.com',
    password: 'Admin123!',
    displayName: '管理员一号',
    role: 'ADMIN'
  },
  
  superAdmin: {
    username: 'superadmin',
    email: 'superadmin@test.com',
    password: 'SuperAdmin123!',
    displayName: '超级管理员',
    role: 'SUPER_ADMIN'
  }
} as const;

export const INVALID_USERS = {
  weakPassword: {
    username: 'weakuser',
    email: 'weak@test.com',
    password: '123',
    displayName: '弱密码用户'
  },
  
  invalidEmail: {
    username: 'invaliduser',
    email: 'not-an-email',
    password: 'Test123!',
    displayName: '无效邮箱用户'
  },
  
  shortUsername: {
    username: 'ab',
    email: 'short@test.com',
    password: 'Test123!',
    displayName: '短用户名'
  }
} as const;