/**
 * Base Page Object for Nightwatch.js
 * Provides common methods and properties for all page objects.
 */

module.exports = {
  /**
   * Common elements used across pages
   */
  elements: {},

  /**
   * Common commands
   */
  commands: [
    {
      /**
       * Waits for the page to fully load.
       * @param {number} timeout - Maximum wait time in ms
       * @returns {this}
       */
      waitForPageLoad(timeout = 10000) {
        return this.waitForElementPresent('body', timeout);
      },

      /**
       * Gets the page title.
       * @param {function} callback - Callback to receive the title
       * @returns {this}
       */
      getPageTitle(callback) {
        return this.api.getTitle(callback);
      },

      /**
       * Gets the current URL.
       * @param {function} callback - Callback to receive the URL
       * @returns {this}
       */
      getCurrentUrl(callback) {
        return this.api.url(callback);
      },

      /**
       * Navigates to a URL.
       * @param {string} url - The URL to navigate to
       * @returns {this}
       */
      navigateTo(url) {
        return this.api.url(url);
      },

      /**
       * Refreshes the current page.
       * @returns {this}
       */
      refreshPage() {
        return this.api.refresh();
      },

      /**
       * Accepts a browser alert.
       * @returns {this}
       */
      acceptAlert() {
        return this.api.acceptAlert();
      },

      /**
       * Dismisses a browser alert.
       * @returns {this}
       */
      dismissAlert() {
        return this.api.dismissAlert();
      },

      /**
       * Gets text from an alert.
       * @param {function} callback - Callback to receive the text
       * @returns {this}
       */
      getAlertText(callback) {
        return this.api.getAlertText(callback);
      },

      /**
       * Scrolls to an element.
       * @param {string} selector - Element selector
       * @returns {this}
       */
      scrollToElement(selector) {
        return this.api.execute(
          function (sel) {
            document.querySelector(sel).scrollIntoView({ behavior: 'smooth', block: 'center' });
          },
          [selector]
        );
      },

      /**
       * Takes a screenshot.
       * @param {string} name - Screenshot name
       * @returns {this}
       */
      takeScreenshot(name) {
        const filename = `screenshots/${name || Date.now()}.png`;
        return this.api.saveScreenshot(filename);
      },

      /**
       * Clears local storage.
       * @returns {this}
       */
      clearLocalStorage() {
        return this.api.execute(function () {
          localStorage.clear();
        });
      },

      /**
       * Clears session storage.
       * @returns {this}
       */
      clearSessionStorage() {
        return this.api.execute(function () {
          sessionStorage.clear();
        });
      },

      /**
       * Sets a value in local storage.
       * @param {string} key - Storage key
       * @param {string} value - Storage value
       * @returns {this}
       */
      setLocalStorageItem(key, value) {
        return this.api.execute(
          function (k, v) {
            localStorage.setItem(k, v);
          },
          [key, value]
        );
      },

      /**
       * Gets a value from local storage.
       * @param {string} key - Storage key
       * @param {function} callback - Callback to receive the value
       * @returns {this}
       */
      getLocalStorageItem(key, callback) {
        return this.api.execute(
          function (k) {
            return localStorage.getItem(k);
          },
          [key],
          callback
        );
      },
    },
  ],
};
