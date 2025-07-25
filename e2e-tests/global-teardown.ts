import { chromium, FullConfig } from '@playwright/test';

async function globalTeardown(config: FullConfig) {
  console.log('🧹 Global teardown started...');
  
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Clean up test data
    console.log('🗑️ Cleaning up test data...');
    
    // Clean up test users (keep admin accounts)
    await page.request.delete('/api/v1/test/cleanup/users', {
      data: { keepAdmins: true }
    });
    
    // Clean up test exams
    await page.request.delete('/api/v1/test/cleanup/exams');
    
    // Clean up test questions
    await page.request.delete('/api/v1/test/cleanup/questions');
    
    // Clean up uploaded files
    await page.request.delete('/api/v1/test/cleanup/files');
    
    console.log('✅ Global teardown completed');
  } catch (error) {
    console.error('❌ Global teardown failed:', error);
    // Don't throw error to prevent test failure
  } finally {
    await browser.close();
  }
}

export default globalTeardown;