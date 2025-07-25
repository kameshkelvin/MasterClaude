import { test, expect } from '@playwright/test';
import { LoginPage } from '../../page-objects/auth/login-page';
import { ExamInterfacePage } from '../../page-objects/exam/exam-interface-page';
import { TEST_USERS } from '../../fixtures/test-users';

test.describe('在线答题流程测试', () => {
  let loginPage: LoginPage;
  let examPage: ExamInterfacePage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    examPage = new ExamInterfacePage(page);
    
    // 登录学生用户
    await loginPage.goto();
    await loginPage.loginAndWaitForRedirect(
      TEST_USERS.student.username,
      TEST_USERS.student.password
    );
  });

  test.describe('考试开始流程', () => {
    test('查看考试说明并开始考试', async ({ page }) => {
      // 选择一个可用的考试
      await page.goto('/exams/1');
      
      // 验证考试信息显示
      await expect(examPage.examTitle).toBeVisible();
      await expect(examPage.examDescription).toBeVisible();
      
      // 查看考试说明
      const instructions = await examPage.getExamInstructions();
      expect(instructions.length).toBeGreaterThan(0);
      
      // 开始考试
      await examPage.startExam();
      
      // 验证考试界面加载
      await examPage.waitForExamLoad();
      await expect(examPage.questionContent).toBeVisible();
      await expect(examPage.timerDisplay).toBeVisible();
    });

    test('考试前检查系统要求', async ({ page }) => {
      await page.goto('/exams/1');
      
      // 检查浏览器兼容性警告
      const browserWarning = page.locator('[data-testid="browser-warning"]');
      if (await browserWarning.isVisible()) {
        const warningText = await browserWarning.textContent();
        expect(warningText).toContain('浏览器');
      }
      
      // 检查全屏模式要求
      await examPage.startExam();
      await examPage.toggleFullscreen();
      
      const isFullscreen = await examPage.isInFullscreen();
      expect(isFullscreen).toBeTruthy();
    });

    test('考试权限验证', async ({ page }) => {
      // 尝试访问未授权的考试
      await page.goto('/exams/999');
      
      // 应该显示权限错误或重定向
      await expect(page.locator('[data-testid="access-denied"], .access-denied')).toBeVisible();
    });
  });

  test.describe('答题功能测试', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/exams/1');
      await examPage.startExam();
    });

    test('单选题答题', async () => {
      const questionText = await examPage.getCurrentQuestion();
      expect(questionText.length).toBeGreaterThan(0);
      
      // 选择第一个选项
      await examPage.selectSingleChoiceOption(0);
      
      // 验证选项被选中
      const selectedOption = examPage.questionOptions.locator('input[type="radio"]:checked');
      await expect(selectedOption).toBeVisible();
      
      // 切换到不同选项
      await examPage.selectSingleChoiceOption(1);
      
      // 验证只有一个选项被选中
      const checkedOptions = examPage.questionOptions.locator('input[type="radio"]:checked');
      expect(await checkedOptions.count()).toBe(1);
    });

    test('多选题答题', async () => {
      // 假设当前题目是多选题
      await examPage.selectMultipleChoiceOptions([0, 2]);
      
      // 验证多个选项被选中
      const checkedOptions = examPage.questionOptions.locator('input[type="checkbox"]:checked');
      expect(await checkedOptions.count()).toBe(2);
      
      // 取消选择一个选项
      await examPage.selectMultipleChoiceOptions([0]);
      
      // 验证选择状态更新
      const updatedCheckedOptions = examPage.questionOptions.locator('input[type="checkbox"]:checked');
      expect(await updatedCheckedOptions.count()).toBe(2); // 0和2仍然选中
    });

    test('填空题答题', async () => {
      const testAnswer = '这是一个测试答案';
      
      await examPage.fillTextAnswer(testAnswer);
      
      // 验证答案被填入
      const textInput = examPage.questionOptions.locator('input[type="text"], textarea');
      expect(await textInput.inputValue()).toBe(testAnswer);
    });

    test('判断题答题', async () => {
      // 选择"正确"
      await examPage.selectTrueFalse(true);
      
      let selectedOption = examPage.questionOptions.locator('input[value="true"]:checked');
      await expect(selectedOption).toBeVisible();
      
      // 切换到"错误"
      await examPage.selectTrueFalse(false);
      
      selectedOption = examPage.questionOptions.locator('input[value="false"]:checked');
      await expect(selectedOption).toBeVisible();
    });

    test('题目导航功能', async () => {
      const totalQuestions = await examPage.getTotalQuestions();
      const currentQuestion = await examPage.getCurrentQuestionNumber();
      
      expect(totalQuestions).toBeGreaterThan(0);
      expect(currentQuestion).toBe(1);
      
      // 下一题
      if (await examPage.isNextButtonEnabled()) {
        await examPage.goToNextQuestion();
        const newQuestionNumber = await examPage.getCurrentQuestionNumber();
        expect(newQuestionNumber).toBe(2);
      }
      
      // 上一题
      if (await examPage.isPreviousButtonEnabled()) {
        await examPage.goToPreviousQuestion();
        const backQuestionNumber = await examPage.getCurrentQuestionNumber();
        expect(backQuestionNumber).toBe(1);
      }
    });

    test('直接跳转到指定题目', async () => {
      const totalQuestions = await examPage.getTotalQuestions();
      
      if (totalQuestions >= 3) {
        await examPage.goToQuestionByNumber(3);
        const currentQuestion = await examPage.getCurrentQuestionNumber();
        expect(currentQuestion).toBe(3);
      }
    });
  });

  test.describe('考试进度和状态', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/exams/1');
      await examPage.startExam();
    });

    test('考试计时器功能', async () => {
      const initialTime = await examPage.getRemainingTime();
      expect(initialTime).toMatch(/\d+:\d+/);
      
      // 等待几秒钟
      await examPage.page.waitForTimeout(3000);
      
      const updatedTime = await examPage.getRemainingTime();
      expect(updatedTime).not.toBe(initialTime);
    });

    test('答题进度显示', async () => {
      const initialProgress = await examPage.getProgressPercentage();
      expect(initialProgress).toBeGreaterThanOrEqual(0);
      
      // 完成一些题目
      await examPage.selectSingleChoiceOption(0);
      await examPage.goToNextQuestion();
      await examPage.selectSingleChoiceOption(1);
      
      const updatedProgress = await examPage.getProgressPercentage();
      expect(updatedProgress).toBeGreaterThan(initialProgress);
    });

    test('自动保存功能', async () => {
      // 答题
      await examPage.selectSingleChoiceOption(0);
      
      // 触发自动保存
      await examPage.autoSaveProgress();
      
      // 验证自动保存指示器
      await examPage.verifyAutoSave();
    });

    test('答题状态跟踪', async () => {
      const totalQuestions = await examPage.getTotalQuestions();
      
      // 答几道题
      await examPage.selectSingleChoiceOption(0);
      await examPage.goToNextQuestion();
      await examPage.selectSingleChoiceOption(1);
      
      // 检查已答题目
      const answeredQuestions = await examPage.getAnsweredQuestions();
      expect(answeredQuestions.length).toBeGreaterThan(0);
      
      // 检查未答题目
      const unansweredQuestions = await examPage.getUnansweredQuestions();
      expect(answeredQuestions.length + unansweredQuestions.length).toBe(totalQuestions);
    });
  });

  test.describe('考试安全机制', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/exams/1');
      await examPage.startExam();
    });

    test('防止考试期间离开页面', async () => {
      // 尝试离开考试页面
      await examPage.attemptToLeaveExam();
      
      // 应该显示警告
      await examPage.expectWarningModal();
      
      // 取消离开
      await examPage.dismissWarning();
    });

    test('全屏模式强制要求', async () => {
      await examPage.toggleFullscreen();
      expect(await examPage.isInFullscreen()).toBeTruthy();
      
      // 模拟退出全屏
      await examPage.page.keyboard.press('Escape');
      
      // 应该显示安全警告
      await examPage.expectExamSecurityWarning();
    });

    test('标签页切换检测', async ({ context }) => {
      // 打开新标签页
      const newPage = await context.newPage();
      await newPage.goto('https://www.google.com');
      
      // 切回考试页面
      await examPage.page.bringToFront();
      
      // 应该检测到标签页切换
      await examPage.expectExamSecurityWarning();
      
      await newPage.close();
    });

    test('考试暂停和恢复', async () => {
      // 暂停考试
      await examPage.pauseExam();
      
      // 验证考试状态
      const pauseIndicator = examPage.page.locator('[data-testid="exam-paused"], .exam-paused');
      await expect(pauseIndicator).toBeVisible();
      
      // 恢复考试
      await examPage.resumeExam();
      
      // 验证考试恢复
      await expect(pauseIndicator).not.toBeVisible();
    });
  });

  test.describe('考试提交流程', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/exams/1');
      await examPage.startExam();
    });

    test('完整答题后提交考试', async ({ page }) => {
      const totalQuestions = await examPage.getTotalQuestions();
      
      // 完成所有题目
      for (let i = 1; i <= totalQuestions; i++) {
        await examPage.goToQuestionByNumber(i);
        await examPage.selectSingleChoiceOption(0);
      }
      
      // 提交考试
      await examPage.submitExam();
      
      // 验证跳转到结果页面
      await expect(page).toHaveURL(/results|submitted/);
    });

    test('部分答题提交考试警告', async () => {
      // 只答一部分题目
      await examPage.selectSingleChoiceOption(0);
      
      // 尝试提交
      await examPage.submitButton.click();
      
      // 应该显示未完成警告
      const warningModal = examPage.page.locator('[data-testid="incomplete-warning"]');
      if (await warningModal.isVisible()) {
        const continueButton = warningModal.getByRole('button', { name: '继续提交' });
        await continueButton.click();
      }
    });

    test('时间到自动提交', async ({ page }) => {
      // 模拟时间到
      await page.evaluate(() => {
        // 触发时间到事件
        window.dispatchEvent(new CustomEvent('exam-time-expired'));
      });
      
      // 应该自动提交并跳转
      await expect(page).toHaveURL(/results|submitted/, { timeout: 30000 });
    });

    test('查看答题情况', async () => {
      // 答几道题
      await examPage.selectSingleChoiceOption(0);
      await examPage.goToNextQuestion();
      await examPage.selectSingleChoiceOption(1);
      
      // 打开答题回顾
      await examPage.openReviewMode();
      
      // 验证答题状态显示
      const answeredQuestions = await examPage.getAnsweredQuestions();
      const unansweredQuestions = await examPage.getUnansweredQuestions();
      
      expect(answeredQuestions.length).toBeGreaterThan(0);
      expect(unansweredQuestions.length).toBeGreaterThan(0);
    });
  });

  test.describe('考试结果查看', () => {
    test('提交后查看考试结果', async ({ page }) => {
      // 完成考试提交
      await page.goto('/exams/1');
      await examPage.startExam();
      
      // 快速完成考试
      const totalQuestions = await examPage.getTotalQuestions();
      for (let i = 1; i <= Math.min(totalQuestions, 5); i++) {
        await examPage.goToQuestionByNumber(i);
        await examPage.selectSingleChoiceOption(0);
      }
      
      await examPage.submitExam();
      
      // 验证结果页面元素
      await expect(page.locator('[data-testid="exam-score"], .exam-score')).toBeVisible();
      await expect(page.locator('[data-testid="exam-result"], .exam-result')).toBeVisible();
      await expect(page.locator('[data-testid="time-used"], .time-used')).toBeVisible();
    });

    test('查看详细答题记录', async ({ page }) => {
      // 假设已经完成考试
      await page.goto('/exam-results/1');
      
      // 查看详细答题记录
      const detailButton = page.getByRole('button', { name: '查看详情' });
      if (await detailButton.isVisible()) {
        await detailButton.click();
        
        // 验证详细记录显示
        await expect(page.locator('[data-testid="answer-details"], .answer-details')).toBeVisible();
        await expect(page.locator('[data-testid="correct-answers"], .correct-answers')).toBeVisible();
      }
    });

    test('考试历史记录查看', async ({ page }) => {
      await page.goto('/my-exams');
      
      // 验证考试历史列表
      await expect(page.locator('[data-testid="exam-history"], .exam-history')).toBeVisible();
      
      // 验证历史记录项
      const historyItems = page.locator('[data-testid="history-item"], .history-item');
      expect(await historyItems.count()).toBeGreaterThan(0);
      
      // 点击查看某次考试记录
      if (await historyItems.first().isVisible()) {
        await historyItems.first().click();
        await expect(page).toHaveURL(/exam-results/);
      }
    });
  });

  test.describe('移动端考试体验', () => {
    test('移动设备考试界面适配', async ({ page }) => {
      // 设置移动设备视口
      await page.setViewportSize({ width: 375, height: 667 });
      
      await page.goto('/exams/1');
      await examPage.startExam();
      
      // 验证移动端界面适配
      await expect(examPage.questionContent).toBeVisible();
      await expect(examPage.questionOptions).toBeVisible();
      
      // 验证按钮可点击
      await expect(examPage.nextButton).toBeVisible();
      await expect(examPage.previousButton).toBeVisible();
    });

    test('移动端触摸操作', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      
      await page.goto('/exams/1');
      await examPage.startExam();
      
      // 测试触摸选择选项
      const option = examPage.questionOptions.locator('input[type="radio"]').first();
      await option.tap();
      
      // 验证选项被选中
      await expect(option).toBeChecked();
    });
  });

  test.describe('网络异常处理', () => {
    test('网络断开时的处理', async ({ page, context }) => {
      await page.goto('/exams/1');
      await examPage.startExam();
      
      // 答题
      await examPage.selectSingleChoiceOption(0);
      
      // 模拟网络断开
      await context.setOffline(true);
      
      // 尝试提交或切换题目
      await examPage.goToNextQuestion();
      
      // 应该显示网络错误提示
      const networkError = page.locator('[data-testid="network-error"], .network-error');
      await expect(networkError).toBeVisible();
      
      // 恢复网络
      await context.setOffline(false);
      
      // 验证数据同步
      await page.reload();
      const answeredQuestions = await examPage.getAnsweredQuestions();
      expect(answeredQuestions.length).toBeGreaterThan(0);
    });

    test('网络慢时的用户体验', async ({ page }) => {
      // 模拟慢网络
      await page.route('**/*', async route => {
        await page.waitForTimeout(2000); // 延迟2秒
        await route.continue();
      });
      
      await page.goto('/exams/1');
      
      // 验证加载指示器显示
      await expect(examPage.loadingSpinner).toBeVisible();
      
      await examPage.startExam();
      
      // 验证操作响应
      await examPage.selectSingleChoiceOption(0);
      
      // 清除路由拦截
      await page.unroute('**/*');
    });
  });
});