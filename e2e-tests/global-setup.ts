import { chromium, FullConfig } from '@playwright/test';
import { TestDataSeeder } from './utils/test-data-seeder';

async function globalSetup(config: FullConfig) {
  console.log('🚀 Global setup started...');
  
  // Launch browser for setup
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Wait for backend services to be ready
    console.log('⏳ Waiting for backend services...');
    await waitForService('http://localhost:8080/health', 120000);
    await waitForService('http://localhost:3000', 60000);

    // Seed test data
    console.log('🌱 Seeding test data...');
    const seeder = new TestDataSeeder(page);
    await seeder.seedTestUsers();
    await seeder.seedTestExams();
    await seeder.seedTestQuestions();

    console.log('✅ Global setup completed');
  } catch (error) {
    console.error('❌ Global setup failed:', error);
    throw error;
  } finally {
    await browser.close();
  }
}

async function waitForService(url: string, timeout: number = 60000): Promise<void> {
  const startTime = Date.now();
  
  while (Date.now() - startTime < timeout) {
    try {
      const response = await fetch(url);
      if (response.ok) {
        console.log(`✅ Service ready: ${url}`);
        return;
      }
    } catch (error) {
      // Service not ready yet, continue waiting
    }
    
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
  
  throw new Error(`Service not ready after ${timeout}ms: ${url}`);
}

export default globalSetup;