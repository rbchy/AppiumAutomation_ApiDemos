package pages;

import base.BaseTest;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/** The ApiDemos main menu and the generic list screens below it. */
public class MainScreenPage {

    /** The 12 entries of the ApiDemos main menu, in order. */
    public static final List<String> MAIN_MENU = List.of(
            "Access'ibility", "Accessibility", "Animation", "App", "Content", "Graphics",
            "Media", "NFC", "OS", "Preference", "Text", "Views");

    private static final By MENU_ITEMS = By.id("android:id/text1");

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    public MainScreenPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public static By byText(String text) {
        return AppiumBy.androidUIAutomator("new UiSelector().text(\"" + text.replace("\"", "\\\"") + "\")");
    }

    public int getMenuItemCount() {
        return driver.findElements(MENU_ITEMS).size();
    }

    public List<WebElement> getAllMenuItems() {
        return driver.findElements(MENU_ITEMS);
    }

    public List<String> getMenuTexts() {
        return getAllMenuItems().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    public void clickMenuItemByText(String text) {
        wait.until(d -> !d.findElements(byText(text)).isEmpty());
        driver.findElement(byText(text)).click();
    }

    /** Scrolls the current list until the item is on screen, then taps it. */
    public void scrollToAndClick(String text) {
        driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))"
                        + ".scrollIntoView(new UiSelector().text(\"" + text + "\"))")).click();
    }

    public boolean isMenuItemVisible(String text) {
        return !driver.findElements(byText(text)).isEmpty();
    }

    /** Waits until an element with exactly this text is on screen. */
    public boolean waitForText(String text) {
        try {
            wait.until(d -> !d.findElements(byText(text)).isEmpty());
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /** The main screen is the only one that lists "Access'ibility". */
    public boolean isOnMainScreen() {
        return BaseTest.APP_PACKAGE.equals(driver.getCurrentPackage())
                && isMenuItemVisible("Access'ibility") && isMenuItemVisible("Views");
    }

    public boolean waitForMainScreen() {
        try {
            wait.until(d -> isOnMainScreen());
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    public String getCurrentAppPackage() {
        return driver.getCurrentPackage();
    }

    /** Brings the app back to its main menu, whatever state the previous test left it in. */
    public void ensureOnMainScreen(AndroidDriver driver) {
        if (!BaseTest.APP_PACKAGE.equals(driver.getCurrentPackage())) {
            driver.activateApp(BaseTest.APP_PACKAGE);
        }
        for (int i = 0; i < 5 && !isOnMainScreen(); i++) {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
            if (!BaseTest.APP_PACKAGE.equals(driver.getCurrentPackage())) {
                driver.activateApp(BaseTest.APP_PACKAGE);
            }
        }
        if (!waitForMainScreen()) {
            driver.terminateApp(BaseTest.APP_PACKAGE);
            driver.activateApp(BaseTest.APP_PACKAGE);
            if (!waitForMainScreen()) {
                throw new IllegalStateException("Could not get back to the ApiDemos main screen");
            }
        }
    }
}
