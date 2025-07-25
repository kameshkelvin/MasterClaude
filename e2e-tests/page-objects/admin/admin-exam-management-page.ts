import { Page, Locator, expect } from '@playwright/test';

export class AdminExamManagementPage {
  readonly page: Page;
  readonly pageTitle: Locator;
  readonly createExamButton: Locator;
  readonly searchInput: Locator;
  readonly statusFilter: Locator;
  readonly categoryFilter: Locator;
  readonly examList: Locator;
  readonly examCards: Locator;
  readonly editButtons: Locator;
  readonly deleteButtons: Locator;
  readonly publishButtons: Locator;
  readonly previewButtons: Locator;
  readonly bulkActionsPanel: Locator;
  readonly selectAllCheckbox: Locator;
  readonly statsCards: Locator;
  readonly loadingSpinner: Locator;
  readonly emptyState: Locator;

  // Create/Edit Modal Elements
  readonly createModal: Locator;
  readonly editModal: Locator;
  readonly examTitleInput: Locator;
  readonly examDescriptionInput: Locator;
  readonly examCategorySelect: Locator;
  readonly examDurationInput: Locator;
  readonly maxAttemptsInput: Locator;
  readonly passingScoreInput: Locator;
  readonly startDateInput: Locator;
  readonly endDateInput: Locator;
  readonly instructionsInput: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;

  // Settings checkboxes
  readonly allowReviewCheckbox: Locator;
  readonly showResultsCheckbox: Locator;
  readonly timeLimitCheckbox: Locator;
  readonly randomizeQuestionsCheckbox: Locator;

  constructor(page: Page) {
    this.page = page;
    this.pageTitle = page.locator('h1').or(page.locator('[data-testid="page-title"]'));
    this.createExamButton = page.getByRole('button', { name: '创建试卷' }).or(page.locator('[data-testid="create-exam"]'));
    this.searchInput = page.getByPlaceholder('搜索试卷').or(page.locator('[data-testid="search-input"]'));
    this.statusFilter = page.locator('select').first().or(page.locator('[data-testid="status-filter"]'));
    this.categoryFilter = page.locator('select').nth(1).or(page.locator('[data-testid="category-filter"]'));
    this.examList = page.locator('[data-testid="exam-list"], .exam-list');
    this.examCards = page.locator('[data-testid="exam-card"], .exam-card');
    this.editButtons = page.getByRole('button', { name: '编辑' });
    this.deleteButtons = page.getByRole('button', { name: '删除' });
    this.publishButtons = page.getByRole('button', { name: '发布' });
    this.previewButtons = page.getByRole('button', { name: '预览' });
    this.bulkActionsPanel = page.locator('[data-testid="bulk-actions"], .bulk-actions');
    this.selectAllCheckbox = page.locator('[data-testid="select-all"], input[type="checkbox"]').first();
    this.statsCards = page.locator('[data-testid="stats-cards"], .stats-cards');
    this.loadingSpinner = page.locator('[data-testid="loading"], .loading');
    this.emptyState = page.locator('[data-testid="empty-state"], .empty-state');

    // Modal elements
    this.createModal = page.locator('[data-testid="create-exam-modal"], .create-exam-modal');
    this.editModal = page.locator('[data-testid="edit-exam-modal"], .edit-exam-modal');
    this.examTitleInput = page.getByLabel('试卷标题').or(page.locator('[data-testid="exam-title-input"]'));
    this.examDescriptionInput = page.getByLabel('试卷描述').or(page.locator('[data-testid="exam-description-input"]'));
    this.examCategorySelect = page.getByLabel('试卷分类').or(page.locator('[data-testid="exam-category-select"]'));
    this.examDurationInput = page.getByLabel('考试时长').or(page.locator('[data-testid="exam-duration-input"]'));
    this.maxAttemptsInput = page.getByLabel('最大尝试次数').or(page.locator('[data-testid="max-attempts-input"]'));
    this.passingScoreInput = page.getByLabel('及格分数').or(page.locator('[data-testid="passing-score-input"]'));
    this.startDateInput = page.getByLabel('开始时间').or(page.locator('[data-testid="start-date-input"]'));
    this.endDateInput = page.getByLabel('结束时间').or(page.locator('[data-testid="end-date-input"]'));
    this.instructionsInput = page.getByLabel('考试说明').or(page.locator('[data-testid="instructions-input"]'));
    this.saveButton = page.getByRole('button', { name: '保存' }).or(page.locator('[data-testid="save-button"]'));
    this.cancelButton = page.getByRole('button', { name: '取消' }).or(page.locator('[data-testid="cancel-button"]'));

    // Settings
    this.allowReviewCheckbox = page.getByLabel('允许回顾题目').or(page.locator('[data-testid="allow-review"]'));
    this.showResultsCheckbox = page.getByLabel('显示考试结果').or(page.locator('[data-testid="show-results"]'));
    this.timeLimitCheckbox = page.getByLabel('启用时间限制').or(page.locator('[data-testid="time-limit"]'));
    this.randomizeQuestionsCheckbox = page.getByLabel('随机排列题目').or(page.locator('[data-testid="randomize-questions"]'));
  }

  async goto() {
    await this.page.goto('/admin/exams');
    await this.waitForLoad();
  }

  async waitForLoad() {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.createExamButton).toBeVisible();
    // Wait for either exam list or empty state
    await Promise.race([
      expect(this.examList).toBeVisible(),
      expect(this.emptyState).toBeVisible()
    ]);
  }

  async getStatsData(): Promise<{
    totalExams: number;
    published: number;
    drafts: number;
    averageScore: number;
  }> {
    const cards = this.statsCards.locator('.card');
    
    const totalExamsText = await cards.nth(0).locator('.stat-value').textContent() || '0';
    const publishedText = await cards.nth(1).locator('.stat-value').textContent() || '0';
    const draftsText = await cards.nth(2).locator('.stat-value').textContent() || '0';
    const averageScoreText = await cards.nth(3).locator('.stat-value').textContent() || '0';
    
    return {
      totalExams: parseInt(totalExamsText.replace(/[^\d]/g, '')),
      published: parseInt(publishedText.replace(/[^\d]/g, '')),
      drafts: parseInt(draftsText.replace(/[^\d]/g, '')),
      averageScore: parseFloat(averageScoreText.replace(/[^\d.]/g, ''))
    };
  }

  async searchExams(query: string) {
    await this.searchInput.fill(query);
    await this.page.waitForTimeout(500); // Wait for debounced search
  }

  async filterByStatus(status: string) {
    await this.statusFilter.selectOption(status);
    await this.page.waitForTimeout(500);
  }

  async filterByCategory(category: string) {
    await this.categoryFilter.selectOption(category);
    await this.page.waitForTimeout(500);
  }

  async getExamCount(): Promise<number> {
    return await this.examCards.count();
  }

  async getExamInfo(index: number): Promise<{
    title: string;
    status: string;
    category: string;
    duration: string;
    questions: number;
    participants: number;
  }> {
    const card = this.examCards.nth(index);
    
    const title = await card.locator('.exam-title').textContent() || '';
    const status = await card.locator('.exam-status').textContent() || '';
    const category = await card.locator('.exam-category').textContent() || '';
    const duration = await card.locator('.exam-duration').textContent() || '';
    const questionsText = await card.locator('.exam-questions').textContent() || '0';
    const participantsText = await card.locator('.exam-participants').textContent() || '0';
    
    return {
      title,
      status,
      category,
      duration,
      questions: parseInt(questionsText.replace(/[^\d]/g, '')),
      participants: parseInt(participantsText.replace(/[^\d]/g, ''))
    };
  }

  async openCreateExamModal() {
    await this.createExamButton.click();
    await expect(this.createModal).toBeVisible();
  }

  async createExam(examData: {
    title: string;
    description: string;
    category: string;
    duration: number;
    maxAttempts?: number;
    passingScore?: number;
    startDate?: string;
    endDate?: string;
    instructions?: string;
    settings?: {
      allowReview?: boolean;
      showResults?: boolean;
      timeLimit?: boolean;
      randomizeQuestions?: boolean;
    };
  }) {
    await this.openCreateExamModal();
    
    // Step 1: Basic Information
    await this.examTitleInput.fill(examData.title);
    await this.examDescriptionInput.fill(examData.description);
    await this.examCategorySelect.selectOption(examData.category);
    await this.examDurationInput.fill(examData.duration.toString());
    
    if (examData.instructions) {
      await this.instructionsInput.fill(examData.instructions);
    }
    
    // Go to next step
    await this.page.getByRole('button', { name: '下一步' }).click();
    
    // Step 2: Time Settings
    if (examData.startDate) {
      await this.startDateInput.fill(examData.startDate);
    }
    
    if (examData.endDate) {
      await this.endDateInput.fill(examData.endDate);
    }
    
    // Go to next step
    await this.page.getByRole('button', { name: '下一步' }).click();
    
    // Step 3: Exam Settings
    if (examData.maxAttempts) {
      await this.maxAttemptsInput.fill(examData.maxAttempts.toString());
    }
    
    if (examData.passingScore) {
      await this.passingScoreInput.fill(examData.passingScore.toString());
    }
    
    // Configure settings
    if (examData.settings) {
      if (examData.settings.allowReview !== undefined) {
        await this.toggleCheckbox(this.allowReviewCheckbox, examData.settings.allowReview);
      }
      
      if (examData.settings.showResults !== undefined) {
        await this.toggleCheckbox(this.showResultsCheckbox, examData.settings.showResults);
      }
      
      if (examData.settings.timeLimit !== undefined) {
        await this.toggleCheckbox(this.timeLimitCheckbox, examData.settings.timeLimit);
      }
      
      if (examData.settings.randomizeQuestions !== undefined) {
        await this.toggleCheckbox(this.randomizeQuestionsCheckbox, examData.settings.randomizeQuestions);
      }
    }
    
    // Submit form
    await this.saveButton.click();
    
    // Wait for modal to close and list to refresh
    await expect(this.createModal).not.toBeVisible();
    await this.page.waitForTimeout(1000);
  }

  private async toggleCheckbox(checkbox: Locator, value: boolean) {
    const isChecked = await checkbox.isChecked();
    if (isChecked !== value) {
      await checkbox.click();
    }
  }

  async editExam(examIndex: number) {
    const editButton = this.examCards.nth(examIndex).locator('button', { hasText: '编辑' });
    await editButton.click();
    await expect(this.editModal).toBeVisible();
  }

  async deleteExam(examIndex: number) {
    const deleteButton = this.examCards.nth(examIndex).locator('button', { hasText: '删除' });
    await deleteButton.click();
    
    // Confirm deletion
    const confirmButton = this.page.getByRole('button', { name: '确认删除' });
    await confirmButton.click();
    
    // Wait for deletion to complete
    await this.page.waitForTimeout(1000);
  }

  async publishExam(examIndex: number) {
    const publishButton = this.examCards.nth(examIndex).locator('button', { hasText: '发布' });
    await publishButton.click();
    
    // Confirm publication
    const confirmButton = this.page.getByRole('button', { name: '确认发布' });
    await confirmButton.click();
    
    // Wait for publication to complete
    await this.page.waitForTimeout(1000);
  }

  async previewExam(examIndex: number) {
    const previewButton = this.examCards.nth(examIndex).locator('button', { hasText: '预览' });
    
    // Open in new tab
    const [newTab] = await Promise.all([
      this.page.waitForEvent('popup'),
      previewButton.click()
    ]);
    
    return newTab;
  }

  async selectExam(examIndex: number) {
    const checkbox = this.examCards.nth(examIndex).locator('input[type="checkbox"]');
    await checkbox.check();
  }

  async selectAllExams() {
    await this.selectAllCheckbox.check();
  }

  async getSelectedExamCount(): Promise<number> {
    const selectedCheckboxes = this.examCards.locator('input[type="checkbox"]:checked');
    return await selectedCheckboxes.count();
  }

  async bulkPublishExams() {
    await expect(this.bulkActionsPanel).toBeVisible();
    const bulkPublishButton = this.bulkActionsPanel.getByRole('button', { name: '批量发布' });
    await bulkPublishButton.click();
    
    // Confirm bulk action
    const confirmButton = this.page.getByRole('button', { name: '确认批量发布' });
    await confirmButton.click();
    
    await this.page.waitForTimeout(2000);
  }

  async bulkDeleteExams() {
    await expect(this.bulkActionsPanel).toBeVisible();
    const bulkDeleteButton = this.bulkActionsPanel.getByRole('button', { name: '批量删除' });
    await bulkDeleteButton.click();
    
    // Confirm bulk action
    const confirmButton = this.page.getByRole('button', { name: '确认批量删除' });
    await confirmButton.click();
    
    await this.page.waitForTimeout(2000);
  }

  async expectExamExists(title: string) {
    const examCard = this.examCards.filter({ hasText: title });
    await expect(examCard).toBeVisible();
  }

  async expectExamNotExists(title: string) {
    const examCard = this.examCards.filter({ hasText: title });
    await expect(examCard).not.toBeVisible();
  }

  async expectEmptyState() {
    await expect(this.emptyState).toBeVisible();
    await expect(this.examCards).toHaveCount(0);
  }

  async waitForLoadingToFinish() {
    await expect(this.loadingSpinner).not.toBeVisible({ timeout: 10000 });
  }
}