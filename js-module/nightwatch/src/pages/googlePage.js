/**
 * Google Page Object - Nightwatch.js
 * Demonstrates Page Object Model pattern for UI testing
 */

const basePage = require('./basePage');

module.exports = {
  url: 'https://www.google.com',

  elements: {
    // Search elements
    searchBox: {
      selector: 'textarea[name="q"], input[name="q"]',
    },
    searchButton: {
      selector: 'input[name="btnK"], button[name="btnK"]',
    },
    luckyButton: {
      selector: 'input[name="btnI"]',
    },

    // Results elements
    searchResults: {
      selector: '#search, #rso',
    },
    resultLinks: {
      selector: '#search a h3',
    },
    resultStats: {
      selector: '#result-stats',
    },

    // Suggestions
    suggestions: {
      selector: 'ul[role="listbox"], div.sbct',
    },
    suggestionItems: {
      selector: 'li[role="presentation"], div.sbct',
    },

    // Navigation
    imagesLink: {
      selector: 'a[href*="imghp"]',
    },
    mapsLink: {
      selector: 'a[href*="maps"]',
    },
    newsLink: {
      selector: 'a[href*="news"]',
    },

    // Cookie consent (may appear in some regions)
    cookieAcceptButton: {
      selector: '#L2AGLb, button[id*="agree"]',
    },
  },

  commands: [
    {
      /**
       * Accept cookie consent if present
       * @returns {object} this for chaining
       */
      acceptCookiesIfPresent() {
        return this.api.element('css selector', '#L2AGLb', result => {
          if (result.status === 0) {
            this.click('@cookieAcceptButton');
          }
        });
      },

      /**
       * Perform a search query
       * @param {string} query - Search term
       * @returns {object} this for chaining
       */
      search(query) {
        return this.waitForElementVisible('@searchBox')
          .clearValue('@searchBox')
          .setValue('@searchBox', query)
          .sendKeys('@searchBox', this.api.Keys.ENTER);
      },

      /**
       * Get search results count
       * @param {function} callback - Callback with count
       * @returns {object} this for chaining
       */
      getResultsCount(callback) {
        return this.getText('@resultStats', result => {
          const match = result.value.match(/[\d,]+/);
          const count = match ? parseInt(match[0].replace(/,/g, '')) : 0;
          callback(count);
        });
      },

      /**
       * Click on a search result by index
       * @param {number} index - Result index (0-based)
       * @returns {object} this for chaining
       */
      clickResult(index) {
        const selector = `#search a h3:nth-of-type(${index + 1})`;
        return this.click(selector);
      },

      /**
       * Get all visible result titles
       * @param {function} callback - Callback with titles array
       * @returns {object} this for chaining
       */
      getResultTitles(callback) {
        return this.api.elements('css selector', '#search a h3', results => {
          const titles = [];
          const elements = results.value;

          if (elements.length === 0) {
            callback(titles);
            return;
          }

          let processed = 0;
          elements.forEach((element, index) => {
            this.api.elementIdText(element.ELEMENT || element['element-6066-11e4-a52e-4f735466cecf'], text => {
              titles[index] = text.value;
              processed++;
              if (processed === elements.length) {
                callback(titles.filter(t => t));
              }
            });
          });
        });
      },

      /**
       * Select a suggestion by text
       * @param {string} text - Partial text to match
       * @returns {object} this for chaining
       */
      selectSuggestion(text) {
        const selector = `ul[role="listbox"] li:contains("${text}")`;
        return this.click(selector);
      },

      /**
       * Navigate to Images search
       * @returns {object} this for chaining
       */
      goToImages() {
        return this.click('@imagesLink').waitForElementVisible('@searchBox');
      },

      /**
       * Verify search results contain expected text
       * @param {string} expectedText - Text to find in results
       * @returns {object} this for chaining
       */
      verifyResultsContain(expectedText) {
        return this.assert.containsText('@searchResults', expectedText);
      },

      /**
       * Wait for search results to load
       * @param {number} timeout - Timeout in ms
       * @returns {object} this for chaining
       */
      waitForResults(timeout = 10000) {
        return this.waitForElementVisible('@searchResults', timeout);
      },

      /**
       * Check if no results message is displayed
       * @param {function} callback - Callback with boolean
       * @returns {object} this for chaining
       */
      hasNoResults(callback) {
        return this.api.element('css selector', '.card-section', result => {
          callback(result.status === 0);
        });
      },
    },
  ],
};
