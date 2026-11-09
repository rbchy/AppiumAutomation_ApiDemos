package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import utils.ExtentReportManager;

import java.time.Duration;

/**
 * Install → launch → reinstall → uninstall → reinstall.
 * The session is opened without an app, so the tests control installation themselves.
 */
public class InstallLifecycleTests {

    private static final String APP_PACKAGE = BaseTest.APP_PACKAGE;
    private static final String APP_PATH = BaseTest.APK.getPath();

    AndroidDriver driver;
    ExtentTest test;

    @BeforeClass
    @Parameters({"deviceName"})
    public void setUp(@Optional("emulator-5554") String deviceName) throws Exception {
        Assert.assertTrue(BaseTest.APK.exists(), "APK not found at " + APP_PATH);
        BaseTest.checkAppiumIsRunning(BaseTest.appiumUrl());
        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(deviceName)
                .setUdid(deviceName)
                .setNewCommandTimeout(Duration.ofSeconds(120));
        driver = new AndroidDriver(BaseTest.appiumUrl(), options);
    }

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
    }

    @Test(priority = 1)
    public void install_FreshInstallSucceeds() {
        if (driver.isAppInstalled(APP_PACKAGE)) {
            driver.removeApp(APP_PACKAGE);
        }
        Assert.assertFalse(driver.isAppInstalled(APP_PACKAGE), "Old install could not be removed");

        driver.installApp(APP_PATH);
        Assert.assertTrue(driver.isAppInstalled(APP_PACKAGE), "Fresh install failed");
    }

    @Test(priority = 2, dependsOnMethods = "install_FreshInstallSucceeds")
    public void install_FreshlyInstalledAppLaunchesSuccessfully() {
        driver.activateApp(APP_PACKAGE);
        Assert.assertTrue(new MainScreenPage(driver).waitForMainScreen(),
                "Freshly installed app did not open its main screen");
    }

    @Test(priority = 3, dependsOnMethods = "install_FreshlyInstalledAppLaunchesSuccessfully")
    public void install_ReinstallOverExistingAppSucceeds() {
        driver.installApp(APP_PATH);
        test.log(Status.INFO, "Reinstalled over the existing app (upgrade path)");
        Assert.assertTrue(driver.isAppInstalled(APP_PACKAGE), "App missing after reinstall/upgrade");
    }

    @Test(priority = 4, dependsOnMethods = "install_ReinstallOverExistingAppSucceeds")
    public void uninstall_AppRemovesSuccessfully() {
        Assert.assertTrue(driver.removeApp(APP_PACKAGE), "removeApp reported failure");
        Assert.assertFalse(driver.isAppInstalled(APP_PACKAGE), "App was not uninstalled");
    }

    @Test(priority = 5, dependsOnMethods = "uninstall_AppRemovesSuccessfully")
    public void install_ReinstallAfterUninstallSucceeds() {
        driver.installApp(APP_PATH);
        Assert.assertTrue(driver.isAppInstalled(APP_PACKAGE), "Could not reinstall after uninstall");
        driver.activateApp(APP_PACKAGE);
        Assert.assertTrue(new MainScreenPage(driver).waitForMainScreen(), "Reinstalled app did not open");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
