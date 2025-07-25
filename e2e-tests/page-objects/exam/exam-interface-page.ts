import { Page, Locator, expect } from '@playwright/test';

export class ExamInterfacePage {
  readonly page: Page;
  readonly examTitle: Locator;
  readonly examDescription: Locator;
  readonly timerDisplay: Locator;
  readonly progressBar: Locator;
  readonly questionNumber: Locator;
  readonly questionContent: Locator;
  readonly questionOptions: Locator;
  readonly nextButton: Locator;
  readonly previousButton: Locator;
  readonly submitButton: Locator;
  readonly reviewButton: Locator;
  readonly questionNavigator: Locator;
  readonly warningModal: Locator;
  readonly confirmSubmitModal: Locator;
  readonly examSidebar: Locator;
  readonly fullscreenButton: Locator;
  readonly helpButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.examTitle = page.locator('[data-testid="exam-title"], .exam-title');
    this.examDescription = page.locator('[data-testid="exam-description"], .exam-description');
    this.timerDisplay = page.locator('[data-testid="exam-timer"], .timer-display');
    this.progressBar = page.locator('[data-testid="progress-bar"], .progress-bar');
    this.questionNumber = page.locator('[data-testid="question-number"], .question-number');
    this.questionContent = page.locator('[data-testid="question-content"], .question-content');
    this.questionOptions = page.locator('[data-testid="question-options"], .question-options');
    this.nextButton = page.getByRole('button', { name: '下一题' }).or(page.locator('[data-testid="next-question"]'));
    this.previousButton = page.getByRole('button', { name: '上一题' }).or(page.locator('[data-testid="previous-question"]'));
    this.submitButton = page.getByRole('button', { name: '提交考试' }).or(page.locator('[data-testid="submit-exam"]'));
    this.reviewButton = page.getByRole('button', { name: '查看题目' }).or(page.locator('[data-testid="review-questions"]'));
    this.questionNavigator = page.locator('[data-testid="question-navigator"], .question-navigator');
    this.warningModal = page.locator('[data-testid="warning-modal"], .warning-modal');
    this.confirmSubmitModal = page.locator('[data-testid="confirm-submit-modal"], .confirm-submit-modal');
    this.examSidebar = page.locator('[data-testid="exam-sidebar"], .exam-sidebar');
    this.fullscreenButton = page.locator('[data-testid="fullscreen-btn"], .fullscreen-btn');
    this.helpButton = page.locator('[data-testid="help-btn"], .help-btn');
  }

  async waitForExamLoad() {
    await expect(this.examTitle).toBeVisible();
    await expect(this.questionContent).toBeVisible();
    await expect(this.timerDisplay).toBeVisible();
  }

  async getExamTitle(): Promise<string> {
    return await this.examTitle.textContent() || '';
  }

  async getCurrentQuestionNumber(): Promise<number> {
    const text = await this.questionNumber.textContent() || '1';
    const match = text.match(/\d+/);
    return match ? parseInt(match[0]) : 1;
  }

  async getTotalQuestions(): Promise<number> {
    const text = await this.questionNumber.textContent() || '1/1';
    const match = text.match(/\/(\d+)/);
    return match ? parseInt(match[1]) : 1;
  }

  async getRemainingTime(): Promise<string> {
    return await this.timerDisplay.textContent() || '00:00';
  }

  async getProgressPercentage(): Promise<number> {
    const progressElement = this.progressBar.locator('[role="progressbar"]');
    const ariaValueNow = await progressElement.getAttribute('aria-valuenow');
    return ariaValueNow ? parseInt(ariaValueNow) : 0;
  }

  async getCurrentQuestion(): Promise<string> {
    return await this.questionContent.textContent() || '';
  }

  async selectSingleChoiceOption(optionIndex: number) {
    const option = this.questionOptions.locator(`input[type="radio"]`).nth(optionIndex);
    await option.check();
  }

  async selectMultipleChoiceOptions(optionIndexes: number[]) {
    for (const index of optionIndexes) {
      const option = this.questionOptions.locator(`input[type="checkbox"]`).nth(index);
      await option.check();
    }
  }

  async fillTextAnswer(text: string) {
    const textInput = this.questionOptions.locator('input[type="text"], textarea');
    await textInput.fill(text);
  }

  async selectTrueFalse(value: boolean) {
    const selector = value ? 'input[value="true"]' : 'input[value="false"]';
    await this.questionOptions.locator(selector).check();
  }

  async goToNextQuestion() {
    await this.nextButton.click();
    // Wait for question to load
    await this.page.waitForTimeout(500);
  }

  async goToPreviousQuestion() {
    await this.previousButton.click();
    // Wait for question to load
    await this.page.waitForTimeout(500);
  }

  async goToQuestionByNumber(questionNumber: number) {
    const questionButton = this.questionNavigator.locator(`button`).nth(questionNumber - 1);
    await questionButton.click();
    await this.page.waitForTimeout(500);
  }

  async isNextButtonEnabled(): Promise<boolean> {
    return await this.nextButton.isEnabled();
  }

  async isPreviousButtonEnabled(): Promise<boolean> {
    return await this.previousButton.isEnabled();
  }

  async openReviewMode() {
    await this.reviewButton.click();
    await expect(this.questionNavigator).toBeVisible();
  }

  async getAnsweredQuestions(): Promise<number[]> {
    const answeredQuestions: number[] = [];
    const questionButtons = this.questionNavigator.locator('button');
    const count = await questionButtons.count();
    
    for (let i = 0; i < count; i++) {
      const button = questionButtons.nth(i);
      const isAnswered = await button.getAttribute('data-answered') === 'true';
      if (isAnswered) {
        answeredQuestions.push(i + 1);
      }
    }
    
    return answeredQuestions;
  }

  async getUnansweredQuestions(): Promise<number[]> {
    const unansweredQuestions: number[] = [];
    const questionButtons = this.questionNavigator.locator('button');
    const count = await questionButtons.count();
    
    for (let i = 0; i < count; i++) {
      const button = questionButtons.nth(i);
      const isAnswered = await button.getAttribute('data-answered') === 'true';
      if (!isAnswered) {
        unansweredQuestions.push(i + 1);
      }
    }
    
    return unansweredQuestions;
  }

  async submitExam() {
    await this.submitButton.click();
    
    // Handle confirmation modal
    if (await this.confirmSubmitModal.isVisible()) {
      const confirmButton = this.confirmSubmitModal.getByRole('button', { name: '确认提交' });
      await confirmButton.click();
    }
    
    // Wait for submission to complete
    await expect(this.page).toHaveURL(/results|submitted/, { timeout: 30000 });
  }

  async attemptToLeaveExam() {
    // Try to navigate away or close tab
    await this.page.goBack();
  }

  async expectWarningModal() {
    await expect(this.warningModal).toBeVisible();
  }

  async dismissWarning() {
    const dismissButton = this.warningModal.getByRole('button', { name: '知道了' });
    await dismissButton.click();
  }

  async toggleFullscreen() {
    await this.fullscreenButton.click();
  }

  async isInFullscreen(): Promise<boolean> {
    return await this.page.evaluate(() => {
      return document.fullscreenElement !== null;
    });
  }

  async expectExamSecurityWarning() {
    // Check for security-related warnings (tab switching, fullscreen exit, etc.)
    const securityWarning = this.page.locator('[data-testid="security-warning"], .security-warning');
    await expect(securityWarning).toBeVisible();
  }

  async getExamInstructions(): Promise<string> {
    const instructions = this.page.locator('[data-testid="exam-instructions"], .exam-instructions');
    return await instructions.textContent() || '';
  }

  async startExam() {
    const startButton = this.page.getByRole('button', { name: '开始考试' });
    await startButton.click();
    await this.waitForExamLoad();
  }

  async pauseExam() {
    const pauseButton = this.page.getByRole('button', { name: '暂停' });
    if (await pauseButton.isVisible()) {
      await pauseButton.click();
    }
  }

  async resumeExam() {
    const resumeButton = this.page.getByRole('button', { name: '继续' });
    if (await resumeButton.isVisible()) {
      await resumeButton.click();
    }
  }

  async autoSaveProgress() {
    // Trigger auto-save by waiting or performing an action
    await this.page.waitForTimeout(5000); // Auto-save typically happens every 5 seconds
  }

  async verifyAutoSave() {
    const saveIndicator = this.page.locator('[data-testid="auto-save-indicator"], .auto-save-indicator');
    await expect(saveIndicator).toContainText('已保存');
  }
}