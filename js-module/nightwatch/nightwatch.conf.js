/**
 * Nightwatch.js Configuration
 * @type {import('nightwatch').NightwatchOptions}
 */
module.exports = {
  src_folders: ['tests'],
  page_objects_path: ['src/pages'],
  custom_commands_path: [],
  custom_assertions_path: [],

  webdriver: {},

  test_workers: {
    enabled: true,
    workers: 'auto',
  },

  test_settings: {
    default: {
      disable_error_log: false,
      launch_url: 'https://www.google.com',

      screenshots: {
        enabled: true,
        path: 'reports/screenshots',
        on_failure: true,
        on_error: true,
      },

      desiredCapabilities: {
        browserName: 'chrome',
      },

      webdriver: {
        start_process: true,
        server_path: '',
      },
    },

    chrome: {
      desiredCapabilities: {
        browserName: 'chrome',
        'goog:chromeOptions': {
          w3c: true,
          args: ['--no-sandbox', '--disable-gpu'],
        },
      },

      webdriver: {
        start_process: true,
        server_path: require('chromedriver').path,
        cli_args: ['--port=9515'],
      },
    },

    'chrome.headless': {
      desiredCapabilities: {
        browserName: 'chrome',
        'goog:chromeOptions': {
          w3c: true,
          args: ['--headless=new', '--no-sandbox', '--disable-gpu', '--window-size=1920,1080'],
        },
      },

      webdriver: {
        start_process: true,
        server_path: require('chromedriver').path,
        cli_args: ['--port=9515'],
      },
    },

    firefox: {
      desiredCapabilities: {
        browserName: 'firefox',
        'moz:firefoxOptions': {
          args: [],
        },
      },

      webdriver: {
        start_process: true,
        server_path: require('geckodriver').path,
        cli_args: ['--port=4444'],
      },
    },

    'firefox.headless': {
      desiredCapabilities: {
        browserName: 'firefox',
        'moz:firefoxOptions': {
          args: ['-headless'],
        },
      },

      webdriver: {
        start_process: true,
        server_path: require('geckodriver').path,
        cli_args: ['--port=4444'],
      },
    },

    selenium: {
      selenium: {
        start_process: true,
        port: 4444,
        server_path: require('selenium-server').path,
        cli_args: {
          'webdriver.gecko.driver': require('geckodriver').path,
          'webdriver.chrome.driver': require('chromedriver').path,
        },
      },

      desiredCapabilities: {
        browserName: 'chrome',
      },
    },

    'selenium.grid': {
      selenium: {
        start_process: false,
        host: 'localhost',
        port: 4444,
      },

      desiredCapabilities: {
        browserName: 'chrome',
      },
    },
  },

  globals: {
    waitForConditionTimeout: 10000,
    retryAssertionTimeout: 5000,
    abortOnAssertionFailure: false,
  },

  output_folder: 'reports',
  custom_commands_path: [],
  custom_assertions_path: [],
  plugins: [],
};
