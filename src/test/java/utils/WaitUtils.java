package utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class WaitUtils {

    private WaitUtils() {
    }

    public static WebElement waitForVisible(AndroidDriver driver, By locator, int timeoutSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Tries to find the element up to {@code attempts} times, half a second apart. */
    public static WebElement retryFindElement(AndroidDriver driver, By locator, int attempts) {
        RuntimeException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return driver.findElement(locator);
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                last = e;
                pause(500);
            }
        }
        throw last;
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
