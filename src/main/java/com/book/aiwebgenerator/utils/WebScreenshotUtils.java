package com.book.aiwebgenerator.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }

    /**
     * Generate a webpage screenshot
     *
     * @param webUrl webpage URL
     * @return compressed screenshot file path, returns null on failure
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("Web page URL cannot be blank");
            return null;
        }
        try {
            // Create temporary directory
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // Image suffix
            final String IMAGE_SUFFIX = ".png";
            // Original screenshot file path
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // Visit the webpage
            webDriver.get(webUrl);
            // Wait for page to finish loading
            waitForPageLoad(webDriver);
            // Take screenshot
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // Save original image
            saveImage(screenshotBytes, imageSavePath);
            log.info("Original screenshot saved successfully: {}", imageSavePath);
            // Compress image
            final String COMPRESSION_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESSION_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("Compressed image saved successfully: {}", compressedImagePath);
            // Delete original image, keep only compressed image
            FileUtil.del(imageSavePath);

            return compressedImagePath;
        } catch (Exception e) {
            log.error("Web page screenshot failed: {}", webUrl, e);
            return null;
        }
    }

    /**
     * Initialize Chrome browser driver
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // Automatically manage ChromeDriver
            WebDriverManager.chromedriver().setup();
            // Configure Chrome options
            ChromeOptions options = new ChromeOptions();
            // Headless mode
            options.addArguments("--headless");
            // Disable GPU (may help avoid issues in some environments)
            options.addArguments("--disable-gpu");
            // Disable sandbox mode (required in Docker environments)
            options.addArguments("--no-sandbox");
            // Disable /dev/shm usage
            options.addArguments("--disable-dev-shm-usage");
            // Set window size
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // Disable extensions
            options.addArguments("--disable-extensions");
            // Set user agent
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // Create driver
            WebDriver driver = new ChromeDriver(options);
            // Set page load timeout
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // Set implicit wait
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("Failed to initialize Chrome browser", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to initialize Chrome browser");
        }
    }

    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
            log.info("Screenshot saved successfully: {}", imagePath);
        } catch (Exception e) {
            log.error("Failed to save screenshot: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save screenshot");
        }
    }

    private static void compressImage(String originImagePath, String compressedImagePath) {
        final float COMPRESSION_RATIO = 0.8f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_RATIO
            );
        } catch (Exception e) {
            log.error("Failed to compress screenshot: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to compress screenshot");
        }
    }

    private static void waitForPageLoad(WebDriver webDriver) {
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            wait.until(driver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );
            Thread.sleep(2000);
            log.info("Page load complete");
        } catch (InterruptedException e) {
            log.error("Failed to wait for page load");
        }
    }

}