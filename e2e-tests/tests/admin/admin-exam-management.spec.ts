import { test, expect } from '@playwright/test';
import { LoginPage } from '../../page-objects/auth/login-page';
import { AdminExamManagementPage } from '../../page-objects/admin/admin-exam-management-page';
import { TEST_USERS } from '../../fixtures/test-users';

test.describe('管理员试卷管理流程', () => {
  let loginPage: LoginPage;
  let adminPage: AdminExamManagementPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    adminPage = new AdminExamManagementPage(page);
    
    // 登录管理员用户
    await loginPage.goto();
    await loginPage.loginAndWaitForRedirect(
      TEST_USERS.admin.username,
      TEST_USERS.admin.password,
      '**/admin/dashboard'
    );
  });

  test.describe('试卷管理页面', () => {
    test('访问试卷管理页面', async ({ page }) => {
      await adminPage.goto();
      
      // 验证页面元素
      await expect(adminPage.pageTitle).toBeVisible();
      await expect(adminPage.createExamButton).toBeVisible();
      await expect(adminPage.searchInput).toBeVisible();
      await expect(adminPage.statusFilter).toBeVisible();
      await expect(adminPage.categoryFilter).toBeVisible();
    });

    test('查看试卷统计数据', async () => {
      await adminPage.goto();
      
      const stats = await adminPage.getStatsData();
      expect(stats.totalExams).toBeGreaterThanOrEqual(0);
      expect(stats.published).toBeGreaterThanOrEqual(0);
      expect(stats.drafts).toBeGreaterThanOrEqual(0);
      expect(stats.averageScore).toBeGreaterThanOrEqual(0);
      
      // 验证统计数据的逻辑关系
      expect(stats.published + stats.drafts).toBeLessThanOrEqual(stats.totalExams);
    });

    test('搜索试卷功能', async () => {
      await adminPage.goto();
      
      const initialCount = await adminPage.getExamCount();
      
      // 搜索特定试卷
      await adminPage.searchExams('数学');
      await adminPage.waitForLoadingToFinish();
      
      const searchResultCount = await adminPage.getExamCount();
      
      // 搜索结果应该不超过初始数量
      expect(searchResultCount).toBeLessThanOrEqual(initialCount);
      
      // 清空搜索
      await adminPage.searchExams('');
      await adminPage.waitForLoadingToFinish();
      
      const clearedSearchCount = await adminPage.getExamCount();
      expect(clearedSearchCount).toBe(initialCount);
    });

    test('按状态筛选试卷', async () => {
      await adminPage.goto();
      
      // 筛选已发布的试卷
      await adminPage.filterByStatus('published');
      await adminPage.waitForLoadingToFinish();
      
      const publishedCount = await adminPage.getExamCount();
      
      // 验证筛选结果
      if (publishedCount > 0) {
        const firstExamInfo = await adminPage.getExamInfo(0);
        expect(firstExamInfo.status).toContain('已发布');
      }
      
      // 筛选草稿状态
      await adminPage.filterByStatus('draft');
      await adminPage.waitForLoadingToFinish();
      
      const draftCount = await adminPage.getExamCount();
      if (draftCount > 0) {
        const firstDraftInfo = await adminPage.getExamInfo(0);
        expect(firstDraftInfo.status).toContain('草稿');
      }
    });

    test('按分类筛选试卷', async () => {
      await adminPage.goto();
      
      // 筛选数学分类
      await adminPage.filterByCategory('数学');
      await adminPage.waitForLoadingToFinish();
      
      const mathCount = await adminPage.getExamCount();
      
      if (mathCount > 0) {
        const firstMathExam = await adminPage.getExamInfo(0);
        expect(firstMathExam.category).toContain('数学');
      }
    });
  });

  test.describe('创建试卷流程', () => {
    const newExam = {
      title: `测试试卷_${Date.now()}`,
      description: '这是一个E2E测试创建的试卷',
      category: '数学',
      duration: 120,
      maxAttempts: 3,
      passingScore: 60,
      startDate: '2024-01-01T09:00',
      endDate: '2024-12-31T18:00',
      instructions: '请仔细阅读题目，认真作答。',
      settings: {
        allowReview: true,
        showResults: true,
        timeLimit: true,
        randomizeQuestions: false
      }
    };

    test('创建新试卷完整流程', async () => {
      await adminPage.goto();
      
      const initialCount = await adminPage.getExamCount();
      
      await adminPage.createExam(newExam);
      
      // 验证试卷创建成功
      await adminPage.expectExamExists(newExam.title);
      
      const newCount = await adminPage.getExamCount();
      expect(newCount).toBe(initialCount + 1);
    });

    test('必填字段验证', async () => {
      await adminPage.goto();
      await adminPage.openCreateExamModal();
      
      // 不填写必填字段直接保存
      await adminPage.saveButton.click();
      
      // 验证错误提示
      const titleError = adminPage.page.locator('[data-testid="title-error"], .field-error');
      await expect(titleError).toBeVisible();
    });

    test('试卷设置配置', async () => {
      await adminPage.goto();
      await adminPage.openCreateExamModal();
      
      // 填写基本信息
      await adminPage.examTitleInput.fill(newExam.title);
      await adminPage.examDescriptionInput.fill(newExam.description);
      await adminPage.examCategorySelect.selectOption(newExam.category);
      await adminPage.examDurationInput.fill(newExam.duration.toString());
      
      // 下一步
      await adminPage.page.getByRole('button', { name: '下一步' }).click();
      
      // 时间设置
      await adminPage.startDateInput.fill(newExam.startDate!);
      await adminPage.endDateInput.fill(newExam.endDate!);
      
      // 下一步到设置页面
      await adminPage.page.getByRole('button', { name: '下一步' }).click();
      
      // 配置试卷设置
      await adminPage.maxAttemptsInput.fill(newExam.maxAttempts!.toString());
      await adminPage.passingScoreInput.fill(newExam.passingScore!.toString());
      
      // 验证设置保存
      await adminPage.saveButton.click();
      
      // 等待模态框关闭
      await expect(adminPage.createModal).not.toBeVisible();
    });

    test('取消创建试卷', async () => {
      await adminPage.goto();
      const initialCount = await adminPage.getExamCount();
      
      await adminPage.openCreateExamModal();
      
      // 填写部分信息
      await adminPage.examTitleInput.fill('临时试卷');
      
      // 取消创建
      await adminPage.cancelButton.click();
      
      // 验证模态框关闭，试卷数量不变
      await expect(adminPage.createModal).not.toBeVisible();
      
      const finalCount = await adminPage.getExamCount();
      expect(finalCount).toBe(initialCount);
    });
  });

  test.describe('试卷编辑和管理', () => {
    test.beforeEach(async () => {
      await adminPage.goto();
      
      // 确保至少有一个试卷存在
      const examCount = await adminPage.getExamCount();
      if (examCount === 0) {
        await adminPage.createExam({
          title: '测试试卷',
          description: '用于测试的试卷',
          category: '数学',
          duration: 60
        });
      }
    });

    test('编辑试卷信息', async () => {
      await adminPage.editExam(0);
      
      // 修改试卷标题
      const newTitle = `修改后的试卷_${Date.now()}`;
      await adminPage.examTitleInput.clear();
      await adminPage.examTitleInput.fill(newTitle);
      
      // 保存修改
      await adminPage.saveButton.click();
      await expect(adminPage.editModal).not.toBeVisible();
      
      // 验证修改生效
      await adminPage.expectExamExists(newTitle);
    });

    test('发布试卷', async () => {
      const examInfo = await adminPage.getExamInfo(0);
      
      if (examInfo.status.includes('草稿')) {
        await adminPage.publishExam(0);
        
        // 验证发布成功
        await adminPage.page.waitForTimeout(2000);
        const updatedInfo = await adminPage.getExamInfo(0);
        expect(updatedInfo.status).toContain('已发布');
      }
    });

    test('删除试卷', async () => {
      const initialCount = await adminPage.getExamCount();
      const firstExamInfo = await adminPage.getExamInfo(0);
      
      await adminPage.deleteExam(0);
      
      // 验证删除成功
      const newCount = await adminPage.getExamCount();
      expect(newCount).toBe(initialCount - 1);
      
      // 验证试卷不存在
      await adminPage.expectExamNotExists(firstExamInfo.title);
    });

    test('预览试卷', async ({ context }) => {
      const previewTab = await adminPage.previewExam(0);
      
      // 验证预览页面打开
      await expect(previewTab).toBeTruthy();
      await previewTab.waitForLoadState();
      
      // 验证预览页面内容
      const previewTitle = previewTab.locator('[data-testid="exam-title"], .exam-title');
      await expect(previewTitle).toBeVisible();
      
      await previewTab.close();
    });

    test('查看试卷详细信息', async () => {
      const examInfo = await adminPage.getExamInfo(0);
      
      // 验证基本信息
      expect(examInfo.title.length).toBeGreaterThan(0);
      expect(examInfo.status.length).toBeGreaterThan(0);
      expect(examInfo.category.length).toBeGreaterThan(0);
      expect(examInfo.duration.length).toBeGreaterThan(0);
      expect(examInfo.questions).toBeGreaterThanOrEqual(0);
      expect(examInfo.participants).toBeGreaterThanOrEqual(0);
    });
  });

  test.describe('批量操作功能', () => {
    test.beforeEach(async () => {
      await adminPage.goto();
      
      // 确保有多个试卷用于批量操作
      const examCount = await adminPage.getExamCount();
      if (examCount < 2) {
        for (let i = examCount; i < 3; i++) {
          await adminPage.createExam({
            title: `批量测试试卷${i + 1}`,
            description: `批量操作测试试卷${i + 1}`,
            category: '测试',
            duration: 60
          });
        }
      }
    });

    test('选择多个试卷', async () => {
      // 选择前两个试卷
      await adminPage.selectExam(0);
      await adminPage.selectExam(1);
      
      const selectedCount = await adminPage.getSelectedExamCount();
      expect(selectedCount).toBe(2);
      
      // 验证批量操作面板显示
      await expect(adminPage.bulkActionsPanel).toBeVisible();
    });

    test('全选试卷', async () => {
      const totalCount = await adminPage.getExamCount();
      
      await adminPage.selectAllExams();
      
      const selectedCount = await adminPage.getSelectedExamCount();
      expect(selectedCount).toBe(totalCount);
    });

    test('批量发布试卷', async () => {
      // 选择多个草稿状态的试卷
      await adminPage.selectExam(0);
      await adminPage.selectExam(1);
      
      await adminPage.bulkPublishExams();
      
      // 验证批量发布成功
      await adminPage.page.waitForTimeout(3000);
      
      const firstExamInfo = await adminPage.getExamInfo(0);
      const secondExamInfo = await adminPage.getExamInfo(1);
      
      // 至少有一个应该是已发布状态
      const hasPublished = firstExamInfo.status.includes('已发布') || 
                          secondExamInfo.status.includes('已发布');
      expect(hasPublished).toBeTruthy();
    });

    test('批量删除试卷', async () => {
      const initialCount = await adminPage.getExamCount();
      
      // 选择要删除的试卷
      await adminPage.selectExam(0);
      await adminPage.selectExam(1);
      
      await adminPage.bulkDeleteExams();
      
      // 验证批量删除成功
      const newCount = await adminPage.getExamCount();
      expect(newCount).toBe(initialCount - 2);
    });
  });

  test.describe('试卷数据管理', () => {
    test('导出试卷数据', async ({ page }) => {
      await adminPage.goto();
      
      // 查找导出按钮
      const exportButton = page.getByRole('button', { name: '导出' });
      if (await exportButton.isVisible()) {
        // 监听下载事件
        const downloadPromise = page.waitForEvent('download');
        
        await exportButton.click();
        
        const download = await downloadPromise;
        expect(download.suggestedFilename()).toMatch(/\.xlsx?|\.csv/);
      }
    });

    test('导入试卷数据', async ({ page }) => {
      await adminPage.goto();
      
      const importButton = page.getByRole('button', { name: '导入' });
      if (await importButton.isVisible()) {
        await importButton.click();
        
        // 验证导入对话框显示
        const importModal = page.locator('[data-testid="import-modal"], .import-modal');
        await expect(importModal).toBeVisible();
        
        // 关闭对话框
        const cancelButton = importModal.getByRole('button', { name: '取消' });
        await cancelButton.click();
      }
    });

    test('试卷数据分析', async ({ page }) => {
      await adminPage.goto();
      
      // 查看详细统计
      const analyticsButton = page.getByRole('button', { name: '数据分析' });
      if (await analyticsButton.isVisible()) {
        await analyticsButton.click();
        
        // 验证分析页面或模态框
        const analyticsContent = page.locator('[data-testid="analytics"], .analytics');
        await expect(analyticsContent).toBeVisible();
      }
    });
  });

  test.describe('权限和安全', () => {
    test('普通用户无法访问管理页面', async ({ page, context }) => {
      // 登出管理员
      await page.goto('/logout');
      
      // 登录学生用户
      await loginPage.goto();
      await loginPage.loginAndWaitForRedirect(
        TEST_USERS.student.username,
        TEST_USERS.student.password
      );
      
      // 尝试访问管理页面
      await page.goto('/admin/exams');
      
      // 应该被重定向或显示权限错误
      await expect(page).not.toHaveURL(/admin.*exams/);
    });

    test('教师权限验证', async ({ page }) => {
      // 登出管理员
      await page.goto('/logout');
      
      // 登录教师用户
      await loginPage.goto();
      await loginPage.loginAndWaitForRedirect(
        TEST_USERS.teacher.username,
        TEST_USERS.teacher.password
      );
      
      // 教师可能有部分管理权限
      await page.goto('/teacher/exams');
      
      // 验证教师界面
      const teacherInterface = page.locator('[data-testid="teacher-exam-management"]');
      if (await teacherInterface.isVisible()) {
        // 验证教师只能管理自己的试卷
        await expect(page.locator('[data-testid="all-exams-view"]')).not.toBeVisible();
      }
    });

    test('防止未授权的试卷操作', async ({ page }) => {
      await adminPage.goto();
      
      // 模拟恶意请求
      const response = await page.request.delete('/api/admin/exams/999', {
        headers: {
          'Authorization': 'Bearer invalid-token'
        }
      });
      
      expect(response.status()).toBe(401);
    });
  });

  test.describe('移动端管理界面', () => {
    test('移动端试卷管理适配', async ({ page }) => {
      // 设置移动设备视口
      await page.setViewportSize({ width: 375, height: 667 });
      
      await adminPage.goto();
      
      // 验证移动端界面适配
      await expect(adminPage.pageTitle).toBeVisible();
      await expect(adminPage.createExamButton).toBeVisible();
      
      // 验证响应式布局
      const mobileMenu = page.locator('[data-testid="mobile-menu"], .mobile-menu');
      if (await mobileMenu.isVisible()) {
        await mobileMenu.click();
        
        // 验证移动端菜单
        const mobileNav = page.locator('[data-testid="mobile-nav"], .mobile-nav');
        await expect(mobileNav).toBeVisible();
      }
    });

    test('移动端操作便利性', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      await adminPage.goto();
      
      // 测试滑动操作
      if (await adminPage.examCards.first().isVisible()) {
        const examCard = adminPage.examCards.first();
        
        // 模拟滑动显示操作按钮
        await examCard.hover();
        
        const actionButtons = examCard.locator('.action-buttons, [data-testid="action-buttons"]');
        if (await actionButtons.isVisible()) {
          await expect(actionButtons).toBeVisible();
        }
      }
    });
  });

  test.describe('性能和加载测试', () => {
    test('大量试卷加载性能', async ({ page }) => {
      await adminPage.goto();
      
      // 记录加载时间
      const startTime = Date.now();
      
      await adminPage.waitForLoadingToFinish();
      
      const loadTime = Date.now() - startTime;
      
      // 验证加载时间合理（小于5秒）
      expect(loadTime).toBeLessThan(5000);
      
      // 验证分页功能
      const pagination = page.locator('[data-testid="pagination"], .pagination');
      if (await pagination.isVisible()) {
        const nextButton = pagination.getByRole('button', { name: '下一页' });
        if (await nextButton.isEnabled()) {
          await nextButton.click();
          await adminPage.waitForLoadingToFinish();
        }
      }
    });

    test('搜索和筛选性能', async () => {
      await adminPage.goto();
      
      // 测试搜索响应时间
      const startTime = Date.now();
      
      await adminPage.searchExams('测试');
      await adminPage.waitForLoadingToFinish();
      
      const searchTime = Date.now() - startTime;
      expect(searchTime).toBeLessThan(3000);
      
      // 测试筛选响应时间
      const filterStartTime = Date.now();
      
      await adminPage.filterByStatus('published');
      await adminPage.waitForLoadingToFinish();
      
      const filterTime = Date.now() - filterStartTime;
      expect(filterTime).toBeLessThan(2000);
    });
  });

  test.describe('错误处理和恢复', () => {
    test('网络错误处理', async ({ page, context }) => {
      await adminPage.goto();
      
      // 断开网络
      await context.setOffline(true);
      
      // 尝试创建试卷
      await adminPage.createExamButton.click();
      
      // 应该显示网络错误
      const networkError = page.locator('[data-testid="network-error"], .network-error');
      await expect(networkError).toBeVisible();
      
      // 恢复网络
      await context.setOffline(false);
      
      // 重试操作
      await page.reload();
      await adminPage.waitForLoad();
    });

    test('服务器错误处理', async ({ page }) => {
      await adminPage.goto();
      
      // 模拟服务器错误
      await page.route('**/api/admin/exams', route => {
        route.fulfill({
          status: 500,
          body: JSON.stringify({ error: 'Internal Server Error' })
        });
      });
      
      await page.reload();
      
      // 验证错误提示
      const serverError = page.locator('[data-testid="server-error"], .server-error');
      await expect(serverError).toBeVisible();
      
      // 清除路由拦截
      await page.unroute('**/api/admin/exams');
    });

    test('数据丢失保护', async ({ page }) => {
      await adminPage.goto();
      await adminPage.openCreateExamModal();
      
      // 填写试卷信息
      await adminPage.examTitleInput.fill('重要试卷');
      await adminPage.examDescriptionInput.fill('重要内容');
      
      // 模拟页面刷新
      await page.reload();
      
      // 再次打开创建模态框
      await adminPage.openCreateExamModal();
      
      // 检查是否有数据恢复提示
      const recoveryPrompt = page.locator('[data-testid="data-recovery"], .data-recovery');
      if (await recoveryPrompt.isVisible()) {
        const restoreButton = recoveryPrompt.getByRole('button', { name: '恢复数据' });
        await restoreButton.click();
        
        // 验证数据恢复
        const titleValue = await adminPage.examTitleInput.inputValue();
        expect(titleValue).toBe('重要试卷');
      }
    });
  });
});