package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.MainScreenPage;
import pages.TextScreenPage;
import utils.ExtentReportManager;
import utils.WaitUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Core navigation, lifecycle, gesture, input and stability checks on ApiDemos. */
public class CombinedApiDemosTests extends BaseTest {

    ExtentTest test;
    MainScreenPage mainPage;

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
    }

    // 1. App opens in the right package
    @Test(priority = 1)
    public void testAppLaunchesWithCorrectPackage() {
        String actualPackage = mainPage.getCurrentAppPackage();
        test.log(Status.INFO, "Current package: " + actualPackage);
        Assert.assertEquals(actualPackage, APP_PACKAGE);
    }

    // 2. The main menu has exactly the 12 expected items, in order
    @Test(priority = 2)
    public void testMainScreenExactMenuItemCount() {
        List<String> texts = mainPage.getMenuTexts();
        test.log(Status.INFO, "Main menu: " + texts);
        Assert.assertEquals(texts.size(), 12, "Menu item count changed — possible UI regression");
        Assert.assertEquals(texts, MainScreenPage.MAIN_MENU, "Main menu items or order changed");
    }

    // 3. No menu item has empty text
    @Test(priority = 3)
    public void testAllMenuItemsHaveNonEmptyText() {
        List<String> texts = mainPage.getMenuTexts();
        Assert.assertFalse(texts.isEmpty(), "No menu items found");
        for (int i = 0; i < texts.size(); i++) {
            Assert.assertFalse(texts.get(i).isBlank(), "Menu item at index " + i + " has empty text");
        }
    }

    // 4. Back from a sub-screen returns to the main menu
    @Test(priority = 4)
    public void testBackNavigationReturnsToMainScreen() {
        mainPage.clickMenuItemByText("Graphics");
        Assert.assertTrue(mainPage.waitForText("Arcs"), "Graphics screen did not open");
        Assert.assertFalse(mainPage.isOnMainScreen(), "Still on the main screen after opening Graphics");

        driver.navigate().back();
        Assert.assertTrue(mainPage.waitForMainScreen(), "Back did not return to the main screen");
        Assert.assertEquals(mainPage.getMenuItemCount(), 12);
    }

    // 5. Data-driven: each main entry opens its own screen (checked by an item only that screen has)
    @DataProvider(name = "screenNames")
    public Object[][] getScreenNames() {
        return new Object[][] {
                {"Accessibility", "Accessibility Node Provider"},
                {"Animation", "Bouncing Balls"},
                {"App", "Action Bar"},
                {"Content", "Assets"},
                {"Graphics", "Arcs"}
        };
    }

    @Test(priority = 5, dataProvider = "screenNames")
    public void testNavigateMultipleScreensDataDriven(String screenName, String expectedItem) {
        mainPage.clickMenuItemByText(screenName);
        boolean opened = mainPage.waitForText(expectedItem);
        test.log(Status.INFO, screenName + " → '" + expectedItem + "' visible: " + opened);
        Assert.assertTrue(opened, screenName + " did not open (no '" + expectedItem + "')");
        Assert.assertFalse(mainPage.isOnMainScreen(), screenName + " left us on the main screen");
        driver.navigate().back();
    }

    // 6. Looking up an element that does not exist throws NoSuchElementException
    @Test(priority = 6)
    public void testClickingInvalidMenuItemThrowsException() {
        Assert.assertThrows(NoSuchElementException.class, () -> driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().text(\"NonExistentScreen123\")")).click());
    }

    // 7. UiScrollable finds "Views" and opens it
    @Test(priority = 7)
    public void testScrollToFindViewsMenuItem() {
        mainPage.scrollToAndClick("Views");
        Assert.assertTrue(mainPage.waitForText("Controls"), "Views screen did not open (no 'Controls')");
        Assert.assertFalse(mainPage.isOnMainScreen(), "Still on the main screen after tapping Views");
        driver.navigate().back();
    }

    // 8. After 3 s in the background the app comes back on its main screen
    @Test(priority = 8)
    public void testAppBackgroundAndForegroundResumesCorrectly() {
        driver.runAppInBackground(Duration.ofSeconds(3));
        Assert.assertTrue(mainPage.waitForMainScreen(), "App did not resume on its main screen");
        Assert.assertEquals(mainPage.getMenuItemCount(), 12, "App did not resume correctly");
    }

    // 9. Soft assertions: every check runs, failures are reported together
    @Test(priority = 9)
    public void testSoftAssertionAcrossMultipleMenuItems() {
        SoftAssert softAssert = new SoftAssert();
        for (String item : List.of("Accessibility", "Animation", "Graphics", "Views")) {
            softAssert.assertTrue(mainPage.isMenuItemVisible(item), item + " missing");
        }
        softAssert.assertFalse(mainPage.isMenuItemVisible("NonExistentItemXYZ"), "Unexpected item shown");
        softAssert.assertAll();
    }

    // 10. The launcher activity is the ApiDemos main activity
    @Test(priority = 10)
    public void testCurrentActivityIsMainActivity() {
        String activity = driver.currentActivity();
        test.log(Status.INFO, "Current activity: " + activity);
        Assert.assertTrue(activity.endsWith(".ApiDemos"), "Unexpected activity: " + activity);
    }

    // 11. Explicit wait: the first menu item becomes visible in time and is the expected one
    @Test(priority = 11)
    public void testExplicitWaitForMenuItemVisibility() {
        WebElement firstItem = WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 10);
        Assert.assertTrue(firstItem.isDisplayed(), "Menu item should be visible within timeout");
        Assert.assertEquals(firstItem.getText(), MainScreenPage.MAIN_MENU.get(0));
    }

    // 12. Retry helper finds the element and returns the right one
    @Test(priority = 12)
    public void testCustomRetryMechanismFindsElementReliably() {
        WebElement el = WaitUtils.retryFindElement(driver,
                By.xpath("//android.widget.TextView[@text='Accessibility']"), 5);
        Assert.assertEquals(el.getText(), "Accessibility");
    }

    // 13. Text typed into an EditText is kept exactly
    @Test(priority = 13)
    public void testTextInputFieldAcceptsAndRetainsValue() {
        TextScreenPage textPage = new TextScreenPage(driver).open();
        Assert.assertTrue(textPage.isEditTextPresent(), "EditText field should be present");

        textPage.enterText("AppiumSeniorTest123");
        Assert.assertEquals(textPage.getEnteredText(), "AppiumSeniorTest123", "Entered text was not retained");
    }

    // 14. The soft keyboard opens on focus and hideKeyboard() closes it
    @Test(priority = 14)
    public void testKeyboardShowsAndHidesOnTextField() {
        TextScreenPage textPage = new TextScreenPage(driver).open();
        textPage.field().click();

        WaitUtils.pause(1000);
        Assert.assertTrue(driver.isKeyboardShown(), "Keyboard should show when the field is focused");

        driver.hideKeyboard();
        WaitUtils.pause(500);
        Assert.assertFalse(driver.isKeyboardShown(), "Keyboard should be hidden after hideKeyboard()");
    }

    // 15. Landscape and back to portrait; the app keeps working in both
    @Test(priority = 15)
    public void testDeviceOrientationChangeKeepsAppFunctional() {
        try {
            driver.rotate(ScreenOrientation.LANDSCAPE);
            Assert.assertEquals(driver.getOrientation(), ScreenOrientation.LANDSCAPE);
            Assert.assertTrue(mainPage.waitForText("Access'ibility"), "Main menu lost in landscape");
            Dimension landscape = driver.manage().window().getSize();
            Assert.assertTrue(landscape.getWidth() > landscape.getHeight(), "Window is not landscape: " + landscape);
        } finally {
            driver.rotate(ScreenOrientation.PORTRAIT);
        }
        Assert.assertEquals(driver.getOrientation(), ScreenOrientation.PORTRAIT);
        Assert.assertTrue(mainPage.waitForMainScreen(), "App not back on its main screen in portrait");
        Assert.assertEquals(mainPage.getMenuItemCount(), 12);
    }

    // 16. Device time is readable and close to the real time
    @Test(priority = 16)
    public void testDeviceTimeIsAccessible() {
        String deviceTime = driver.getDeviceTime();
        test.log(Status.INFO, "Device time: " + deviceTime);
        OffsetDateTime parsed = OffsetDateTime.parse(deviceTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        long driftMinutes = Math.abs(Duration.between(parsed, OffsetDateTime.now()).toMinutes());
        Assert.assertTrue(driftMinutes < 10, "Device clock is " + driftMinutes + " min off");
    }

    // 17. terminate + activate returns to a clean main screen
    @Test(priority = 17)
    public void testAppResetReturnsToCleanState() {
        mainPage.clickMenuItemByText("Graphics");
        Assert.assertTrue(mainPage.waitForText("Arcs"), "Graphics screen did not open");

        Assert.assertTrue(driver.terminateApp(APP_PACKAGE), "terminateApp reported the app was not running");
        driver.activateApp(APP_PACKAGE);

        Assert.assertTrue(mainPage.waitForMainScreen(), "App did not return to clean main screen after reset");
        Assert.assertEquals(mainPage.getMenuItemCount(), 12);
    }

    // 18. A sub-screen is still open after background / foreground
    @Test(priority = 18)
    public void testAppRetainsSubScreenStateAfterBackgrounding() {
        mainPage.clickMenuItemByText("Graphics");
        Assert.assertTrue(mainPage.waitForText("Arcs"), "Graphics screen did not open");

        driver.runAppInBackground(Duration.ofSeconds(3));

        Assert.assertTrue(mainPage.waitForText("Arcs"), "Graphics screen was lost after background/foreground");
        Assert.assertFalse(mainPage.isOnMainScreen(), "App fell back to the main screen");
        driver.navigate().back();
    }

    // 19. Every main menu item lies inside the window and has a size
    @Test(priority = 19)
    public void testMenuItemBoundingBoxWithinScreenBounds() {
        Dimension window = driver.manage().window().getSize();
        for (WebElement item : mainPage.getAllMenuItems()) {
            Rectangle r = item.getRect();
            Assert.assertTrue(r.getWidth() > 0 && r.getHeight() > 0, item.getText() + " has no size");
            Assert.assertTrue(r.getX() >= 0 && r.getY() >= 0
                            && r.getX() + r.getWidth() <= window.getWidth()
                            && r.getY() + r.getHeight() <= window.getHeight(),
                    item.getText() + " " + r + " is outside the window " + window);
        }
    }

    // 20. Rapid navigation keeps the same session and ends on the main screen
    @Test(priority = 20)
    public void testSessionRemainsStableAcrossRapidNavigation() {
        String sessionIdBefore = driver.getSessionId().toString();
        for (String screen : List.of("Accessibility", "Animation", "App", "Content")) {
            mainPage.clickMenuItemByText(screen);
            driver.navigate().back();
            Assert.assertTrue(mainPage.waitForMainScreen(), "Not back on main after " + screen);
        }
        Assert.assertEquals(driver.getSessionId().toString(), sessionIdBefore, "Session ID changed");
    }

    // 21. Cold launch to first rendered menu item in under 10 s
    @Test(priority = 21)
    public void testAppLaunchPerformanceWithinThreshold() {
        driver.terminateApp(APP_PACKAGE);
        long start = System.currentTimeMillis();
        driver.activateApp(APP_PACKAGE);
        WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 15);
        long durationMs = System.currentTimeMillis() - start;

        test.log(Status.INFO, "App launch + render time: " + durationMs + " ms");
        Assert.assertTrue(durationMs < 10000, "App launch took " + durationMs + " ms");
    }
}
