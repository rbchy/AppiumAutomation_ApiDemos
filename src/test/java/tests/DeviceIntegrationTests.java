package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.TextScreenPage;
import utils.Adb;
import utils.ExtentReportManager;

import java.time.Duration;
import java.util.Map;

/** The app working with the rest of the device: direct activity launch, notifications, clipboard. */
public class DeviceIntegrationTests extends BaseTest {

    private static final By ALLOW_BUTTON = By.id("com.android.permissioncontroller:id/permission_allow_button");

    ExtentTest test;
    MainScreenPage mainPage;

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
    }

    /** Opens a screen directly by its activity, the way a deep link or a shortcut would. */
    @Test(priority = 1, groups = "deeplink")
    public void deepLink_StartActivityOpensControlsScreen() {
        driver.executeScript("mobile: startActivity",
                Map.of("intent", APP_PACKAGE + "/.view.Controls1"));

        Assert.assertTrue(new TextScreenPage(driver).field().isDisplayed(), "Controls screen did not open");
        Assert.assertTrue(driver.currentActivity().endsWith("Controls1"),
                "Unexpected activity: " + driver.currentActivity());
    }

    /** App › Notification › IncomingMessage posts a notification that shows up in the shade. */
    @Test(priority = 2, groups = "notifications")
    public void notification_AppNotificationAppearsInShade() {
        // Android 13+: legacy apps also need the notification permission
        try {
            Adb.shell(deviceName, "pm grant " + APP_PACKAGE + " android.permission.POST_NOTIFICATIONS");
        } catch (IllegalStateException e) {
            System.out.println("[notification] " + e.getMessage());
        }
        mainPage.clickMenuItemByText("App");
        mainPage.scrollToAndClick("Notification");
        mainPage.clickMenuItemByText("IncomingMessage");
        mainPage.clickMenuItemByText("Show App Notification");
        if (!driver.findElements(ALLOW_BUTTON).isEmpty()) {
            driver.findElement(ALLOW_BUTTON).click();
            mainPage.clickMenuItemByText("Show App Notification");
        }

        driver.openNotifications();
        try {
            boolean shown = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> d.getPageSource().contains("API Demos"));
            System.out.println("[shade] " + driver.getPageSource().replaceAll("(?s).*?text=\"([^\"]*)\"", "$1 | ").replaceAll("(?s)\\|[^|]*$", ""));
            test.log(Status.INFO, "Notification from API Demos visible in the shade: " + shown);
            Assert.assertTrue(shown, "No API Demos notification in the shade");
        } catch (org.openqa.selenium.TimeoutException e) {
            System.out.println("[logcat] " + Adb.shell(deviceName,
                    "(dumpsys package io.appium.android.apis | grep -iE 'targetSdk|POST_NOTIF'; logcat -d -s NotificationService:* | tail -8; dumpsys notification --noredact | grep -A3 'io.appium.android.apis' | head -20) || true"));
            System.out.println("[shade] " + driver.getPageSource().replaceAll("(?s).*?text=\"([^\"]*)\"", "$1 | ").replaceAll("(?s)\\|[^|]*$", ""));
            throw e;
        } finally {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
        }
    }

    /** Text set on the device clipboard can be read back unchanged. */
    @Test(priority = 3, groups = "clipboard")
    public void clipboard_TextRoundTrip() {
        String value = "ApiDemos-clip-" + System.currentTimeMillis();
        driver.setClipboardText(value);
        Assert.assertEquals(driver.getClipboardText(), value);
    }
}
