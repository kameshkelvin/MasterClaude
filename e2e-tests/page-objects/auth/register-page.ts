import { Page, Locator, expect } from '@playwright/test';

export class RegisterPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly phoneInput: Locator;
  readonly registerButton: Locator;
  readonly termsCheckbox: Locator;
  readonly privacyCheckbox: Locator;
  readonly loginLink: Locator;
  readonly errorMessage: Locator;
  readonly successMessage: Locator;
  readonly loadingSpinner: Locator;
  readonly registerForm: Locator;
  readonly passwordStrengthMeter: Locator;
  readonly showPasswordButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.getByLabel('用户名').or(page.locator('[data-testid="username-input"]'));
    this.emailInput = page.getByLabel('邮箱').or(page.locator('[data-testid="email-input"]'));
    this.passwordInput = page.getByLabel('密码', { exact: true }).or(page.locator('[data-testid="password-input"]'));
    this.confirmPasswordInput = page.getByLabel('确认密码').or(page.locator('[data-testid="confirm-password-input"]'));
    this.firstNameInput = page.getByLabel('姓').or(page.locator('[data-testid="first-name-input"]'));
    this.lastNameInput = page.getByLabel('名').or(page.locator('[data-testid="last-name-input"]'));
    this.phoneInput = page.getByLabel('手机号').or(page.locator('[data-testid="phone-input"]'));
    this.registerButton = page.getByRole('button', { name: '注册' }).or(page.locator('[data-testid="register-button"]'));
    this.termsCheckbox = page.getByLabel('我同意服务条款').or(page.locator('[data-testid="terms-checkbox"]'));
    this.privacyCheckbox = page.getByLabel('我同意隐私政策').or(page.locator('[data-testid="privacy-checkbox"]'));
    this.loginLink = page.getByText('已有账户？登录').or(page.locator('[data-testid="login-link"]'));
    this.errorMessage = page.locator('.error-message, .alert-error, [data-testid="error-message"]');
    this.successMessage = page.locator('.success-message, .alert-success, [data-testid="success-message"]');
    this.loadingSpinner = page.locator('.loading, .spinner, [data-testid="loading"]');
    this.registerForm = page.locator('form').or(page.locator('[data-testid="register-form"]'));
    this.passwordStrengthMeter = page.locator('[data-testid="password-strength"], .password-strength');
    this.showPasswordButton = page.locator('[data-testid="show-password"], .show-password-btn');
  }

  async goto() {
    await this.page.goto('/register');
    await this.waitForLoad();
  }

  async waitForLoad() {
    await expect(this.registerForm).toBeVisible();
    await expect(this.usernameInput).toBeVisible();
    await expect(this.emailInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
  }

  async register(userData: {
    username: string;
    email: string;
    password: string;
    confirmPassword?: string;
    firstName?: string;
    lastName?: string;
    phone?: string;
    acceptTerms?: boolean;
    acceptPrivacy?: boolean;
  }) {
    await this.usernameInput.fill(userData.username);
    await this.emailInput.fill(userData.email);
    await this.passwordInput.fill(userData.password);
    
    if (userData.confirmPassword !== undefined) {
      await this.confirmPasswordInput.fill(userData.confirmPassword);
    } else {
      await this.confirmPasswordInput.fill(userData.password);
    }

    if (userData.firstName) {
      await this.firstNameInput.fill(userData.firstName);
    }

    if (userData.lastName) {
      await this.lastNameInput.fill(userData.lastName);
    }

    if (userData.phone) {
      await this.phoneInput.fill(userData.phone);
    }

    // Accept terms and privacy policy
    if (userData.acceptTerms !== false) {
      await this.termsCheckbox.check();
    }

    if (userData.acceptPrivacy !== false) {
      await this.privacyCheckbox.check();
    }

    // Wait for any validation to complete
    await this.page.waitForTimeout(500);

    await this.registerButton.click();
  }

  async registerAndWaitForSuccess(userData: any) {
    await this.register(userData);
    await this.expectRegistrationSuccess();
  }

  async expectRegistrationSuccess() {
    await Promise.race([
      expect(this.successMessage).toBeVisible(),
      expect(this.page).toHaveURL(/login|verify/),
    ]);
  }

  async expectRegistrationError(errorText?: string) {
    await expect(this.errorMessage).toBeVisible();
    if (errorText) {
      await expect(this.errorMessage).toContainText(errorText);
    }
  }

  async getPasswordStrength(): Promise<string> {
    if (await this.passwordStrengthMeter.isVisible()) {
      return await this.passwordStrengthMeter.textContent() || '';
    }
    return '';
  }

  async getValidationErrors(): Promise<string[]> {
    const errors: string[] = [];
    
    const fieldErrors = [
      { locator: '[data-testid="username-error"], .username-error', field: 'username' },
      { locator: '[data-testid="email-error"], .email-error', field: 'email' }, 
      { locator: '[data-testid="password-error"], .password-error', field: 'password' },
      { locator: '[data-testid="confirm-password-error"], .confirm-password-error', field: 'confirmPassword' },
      { locator: '[data-testid="first-name-error"], .first-name-error', field: 'firstName' },
      { locator: '[data-testid="last-name-error"], .last-name-error', field: 'lastName' },
      { locator: '[data-testid="phone-error"], .phone-error', field: 'phone' }
    ];

    for (const { locator, field } of fieldErrors) {
      const errorElement = this.page.locator(locator);
      if (await errorElement.isVisible()) {
        const text = await errorElement.textContent();
        errors.push(`${field}: ${text}`);
      }
    }
    
    return errors;
  }

  async clearForm() {
    await this.usernameInput.clear();
    await this.emailInput.clear();
    await this.passwordInput.clear();
    await this.confirmPasswordInput.clear();
    await this.firstNameInput.clear();
    await this.lastNameInput.clear();
    await this.phoneInput.clear();
    
    if (await this.termsCheckbox.isChecked()) {
      await this.termsCheckbox.uncheck();
    }
    
    if (await this.privacyCheckbox.isChecked()) {
      await this.privacyCheckbox.uncheck();
    }
  }

  async isFormValid(): Promise<boolean> {
    const username = await this.usernameInput.inputValue();
    const email = await this.emailInput.inputValue();
    const password = await this.passwordInput.inputValue();
    const confirmPassword = await this.confirmPasswordInput.inputValue();
    const termsAccepted = await this.termsCheckbox.isChecked();
    const privacyAccepted = await this.privacyCheckbox.isChecked();

    return username.length > 0 && 
           email.length > 0 && 
           password.length > 0 && 
           password === confirmPassword &&
           termsAccepted &&
           privacyAccepted;
  }

  async togglePasswordVisibility() {
    await this.showPasswordButton.click();
  }

  async checkPasswordsMatch(): Promise<boolean> {
    const password = await this.passwordInput.inputValue();
    const confirmPassword = await this.confirmPasswordInput.inputValue();
    return password === confirmPassword;
  }
}