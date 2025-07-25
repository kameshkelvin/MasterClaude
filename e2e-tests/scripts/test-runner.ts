import { spawn } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';

interface TestRunOptions {
  project?: string[];
  headed?: boolean;
  debug?: boolean;
  ui?: boolean;
  workers?: number;
  reporter?: string[];
  outputDir?: string;
  grep?: string;
  grepInvert?: string;
  maxFailures?: number;
  retries?: number;
  timeout?: number;
  updateSnapshots?: boolean;
  trace?: 'on' | 'off' | 'on-first-retry' | 'retain-on-failure';
}

export class TestRunner {
  private readonly baseDir: string;
  private readonly configFile: string;

  constructor() {
    this.baseDir = path.resolve(__dirname, '..');
    this.configFile = path.join(this.baseDir, 'playwright.config.ts');
  }

  async runTests(options: TestRunOptions = {}): Promise<void> {
    const args = this.buildPlaywrightArgs(options);
    
    console.log('🚀 Starting E2E Tests...');
    console.log('📋 Command:', `npx playwright test ${args.join(' ')}`);
    
    return new Promise((resolve, reject) => {
      const process = spawn('npx', ['playwright', 'test', ...args], {
        cwd: this.baseDir,
        stdio: 'inherit',
        shell: true
      });

      process.on('close', (code) => {
        if (code === 0) {
          console.log('✅ Tests completed successfully!');
          resolve();
        } else {
          console.log(`❌ Tests failed with exit code ${code}`);
          reject(new Error(`Tests failed with exit code ${code}`));
        }
      });

      process.on('error', (error) => {
        console.error('❌ Error running tests:', error);
        reject(error);
      });
    });
  }

  private buildPlaywrightArgs(options: TestRunOptions): string[] {
    const args: string[] = [];

    if (options.project && options.project.length > 0) {
      options.project.forEach(project => {
        args.push('--project', project);
      });
    }

    if (options.headed) {
      args.push('--headed');
    }

    if (options.debug) {
      args.push('--debug');
    }

    if (options.ui) {
      args.push('--ui');
    }

    if (options.workers !== undefined) {
      args.push('--workers', options.workers.toString());
    }

    if (options.reporter && options.reporter.length > 0) {
      options.reporter.forEach(reporter => {
        args.push('--reporter', reporter);
      });
    }

    if (options.outputDir) {
      args.push('--output', options.outputDir);
    }

    if (options.grep) {
      args.push('--grep', options.grep);
    }

    if (options.grepInvert) {
      args.push('--grep-invert', options.grepInvert);
    }

    if (options.maxFailures !== undefined) {
      args.push('--max-failures', options.maxFailures.toString());
    }

    if (options.retries !== undefined) {
      args.push('--retries', options.retries.toString());
    }

    if (options.timeout !== undefined) {
      args.push('--timeout', options.timeout.toString());
    }

    if (options.updateSnapshots) {
      args.push('--update-snapshots');
    }

    if (options.trace) {
      args.push('--trace', options.trace);
    }

    return args;
  }

  async runUserFlowTests(): Promise<void> {
    console.log('👤 Running User Flow Tests...');
    await this.runTests({
      project: ['user-flows'],
      reporter: ['html', 'json', 'junit'],
      trace: 'retain-on-failure'
    });
  }

  async runAdminFlowTests(): Promise<void> {
    console.log('👨‍💼 Running Admin Flow Tests...');
    await this.runTests({
      project: ['admin-flows'],
      reporter: ['html', 'json', 'junit'],
      trace: 'retain-on-failure'
    });
  }

  async runMobileTests(): Promise<void> {
    console.log('📱 Running Mobile Tests...');
    await this.runTests({
      project: ['mobile-chrome', 'mobile-safari'],
      reporter: ['html', 'json'],
      trace: 'on-first-retry'
    });
  }

  async runPerformanceTests(): Promise<void> {
    console.log('⚡ Running Performance Tests...');
    await this.runTests({
      project: ['performance'],
      reporter: ['html', 'json'],
      workers: 1,
      timeout: 60000
    });
  }

  async runRegressionSuite(): Promise<void> {
    console.log('🔍 Running Full Regression Suite...');
    await this.runTests({
      project: ['user-flows', 'admin-flows', 'mobile-chrome'],
      reporter: ['html', 'json', 'junit', 'github'],
      trace: 'retain-on-failure',
      retries: 2,
      workers: 4
    });
  }

  async runSmokeSuite(): Promise<void> {
    console.log('💨 Running Smoke Tests...');
    await this.runTests({
      grep: '@smoke',
      reporter: ['line', 'json'],
      workers: 2,
      maxFailures: 3
    });
  }

  async runCriticalPath(): Promise<void> {
    console.log('🎯 Running Critical Path Tests...');
    await this.runTests({
      grep: '@critical',
      reporter: ['html', 'json', 'junit'],
      trace: 'on',
      retries: 1,
      workers: 1
    });
  }

  async generateCoverageReport(): Promise<void> {
    console.log('📊 Generating Coverage Report...');
    
    const coverageDir = path.join(this.baseDir, 'coverage');
    if (!fs.existsSync(coverageDir)) {
      fs.mkdirSync(coverageDir, { recursive: true });
    }

    // Run tests with coverage collection
    await this.runTests({
      reporter: ['html', 'json'],
      trace: 'on',
      outputDir: coverageDir
    });

    console.log('📈 Coverage report generated in:', coverageDir);
  }

  async runHealthCheck(): Promise<void> {
    console.log('🏥 Running Health Check Tests...');
    
    try {
      await this.runTests({
        grep: 'health|login|basic',
        reporter: ['line'],
        workers: 1,
        timeout: 30000,
        maxFailures: 1
      });
      
      console.log('✅ System health check passed!');
    } catch (error) {
      console.error('❌ System health check failed!');
      throw error;
    }
  }

  async runCI(): Promise<void> {
    console.log('🤖 Running CI Test Suite...');
    
    process.env.CI = 'true';
    
    await this.runTests({
      project: ['user-flows', 'admin-flows'],
      reporter: ['github', 'json', 'junit'],
      trace: 'retain-on-failure',
      retries: 2,
      workers: 2,
      maxFailures: 10
    });
  }

  async installDependencies(): Promise<void> {
    console.log('📦 Installing Playwright browsers...');
    
    return new Promise((resolve, reject) => {
      const process = spawn('npx', ['playwright', 'install', '--with-deps'], {
        cwd: this.baseDir,
        stdio: 'inherit',
        shell: true
      });

      process.on('close', (code) => {
        if (code === 0) {
          console.log('✅ Browsers installed successfully!');
          resolve();
        } else {
          reject(new Error(`Browser installation failed with exit code ${code}`));
        }
      });

      process.on('error', reject);
    });
  }

  async checkHealth(): Promise<boolean> {
    try {
      console.log('🔍 Checking system health...');
      
      // Check if services are running
      const response = await fetch('http://localhost:3000/health', {
        method: 'GET',
        timeout: 5000
      } as any);
      
      if (response.ok) {
        console.log('✅ Frontend service is healthy');
        return true;
      } else {
        console.log('❌ Frontend service is not responding');
        return false;
      }
    } catch (error) {
      console.log('❌ Health check failed:', error);
      return false;
    }
  }

  async showReports(): Promise<void> {
    const reportsDir = path.join(this.baseDir, 'playwright-report');
    
    if (fs.existsSync(reportsDir)) {
      console.log(`📊 Test reports available at: ${reportsDir}`);
      console.log('🌐 Open HTML report: npx playwright show-report');
    } else {
      console.log('❌ No test reports found. Run tests first.');
    }
  }
}

// CLI interface
if (require.main === module) {
  const runner = new TestRunner();
  const command = process.argv[2];

  (async () => {
    try {
      switch (command) {
        case 'install':
          await runner.installDependencies();
          break;
        case 'health':
          const isHealthy = await runner.checkHealth();
          process.exit(isHealthy ? 0 : 1);
          break;
        case 'smoke':
          await runner.runSmokeSuite();
          break;
        case 'critical':
          await runner.runCriticalPath();
          break;
        case 'user':
          await runner.runUserFlowTests();
          break;
        case 'admin':
          await runner.runAdminFlowTests();
          break;
        case 'mobile':
          await runner.runMobileTests();
          break;
        case 'performance':
          await runner.runPerformanceTests();
          break;
        case 'regression':
          await runner.runRegressionSuite();
          break;
        case 'ci':
          await runner.runCI();
          break;
        case 'coverage':
          await runner.generateCoverageReport();
          break;
        case 'reports':
          await runner.showReports();
          break;
        case 'all':
          await runner.runRegressionSuite();
          await runner.generateCoverageReport();
          break;
        default:
          console.log(`
🎭 E2E Test Runner

Usage: npm run test:e2e <command>

Commands:
  install     Install Playwright browsers
  health      Check system health
  smoke       Run smoke tests
  critical    Run critical path tests
  user        Run user flow tests
  admin       Run admin flow tests
  mobile      Run mobile tests
  performance Run performance tests
  regression  Run full regression suite
  ci          Run CI test suite
  coverage    Generate coverage report
  reports     Show test reports
  all         Run everything

Examples:
  npm run test:e2e smoke
  npm run test:e2e user
  npm run test:e2e regression
          `);
          break;
      }
    } catch (error) {
      console.error('❌ Test execution failed:', error);
      process.exit(1);
    }
  })();
}