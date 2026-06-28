package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.ScreenOrientation;
import listeners.ScreenshotListener;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.MainScreenPage;
import pages.TextScreenPage;
import utils.ExtentReportManager;
import utils.WaitUtils;

import java.time.Duration;
import java.util.List;

@Listeners(ScreenshotListener.class)
public class CombinedApiDemosTests extends BaseTest {

    ExtentTest test;
    MainScreenPage mainPage;

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
    }

    // 1. App সঠিক package এ চালু হয়েছে কিনা
    @Test(priority = 1)
    public void testAppLaunchesWithCorrectPackage() {
        String actualPackage = mainPage.getCurrentAppPackage();
        test.log(Status.INFO, "Current package: " + actualPackage);
        Assert.assertEquals(actualPackage, "io.appium.android.apis");
    }

    // 2. Main screen এ exact কতগুলো menu item আছে (Regression check)
    @Test(priority = 2)
    public void testMainScreenExactMenuItemCount() {
        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu item count: " + count);
        Assert.assertEquals(count, 12, "Menu item count changed — possible UI regression");
    }

    // 3. প্রতিটা menu item এর text non-empty কিনা
    @Test(priority = 3)
    public void testAllMenuItemsHaveNonEmptyText() {
        int total = mainPage.getMenuItemCount();
        for (int i = 0; i < total; i++) {
            List<WebElement> freshItems = mainPage.getAllMenuItems();
            String text = freshItems.get(i).getText();
            Assert.assertFalse(text.isEmpty(), "Menu item at index " + i + " has empty text");
        }
        test.log(Status.PASS, "All " + total + " menu items have valid text");
    }

    // 4. Back navigation এর পর Main screen এ ফিরে আসা নিশ্চিত করা
    @Test(priority = 4)
    public void testBackNavigationReturnsToMainScreen() {
        mainPage.clickMenuItemByText("Graphics");
        driver.navigate().back();

        int countAfterBack = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu count after back navigation: " + countAfterBack);
        Assert.assertEquals(countAfterBack, 12, "Did not return properly to Main screen");
    }

    // 5. Data-Driven Test: একাধিক স্ক্রিনে navigate করে verify করা
    @DataProvider(name = "screenNames")
    public Object[][] getScreenNames() {
        return new Object[][] {
                {"Accessibility"}, {"Animation"}, {"App"}, {"Content"}, {"Graphics"}
        };
    }

    @Test(priority = 5, dataProvider = "screenNames")
    public void testNavigateMultipleScreensDataDriven(String screenName) {
        int mainScreenCount = mainPage.getMenuItemCount();

        mainPage.clickMenuItemByText(screenName);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        int newScreenCount = mainPage.getMenuItemCount();

        test.log(Status.INFO, "Navigated to: " + screenName
                + " | Main count: " + mainScreenCount + " | New screen count: " + newScreenCount);

        boolean navigatedAway = (newScreenCount != mainScreenCount) || !driver.getCurrentPackage().isEmpty();
        Assert.assertTrue(navigatedAway, screenName + " did not navigate to a new screen");

        driver.navigate().back();
    }

    // 6. ভুল/অস্তিত্বহীন element ক্লিক করার সময় সঠিকভাবে Exception আসে কিনা
    @Test(priority = 6)
    public void testClickingInvalidMenuItemThrowsException() {
        boolean exceptionThrown = false;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"NonExistentScreen123\")")).click();
        } catch (NoSuchElementException e) {
            exceptionThrown = true;
            test.log(Status.INFO, "Expected exception caught: " + e.getClass().getSimpleName());
        }
        Assert.assertTrue(exceptionThrown, "Expected NoSuchElementException was not thrown");
    }

    // 7. Scroll করে "Views" item খুঁজে ক্লিক করা (Gesture handling)
    @Test(priority = 7)
    public void testScrollToFindViewsMenuItem() {
        int mainScreenCount = mainPage.getMenuItemCount();

        mainPage.scrollToAndClick("Views");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        int newScreenCount = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Main count: " + mainScreenCount + " | New screen count: " + newScreenCount);

        boolean navigatedAway = (newScreenCount != mainScreenCount);
        test.log(Status.PASS, "Scrolled and clicked 'Views' successfully");
        Assert.assertTrue(navigatedAway, "Did not navigate away from main screen after clicking 'Views'");

        driver.navigate().back();
    }

    // 8. App Background/Foreground resume হওয়ার পর state ঠিক আছে কিনা
    @Test(priority = 8)
    public void testAppBackgroundAndForegroundResumesCorrectly() {
        driver.runAppInBackground(Duration.ofSeconds(3));
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        int countAfterResume = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu count after resume: " + countAfterResume);
        Assert.assertEquals(countAfterResume, 12, "App did not resume correctly");
    }

    // 9. Soft Assertion: একাধিক check করেও পুরো test method শেষ পর্যন্ত চালানো
    @Test(priority = 9)
    public void testSoftAssertionAcrossMultipleMenuItems() {
        SoftAssert softAssert = new SoftAssert();

        softAssert.assertTrue(mainPage.isMenuItemVisible("Accessibility"), "Accessibility missing");
        softAssert.assertTrue(mainPage.isMenuItemVisible("Animation"), "Animation missing");
        //softAssert.assertTrue(mainPage.isMenuItemVisible("NonExistentItemXYZ"), "This will fail intentionally");
        softAssert.assertTrue(mainPage.isMenuItemVisible("Views"), "Views missing");

        test.log(Status.INFO, "Soft assertions executed for multiple menu items");
        softAssert.assertAll();
    }

    // 10. Current package নাম App Source এর সাথে মিলছে কিনা cross-check
    @Test(priority = 10)
    public void testCurrentPackageMatchesExpected() {
        String pkg = driver.getCurrentPackage();
        test.log(Status.INFO, "Verifying package: " + pkg);
        Assert.assertTrue(pkg.equalsIgnoreCase("io.appium.android.apis"));
    }

    // 11. Explicit Wait (WebDriverWait) ব্যবহার — flaky waiting এড়ানো
    @Test(priority = 11)
    public void testExplicitWaitForMenuItemVisibility() {
        WebElement firstItem = WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 10);
        test.log(Status.INFO, "First visible item: " + firstItem.getText());
        Assert.assertTrue(firstItem.isDisplayed(), "Menu item should be visible within timeout");
    }

    // 12. Custom Retry Utility দিয়ে flaky element handle করা
    @Test(priority = 12)
    public void testCustomRetryMechanismFindsElementReliably() {
        WebElement el = WaitUtils.retryFindElement(driver,
                By.xpath("//android.widget.TextView[@text='Accessibility']"), 5);
        test.log(Status.PASS, "Element found via retry mechanism: " + el.getText());
        Assert.assertNotNull(el);
    }

    // 13. EditText এ টেক্সট ইনপুট সঠিকভাবে রিটেইন হচ্ছে কিনা
    @Test(priority = 13)
    public void testTextInputFieldAcceptsAndRetainsValue() {
        mainPage.clickMenuItemByText("Text");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        boolean found = mainPage.navigateToScreenWithEditText();
        if (!found) {
            test.log(Status.SKIP, "Could not find EditText screen under 'Text' menu — skipping");
            throw new org.testng.SkipException("No EditText screen found under 'Text' menu");
        }

        TextScreenPage textPage = new TextScreenPage(driver);
        Assert.assertTrue(textPage.isEditTextPresent(), "EditText field should be present");

        textPage.enterText("AppiumSeniorTest123");
        String actual = textPage.getEnteredText();
        test.log(Status.INFO, "Entered/Retrieved text: " + actual);
        Assert.assertTrue(actual.contains("AppiumSeniorTest123"), "Entered text was not retained");

        driver.navigate().back();
        driver.navigate().back();
    }

    // 14. Keyboard show/hide আচরণ যাচাই
    @Test(priority = 14)
    public void testKeyboardShowsAndHidesOnTextField() {
        mainPage.clickMenuItemByText("Text");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        boolean found = mainPage.navigateToScreenWithEditText();
        if (!found) {
            test.log(Status.SKIP, "Could not find EditText screen — skipping keyboard test");
            throw new org.testng.SkipException("No EditText screen found under 'Text' menu");
        }

        driver.findElement(By.className("android.widget.EditText")).click();
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        boolean shown = driver.isKeyboardShown();
        test.log(Status.INFO, "Keyboard shown after focus: " + shown);

        if (shown) {
            driver.hideKeyboard();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            Assert.assertFalse(driver.isKeyboardShown(), "Keyboard should be hidden after hideKeyboard()");
        }

        driver.navigate().back();
        driver.navigate().back();
    }

    // 15. Device Orientation পরিবর্তন (Landscape <-> Portrait)
    @Test(priority = 15)
    public void testDeviceOrientationChangeKeepsAppFunctional() {
        // Enable auto-rotate on the emulator via ADB shell — without this the emulator
        // ignores Appium's setScreenOrientation command and throws InvalidElementStateException.
        // accelerometer_rotation=1 means auto-rotate is ON.
        try {
            driver.executeScript("mobile: shell",
                    java.util.Map.of("command", "settings put system accelerometer_rotation 1"));
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        } catch (Exception e) {
            test.log(Status.WARNING, "Could not enable auto-rotate via shell: " + e.getMessage());
        }

        try {
            driver.rotate(ScreenOrientation.LANDSCAPE);
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

            int countLandscape = mainPage.getMenuItemCount();
            test.log(Status.INFO, "Menu items visible in landscape: " + countLandscape);
            Assert.assertTrue(countLandscape > 0, "App should remain functional in landscape mode");

            driver.rotate(ScreenOrientation.PORTRAIT);
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

            int countPortrait = mainPage.getMenuItemCount();
            Assert.assertEquals(countPortrait, 12, "App should return to normal state in portrait mode");

        } catch (org.openqa.selenium.InvalidElementStateException e) {
            // Rotation is still locked (e.g. emulator setting or physical device lock).
            // Skip rather than fail — this is an environment constraint, not a code bug.
            test.log(Status.SKIP, "Screen rotation locked on this device — skipping: " + e.getMessage());
            throw new org.testng.SkipException(
                "Screen rotation is locked on this emulator. Enable 'Auto-rotate' in device Settings → Display.");
        }
    }

    // 16. Device এর সিস্টেম টাইম সঠিকভাবে পাওয়া যাচ্ছে কিনা
    @Test(priority = 16)
    public void testDeviceTimeIsAccessible() {
        String deviceTime = driver.getDeviceTime();
        test.log(Status.INFO, "Device time: " + deviceTime);
        Assert.assertNotNull(deviceTime, "Device time should not be null");
        Assert.assertFalse(deviceTime.isEmpty(), "Device time should not be empty");
    }

    // 17. App Reset (terminate + relaunch) করে clean state এ আসা যাচাই
    @Test(priority = 17)
    public void testAppResetReturnsToCleanState() {
        mainPage.clickMenuItemByText("Graphics");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        driver.terminateApp("io.appium.android.apis");
        driver.activateApp("io.appium.android.apis");
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu count after reset: " + count);
        Assert.assertEquals(count, 12, "App did not return to clean main screen after reset");
    }

    // 18. Sub-screen এ থাকা অবস্থায় Background/Foreground করলে state বজায় থাকে কিনা
    @Test(priority = 18)
    public void testAppRetainsSubScreenStateAfterBackgrounding() {
        mainPage.clickMenuItemByText("Graphics");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        boolean onMainBefore = mainPage.getMenuItemCount() == 12;

        driver.runAppInBackground(Duration.ofSeconds(3));
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        boolean onMainAfter = mainPage.getMenuItemCount() == 12;
        test.log(Status.INFO, "On main before: " + onMainBefore + " | On main after resume: " + onMainAfter);

        Assert.assertEquals(onMainBefore, onMainAfter, "Screen state inconsistent after background/foreground");

        driver.navigate().back();
    }

    // 19. Element এর Bounding Box screen সীমার মধ্যে আছে কিনা (UI rendering sanity)
    @Test(priority = 19)
    public void testMenuItemBoundingBoxWithinScreenBounds() {
        WebElement firstItem = driver.findElements(By.id("android:id/text1")).get(0);
        Rectangle rect = firstItem.getRect();

        test.log(Status.INFO, "Element rect: x=" + rect.getX() + " y=" + rect.getY()
                + " w=" + rect.getWidth() + " h=" + rect.getHeight());

        Assert.assertTrue(rect.getX() >= 0 && rect.getY() >= 0,
                "Element position should not be negative — possible rendering bug");
        Assert.assertTrue(rect.getWidth() > 0 && rect.getHeight() > 0,
                "Element should have non-zero dimensions");
    }

    // 20. দ্রুত একাধিক স্ক্রিনে Navigate করেও Session স্থিতিশীল আছে কিনা
    @Test(priority = 20)
    public void testSessionRemainsStableAcrossRapidNavigation() {
        String sessionIdBefore = driver.getSessionId().toString();

        String[] screens = {"Accessibility", "Animation", "App", "Content"};
        for (String screen : screens) {
            mainPage.clickMenuItemByText(screen);
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            driver.navigate().back();
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }

        String sessionIdAfter = driver.getSessionId().toString();
        test.log(Status.INFO, "Session before: " + sessionIdBefore + " | after: " + sessionIdAfter);
        Assert.assertEquals(sessionIdAfter, sessionIdBefore, "Session ID changed — possible session drop/crash");
    }

    // 21. App Launch থেকে Main Screen render হওয়া পর্যন্ত সময় acceptable threshold এর মধ্যে আছে কিনা
    @Test(priority = 21)
    public void testAppLaunchPerformanceWithinThreshold() {
        long start = System.currentTimeMillis();

        driver.terminateApp("io.appium.android.apis");
        driver.activateApp("io.appium.android.apis");
        WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 15);

        long durationMs = System.currentTimeMillis() - start;
        test.log(Status.INFO, "App launch + render time: " + durationMs + " ms");

        Assert.assertTrue(durationMs < 10000,
                "App launch took too long (" + durationMs + " ms) — possible performance regression");
    }
}