# 在线考试系统 E2E 测试套件

基于 Playwright 的端到端测试框架，用于测试在线考试系统的核心功能。

## 🎯 测试范围

### 1. 用户认证流程 (`tests/auth/`)
- ✅ 用户注册（必填字段验证、重复注册检查、密码强度验证）
- ✅ 用户登录（不同角色登录、错误处理、记住我功能）
- ✅ 权限验证（角色访问控制、页面权限检查）
- ✅ 安全测试（防 SQL 注入、XSS 防护、密码复杂度）

### 2. 在线答题流程 (`tests/exam/`)
- ✅ 考试开始（考试说明、系统检查、权限验证）
- ✅ 答题功能（单选、多选、填空、判断题）
- ✅ 题目导航（上一题、下一题、直接跳转）
- ✅ 考试进度（计时器、进度条、自动保存）
- ✅ 安全机制（防作弊、全屏检测、标签页切换监控）
- ✅ 考试提交（完整提交、部分提交警告、时间到自动提交）
- ✅ 结果查看（成绩显示、详细记录、历史查询）

### 3. 管理员试卷管理 (`tests/admin/`)
- ✅ 试卷管理（创建、编辑、删除、预览）
- ✅ 批量操作（批量发布、批量删除、全选功能）
- ✅ 搜索筛选（按标题搜索、状态筛选、分类筛选）
- ✅ 数据统计（试卷统计、参与统计、成绩分析）
- ✅ 权限控制（管理员权限、教师权限、安全验证）
- ✅ 移动端适配（响应式界面、触摸操作）

## 🏗️ 项目结构

```
e2e-tests/
├── 📁 tests/                    # 测试用例
│   ├── 📁 auth/                 # 认证相关测试
│   │   └── user-auth.spec.ts    # 用户注册登录测试
│   ├── 📁 exam/                 # 考试相关测试
│   │   └── exam-taking.spec.ts  # 在线答题测试
│   └── 📁 admin/                # 管理相关测试
│       └── admin-exam-management.spec.ts  # 试卷管理测试
├── 📁 page-objects/             # 页面对象模型
│   ├── 📁 auth/                 # 认证页面对象
│   │   ├── login-page.ts        # 登录页面
│   │   └── register-page.ts     # 注册页面
│   ├── 📁 exam/                 # 考试页面对象
│   │   └── exam-interface-page.ts  # 考试界面
│   └── 📁 admin/                # 管理页面对象
│       └── admin-exam-management-page.ts  # 试卷管理页面
├── 📁 fixtures/                 # 测试数据
│   └── test-users.ts            # 测试用户数据
├── 📁 utils/                    # 工具类
│   ├── global-setup.ts          # 全局设置
│   ├── global-teardown.ts       # 全局清理
│   └── test-data-seeder.ts      # 测试数据种子
├── 📁 scripts/                  # 脚本工具
│   └── test-runner.ts           # 测试运行器
├── playwright.config.ts         # Playwright 配置
├── package.json                 # 项目依赖
└── README.md                    # 项目文档
```

## 🚀 快速开始

### 1. 环境准备

```bash
# 安装依赖
npm install

# 安装 Playwright 浏览器
npm run test:install

# 检查系统健康状态
npm run test:health
```

### 2. 运行测试

```bash
# 运行所有测试
npm run test

# 运行特定测试套件
npm run test:user-flows      # 用户流程测试
npm run test:admin-flows     # 管理员流程测试
npm run test:mobile         # 移动端测试

# 运行特定浏览器测试
npm run test:chrome         # Chrome 浏览器
npm run test:firefox        # Firefox 浏览器
npm run test:safari         # Safari 浏览器

# 交互式测试
npm run test:ui             # 使用 Playwright UI
npm run test:headed         # 有头模式运行
npm run test:debug          # 调试模式
```

### 3. 测试报告

```bash
# 查看测试报告
npm run test:report

# 生成覆盖率报告
npm run test:coverage

# CI/CD 模式运行
npm run test:ci
```

## 🎨 测试分类

### 冒烟测试 (Smoke Tests)
快速验证系统基本功能是否正常：
```bash
npm run test:smoke
```

### 关键路径测试 (Critical Path)
测试业务核心流程：
```bash
npm run test:critical
```

### 回归测试 (Regression)
完整的功能验证测试：
```bash
npm run test:regression
```

### 性能测试 (Performance)
测试系统性能和响应时间：
```bash
npm run test:performance
```

## 📊 测试配置

### 多项目配置
测试框架支持多个项目配置：

- **user-flows**: 用户端功能测试
- **admin-flows**: 管理端功能测试
- **mobile-chrome**: 移动端 Chrome 测试
- **mobile-safari**: 移动端 Safari 测试
- **performance**: 性能测试项目

### 多浏览器支持
- Chrome (桌面 + 移动)
- Firefox (桌面)
- Safari (桌面 + 移动)
- Microsoft Edge

### 多报告格式
- HTML 报告 (详细的可视化报告)
- JSON 报告 (程序化处理)
- JUnit 报告 (CI/CD 集成)
- GitHub Actions 报告

## 🛠️ 配置说明

### 环境变量
```bash
# 应用服务地址
FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080

# 测试配置
TEST_TIMEOUT=30000
HEADLESS=true
WORKERS=4

# 数据库配置（仅用于测试数据准备）
TEST_DB_URL=postgresql://test:test@localhost:5432/exam_test
```

### 全局设置
在 `global-setup.ts` 中配置：
- 服务健康检查
- 测试数据种子
- 数据库初始化
- 认证令牌准备

### 测试数据管理
使用 `test-data-seeder.ts` 管理测试数据：
- 测试用户创建
- 试卷模板准备
- 权限配置
- 清理策略

## 🎭 页面对象模型 (POM)

### 设计原则
- **封装性**: 页面元素和操作方法封装在类中
- **可维护性**: 页面变更只需修改对应的页面对象
- **可复用性**: 页面对象可在多个测试中复用
- **可读性**: 测试代码更接近自然语言

### 页面对象示例
```typescript
export class LoginPage {
  async goto() { /* 导航到登录页 */ }
  async login(username: string, password: string) { /* 执行登录 */ }
  async expectLoginSuccess() { /* 验证登录成功 */ }
  async expectLoginError(message?: string) { /* 验证登录失败 */ }
}
```

## 🔍 测试最佳实践

### 1. 测试独立性
- 每个测试用例独立运行
- 不依赖其他测试的状态
- 使用 `beforeEach` 准备测试环境

### 2. 等待策略
- 使用 `expect().toBeVisible()` 而不是 `waitForTimeout()`
- 等待元素状态而不是固定时间
- 合理设置超时时间

### 3. 元素定位
- 优先使用语义化选择器 (`getByRole`, `getByLabel`)
- 使用 `data-testid` 作为稳定的测试标识
- 避免使用脆弱的 CSS 选择器

### 4. 断言原则
- 断言用户可见的行为
- 使用描述性的断言消息
- 验证业务逻辑而不是实现细节

### 5. 错误处理
- 处理网络错误和超时
- 测试异常场景
- 提供清晰的错误信息

## 📈 持续集成

### GitHub Actions 集成
```yaml
- name: Run E2E Tests
  run: |
    npm run test:install
    npm run test:ci
    
- name: Upload Test Results
  uses: actions/upload-artifact@v3
  with:
    name: playwright-report
    path: e2e-tests/playwright-report/
```

### 并行执行
- 支持多 worker 并行执行
- 自动负载均衡
- 失败重试机制

### 报告集成
- 自动生成测试报告
- 集成到 PR 评论
- 失败截图和视频录制

## 🔧 故障排除

### 常见问题

1. **浏览器启动失败**
   ```bash
   npx playwright install --with-deps
   ```

2. **测试超时**
   - 检查服务是否正常运行
   - 增加 timeout 配置
   - 优化等待策略

3. **元素定位失败**
   - 检查页面是否正确加载
   - 验证选择器是否正确
   - 使用调试模式查看页面状态

4. **数据准备失败**
   - 检查数据库连接
   - 验证种子数据脚本
   - 确认测试环境配置

### 调试技巧

```bash
# 启用调试模式
npm run test:debug

# 显示浏览器界面
npm run test:headed

# 使用 Playwright Inspector
npx playwright test --debug

# 查看测试轨迹
npx playwright show-trace trace.zip
```

## 📝 贡献指南

### 添加新测试
1. 在相应目录创建测试文件
2. 遵循命名约定 `*.spec.ts`
3. 添加适当的测试标签
4. 更新文档说明

### 页面对象维护
1. 保持页面对象的单一职责
2. 提供清晰的方法命名
3. 添加必要的注释
4. 处理异常情况

### 代码规范
- 使用 TypeScript 严格模式
- 遵循 ESLint 规则
- 添加有意义的断言消息
- 保持测试的可读性

## 📞 支持与反馈

如有问题或建议，请：
1. 查看文档和故障排除指南
2. 在项目中创建 Issue
3. 联系测试团队

---

**测试愉快！** 🎭✨