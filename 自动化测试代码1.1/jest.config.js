module.exports = {
  testEnvironment: 'node',
  setupFilesAfterEnv: ['<rootDir>/tests/setup.js'],
  testMatch: ['**/tests/**/*.test.js'],
  collectCoverageFrom: [
    'tests/**/*.js',
    'scripts/**/*.js',
    '!tests/setup.js',
    '!tests/test_helper.js',
    '!scripts/setup_database_step_by_step.js',
    '!scripts/clean_database.js',    
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'lcov', 'html'],
  reporters: [
    'default',
    ['jest-html-reporter', {
      pageTitle: '线上购物系统完整测试报告',
      outputPath: './reports/test-report.html',
      includeFailureMsg: true,
      includeConsoleLog: true
    }]
  ],
  testTimeout: 30000,
  verbose: true
};