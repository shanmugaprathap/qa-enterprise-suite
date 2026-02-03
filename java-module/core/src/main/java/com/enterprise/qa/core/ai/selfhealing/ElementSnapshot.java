package com.enterprise.qa.core.ai.selfhealing;

import lombok.Data;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures and stores element attributes for self-healing purposes.
 * This snapshot is used to find similar elements when the original locator fails.
 */
@Data
public class ElementSnapshot {

    private String tagName;
    private String id;
    private String name;
    private String className;
    private String text;
    private String type;
    private String placeholder;
    private String ariaLabel;
    private String href;
    private String src;
    private String value;
    private int x;
    private int y;
    private int width;
    private int height;
    private Map<String, String> dataAttributes = new HashMap<>();
    private String lastKnownLocator;
    private long capturedAt;

    /**
     * Captures a snapshot of the given element.
     *
     * @param element the element to capture
     * @return the element snapshot
     */
    public static ElementSnapshot capture(WebElement element) {
        ElementSnapshot snapshot = new ElementSnapshot();

        // Basic attributes
        snapshot.setTagName(safeGetTagName(element));
        snapshot.setId(safeGetAttribute(element, "id"));
        snapshot.setName(safeGetAttribute(element, "name"));
        snapshot.setClassName(safeGetAttribute(element, "class"));
        snapshot.setText(safeGetText(element));
        snapshot.setType(safeGetAttribute(element, "type"));
        snapshot.setPlaceholder(safeGetAttribute(element, "placeholder"));
        snapshot.setAriaLabel(safeGetAttribute(element, "aria-label"));
        snapshot.setHref(safeGetAttribute(element, "href"));
        snapshot.setSrc(safeGetAttribute(element, "src"));
        snapshot.setValue(safeGetAttribute(element, "value"));

        // Position and size
        try {
            snapshot.setX(element.getLocation().getX());
            snapshot.setY(element.getLocation().getY());
            snapshot.setWidth(element.getSize().getWidth());
            snapshot.setHeight(element.getSize().getHeight());
        } catch (Exception e) {
            // Position/size may not be available for all elements
        }

        // Capture data-* attributes
        snapshot.setDataAttributes(captureDataAttributes(element));

        // Timestamp
        snapshot.setCapturedAt(System.currentTimeMillis());

        return snapshot;
    }

    /**
     * Captures data-* attributes from an element.
     */
    private static Map<String, String> captureDataAttributes(WebElement element) {
        Map<String, String> dataAttrs = new HashMap<>();

        // Common data attributes to check
        String[] commonDataAttrs = {
                "data-testid", "data-test-id", "data-cy", "data-qa",
                "data-automation", "data-automation-id", "data-id",
                "data-name", "data-value", "data-type", "data-action"
        };

        for (String attr : commonDataAttrs) {
            String value = safeGetAttribute(element, attr);
            if (value != null && !value.isEmpty()) {
                dataAttrs.put(attr, value);
            }
        }

        return dataAttrs;
    }

    /**
     * Safely gets an attribute value.
     */
    private static String safeGetAttribute(WebElement element, String attribute) {
        try {
            return element.getAttribute(attribute);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely gets the tag name.
     */
    private static String safeGetTagName(WebElement element) {
        try {
            return element.getTagName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely gets the text content.
     */
    private static String safeGetText(WebElement element) {
        try {
            String text = element.getText();
            // Limit text length to avoid memory issues
            if (text != null && text.length() > 500) {
                text = text.substring(0, 500);
            }
            return text;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if this snapshot has a data-testid or similar attribute.
     *
     * @return true if a test ID attribute exists
     */
    public boolean hasTestId() {
        return dataAttributes.containsKey("data-testid") ||
               dataAttributes.containsKey("data-test-id") ||
               dataAttributes.containsKey("data-cy") ||
               dataAttributes.containsKey("data-qa");
    }

    /**
     * Gets the test ID if available.
     *
     * @return the test ID or null
     */
    public String getTestId() {
        if (dataAttributes.containsKey("data-testid")) {
            return dataAttributes.get("data-testid");
        }
        if (dataAttributes.containsKey("data-test-id")) {
            return dataAttributes.get("data-test-id");
        }
        if (dataAttributes.containsKey("data-cy")) {
            return dataAttributes.get("data-cy");
        }
        if (dataAttributes.containsKey("data-qa")) {
            return dataAttributes.get("data-qa");
        }
        return null;
    }

    /**
     * Checks if this snapshot represents an interactive element.
     *
     * @return true if the element is interactive
     */
    public boolean isInteractive() {
        if (tagName == null) {
            return false;
        }

        String tag = tagName.toLowerCase();
        return tag.equals("input") || tag.equals("button") || tag.equals("a") ||
               tag.equals("select") || tag.equals("textarea") ||
               (tag.equals("div") && className != null && className.contains("btn"));
    }

    /**
     * Gets a descriptive label for this element.
     *
     * @return a human-readable label
     */
    public String getDescriptiveLabel() {
        // Priority: aria-label > placeholder > text > id > name
        if (ariaLabel != null && !ariaLabel.isEmpty()) {
            return ariaLabel;
        }
        if (placeholder != null && !placeholder.isEmpty()) {
            return placeholder;
        }
        if (text != null && !text.isEmpty() && text.length() <= 50) {
            return text;
        }
        if (id != null && !id.isEmpty()) {
            return id;
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return tagName != null ? tagName : "unknown";
    }

    @Override
    public String toString() {
        return String.format("ElementSnapshot{tag=%s, id=%s, text='%s', pos=(%d,%d), size=(%dx%d)}",
                tagName, id,
                text != null && text.length() > 20 ? text.substring(0, 20) + "..." : text,
                x, y, width, height);
    }
}
