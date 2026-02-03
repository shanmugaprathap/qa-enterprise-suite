package com.enterprise.qa.core.ai.selfhealing;

import com.enterprise.qa.core.config.ConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI-powered self-healing locator that automatically recovers from broken selectors.
 * Uses multiple fallback strategies and scoring to find the best alternative element.
 */
@Slf4j
public class SelfHealingLocator {

    private final WebDriver driver;
    private final LocatorScorer scorer;
    private final Map<String, ElementSnapshot> snapshotCache;
    private final boolean enabled;

    public SelfHealingLocator(WebDriver driver) {
        this.driver = driver;
        this.scorer = new LocatorScorer();
        this.snapshotCache = new HashMap<>();
        this.enabled = ConfigManager.getInstance().isSelfHealingEnabled();
    }

    /**
     * Finds an element with self-healing capability.
     *
     * @param primaryLocator the primary locator to try first
     * @param elementName    a descriptive name for the element (used for healing)
     * @return the found element
     * @throws NoSuchElementException if element cannot be found even with healing
     */
    public WebElement findElement(By primaryLocator, String elementName) {
        // Try primary locator first
        try {
            WebElement element = driver.findElement(primaryLocator);
            if (element.isDisplayed()) {
                // Cache snapshot for future healing
                cacheElementSnapshot(elementName, element, primaryLocator);
                log.debug("Found element '{}' with primary locator", elementName);
                return element;
            }
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            log.warn("Primary locator failed for '{}': {}", elementName, primaryLocator);
        }

        // If self-healing is disabled, throw immediately
        if (!enabled) {
            throw new NoSuchElementException("Element not found: " + elementName);
        }

        // Attempt self-healing
        return healAndFind(primaryLocator, elementName);
    }

    /**
     * Finds all elements matching a locator with self-healing capability.
     *
     * @param primaryLocator the primary locator to try
     * @param elementName    a descriptive name for the elements
     * @return list of found elements
     */
    public List<WebElement> findElements(By primaryLocator, String elementName) {
        try {
            List<WebElement> elements = driver.findElements(primaryLocator);
            if (!elements.isEmpty()) {
                return elements;
            }
        } catch (Exception e) {
            log.warn("Primary locator failed for '{}': {}", elementName, e.getMessage());
        }

        if (!enabled) {
            return Collections.emptyList();
        }

        // Try alternative strategies
        return healAndFindMultiple(primaryLocator, elementName);
    }

    /**
     * Attempts to heal a broken locator and find the element.
     *
     * @param originalLocator the original broken locator
     * @param elementName     the element name
     * @return the healed element
     */
    private WebElement healAndFind(By originalLocator, String elementName) {
        log.info("Attempting self-healing for element: {}", elementName);

        // Get cached snapshot if available
        ElementSnapshot snapshot = snapshotCache.get(elementName);

        // Generate alternative locators
        List<By> alternatives = generateAlternativeLocators(originalLocator, snapshot);

        // Try each alternative and score the results
        List<ScoredElement> candidates = new ArrayList<>();

        for (By alternative : alternatives) {
            try {
                List<WebElement> elements = driver.findElements(alternative);
                for (WebElement element : elements) {
                    if (element.isDisplayed()) {
                        double score = scorer.scoreElement(element, snapshot);
                        candidates.add(new ScoredElement(element, alternative, score));
                    }
                }
            } catch (Exception e) {
                log.trace("Alternative locator failed: {}", alternative);
            }
        }

        if (candidates.isEmpty()) {
            log.error("Self-healing failed for element: {}", elementName);
            throw new NoSuchElementException("Could not heal locator for: " + elementName);
        }

        // Sort by score and return the best match
        candidates.sort((a, b) -> Double.compare(b.score, a.score));
        ScoredElement best = candidates.get(0);

        log.info("Self-healed element '{}' with score {} using: {}",
                elementName, best.score, best.locator);

        // Cache the healed snapshot
        cacheElementSnapshot(elementName, best.element, best.locator);

        return best.element;
    }

    /**
     * Attempts to heal and find multiple elements.
     *
     * @param originalLocator the original locator
     * @param elementName     the element name
     * @return list of found elements
     */
    private List<WebElement> healAndFindMultiple(By originalLocator, String elementName) {
        log.info("Attempting self-healing for elements: {}", elementName);

        List<By> alternatives = generateAlternativeLocators(originalLocator, null);

        for (By alternative : alternatives) {
            try {
                List<WebElement> elements = driver.findElements(alternative);
                if (!elements.isEmpty()) {
                    log.info("Self-healed elements '{}' using: {}", elementName, alternative);
                    return elements;
                }
            } catch (Exception e) {
                log.trace("Alternative locator failed: {}", alternative);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Generates alternative locators based on the original and cached snapshot.
     *
     * @param original the original locator
     * @param snapshot the cached element snapshot
     * @return list of alternative locators
     */
    private List<By> generateAlternativeLocators(By original, ElementSnapshot snapshot) {
        List<By> alternatives = new ArrayList<>();

        // Extract locator info from the original
        String locatorString = original.toString();

        // If we have a snapshot, use its attributes
        if (snapshot != null) {
            // Try by ID
            if (snapshot.getId() != null && !snapshot.getId().isEmpty()) {
                alternatives.add(By.id(snapshot.getId()));
            }

            // Try by name
            if (snapshot.getName() != null && !snapshot.getName().isEmpty()) {
                alternatives.add(By.name(snapshot.getName()));
            }

            // Try by class name
            if (snapshot.getClassName() != null && !snapshot.getClassName().isEmpty()) {
                String[] classes = snapshot.getClassName().split("\\s+");
                for (String cls : classes) {
                    if (!cls.isEmpty() && !isGenericClass(cls)) {
                        alternatives.add(By.className(cls));
                    }
                }
            }

            // Try by text content
            if (snapshot.getText() != null && !snapshot.getText().isEmpty()) {
                String text = snapshot.getText().trim();
                if (text.length() <= 50) {
                    alternatives.add(By.xpath("//*[normalize-space(text())='" + escapeXPath(text) + "']"));
                    alternatives.add(By.xpath("//*[contains(text(),'" + escapeXPath(text) + "')]"));
                }
            }

            // Try by data attributes
            for (Map.Entry<String, String> attr : snapshot.getDataAttributes().entrySet()) {
                alternatives.add(By.cssSelector("[" + attr.getKey() + "='" + attr.getValue() + "']"));
            }

            // Try by aria-label
            if (snapshot.getAriaLabel() != null && !snapshot.getAriaLabel().isEmpty()) {
                alternatives.add(By.cssSelector("[aria-label='" + snapshot.getAriaLabel() + "']"));
            }

            // Try by tag name with specific attributes
            String tag = snapshot.getTagName();
            if (tag != null) {
                if (snapshot.getType() != null) {
                    alternatives.add(By.cssSelector(tag + "[type='" + snapshot.getType() + "']"));
                }
                if (snapshot.getPlaceholder() != null) {
                    alternatives.add(By.cssSelector(tag + "[placeholder='" + snapshot.getPlaceholder() + "']"));
                }
            }
        }

        // Generate generic alternatives from original locator
        alternatives.addAll(generateGenericAlternatives(locatorString));

        return alternatives.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Generates generic alternatives from the original locator string.
     */
    private List<By> generateGenericAlternatives(String locatorString) {
        List<By> alternatives = new ArrayList<>();

        // Try to extract value from locator string
        if (locatorString.contains("By.id:")) {
            String id = extractValue(locatorString, "By.id:");
            // Try partial ID match
            alternatives.add(By.cssSelector("[id*='" + id + "']"));
            alternatives.add(By.cssSelector("[id$='" + id + "']"));
        }

        if (locatorString.contains("By.name:")) {
            String name = extractValue(locatorString, "By.name:");
            alternatives.add(By.cssSelector("[name*='" + name + "']"));
        }

        if (locatorString.contains("By.className:")) {
            String className = extractValue(locatorString, "By.className:");
            alternatives.add(By.cssSelector("[class*='" + className + "']"));
        }

        return alternatives;
    }

    /**
     * Caches an element snapshot for future healing attempts.
     */
    private void cacheElementSnapshot(String name, WebElement element, By locator) {
        try {
            ElementSnapshot snapshot = ElementSnapshot.capture(element);
            snapshot.setLastKnownLocator(locator.toString());
            snapshotCache.put(name, snapshot);
        } catch (Exception e) {
            log.debug("Failed to cache element snapshot: {}", e.getMessage());
        }
    }

    /**
     * Extracts a value from a locator string.
     */
    private String extractValue(String locatorString, String prefix) {
        int start = locatorString.indexOf(prefix) + prefix.length();
        return locatorString.substring(start).trim();
    }

    /**
     * Escapes special characters for XPath.
     */
    private String escapeXPath(String text) {
        if (text.contains("'")) {
            return "concat('" + text.replace("'", "',\"'\",'") + "')";
        }
        return text;
    }

    /**
     * Checks if a class name is generic and not useful for locating.
     */
    private boolean isGenericClass(String className) {
        Set<String> genericClasses = Set.of(
                "container", "wrapper", "content", "row", "col", "btn",
                "active", "disabled", "hidden", "visible", "clearfix"
        );
        return genericClasses.contains(className.toLowerCase());
    }

    /**
     * Clears the element snapshot cache.
     */
    public void clearCache() {
        snapshotCache.clear();
    }

    /**
     * Gets the number of cached element snapshots.
     */
    public int getCacheSize() {
        return snapshotCache.size();
    }

    /**
     * Inner class to hold scored element candidates.
     */
    private static class ScoredElement {
        final WebElement element;
        final By locator;
        final double score;

        ScoredElement(WebElement element, By locator, double score) {
            this.element = element;
            this.locator = locator;
            this.score = score;
        }
    }
}
