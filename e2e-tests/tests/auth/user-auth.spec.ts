import { test, expect } from '@playwright/test';
import { LoginPage } from '../../page-objects/auth/login-page';
import { RegisterPage } from '../../page-objects/auth/register-page';
import { TEST_USERS, INVALID_USERS } from '../../fixtures/test-users';

test.describe('用户注册和登录流程', () => {
  let loginPage: LoginPage;
  let registerPage: RegisterPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    registerPage = new RegisterPage(page);
  });

  test.describe('用户登录流程', () => {
    test('学生用户登录成功', async ({ page }) => {
      await loginPage.goto();
      
      await loginPage.loginAndWaitForRedirect(
        TEST_USERS.student.username,
        TEST_USERS.student.password,
        '**/dashboard'
      );
      
      // 验证登录成功
      await expect(page).toHaveURL(/dashboard/);
      await expect(page.locator('[data-testid="user-menu"], .user-menu')).toBeVisible();
      
      // 验证学生角色权限
      await expect(page.locator('[data-testid="exam-list"], .exam-list')).toBeVisible();
      await expect(page.locator('[data-testid="admin-menu"], .admin-menu')).not.toBeVisible();
    });

    test('教师用户登录成功', async ({ page }) => {
      await loginPage.goto();
      
      await loginPage.loginAndWaitForRedirect(
        TEST_USERS.teacher.username,
        TEST_USERS.teacher.password,
        '**/dashboard'
      );
      
      // 验证登录成功
      await expect(page).toHaveURL(/dashboard/);
      
      // 验证教师角色权限
      await expect(page.locator('[data-testid="teacher-tools"], .teacher-tools')).toBeVisible();
      await expect(page.locator('[data-testid="create-exam-btn"], .create-exam-btn')).toBeVisible();
    });

    test('管理员用户登录成功', async ({ page }) => {
      await loginPage.goto();
      
      await loginPage.loginAndWaitForRedirect(
        TEST_USERS.admin.username,
        TEST_USERS.admin.password,
        '**/admin/dashboard'
      );
      
      // 验证管理员登录成功
      await expect(page).toHaveURL(/admin.*dashboard/);
      
      // 验证管理员权限
      await expect(page.locator('[data-testid="admin-menu"], .admin-menu')).toBeVisible();
      await expect(page.locator('[data-testid="user-management"], .user-management')).toBeVisible();
    });

    test('错误用户名登录失败', async () => {
      await loginPage.goto();
      
      await loginPage.login('nonexistent_user', 'wrongpassword');
      
      await loginPage.expectLoginError('用户名或密码错误');
      await expect(loginPage.page).toHaveURL(/login/);
    });

    test('错误密码登录失败', async () => {
      await loginPage.goto();
      
      await loginPage.login(TEST_USERS.student.username, 'wrongpassword');
      
      await loginPage.expectLoginError('用户名或密码错误');
      await expect(loginPage.page).toHaveURL(/login/);
    });

    test('空用户名和密码验证', async () => {
      await loginPage.goto();
      
      await loginPage.login('', '');
      
      const errors = await loginPage.getValidationErrors();
      expect(errors.length).toBeGreaterThan(0);
      expect(errors.some(error => error.includes('用户名'))).toBeTruthy();
      expect(errors.some(error => error.includes('密码'))).toBeTruthy();
    });

    test('记住我功能', async ({ page }) => {
      await loginPage.goto();
      
      await loginPage.login(TEST_USERS.student.username, TEST_USERS.student.password, true);
      await loginPage.expectLoginSuccess();
      
      // 验证记住我状态存储
      const rememberToken = await page.evaluate(() => localStorage.getItem('rememberMe'));
      expect(rememberToken).toBeTruthy();
    });

    test('密码可见性切换', async () => {
      await loginPage.goto();
      
      await loginPage.fillPassword('testpassword');
      
      // 初始状态密码应该被隐藏
      expect(await loginPage.isPasswordVisible()).toBeFalsy();
      
      // 切换密码可见性
      await loginPage.togglePasswordVisibility();
      expect(await loginPage.isPasswordVisible()).toBeTruthy();
      
      // 再次切换
      await loginPage.togglePasswordVisibility();
      expect(await loginPage.isPasswordVisible()).toBeFalsy();
    });

    test('登录表单验证', async () => {
      await loginPage.goto();
      
      // 测试表单验证状态
      expect(await loginPage.isFormValid()).toBeFalsy();
      
      await loginPage.fillUsername(TEST_USERS.student.username);
      expect(await loginPage.isFormValid()).toBeFalsy();
      
      await loginPage.fillPassword(TEST_USERS.student.password);
      expect(await loginPage.isFormValid()).toBeTruthy();
    });
  });

  test.describe('用户注册流程', () => {
    test('新用户注册成功', async ({ page }) => {
      await registerPage.goto();
      
      const newUser = {
        username: `newuser_${Date.now()}`,
        email: `newuser_${Date.now()}@test.com`,
        password: 'NewUser123!',
        firstName: '新',
        lastName: '用户',
        phone: '13800138000'
      };
      
      await registerPage.registerAndWaitForSuccess(newUser);
      
      // 验证注册成功，跳转到验证或登录页面
      await expect(page).toHaveURL(/login|verify/);
    });

    test('重复用户名注册失败', async () => {
      await registerPage.goto();
      
      const existingUser = {
        username: TEST_USERS.student.username, // 使用已存在的用户名
        email: `new_${Date.now()}@test.com`,
        password: 'NewUser123!'
      };
      
      await registerPage.register(existingUser);
      
      await registerPage.expectRegistrationError('用户名已存在');
    });

    test('重复邮箱注册失败', async () => {
      await registerPage.goto();
      
      const duplicateEmailUser = {
        username: `newuser_${Date.now()}`,
        email: TEST_USERS.student.email, // 使用已存在的邮箱
        password: 'NewUser123!'
      };
      
      await registerPage.register(duplicateEmailUser);
      
      await registerPage.expectRegistrationError('邮箱已被使用');
    });

    test('弱密码注册失败', async () => {
      await registerPage.goto();
      
      await registerPage.register(INVALID_USERS.weakPassword);
      
      const errors = await registerPage.getValidationErrors();
      expect(errors.some(error => error.includes('密码强度'))).toBeTruthy();
    });

    test('无效邮箱格式注册失败', async () => {
      await registerPage.goto();
      
      await registerPage.register(INVALID_USERS.invalidEmail);
      
      const errors = await registerPage.getValidationErrors();
      expect(errors.some(error => error.includes('邮箱格式'))).toBeTruthy();
    });

    test('用户名长度验证', async () => {
      await registerPage.goto();
      
      await registerPage.register(INVALID_USERS.shortUsername);
      
      const errors = await registerPage.getValidationErrors();
      expect(errors.some(error => error.includes('用户名'))).toBeTruthy();
    });

    test('密码确认不匹配', async () => {
      await registerPage.goto();
      
      const userWithMismatchedPassword = {
        username: `testuser_${Date.now()}`,
        email: `testuser_${Date.now()}@test.com`,
        password: 'Password123!',
        confirmPassword: 'DifferentPassword123!'
      };
      
      await registerPage.register(userWithMismatchedPassword);
      
      const errors = await registerPage.getValidationErrors();
      expect(errors.some(error => error.includes('密码不匹配'))).toBeTruthy();
    });

    test('服务条款和隐私政策必须同意', async () => {
      await registerPage.goto();
      
      const userWithoutAgreement = {
        username: `testuser_${Date.now()}`,
        email: `testuser_${Date.now()}@test.com`,
        password: 'Password123!',
        acceptTerms: false,
        acceptPrivacy: false
      };
      
      await registerPage.register(userWithoutAgreement);
      
      // 验证无法提交或显示错误
      const errors = await registerPage.getValidationErrors();
      expect(errors.length).toBeGreaterThan(0);
    });

    test('密码强度指示器', async () => {
      await registerPage.goto();
      
      // 测试弱密码
      await registerPage.passwordInput.fill('123');
      let strength = await registerPage.getPasswordStrength();
      expect(strength).toContain('弱');
      
      // 测试中等密码
      await registerPage.passwordInput.fill('Password123');
      strength = await registerPage.getPasswordStrength();
      expect(strength).toContain('中等');
      
      // 测试强密码
      await registerPage.passwordInput.fill('SecurePassword123!@#');
      strength = await registerPage.getPasswordStrength();
      expect(strength).toContain('强');
    });

    test('注册表单清空功能', async () => {
      await registerPage.goto();
      
      // 填写表单
      await registerPage.usernameInput.fill('testuser');
      await registerPage.emailInput.fill('test@example.com');
      await registerPage.passwordInput.fill('password123');
      await registerPage.termsCheckbox.check();
      
      // 清空表单
      await registerPage.clearForm();
      
      // 验证表单已清空
      expect(await registerPage.usernameInput.inputValue()).toBe('');
      expect(await registerPage.emailInput.inputValue()).toBe('');
      expect(await registerPage.passwordInput.inputValue()).toBe('');
      expect(await registerPage.termsCheckbox.isChecked()).toBeFalsy();
    });

    test('注册表单验证状态', async () => {
      await registerPage.goto();
      
      // 初始状态应该无效
      expect(await registerPage.isFormValid()).toBeFalsy();
      
      // 逐步填写必填字段
      await registerPage.usernameInput.fill('validuser');
      expect(await registerPage.isFormValid()).toBeFalsy();
      
      await registerPage.emailInput.fill('valid@test.com');
      expect(await registerPage.isFormValid()).toBeFalsy();
      
      await registerPage.passwordInput.fill('ValidPassword123!');
      await registerPage.confirmPasswordInput.fill('ValidPassword123!');
      expect(await registerPage.isFormValid()).toBeFalsy();
      
      await registerPage.termsCheckbox.check();
      await registerPage.privacyCheckbox.check();
      expect(await registerPage.isFormValid()).toBeTruthy();
    });
  });

  test.describe('认证流程集成测试', () => {
    test('注册成功后立即登录', async ({ page }) => {
      const newUser = {
        username: `integrationuser_${Date.now()}`,
        email: `integrationuser_${Date.now()}@test.com`,
        password: 'Integration123!',
        firstName: '集成',
        lastName: '测试'
      };
      
      // 注册新用户
      await registerPage.goto();
      await registerPage.registerAndWaitForSuccess(newUser);
      
      // 如果跳转到登录页面，立即登录
      if (page.url().includes('/login')) {
        await loginPage.loginAndWaitForRedirect(newUser.username, newUser.password);
        await expect(page).toHaveURL(/dashboard/);
      }
    });

    test('登录页面和注册页面导航', async ({ page }) => {
      // 从登录页面导航到注册页面
      await loginPage.goto();
      await loginPage.registerLink.click();
      await expect(page).toHaveURL(/register/);
      
      // 从注册页面导航回登录页面
      await registerPage.loginLink.click();
      await expect(page).toHaveURL(/login/);
    });

    test('忘记密码功能链接', async ({ page }) => {
      await loginPage.goto();
      
      await loginPage.forgotPasswordLink.click();
      await expect(page).toHaveURL(/forgot-password|reset-password/);
    });
  });

  test.describe('安全性测试', () => {
    test('防止SQL注入攻击', async () => {
      await loginPage.goto();
      
      const sqlInjectionAttempts = [
        "admin'; DROP TABLE users; --",
        "admin' OR '1'='1",
        "admin' UNION SELECT * FROM users --"
      ];
      
      for (const maliciousInput of sqlInjectionAttempts) {
        await loginPage.login(maliciousInput, 'password');
        await loginPage.expectLoginError();
        await loginPage.clearForm();
      }
    });

    test('防止XSS攻击', async () => {
      await registerPage.goto();
      
      const xssPayload = '<script>alert("XSS")</script>';
      
      const userWithXSS = {
        username: xssPayload,
        email: 'xss@test.com',
        password: 'Password123!'
      };
      
      await registerPage.register(userWithXSS);
      
      // 验证XSS脚本没有被执行
      const alerts = page.locator('script');
      expect(await alerts.count()).toBe(0);
    });

    test('密码复杂度要求', async () => {
      await registerPage.goto();
      
      const weakPasswords = [
        '123456',
        'password',
        'qwerty',
        '111111',
        'abc123'
      ];
      
      for (const weakPassword of weakPasswords) {
        const user = {
          username: `testuser_${Date.now()}`,
          email: `test_${Date.now()}@test.com`,
          password: weakPassword
        };
        
        await registerPage.register(user);
        
        const errors = await registerPage.getValidationErrors();
        expect(errors.some(error => error.includes('密码'))).toBeTruthy();
        
        await registerPage.clearForm();
      }
    });
  });
});