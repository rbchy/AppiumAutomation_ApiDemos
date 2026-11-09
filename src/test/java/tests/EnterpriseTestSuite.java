package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.TextScreenPage;
import utils.ExtentReportManager;
import utils.WaitUtils;

import java.util.List;
import java.util.Map;

/** Checks grouped the way an enterprise regression plan is organised: smoke, lifecycle, navigation, … */
public class EnterpriseTestSuite extends BaseTest {

    ExtentTest test;
    MainScreenPage mainPage;

    @BeforeMethod(alwaysRun = true)
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
    }

    // ---------- SMOKE ----------

    @Test(priority = 1, groups = "smoke")
    public void smoke_AppLaunchesAndMainScreenLoads() {
        Assert.assertTrue(mainPage.isOnMainScreen(), "Main screen failed to load");
        Assert.assertEquals(mainPage.getMenuItemCount(), 12);
    }

    @Test(priority = 2, groups = "smoke")
    public void smoke_BasicNavigationWorks() {
        mainPage.scrollToAndClick("Views");
        Assert.assertTrue(mainPage.waitForText("Controls"), "Views did not open");
        driver.navigate().back();
        Assert.assertTrue(mainPage.waitForMainScreen(), "Back did not return to the main screen");
    }

    // ---------- LIFECYCLE ----------

    @Test(priority = 3, groups = "lifecycle")
    public void lifecycle_AppKillAndRelaunchRecoversCleanly() {
        Assert.assertTrue(driver.terminateApp(APP_PACKAGE), "App was not running before kill");
        Assert.assertNotEquals(driver.getCurrentPackage(), APP_PACKAGE, "App still in front after kill");
        driver.activateApp(APP_PACKAGE);
        Assert.assertTrue(mainPage.waitForMainScreen(), "App did not recover cleanly after kill and relaunch");
    }

    @Test(priority = 4, groups = "lifecycle")
    public void lifecycle_RotationDoesNotCrashApp() {
        try {
            driver.rotate(ScreenOrientation.LANDSCAPE);
            Assert.assertEquals(driver.getOrientation(), ScreenOrientation.LANDSCAPE);
            Assert.assertTrue(mainPage.waitForText("Access'ibility"), "App unresponsive in landscape");
            Assert.assertEquals(driver.getCurrentPackage(), APP_PACKAGE, "App left the foreground on rotation");
        } finally {
            driver.rotate(ScreenOrientation.PORTRAIT);
        }
        Assert.assertTrue(mainPage.waitForMainScreen(), "App not usable after rotating back");
    }

    // ---------- NAVIGATION ----------

    /** Three levels deep (Views › Controls › 1. Light Theme), then back one level at a time. */
    @Test(priority = 5, groups = "navigation")
    public void navigation_MultiStepBackButtonNavigatesCorrectly() {
        new TextScreenPage(driver).open();

        driver.navigate().back();
        Assert.assertTrue(mainPage.waitForText("1. Light Theme"), "Back #1 should show the Controls list");
        driver.navigate().back();
        Assert.assertTrue(mainPage.waitForText("Controls"), "Back #2 should show the Views list");
        Assert.assertFalse(mainPage.isOnMainScreen(), "Back #2 went too far");
        driver.navigate().back();
        Assert.assertTrue(mainPage.waitForMainScreen(), "Back #3 should show the main screen");
    }

    @Test(priority = 6, groups = "navigation")
    public void navigation_RapidMenuSwitchingDoesNotBreakApp() {
        for (String menu : List.of("Graphics", "Media", "OS")) {
            mainPage.clickMenuItemByText(menu);
            Assert.assertTrue(new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                    .until(d -> !mainPage.isOnMainScreen()), menu + " did not open");
            driver.navigate().back();
            Assert.assertTrue(mainPage.waitForMainScreen(), "Not back on main after " + menu);
        }
        Assert.assertEquals(mainPage.getMenuItemCount(), 12, "App broke during rapid menu switching");
    }

    // ---------- DEVICE ----------

    @Test(priority = 7, groups = "device")
    public void device_ScreenDimensionsAreValid() {
        Dimension size = driver.manage().window().getSize();
        test.log(Status.INFO, "Screen size: " + size.getWidth() + "x" + size.getHeight());
        Assert.assertTrue(size.getWidth() > 0 && size.getHeight() > size.getWidth(),
                "Expected a portrait window, got " + size);
    }

    @Test(priority = 8, groups = "device")
    @SuppressWarnings("unchecked")
    public void device_PlatformVersionIsAccessible() {
        Map<String, Object> info = (Map<String, Object>) driver.executeScript("mobile: deviceInfo");
        String fromDevice = String.valueOf(info.get("platformVersion"));
        String fromSession = String.valueOf(driver.getCapabilities().getCapability("platformVersion"));
        test.log(Status.INFO, "Device: Android " + fromDevice + " (API " + info.get("apiVersion") + ")");
        Assert.assertTrue(fromDevice.matches("\\d+(\\.\\d+)*"), "Unexpected platform version: " + fromDevice);
        Assert.assertEquals(fromSession, fromDevice, "Session and device disagree on the Android version");
    }

    // ---------- NEGATIVE ----------

    @Test(priority = 10, groups = "negative")
    public void negative_EmptyInputDoesNotCrashApp() {
        TextScreenPage textPage = new TextScreenPage(driver).open();
        textPage.enterText("temporary");
        textPage.clear();

        Assert.assertEquals(textPage.getEnteredText(), "", "Field should be empty after clearing");
        Assert.assertEquals(driver.getCurrentPackage(), APP_PACKAGE, "App left the foreground");
        Assert.assertTrue(textPage.field().isDisplayed(), "Screen no longer usable after empty input");
    }

    @Test(priority = 11, groups = "negative")
    public void negative_InvalidElementInteractionThrowsGracefulException() {
        Assert.assertThrows(NoSuchElementException.class, () -> driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().text(\"ThisScreenDoesNotExist999\")")).click());
        Assert.assertTrue(mainPage.isOnMainScreen(), "Failed lookup should not change the screen");
    }

    // ---------- PERFORMANCE ----------

    @Test(priority = 12, groups = "performance")
    public void performance_ScrollResponseTimeWithinThreshold() {
        long start = System.currentTimeMillis();
        mainPage.scrollToAndClick("Views");
        Assert.assertTrue(mainPage.waitForText("Controls"), "Views did not open");
        long durationMs = System.currentTimeMillis() - start;

        test.log(Status.INFO, "Scroll + open duration: " + durationMs + " ms");
        Assert.assertTrue(durationMs < 5000, "Scroll and open took " + durationMs + " ms");
    }

    @Test(priority = 13, groups = "performance")
    public void performance_ElementWaitTimeWithinThreshold() {
        long start = System.currentTimeMillis();
        WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 10);
        long durationMs = System.currentTimeMillis() - start;
        Assert.assertTrue(durationMs < 3000, "Element took " + durationMs + " ms to become visible");
    }

    // ---------- SECURITY ----------

    @Test(priority = 14, groups = "security")
    public void security_AppStateClearsAfterTermination() {
        new TextScreenPage(driver).open().enterText("secret-value-123");

        driver.terminateApp(APP_PACKAGE);
        driver.activateApp(APP_PACKAGE);

        Assert.assertTrue(mainPage.waitForMainScreen(), "App did not restart on its main screen");
        Assert.assertFalse(driver.getPageSource().contains("secret-value-123"),
                "Typed text survived app termination");
    }

    // ---------- ACCESSIBILITY ----------

    @Test(priority = 15, groups = "accessibility")
    public void accessibility_MenuItemsHaveAccessibilityLabels() {
        List<WebElement> items = mainPage.getAllMenuItems();
        Assert.assertEquals(items.size(), 12);
        for (WebElement item : items) {
            String desc = item.getAttribute("content-desc");
            String text = item.getText();
            Assert.assertTrue((desc != null && !desc.isBlank()) || (text != null && !text.isBlank()),
                    "Menu item without an accessible label");
        }
    }

    /** Android's guideline: touch targets at least 48 dp high. */
    @Test(priority = 16, groups = "accessibility")
    public void accessibility_TapTargetsAreReasonablySized() {
        long dpi = ((Number) driver.executeScript("mobile: getDisplayDensity")).longValue();
        int minPx = (int) Math.floor(48 * dpi / 160.0) - 1;
        test.log(Status.INFO, "Display density " + dpi + " dpi → 48 dp = " + (minPx + 1) + " px");
        for (WebElement item : mainPage.getAllMenuItems()) {
            int h = item.getRect().getHeight();
            Assert.assertTrue(h >= minPx, item.getText() + " is " + h + " px high, below 48 dp (" + minPx + " px)");
        }
    }
}
