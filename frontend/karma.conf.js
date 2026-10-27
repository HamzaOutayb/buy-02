const path = require('path');

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-junit-reporter'),
      require('karma-spec-reporter'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {
        random: false
      },
      clearContext: false
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },
    junitReporter: {
      outputDir: path.join(__dirname, 'test-results'),
      outputFile: 'frontend-test-results.xml',
      useBrowserName: false,
      suite: 'buy-frontend',
      classNameFormatter: (_browser, result) => {
        const suiteName = (result.suite || [])
          .filter(Boolean)
          .join('.')
          .replace(/[^A-Za-z0-9_$.-]+/g, '_');

        return suiteName ? `buy-frontend.${suiteName}` : 'buy-frontend';
      }
    },
    specReporter: {
      suppressErrorSummary: false,
      suppressFailed: false,
      suppressPassed: false,
      showSpecTiming: false,
      prefixes: {
        successful: 'PASS: ',
        failed: 'FAIL: ',
        skipped: 'SKIP: '
      }
    },
    reporters: ['spec', 'junit'],
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox']
      }
    },
    browsers: [process.env.CI ? 'ChromeHeadlessNoSandbox' : 'ChromeHeadless'],
    singleRun: true,
    restartOnFileChange: false
  });
};
