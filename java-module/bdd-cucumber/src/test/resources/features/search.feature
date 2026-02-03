@search @smoke
Feature: Google Search Functionality
  As a user
  I want to search for information on Google
  So that I can find relevant results

  Background:
    Given I am on the Google homepage

  @critical
  Scenario: Perform a basic search
    When I search for "Selenium WebDriver"
    Then I should see search results
    And the page title should contain "Selenium WebDriver"

  @regression
  Scenario: Search shows suggestions
    When I type "playwright" in the search box
    Then I should see search suggestions

  Scenario Outline: Search for different terms
    When I search for "<searchTerm>"
    Then I should see search results
    And the page title should contain "<searchTerm>"

    Examples:
      | searchTerm          |
      | test automation     |
      | cucumber bdd        |
      | rest assured api    |

  @negative
  Scenario: Empty search stays on homepage
    When I submit an empty search
    Then I should remain on the homepage
