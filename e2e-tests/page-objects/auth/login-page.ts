import { Page, Locator, expect } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly rememberMeCheckbox: Locator;
  readonly forgotPasswordLink: Locator;
  readonly registerLink: Locator;
  readonly errorMessage: Locator;
  readonly successMessage: Locator;
  readonly loadingSpinner: Locator;
  readonly showPasswordButton: Locator;
  readonly loginForm: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.getByLabel('用户名').or(page.locator('[data-testid="username-input"]'));
    this.passwordInput = page.getByLabel('密码').or(page.locator('[data-testid="password-input"]'));
    this.loginButton = page.getByRole('button', { name: '登录' }).or(page.locator('[data-testid="login-button"]'));
    this.rememberMeCheckbox = page.getByLabel('记住我').or(page.locator('[data-testid="remember-me"]'));
    this.forgotPasswordLink = page.getByText('忘记密码').or(page.locator('[data-testid="forgot-password"]'));
    this.registerLink = page.getByText('注册账户').or(page.locator('[data-testid="register-link"]'));
    this.errorMessage = page.locator('.error-message, .alert-error, [data-testid="error-message"]');
    this.successMessage = page.locator('.success-message, .alert-success, [data-testid="success-message"]');
    this.loadingSpinner = page.locator('.loading, .spinner, [data-testid="loading"]');
    this.showPasswordButton = page.locator('[data-testid="show-password"], .show-password-btn');
    this.loginForm = page.locator('form').or(page.locator('[data-testid="login-form"]'));
  }

  async goto() {
    await this.page.goto('/login');
    await this.waitForLoad();
  }

  async waitForLoad() {
    await expect(this.loginForm).toBeVisible();
    await expect(this.usernameInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
    await expect(this.loginButton).toBeVisible();
  }

  async login(username: string, password: string, rememberMe: boolean = false) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    
    if (rememberMe) {
      await this.rememberMeCheckbox.check();
    }
    
    // Wait for any validation to complete
    await this.page.waitForTimeout(500);
    
    await this.loginButton.click();
  }

  async loginAndWaitForRedirect(username: string, password: string, expectedUrl?: string) {
    await this.login(username, password);
    
    // Wait for navigation or success
    await Promise.race([
      this.page.waitForURL(expectedUrl || '**/dashboard**', { timeout: 10000 }),
      this.page.waitForURL('**/admin/dashboard**', { timeout: 10000 }),
      this.successMessage.waitFor({ timeout: 5000 })
    ]);
  }

  async expectLoginSuccess() {
    // Wait for either redirect or success message
    await Promise.race([
      expect(this.page).toHaveURL(/dashboard/),
      expect(this.successMessage).toBeVisible()
    ]);
  }

  async expectLoginError(errorText?: string) {
    await expect(this.errorMessage).toBeVisible();
    if (errorText) {
      await expect(this.errorMessage).toContainText(errorText);
    }
  }

  async togglePasswordVisibility() {
    await this.showPasswordButton.click();
  }

  async isPasswordVisible(): Promise<boolean> {
    const type = await this.passwordInput.getAttribute('type');
    return type === 'text';
  }

  async clearForm() {
    await this.usernameInput.clear();
    await this.passwordInput.clear();
    if (await this.rememberMeCheckbox.isChecked()) {
      await this.rememberMeCheckbox.uncheck();
    }
  }

  async fillUsername(username: string) {
    await this.usernameInput.fill(username);
  }

  async fillPassword(password: string) {
    await this.passwordInput.fill(password);
  }

  async submitForm() {
    await this.loginButton.click();
  }

  async isFormValid(): Promise<boolean> {
    const username = await this.usernameInput.inputValue();
    const password = await this.passwordInput.inputValue();
    return username.length > 0 && password.length > 0;
  }

  async getValidationErrors(): Promise<string[]> {
    const errors: string[] = [];
    
    // Check for field-specific validation errors
    const usernameError = this.page.locator('[data-testid="username-error"], .username-error');
    const passwordError = this.page.locator('[data-testid="password-error"], .password-error');
    
    if (await usernameError.isVisible()) {
      errors.push(await usernameError.textContent() || 'Username error');
    }
    
    if (await passwordError.isVisible()) {
      errors.push(await passwordError.textContent() || 'Password error');
    }
    
    return errors;
  }

  async isLoadingVisible(): Promise<boolean> {
    return await this.loadingSpinner.isVisible();
  }

  async waitForLoading() {
    await expect(this.loadingSpinner).toBeVisible();
    await expect(this.loadingSpinner).not.toBeVisible({ timeout: 10000 });
  }
}