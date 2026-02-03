package com.enterprise.qa.core.accessibility;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.*;

/**
 * Accessibility testing utilities for WCAG 2.1 compliance validation.
 * Performs automated accessibility checks on web pages.
 */
@Slf4j
public class AccessibilityTestingUtils {

    /**
     * Runs a comprehensive accessibility audit on the current page.
     */
    public static AccessibilityAuditResult runAudit(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        // Run all checks
        violations.addAll(checkImagesForAltText(driver));
        violations.addAll(checkFormLabels(driver));
        violations.addAll(checkHeadingStructure(driver));
        violations.addAll(checkLinkText(driver));
        violations.addAll(checkColorContrast(driver));
        violations.addAll(checkKeyboardAccessibility(driver));
        violations.addAll(checkAriaAttributes(driver));
        violations.addAll(checkLanguageAttribute(driver));
        violations.addAll(checkFocusIndicators(driver));
        violations.addAll(checkTabIndex(driver));

        // Categorize by severity
        long critical = violations.stream().filter(v -> "CRITICAL".equals(v.getSeverity())).count();
        long serious = violations.stream().filter(v -> "SERIOUS".equals(v.getSeverity())).count();
        long moderate = violations.stream().filter(v -> "MODERATE".equals(v.getSeverity())).count();
        long minor = violations.stream().filter(v -> "MINOR".equals(v.getSeverity())).count();

        return AccessibilityAuditResult.builder()
                .pageUrl(driver.getCurrentUrl())
                .pageTitle(driver.getTitle())
                .totalViolations(violations.size())
                .criticalCount((int) critical)
                .seriousCount((int) serious)
                .moderateCount((int) moderate)
                .minorCount((int) minor)
                .passed(critical == 0 && serious == 0)
                .violations(violations)
                .wcagLevel(critical == 0 && serious == 0 ? "A" : "FAIL")
                .build();
    }

    /**
     * Checks all images for alt text.
     * WCAG 1.1.1: Non-text Content (Level A)
     */
    public static List<AccessibilityViolation> checkImagesForAltText(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        List<WebElement> images = driver.findElements(By.tagName("img"));
        for (WebElement img : images) {
            String alt = img.getAttribute("alt");
            String src = img.getAttribute("src");

            if (alt == null) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 1.1.1")
                        .description("Image missing alt attribute")
                        .severity("CRITICAL")
                        .element(String.format("<img src='%s'>", truncate(src, 50)))
                        .recommendation("Add alt attribute to image")
                        .wcagCriteria("1.1.1 Non-text Content")
                        .build());
            } else if (alt.isEmpty() && !isDecorativeImage(img)) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 1.1.1")
                        .description("Image has empty alt text but may not be decorative")
                        .severity("MODERATE")
                        .element(String.format("<img src='%s' alt=''>", truncate(src, 50)))
                        .recommendation("Provide meaningful alt text or mark as decorative with role='presentation'")
                        .wcagCriteria("1.1.1 Non-text Content")
                        .build());
            }
        }

        return violations;
    }

    /**
     * Checks form inputs for associated labels.
     * WCAG 1.3.1: Info and Relationships (Level A)
     */
    public static List<AccessibilityViolation> checkFormLabels(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        List<WebElement> inputs = driver.findElements(
                By.cssSelector("input:not([type='hidden']):not([type='submit']):not([type='button']), " +
                        "select, textarea"));

        for (WebElement input : inputs) {
            String id = input.getAttribute("id");
            String ariaLabel = input.getAttribute("aria-label");
            String ariaLabelledBy = input.getAttribute("aria-labelledby");
            String placeholder = input.getAttribute("placeholder");

            boolean hasLabel = false;

            // Check for associated label
            if (id != null && !id.isEmpty()) {
                List<WebElement> labels = driver.findElements(By.cssSelector("label[for='" + id + "']"));
                hasLabel = !labels.isEmpty();
            }

            // Check for wrapping label
            if (!hasLabel) {
                try {
                    WebElement parent = input.findElement(By.xpath("./ancestor::label"));
                    hasLabel = parent != null;
                } catch (Exception e) {
                    // No wrapping label
                }
            }

            // Check for ARIA labels
            hasLabel = hasLabel || (ariaLabel != null && !ariaLabel.isEmpty()) ||
                       (ariaLabelledBy != null && !ariaLabelledBy.isEmpty());

            if (!hasLabel) {
                String severity = (placeholder != null && !placeholder.isEmpty()) ? "MODERATE" : "SERIOUS";
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 1.3.1")
                        .description("Form input without accessible label")
                        .severity(severity)
                        .element(getElementDescription(input))
                        .recommendation("Add a <label> element, aria-label, or aria-labelledby attribute")
                        .wcagCriteria("1.3.1 Info and Relationships")
                        .build());
            }
        }

        return violations;
    }

    /**
     * Checks heading structure for proper hierarchy.
     * WCAG 1.3.1: Info and Relationships (Level A)
     */
    public static List<AccessibilityViolation> checkHeadingStructure(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        List<WebElement> headings = driver.findElements(By.cssSelector("h1, h2, h3, h4, h5, h6"));

        if (headings.isEmpty()) {
            violations.add(AccessibilityViolation.builder()
                    .rule("WCAG 1.3.1")
                    .description("Page has no headings")
                    .severity("MODERATE")
                    .recommendation("Add heading elements to provide document structure")
                    .wcagCriteria("1.3.1 Info and Relationships")
                    .build());
            return violations;
        }

        // Check for h1
        long h1Count = headings.stream()
                .filter(h -> "h1".equalsIgnoreCase(h.getTagName()))
                .count();

        if (h1Count == 0) {
            violations.add(AccessibilityViolation.builder()
                    .rule("WCAG 1.3.1")
                    .description("Page missing h1 heading")
                    .severity("SERIOUS")
                    .recommendation("Add an h1 element as the main page heading")
                    .wcagCriteria("1.3.1 Info and Relationships")
                    .build());
        } else if (h1Count > 1) {
            violations.add(AccessibilityViolation.builder()
                    .rule("WCAG 1.3.1")
                    .description("Multiple h1 headings found")
                    .severity("MODERATE")
                    .recommendation("Use only one h1 per page")
                    .wcagCriteria("1.3.1 Info and Relationships")
                    .build());
        }

        // Check for skipped heading levels
        int previousLevel = 0;
        for (WebElement heading : headings) {
            int currentLevel = Integer.parseInt(heading.getTagName().substring(1));

            if (previousLevel > 0 && currentLevel > previousLevel + 1) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 1.3.1")
                        .description(String.format("Heading level skipped: h%d to h%d", previousLevel, currentLevel))
                        .severity("MODERATE")
                        .element(String.format("<%s>%s</%s>", heading.getTagName(),
                                truncate(heading.getText(), 30), heading.getTagName()))
                        .recommendation("Do not skip heading levels")
                        .wcagCriteria("1.3.1 Info and Relationships")
                        .build());
            }

            previousLevel = currentLevel;
        }

        return violations;
    }

    /**
     * Checks links for descriptive text.
     * WCAG 2.4.4: Link Purpose (In Context) (Level A)
     */
    public static List<AccessibilityViolation> checkLinkText(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        List<WebElement> links = driver.findElements(By.tagName("a"));
        Set<String> genericPhrases = Set.of("click here", "read more", "learn more", "here", "more", "link");

        for (WebElement link : links) {
            String text = link.getText().trim().toLowerCase();
            String ariaLabel = link.getAttribute("aria-label");
            String title = link.getAttribute("title");

            String accessibleName = !text.isEmpty() ? text :
                    (ariaLabel != null ? ariaLabel : (title != null ? title : ""));

            if (accessibleName.isEmpty()) {
                // Check for image with alt
                List<WebElement> images = link.findElements(By.tagName("img"));
                boolean hasImageWithAlt = images.stream()
                        .anyMatch(img -> {
                            String alt = img.getAttribute("alt");
                            return alt != null && !alt.isEmpty();
                        });

                if (!hasImageWithAlt) {
                    violations.add(AccessibilityViolation.builder()
                            .rule("WCAG 2.4.4")
                            .description("Link has no accessible name")
                            .severity("SERIOUS")
                            .element(String.format("<a href='%s'>", truncate(link.getAttribute("href"), 50)))
                            .recommendation("Add visible text, aria-label, or title to the link")
                            .wcagCriteria("2.4.4 Link Purpose (In Context)")
                            .build());
                }
            } else if (genericPhrases.contains(accessibleName)) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 2.4.4")
                        .description("Link text is not descriptive: '" + accessibleName + "'")
                        .severity("MODERATE")
                        .element(String.format("<a href='%s'>%s</a>",
                                truncate(link.getAttribute("href"), 50), accessibleName))
                        .recommendation("Use descriptive link text that indicates the link destination")
                        .wcagCriteria("2.4.4 Link Purpose (In Context)")
                        .build());
            }
        }

        return violations;
    }

    /**
     * Basic color contrast check (simplified - full check requires computed styles).
     * WCAG 1.4.3: Contrast (Minimum) (Level AA)
     */
    public static List<AccessibilityViolation> checkColorContrast(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        // This is a simplified check - full contrast analysis requires
        // extracting computed colors and calculating contrast ratios
        // For comprehensive contrast checking, consider using axe-core

        // Check for text with same color as background (basic check)
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Long elementsWithLowContrast = (Long) js.executeScript(
                    "var count = 0; " +
                    "document.querySelectorAll('*').forEach(function(el) { " +
                    "  var style = window.getComputedStyle(el); " +
                    "  if (style.color === style.backgroundColor) count++; " +
                    "}); " +
                    "return count;"
            );

            if (elementsWithLowContrast > 0) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 1.4.3")
                        .description(elementsWithLowContrast + " elements may have insufficient color contrast")
                        .severity("SERIOUS")
                        .recommendation("Ensure text has at least 4.5:1 contrast ratio with background")
                        .wcagCriteria("1.4.3 Contrast (Minimum)")
                        .build());
            }
        } catch (Exception e) {
            log.debug("Color contrast check failed: {}", e.getMessage());
        }

        return violations;
    }

    /**
     * Checks for keyboard accessibility.
     * WCAG 2.1.1: Keyboard (Level A)
     */
    public static List<AccessibilityViolation> checkKeyboardAccessibility(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        // Check for interactive elements with onclick but no keyboard handler
        List<WebElement> clickableElements = driver.findElements(
                By.cssSelector("[onclick]:not(a):not(button):not(input):not(select):not(textarea)"));

        for (WebElement element : clickableElements) {
            String onKeyDown = element.getAttribute("onkeydown");
            String onKeyPress = element.getAttribute("onkeypress");
            String onKeyUp = element.getAttribute("onkeyup");
            String role = element.getAttribute("role");
            String tabIndex = element.getAttribute("tabindex");

            boolean hasKeyboardHandler = onKeyDown != null || onKeyPress != null || onKeyUp != null;
            boolean hasFocusableRole = "button".equals(role) || "link".equals(role);
            boolean isFocusable = tabIndex != null;

            if (!hasKeyboardHandler && !hasFocusableRole) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 2.1.1")
                        .description("Clickable element not keyboard accessible")
                        .severity("CRITICAL")
                        .element(getElementDescription(element))
                        .recommendation("Add keyboard event handlers or use native interactive elements")
                        .wcagCriteria("2.1.1 Keyboard")
                        .build());
            }
        }

        return violations;
    }

    /**
     * Checks ARIA attributes for validity.
     * WCAG 4.1.2: Name, Role, Value (Level A)
     */
    public static List<AccessibilityViolation> checkAriaAttributes(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        // Check for elements with ARIA roles
        List<WebElement> ariaElements = driver.findElements(By.cssSelector("[role]"));

        Set<String> validRoles = Set.of("alert", "alertdialog", "application", "article", "banner",
                "button", "cell", "checkbox", "columnheader", "combobox", "complementary",
                "contentinfo", "definition", "dialog", "directory", "document", "feed", "figure",
                "form", "grid", "gridcell", "group", "heading", "img", "link", "list", "listbox",
                "listitem", "log", "main", "marquee", "math", "menu", "menubar", "menuitem",
                "menuitemcheckbox", "menuitemradio", "navigation", "none", "note", "option",
                "presentation", "progressbar", "radio", "radiogroup", "region", "row", "rowgroup",
                "rowheader", "scrollbar", "search", "searchbox", "separator", "slider", "spinbutton",
                "status", "switch", "tab", "table", "tablist", "tabpanel", "term", "textbox",
                "timer", "toolbar", "tooltip", "tree", "treegrid", "treeitem");

        for (WebElement element : ariaElements) {
            String role = element.getAttribute("role");
            if (!validRoles.contains(role.toLowerCase())) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 4.1.2")
                        .description("Invalid ARIA role: " + role)
                        .severity("SERIOUS")
                        .element(getElementDescription(element))
                        .recommendation("Use a valid ARIA role")
                        .wcagCriteria("4.1.2 Name, Role, Value")
                        .build());
            }
        }

        return violations;
    }

    /**
     * Checks for language attribute on HTML element.
     * WCAG 3.1.1: Language of Page (Level A)
     */
    public static List<AccessibilityViolation> checkLanguageAttribute(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        try {
            WebElement html = driver.findElement(By.tagName("html"));
            String lang = html.getAttribute("lang");

            if (lang == null || lang.isEmpty()) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 3.1.1")
                        .description("HTML element missing lang attribute")
                        .severity("SERIOUS")
                        .recommendation("Add lang attribute to <html> element (e.g., lang='en')")
                        .wcagCriteria("3.1.1 Language of Page")
                        .build());
            }
        } catch (Exception e) {
            log.debug("Language check failed: {}", e.getMessage());
        }

        return violations;
    }

    /**
     * Checks for visible focus indicators.
     * WCAG 2.4.7: Focus Visible (Level AA)
     */
    public static List<AccessibilityViolation> checkFocusIndicators(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        // Check for CSS that removes focus outlines
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Boolean hasOutlineNone = (Boolean) js.executeScript(
                    "var styles = document.querySelectorAll('style'); " +
                    "var hasOutlineNone = false; " +
                    "styles.forEach(function(style) { " +
                    "  if (style.textContent.includes('outline: none') || " +
                    "      style.textContent.includes('outline:none') || " +
                    "      style.textContent.includes('outline: 0')) { " +
                    "    hasOutlineNone = true; " +
                    "  } " +
                    "}); " +
                    "return hasOutlineNone;"
            );

            if (Boolean.TRUE.equals(hasOutlineNone)) {
                violations.add(AccessibilityViolation.builder()
                        .rule("WCAG 2.4.7")
                        .description("CSS may be removing focus indicators")
                        .severity("SERIOUS")
                        .recommendation("Ensure focus indicators are visible; if removing default outline, provide custom focus styles")
                        .wcagCriteria("2.4.7 Focus Visible")
                        .build());
            }
        } catch (Exception e) {
            log.debug("Focus indicator check failed: {}", e.getMessage());
        }

        return violations;
    }

    /**
     * Checks for positive tabindex values.
     * WCAG 2.4.3: Focus Order (Level A)
     */
    public static List<AccessibilityViolation> checkTabIndex(WebDriver driver) {
        List<AccessibilityViolation> violations = new ArrayList<>();

        List<WebElement> elementsWithTabindex = driver.findElements(By.cssSelector("[tabindex]"));

        for (WebElement element : elementsWithTabindex) {
            String tabIndex = element.getAttribute("tabindex");
            try {
                int value = Integer.parseInt(tabIndex);
                if (value > 0) {
                    violations.add(AccessibilityViolation.builder()
                            .rule("WCAG 2.4.3")
                            .description("Positive tabindex value: " + value)
                            .severity("MODERATE")
                            .element(getElementDescription(element))
                            .recommendation("Avoid positive tabindex values; use tabindex='0' or '-1'")
                            .wcagCriteria("2.4.3 Focus Order")
                            .build());
                }
            } catch (NumberFormatException e) {
                // Invalid tabindex value
            }
        }

        return violations;
    }

    private static boolean isDecorativeImage(WebElement img) {
        String role = img.getAttribute("role");
        return "presentation".equals(role) || "none".equals(role);
    }

    private static String getElementDescription(WebElement element) {
        String tag = element.getTagName();
        String id = element.getAttribute("id");
        String className = element.getAttribute("class");

        StringBuilder desc = new StringBuilder("<" + tag);
        if (id != null && !id.isEmpty()) desc.append(" id='").append(id).append("'");
        if (className != null && !className.isEmpty())
            desc.append(" class='").append(truncate(className, 30)).append("'");
        desc.append(">");

        return desc.toString();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    // Result classes

    @Data
    @Builder
    public static class AccessibilityAuditResult {
        private String pageUrl;
        private String pageTitle;
        private int totalViolations;
        private int criticalCount;
        private int seriousCount;
        private int moderateCount;
        private int minorCount;
        private boolean passed;
        private String wcagLevel;
        private List<AccessibilityViolation> violations;
    }

    @Data
    @Builder
    public static class AccessibilityViolation {
        private String rule;
        private String description;
        private String severity;
        private String element;
        private String recommendation;
        private String wcagCriteria;
    }
}
