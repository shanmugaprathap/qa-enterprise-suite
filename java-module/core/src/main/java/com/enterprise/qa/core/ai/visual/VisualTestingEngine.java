package com.enterprise.qa.core.ai.visual;

import com.enterprise.qa.core.config.ConfigManager;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * AI-powered visual testing engine for detecting UI regressions.
 * Performs pixel-level comparison with intelligent difference detection.
 */
@Slf4j
public class VisualTestingEngine {

    private final Path baselinePath;
    private final Path actualPath;
    private final Path diffPath;
    private final double defaultThreshold;

    public VisualTestingEngine() {
        String basePath = ConfigManager.getInstance().get("visual.test.path", "target/visual-tests");
        this.baselinePath = Paths.get(basePath, "baseline");
        this.actualPath = Paths.get(basePath, "actual");
        this.diffPath = Paths.get(basePath, "diff");
        this.defaultThreshold = ConfigManager.getInstance()
                .getInt("visual.test.threshold", 5) / 100.0;

        createDirectories();
    }

    /**
     * Captures a screenshot and compares it against the baseline.
     *
     * @param driver     the WebDriver
     * @param screenshotName unique name for this screenshot
     * @return comparison result
     */
    public VisualComparisonResult compareScreenshot(WebDriver driver, String screenshotName) {
        return compareScreenshot(driver, screenshotName, defaultThreshold);
    }

    /**
     * Captures a screenshot and compares it against the baseline with custom threshold.
     *
     * @param driver         the WebDriver
     * @param screenshotName unique name for this screenshot
     * @param threshold      acceptable difference threshold (0.0 - 1.0)
     * @return comparison result
     */
    public VisualComparisonResult compareScreenshot(WebDriver driver, String screenshotName,
                                                     double threshold) {
        try {
            // Capture current screenshot
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            BufferedImage actualImage = ImageIO.read(new ByteArrayInputStream(screenshotBytes));

            // Save actual screenshot
            Path actualFile = actualPath.resolve(screenshotName + ".png");
            ImageIO.write(actualImage, "PNG", actualFile.toFile());

            // Check for baseline
            Path baselineFile = baselinePath.resolve(screenshotName + ".png");
            if (!Files.exists(baselineFile)) {
                // First run - save as baseline
                Files.copy(actualFile, baselineFile);
                log.info("Created baseline for: {}", screenshotName);
                return VisualComparisonResult.builder()
                        .screenshotName(screenshotName)
                        .passed(true)
                        .isNewBaseline(true)
                        .message("Baseline created")
                        .build();
            }

            // Load baseline
            BufferedImage baselineImage = ImageIO.read(baselineFile.toFile());

            // Perform comparison
            return performComparison(screenshotName, baselineImage, actualImage, threshold);

        } catch (IOException e) {
            log.error("Visual comparison failed for {}: {}", screenshotName, e.getMessage());
            return VisualComparisonResult.builder()
                    .screenshotName(screenshotName)
                    .passed(false)
                    .message("Comparison error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Captures a screenshot of a specific element.
     *
     * @param element        the element to capture
     * @param screenshotName unique name for this screenshot
     * @return comparison result
     */
    public VisualComparisonResult compareElement(WebElement element, String screenshotName) {
        try {
            byte[] screenshotBytes = element.getScreenshotAs(OutputType.BYTES);
            BufferedImage actualImage = ImageIO.read(new ByteArrayInputStream(screenshotBytes));

            Path actualFile = actualPath.resolve(screenshotName + ".png");
            ImageIO.write(actualImage, "PNG", actualFile.toFile());

            Path baselineFile = baselinePath.resolve(screenshotName + ".png");
            if (!Files.exists(baselineFile)) {
                Files.copy(actualFile, baselineFile);
                return VisualComparisonResult.builder()
                        .screenshotName(screenshotName)
                        .passed(true)
                        .isNewBaseline(true)
                        .message("Baseline created")
                        .build();
            }

            BufferedImage baselineImage = ImageIO.read(baselineFile.toFile());
            return performComparison(screenshotName, baselineImage, actualImage, defaultThreshold);

        } catch (IOException e) {
            log.error("Element comparison failed: {}", e.getMessage());
            return VisualComparisonResult.builder()
                    .screenshotName(screenshotName)
                    .passed(false)
                    .message("Comparison error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Updates the baseline with the current screenshot.
     *
     * @param screenshotName the screenshot to update
     */
    public void updateBaseline(String screenshotName) {
        try {
            Path actualFile = actualPath.resolve(screenshotName + ".png");
            Path baselineFile = baselinePath.resolve(screenshotName + ".png");

            if (Files.exists(actualFile)) {
                Files.deleteIfExists(baselineFile);
                Files.copy(actualFile, baselineFile);
                log.info("Updated baseline: {}", screenshotName);
            }
        } catch (IOException e) {
            log.error("Failed to update baseline: {}", e.getMessage());
        }
    }

    /**
     * Performs pixel-level comparison between two images.
     */
    private VisualComparisonResult performComparison(String name, BufferedImage baseline,
                                                      BufferedImage actual, double threshold) {
        // Check dimensions
        if (baseline.getWidth() != actual.getWidth() ||
                baseline.getHeight() != actual.getHeight()) {
            return VisualComparisonResult.builder()
                    .screenshotName(name)
                    .passed(false)
                    .differencePercentage(100.0)
                    .message(String.format("Size mismatch: baseline=%dx%d, actual=%dx%d",
                            baseline.getWidth(), baseline.getHeight(),
                            actual.getWidth(), actual.getHeight()))
                    .build();
        }

        int width = baseline.getWidth();
        int height = baseline.getHeight();
        int totalPixels = width * height;
        int differentPixels = 0;

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        List<DifferenceRegion> regions = new ArrayList<>();

        // Pixel-by-pixel comparison
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int baselinePixel = baseline.getRGB(x, y);
                int actualPixel = actual.getRGB(x, y);

                if (pixelsDiffer(baselinePixel, actualPixel)) {
                    differentPixels++;
                    diffImage.setRGB(x, y, Color.RED.getRGB());
                } else {
                    // Grayscale for matching pixels
                    int gray = (baselinePixel >> 16 & 0xFF) / 3 +
                               (baselinePixel >> 8 & 0xFF) / 3 +
                               (baselinePixel & 0xFF) / 3;
                    diffImage.setRGB(x, y, new Color(gray, gray, gray).getRGB());
                }
            }
        }

        double differencePercentage = (double) differentPixels / totalPixels * 100;
        boolean passed = differencePercentage <= threshold * 100;

        // Save diff image
        try {
            Path diffFile = diffPath.resolve(name + "_diff.png");
            ImageIO.write(diffImage, "PNG", diffFile.toFile());
        } catch (IOException e) {
            log.warn("Could not save diff image: {}", e.getMessage());
        }

        String message = passed ?
                String.format("Visual comparison passed (%.2f%% difference)", differencePercentage) :
                String.format("Visual regression detected (%.2f%% difference, threshold: %.2f%%)",
                        differencePercentage, threshold * 100);

        return VisualComparisonResult.builder()
                .screenshotName(name)
                .passed(passed)
                .differencePercentage(differencePercentage)
                .differentPixelCount(differentPixels)
                .totalPixelCount(totalPixels)
                .threshold(threshold * 100)
                .baselinePath(baselinePath.resolve(name + ".png").toString())
                .actualPath(actualPath.resolve(name + ".png").toString())
                .diffPath(diffPath.resolve(name + "_diff.png").toString())
                .message(message)
                .build();
    }

    /**
     * Determines if two pixels are different with tolerance for anti-aliasing.
     */
    private boolean pixelsDiffer(int pixel1, int pixel2) {
        int tolerance = 10; // Allow slight variation for anti-aliasing

        int r1 = (pixel1 >> 16) & 0xFF;
        int g1 = (pixel1 >> 8) & 0xFF;
        int b1 = pixel1 & 0xFF;

        int r2 = (pixel2 >> 16) & 0xFF;
        int g2 = (pixel2 >> 8) & 0xFF;
        int b2 = pixel2 & 0xFF;

        return Math.abs(r1 - r2) > tolerance ||
               Math.abs(g1 - g2) > tolerance ||
               Math.abs(b1 - b2) > tolerance;
    }

    private void createDirectories() {
        try {
            Files.createDirectories(baselinePath);
            Files.createDirectories(actualPath);
            Files.createDirectories(diffPath);
        } catch (IOException e) {
            log.error("Failed to create visual test directories: {}", e.getMessage());
        }
    }

    @Data
    @Builder
    public static class VisualComparisonResult {
        private String screenshotName;
        private boolean passed;
        private boolean isNewBaseline;
        private double differencePercentage;
        private int differentPixelCount;
        private int totalPixelCount;
        private double threshold;
        private String baselinePath;
        private String actualPath;
        private String diffPath;
        private String message;
        private List<DifferenceRegion> differenceRegions;
    }

    @Data
    @Builder
    public static class DifferenceRegion {
        private int x;
        private int y;
        private int width;
        private int height;
        private String description;
    }
}
